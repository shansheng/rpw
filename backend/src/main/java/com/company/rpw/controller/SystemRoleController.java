package com.company.rpw.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.rpw.common.R;
import com.company.rpw.entity.SysRole;
import com.company.rpw.mapper.SysRoleMapper;
import com.company.rpw.mapper.SysRoleMenuMapper;
import com.company.rpw.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统角色 Controller
 * 端点对齐 yudao-ui-admin-vben 的 system/role 页面：
 * page / simple-list / get / create / update / delete / delete-list / export-excel
 */
@RestController
@RequestMapping("/api/v1/system/role")
@RequiredArgsConstructor
public class SystemRoleController {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    /** 角色分页列表 */
    @GetMapping("/page")
    public R<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(SysRole::getName, name);
        }
        if (code != null && !code.isEmpty()) {
            wrapper.like(SysRole::getCode, code);
        }
        if (status != null) {
            wrapper.eq(SysRole::getStatus, status);
        }
        wrapper.orderByDesc(SysRole::getId);
        IPage<SysRole> page = new Page<>(pageNo, pageSize);
        page = roleMapper.selectPage(page, wrapper);
        List<Map<String, Object>> list = page.getRecords().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getName());
            m.put("code", r.getCode());
            m.put("sort", r.getSort());
            m.put("status", r.getStatus());
            m.put("type", r.getType());
            m.put("dataScope", r.getDataScope());
            m.put("dataScopeDeptIds", r.getDataScopeDeptIds());
            m.put("remark", r.getRemark());
            m.put("createTime", r.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        return R.ok(result);
    }

    /** 角色精简列表（供下拉选择） */
    @GetMapping("/simple-list")
    public R<List<SysRole>> simpleList() {
        return R.ok(roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSort)));
    }

    /** 角色详情 */
    @GetMapping("/get")
    public R<SysRole> get(@RequestParam Long id) {
        return R.ok(roleMapper.selectById(id));
    }

    /** 新增角色 */
    @PostMapping("/create")
    public R<Long> create(@RequestBody SysRole req) {
        if (req.getName() == null || req.getName().isEmpty()) {
            return R.fail(400, "角色名称不能为空");
        }
        if (req.getCode() == null || req.getCode().isEmpty()) {
            return R.fail(400, "角色标识不能为空");
        }
        if (roleMapper.selectByCode(req.getCode()) != null) {
            return R.fail(400, "角色标识已存在");
        }
        SysRole r = new SysRole();
        r.setName(req.getName());
        r.setCode(req.getCode());
        r.setSort(req.getSort() == null ? 0 : req.getSort());
        r.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        r.setType(req.getType() == null ? 2 : req.getType());
        r.setDataScope(req.getDataScope() == null ? 1 : req.getDataScope());
        r.setDataScopeDeptIds(req.getDataScopeDeptIds());
        r.setRemark(req.getRemark());
        roleMapper.insert(r);
        return R.ok(r.getId());
    }

    /** 修改角色（仅覆盖请求中非空字段，避免误清数据权限等） */
    @PutMapping("/update")
    public R<Boolean> update(@RequestBody SysRole req) {
        if (req.getId() == null) {
            return R.fail(400, "角色ID不能为空");
        }
        SysRole existing = roleMapper.selectById(req.getId());
        if (existing == null) {
            return R.fail(404, "角色不存在");
        }
        if (req.getName() != null) {
            existing.setName(req.getName());
        }
        if (req.getCode() != null && !req.getCode().equals(existing.getCode())) {
            if (roleMapper.selectByCode(req.getCode()) != null) {
                return R.fail(400, "角色标识已存在");
            }
            existing.setCode(req.getCode());
        }
        if (req.getSort() != null) {
            existing.setSort(req.getSort());
        }
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }
        if (req.getType() != null) {
            existing.setType(req.getType());
        }
        if (req.getRemark() != null) {
            existing.setRemark(req.getRemark());
        }
        // 数据权限由 /system/permission/assign-role-data-scope 单独维护，
        // 基础表单不传 dataScope 时此处不覆盖。
        if (req.getDataScope() != null) {
            existing.setDataScope(req.getDataScope());
        }
        if (req.getDataScopeDeptIds() != null) {
            existing.setDataScopeDeptIds(req.getDataScopeDeptIds());
        }
        return R.ok(roleMapper.updateById(existing) > 0);
    }

    /** 删除角色（级联清角色菜单、用户角色） */
    @DeleteMapping("/delete")
    public R<Boolean> delete(@RequestParam Long id) {
        roleMenuMapper.deleteByRoleId(id);
        userRoleMapper.deleteByRoleId(id);
        return R.ok(roleMapper.deleteById(id) > 0);
    }

    /** 批量删除角色 */
    @DeleteMapping("/delete-list")
    public R<Boolean> deleteList(@RequestParam List<Long> ids) {
        for (Long id : ids) {
            roleMenuMapper.deleteByRoleId(id);
            userRoleMapper.deleteByRoleId(id);
            roleMapper.deleteById(id);
        }
        return R.ok(true);
    }

    /** 导出角色（CSV） */
    @GetMapping("/export-excel")
    public void exportExcel(HttpServletResponse response,
                           @RequestParam(required = false) String name,
                           @RequestParam(required = false) String code,
                           @RequestParam(required = false) Integer status) throws Exception {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(SysRole::getName, name);
        }
        if (code != null && !code.isEmpty()) {
            wrapper.like(SysRole::getCode, code);
        }
        if (status != null) {
            wrapper.eq(SysRole::getStatus, status);
        }
        wrapper.orderByDesc(SysRole::getId);
        List<SysRole> rows = roleMapper.selectList(wrapper);

        response.setContentType("text/csv;charset=utf-8");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + URLEncoder.encode("角色.xls", StandardCharsets.UTF_8.name()));
        try (PrintWriter out = response.getWriter()) {
            out.println("角色编号,角色名称,角色标识,显示顺序,状态,类型,备注");
            for (SysRole r : rows) {
                out.println(String.join(",",
                        String.valueOf(r.getId() == null ? "" : r.getId()),
                        csv(r.getName()), csv(r.getCode()),
                        String.valueOf(r.getSort() == null ? 0 : r.getSort()),
                        String.valueOf(r.getStatus() == null ? 1 : r.getStatus()),
                        String.valueOf(r.getType() == null ? 2 : r.getType()),
                        csv(r.getRemark())));
            }
        }
    }

    private String csv(String s) {
        if (s == null) {
            return "";
        }
        return s.contains(",") ? "\"" + s + "\"" : s;
    }
}
