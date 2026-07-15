package com.company.rpw.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.rpw.common.R;
import com.company.rpw.entity.SysMenu;
import com.company.rpw.mapper.SysMenuMapper;
import com.company.rpw.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统菜单 Controller
 * 端点对齐 yudao-ui-admin-vben 的 system/menu 页面：
 * list / simple-list / get / create / update / delete / delete-list
 */
@RestController
@RequestMapping("/api/v1/system/menu")
@RequiredArgsConstructor
public class SystemMenuController {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    /** 菜单列表（扁平，前端按 parentId 自行成树） */
    @GetMapping("/list")
    public R<List<SysMenu>> list() {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysMenu::getParentId).orderByAsc(SysMenu::getSort);
        return R.ok(menuMapper.selectList(wrapper));
    }

    /** 菜单精简列表（选父级、画权限树） */
    @GetMapping("/simple-list")
    public R<List<SysMenu>> simpleList() {
        return R.ok(menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort)));
    }

    /** 菜单详情 */
    @GetMapping("/get")
    public R<SysMenu> get(@RequestParam Long id) {
        return R.ok(menuMapper.selectById(id));
    }

    /** 新增菜单 */
    @PostMapping("/create")
    public R<Long> create(@RequestBody SysMenu req) {
        R<String> v = validate(req, true);
        if (v != null) {
            return R.fail(400, v.getMessage());
        }
        if (req.getParentId() == null) {
            req.setParentId(0L);
        }
        if (req.getStatus() == null) {
            req.setStatus(1);
        }
        if (req.getVisible() == null) {
            req.setVisible(1);
        }
        if (req.getKeepAlive() == null) {
            req.setKeepAlive(0);
        }
        if (req.getAlwaysShow() == null) {
            req.setAlwaysShow(1);
        }
        menuMapper.insert(req);
        return R.ok(req.getId());
    }

    /** 修改菜单（仅覆盖非空字段） */
    @PutMapping("/update")
    public R<Boolean> update(@RequestBody SysMenu req) {
        if (req.getId() == null) {
            return R.fail(400, "菜单ID不能为空");
        }
        SysMenu existing = menuMapper.selectById(req.getId());
        if (existing == null) {
            return R.fail(404, "菜单不存在");
        }
        if (req.getName() != null) {
            existing.setName(req.getName());
        }
        if (req.getPermission() != null) {
            existing.setPermission(req.getPermission());
        }
        if (req.getType() != null) {
            existing.setType(req.getType());
        }
        if (req.getSort() != null) {
            existing.setSort(req.getSort());
        }
        if (req.getParentId() != null) {
            existing.setParentId(req.getParentId());
        }
        if (req.getPath() != null) {
            existing.setPath(req.getPath());
        }
        if (req.getIcon() != null) {
            existing.setIcon(req.getIcon());
        }
        if (req.getComponent() != null) {
            existing.setComponent(req.getComponent());
        }
        if (req.getComponentName() != null) {
            existing.setComponentName(req.getComponentName());
        }
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }
        if (req.getVisible() != null) {
            existing.setVisible(req.getVisible());
        }
        if (req.getKeepAlive() != null) {
            existing.setKeepAlive(req.getKeepAlive());
        }
        if (req.getAlwaysShow() != null) {
            existing.setAlwaysShow(req.getAlwaysShow());
        }
        R<String> v = validate(existing, false);
        if (v != null) {
            return R.fail(400, v.getMessage());
        }
        return R.ok(menuMapper.updateById(existing) > 0);
    }

    /** 删除菜单（无子节点方可删，级联清角色菜单） */
    @DeleteMapping("/delete")
    public R<Boolean> delete(@RequestParam Long id) {
        long children = menuMapper.countByParentId(id);
        if (children > 0) {
            return R.fail(400, "存在子菜单，无法删除");
        }
        roleMenuMapper.deleteByMenuId(id);
        return R.ok(menuMapper.deleteById(id) > 0);
    }

    /** 批量删除菜单 */
    @DeleteMapping("/delete-list")
    public R<Boolean> deleteList(@RequestParam List<Long> ids) {
        for (Long id : ids) {
            long children = menuMapper.countByParentId(id);
            if (children > 0) {
                return R.fail(400, "菜单 " + id + " 存在子菜单，无法删除");
            }
            roleMenuMapper.deleteByMenuId(id);
            menuMapper.deleteById(id);
        }
        return R.ok(true);
    }

    /** 校验目录/菜单/按钮的必填与路径规则 */
    private R<String> validate(SysMenu m, boolean isCreate) {
        if (m.getName() == null || m.getName().isEmpty()) {
            return R.fail(400, "菜单名称不能为空");
        }
        if (m.getType() == null) {
            return R.fail(400, "菜单类型不能为空");
        }
        if (m.getParentId() == null) {
            m.setParentId(0L);
        }
        if (m.getType() == 1 || m.getType() == 2) {
            if (m.getIcon() == null || m.getIcon().isEmpty()) {
                return R.fail(400, "目录/菜单必须设置图标");
            }
            String p = m.getPath();
            if (p == null || p.isEmpty()) {
                return R.fail(400, "目录/菜单必须设置路由地址");
            }
            boolean abs = p.startsWith("/");
            if (m.getParentId() == 0 && !abs) {
                return R.fail(400, "顶级目录/菜单路径必须以 / 开头");
            }
            if (m.getParentId() != 0 && abs) {
                return R.fail(400, "子级目录/菜单路径不能以 / 开头");
            }
        }
        if (m.getType() == 3) {
            if (m.getPermission() == null || m.getPermission().isEmpty()) {
                return R.fail(400, "按钮必须设置权限标识");
            }
        }
        return null;
    }
}
