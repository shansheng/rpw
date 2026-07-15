package com.company.rpw.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.rpw.entity.Organization;
import com.company.rpw.mapper.OrganizationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 组织架构服务类（统一 局/公司/项目 + 部门 树）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService extends ServiceImpl<OrganizationMapper, Organization> {

    private final OrganizationMapper organizationMapper;

    /**
     * 根据父ID查询子节点
     * @param parentId 父节点ID
     * @return 子节点列表
     */
    public List<Organization> getByParentId(Long parentId) {
        return organizationMapper.selectByParentId(parentId);
    }

    /**
     * 根据级别查询（用于下拉：如项目级 orgLevel=3）
     * @param orgLevel 组织级别
     * @return 组织列表
     */
    public List<Organization> getByLevel(Integer orgLevel) {
        return organizationMapper.selectByLevel(orgLevel);
    }

    /**
     * 查询树形结构（同级排序：部门在前、组织在后，再按 sort、id 升序）
     * @return 树形结构列表
     */
    public List<Organization> getTree() {
        List<Organization> topNodes = lambdaQuery()
                .and(wrapper -> wrapper.isNull(Organization::getParentId)
                        .or()
                        .eq(Organization::getParentId, 0L))
                .eq(Organization::getDeleted, 0)
                .list();
        sortChildren(topNodes);
        for (Organization node : topNodes) {
            buildTree(node);
        }
        return topNodes;
    }

    private void buildTree(Organization node) {
        List<Organization> children = lambdaQuery()
                .eq(Organization::getParentId, node.getId())
                .eq(Organization::getDeleted, 0)
                .list();
        sortChildren(children);
        node.setChildren(children);
        for (Organization child : children) {
            buildTree(child);
        }
    }

    /** 同级排序：部门(2)在前、组织(1)在后；再按 sort、id 升序 */
    private void sortChildren(List<Organization> nodes) {
        nodes.sort(Comparator.comparingInt((Organization o) -> o.getNodeType() != null && o.getNodeType() == 2 ? 0 : 1)
                .thenComparingInt(o -> o.getSort() == null ? 0 : o.getSort())
                .thenComparing(o -> o.getId() == null ? 0L : o.getId()));
    }

    /**
     * 新增组织（自动推导层级并校验父子关系）
     */
    public boolean create(Organization organization) {
        deriveAndValidate(organization, null);
        if (organization.getNodeType() == null) organization.setNodeType(1);
        if (organization.getSort() == null) organization.setSort(0);
        return save(organization);
    }

    /**
     * 修改组织（自动推导层级并校验父子关系）
     */
    public boolean updateOrg(Long id, Organization organization) {
        organization.setId(id);
        deriveAndValidate(organization, id);
        return updateById(organization);
    }

    /**
     * 根据 parentId + nodeType 推导 orgLevel，并校验层级合法性。
     * - 部门节点(nodeType=2)：orgLevel 继承父节点级别，父节点必须是组织节点。
     * - 组织节点(nodeType=1)：顶级为局(level=1)；否则 level = 父.level + 1（≤3）。
     */
    private void deriveAndValidate(Organization organization, Long selfId) {
        Long parentId = organization.getParentId();
        Integer nodeType = organization.getNodeType();
        if (nodeType == null) nodeType = 1;

        if (parentId == null || parentId == 0L) {
            if (nodeType == 2) {
                throw new RuntimeException("部门节点必须挂在某个组织节点下");
            }
            organization.setOrgLevel(1);
            return;
        }

        Organization parent = getById(parentId);
        if (parent == null || (parent.getDeleted() != null && parent.getDeleted() == 1)) {
            throw new RuntimeException("上级组织不存在");
        }
        // 不允许将节点挂在自己或自己的子孙下（移动场景简单防护）
        if (selfId != null && parentId.equals(selfId)) {
            throw new RuntimeException("不能将节点挂到自己下");
        }

        if (nodeType == 2) {
            // 部门：必须挂在组织节点下，继承其父级级别
            if (parent.getNodeType() != null && parent.getNodeType() == 2) {
                throw new RuntimeException("部门下不能再建部门，请挂在局/公司/项目下");
            }
            organization.setOrgLevel(parent.getOrgLevel());
        } else {
            // 组织节点：父必须是组织节点，且层级 +1（≤3）
            if (parent.getNodeType() != null && parent.getNodeType() == 2) {
                throw new RuntimeException("组织节点必须挂在组织节点（局/公司）下，不能挂在部门下");
            }
            int newLevel = (parent.getOrgLevel() == null ? 1 : parent.getOrgLevel()) + 1;
            if (newLevel > 3) {
                throw new RuntimeException("组织层级最深为 局→公司→项目 三级，无法继续下挂");
            }
            organization.setOrgLevel(newLevel);
        }
    }

    /**
     * 删除组织（检查是否有子节点）
     * @param id 组织ID
     * @return 是否成功
     */
    public boolean delete(Long id) {
        long childCount = lambdaQuery()
                .eq(Organization::getParentId, id)
                .eq(Organization::getDeleted, 0)
                .count();

        if (childCount > 0) {
            throw new RuntimeException("该节点下存在子节点，无法删除");
        }
        return removeById(id);
    }
}
