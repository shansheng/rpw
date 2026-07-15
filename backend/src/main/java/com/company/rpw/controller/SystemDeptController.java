package com.company.rpw.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.rpw.common.R;
import com.company.rpw.entity.SysDept;
import com.company.rpw.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统部门 Controller（数据权限部门范围树）
 * 端点对齐 yudao-ui-admin-vben：list / simple-list / get / create / update / delete / delete-list
 */
@RestController
@RequestMapping("/api/v1/system/dept")
@RequiredArgsConstructor
public class SystemDeptController {

    private final SysDeptMapper deptMapper;

    /** 部门列表（扁平，前端按 parentId 成树） */
    @GetMapping("/list")
    public R<List<SysDept>> list() {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysDept::getParentId).orderByAsc(SysDept::getSort);
        return R.ok(deptMapper.selectList(wrapper));
    }

    /** 部门精简列表（数据权限部门范围树） */
    @GetMapping("/simple-list")
    public R<List<SysDept>> simpleList() {
        return R.ok(deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort)));
    }

    /** 部门详情 */
    @GetMapping("/get")
    public R<SysDept> get(@RequestParam Long id) {
        return R.ok(deptMapper.selectById(id));
    }

    /** 新增部门 */
    @PostMapping("/create")
    public R<Long> create(@RequestBody SysDept req) {
        if (req.getName() == null || req.getName().isEmpty()) {
            return R.fail(400, "部门名称不能为空");
        }
        if (req.getParentId() == null) {
            req.setParentId(0L);
        }
        if (req.getSort() == null) {
            req.setSort(0);
        }
        if (req.getStatus() == null) {
            req.setStatus(1);
        }
        deptMapper.insert(req);
        return R.ok(req.getId());
    }

    /** 修改部门（仅覆盖非空字段） */
    @PutMapping("/update")
    public R<Boolean> update(@RequestBody SysDept req) {
        if (req.getId() == null) {
            return R.fail(400, "部门ID不能为空");
        }
        SysDept existing = deptMapper.selectById(req.getId());
        if (existing == null) {
            return R.fail(404, "部门不存在");
        }
        if (req.getName() != null) {
            existing.setName(req.getName());
        }
        if (req.getParentId() != null) {
            existing.setParentId(req.getParentId());
        }
        if (req.getSort() != null) {
            existing.setSort(req.getSort());
        }
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }
        if (req.getLeaderUserId() != null) {
            existing.setLeaderUserId(req.getLeaderUserId());
        }
        if (req.getPhone() != null) {
            existing.setPhone(req.getPhone());
        }
        if (req.getEmail() != null) {
            existing.setEmail(req.getEmail());
        }
        return R.ok(deptMapper.updateById(existing) > 0);
    }

    /** 删除部门（无子部门方可删） */
    @DeleteMapping("/delete")
    public R<Boolean> delete(@RequestParam Long id) {
        long children = deptMapper.countByParentId(id);
        if (children > 0) {
            return R.fail(400, "存在子部门，无法删除");
        }
        return R.ok(deptMapper.deleteById(id) > 0);
    }

    /** 批量删除部门 */
    @DeleteMapping("/delete-list")
    public R<Boolean> deleteList(@RequestParam List<Long> ids) {
        for (Long id : ids) {
            long children = deptMapper.countByParentId(id);
            if (children > 0) {
                return R.fail(400, "部门 " + id + " 存在子部门，无法删除");
            }
            deptMapper.deleteById(id);
        }
        return R.ok(true);
    }
}
