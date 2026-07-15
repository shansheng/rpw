package com.company.rpw.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 组织数据迁移：将历史上独立的 company / project 表数据并入统一的 organization 组织树，
 * 并将所有依赖模块（resource_plan_*、warning_record）中的 project_id 重映射到新的组织树节点 id。
 *
 * <p>幂等：按「名称 + 级别 + 父节点」判断节点是否已存在，已存在则复用，不重复插入；
 * project_id 重映射为 old->new 的固定映射，重复执行为 no-op。</p>
 *
 * <p>依赖 SchemaInitializer 先建好 organization 的扩展列（node_type / sort / project_code / status / plan_*），
 * 故注入 SchemaInitializer 以强制其 @PostConstruct 先于本类执行。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@Order(2)
public class OrganizationDataInitializer {

    private final DataSource dataSource;
    /** 注入以强制 SchemaInitializer（建列）先执行 */
    private final SchemaInitializer schemaInitializer;

    /** 依赖模块中引用 project_id 的表（需随迁移重映射） */
    private static final String[] DEPENDENT_TABLES = {
            "resource_plan_labor", "resource_plan_material", "resource_plan_equipment",
            "resource_plan_hardware", "resource_plan_office", "resource_plan_safety",
            "resource_plan_circulation", "resource_plan_subcontract", "warning_record",
    };

    @PostConstruct
    public void init() {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Long rootId = ensureRoot(conn);
                migrateCompanies(conn, rootId);
                Map<Long, Long> projectMap = migrateProjects(conn, rootId);
                seedDepartments(conn);
                remapDependentTables(conn, projectMap);
                conn.commit();
                log.info("[OrganizationDataInitializer] 组织树数据迁移完成（rootId={}, 项目映射 {} 条）",
                        rootId, projectMap.size());
            } catch (Exception e) {
                conn.rollback();
                log.error("[OrganizationDataInitializer] 组织数据迁移失败，已回滚", e);
            }
        } catch (Exception e) {
            log.error("[OrganizationDataInitializer] 无法获取数据库连接", e);
        }
    }

    /** 确保存在局级根节点（无则种子「中交第三公路工程局」） */
    private Long ensureRoot(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM organization WHERE org_level=1 AND node_type=1 AND deleted=0 LIMIT 1")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO organization (org_name, org_level, parent_id, node_type, sort, created_at, updated_at, deleted) "
                        + "VALUES (?,1,NULL,1,0,NOW(),NOW(),0)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "中交第三公路工程局");
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getLong(1);
        }
        return null;
    }

    /** company 表 -> organization 的 level2 节点（挂在局根下） */
    private void migrateCompanies(Connection conn, Long rootId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, company_name FROM company WHERE deleted=0")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String name = rs.getString("company_name");
                if (findOrgId(conn,
                        "SELECT id FROM organization WHERE org_level=2 AND node_type=1 AND parent_id=? AND org_name=? AND deleted=0",
                        rootId, name) != null) {
                    continue;
                }
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO organization (org_name, org_level, parent_id, node_type, sort, created_at, updated_at, deleted) "
                                + "VALUES (?,2,?,1,0,NOW(),NOW(),0)")) {
                    ins.setString(1, name);
                    ins.setLong(2, rootId);
                    ins.executeUpdate();
                }
            }
        }
    }

    /** project 表 -> organization 的 level3 节点（挂在对应公司下）；返回 old project.id -> new org.id 映射 */
    private Map<Long, Long> migrateProjects(Connection conn, Long rootId) throws SQLException {
        Map<Long, Long> projectMap = new HashMap<>();

        // company.id -> organization(level2).id
        Map<Long, Long> companyMap = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT c.id, o.id FROM company c "
                        + "JOIN organization o ON o.org_name = c.company_name "
                        + "AND o.org_level=2 AND o.node_type=1 AND o.parent_id=? AND o.deleted=0 "
                        + "WHERE c.deleted=0")) {
            ps.setLong(1, rootId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) companyMap.put(rs.getLong(1), rs.getLong(2));
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, project_code, project_name, company_id, status, start_date, end_date "
                        + "FROM project WHERE deleted=0")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long pid = rs.getLong("id");
                String code = rs.getString("project_code");
                String name = rs.getString("project_name");
                long compId = rs.getLong("company_id");
                Long compOrg = companyMap.getOrDefault(compId, rootId);
                Integer statusInt = mapStatus(rs.getString("status"));
                Date start = rs.getDate("start_date");
                Date end = rs.getDate("end_date");

                Long existing = findOrgId(conn,
                        "SELECT id FROM organization WHERE org_level=3 AND node_type=1 AND parent_id=? AND org_name=? AND deleted=0",
                        compOrg, name);
                Long newId;
                if (existing != null) {
                    newId = existing;
                } else {
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO organization (org_name, org_level, parent_id, node_type, sort, "
                                    + "project_code, status, plan_start_date, plan_end_date, created_at, updated_at, deleted) "
                                    + "VALUES (?,3,?,1,0,?,?,?,?,NOW(),NOW(),0)",
                            Statement.RETURN_GENERATED_KEYS)) {
                        ins.setString(1, name);
                        ins.setLong(2, compOrg);
                        ins.setString(3, code);
                        if (statusInt == null) ins.setNull(4, Types.INTEGER);
                        else ins.setInt(4, statusInt);
                        if (start == null) ins.setNull(5, Types.DATE);
                        else ins.setDate(5, start);
                        if (end == null) ins.setNull(6, Types.DATE);
                        else ins.setDate(6, end);
                        ins.executeUpdate();
                        ResultSet gk = ins.getGeneratedKeys();
                        gk.next();
                        newId = gk.getLong(1);
                    }
                }
                projectMap.put(pid, newId);
            }
        }
        return projectMap;
    }

    /**
     * 为每个组织节点(node_type=1)按 department / section 字段生成部门子节点(node_type=2)。
     * 满足「每一级包含部门、部门在上」：部门节点继承父级 org_level，由 getTree() 排序置顶。
     * 幂等：按 (parent_id, org_name, node_type=2) 去重，重复执行为 no-op。
     */
    private void seedDepartments(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, org_level, department, section FROM organization "
                        + "WHERE node_type=1 AND deleted=0")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long pid = rs.getLong("id");
                int level = rs.getInt("org_level");
                String[] vals = {rs.getString("department"), rs.getString("section")};
                for (String v : vals) {
                    if (v == null || v.trim().isEmpty()) continue;
                    v = v.trim();
                    if (findOrgId(conn,
                            "SELECT id FROM organization WHERE node_type=2 AND parent_id=? AND org_name=? AND deleted=0",
                            pid, v) != null) {
                        continue;
                    }
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO organization (org_name, org_level, parent_id, node_type, sort, created_at, updated_at, deleted) "
                                    + "VALUES (?,?,?,2,0,NOW(),NOW(),0)")) {
                        ins.setString(1, v);
                        ins.setInt(2, level);
                        ins.setLong(3, pid);
                        ins.executeUpdate();
                    }
                }
            }
        }
    }

    /** 将依赖表中的 project_id 由旧 project.id 重映射到新的组织树节点 id */
    private void remapDependentTables(Connection conn, Map<Long, Long> projectMap) throws SQLException {
        if (projectMap.isEmpty()) return;
        for (String table : DEPENDENT_TABLES) {
            for (Map.Entry<Long, Long> e : projectMap.entrySet()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE " + table + " SET project_id=? WHERE project_id=?")) {
                    ps.setLong(1, e.getValue());
                    ps.setLong(2, e.getKey());
                    ps.executeUpdate();
                } catch (SQLException ex) {
                    // 表可能尚未创建（全新库），忽略
                    log.warn("[OrganizationDataInitializer] 重映射表 {} 跳过：{}", table, ex.getMessage());
                }
            }
        }
    }

    private Long findOrgId(Connection conn, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Long) ps.setLong(i + 1, (Long) args[i]);
                else ps.setString(i + 1, (String) args[i]);
            }
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        }
        return null;
    }

    /** project.status(VARCHAR) -> organization.status(INT)：ACTIVE=1进行中 COMPLETED=2已完工 INACTIVE=3已暂停 */
    private Integer mapStatus(String s) {
        if (s == null) return null;
        return switch (s) {
            case "ACTIVE" -> 1;
            case "COMPLETED" -> 2;
            case "INACTIVE" -> 3;
            default -> 1;
        };
    }
}
