package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.entity.Organization;
import com.company.rpw.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织架构控制器
 */
@RestController
@RequestMapping("/api/v1/organization")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    /**
     * 查询组织列表（支持按orgLevel筛选）
     * GET /api/v1/organization/list
     * @param orgLevel 组织级别（可选）
     * @return 组织列表
     */
    @GetMapping("/list")
    public R<List<Organization>> list(@RequestParam(required = false) Integer orgLevel) {

        List<Organization> list;
        if (orgLevel != null) {
            list = organizationService.getByLevel(orgLevel);
        } else {
            list = organizationService.list();
        }

        return R.ok(list);
    }

    /**
     * 查询树形结构
     * GET /api/v1/organization/tree
     * @return 树形结构
     */
    @GetMapping("/tree")
    public R<List<Organization>> tree() {
        List<Organization> tree = organizationService.getTree();
        return R.ok(tree);
    }

    /**
     * 根据ID查询组织
     * GET /api/v1/organization/{id}
     * @param id 组织ID
     * @return 组织详情
     */
    @GetMapping("/{id}")
    public R<Organization> getById(@PathVariable Long id) {
        Organization organization = organizationService.getById(id);
        return R.ok(organization);
    }

    /**
     * 新增组织
     * POST /api/v1/organization
     * @param organization 组织信息
     * @return 是否成功
     */
    @PostMapping
    public R<Boolean> create(@RequestBody Organization organization) {
        boolean result = organizationService.create(organization);
        return result ? R.ok(true) : R.fail(500, "新增失败");
    }

    /**
     * 修改组织
     * PUT /api/v1/organization/{id}
     * @param id 组织ID
     * @param organization 组织信息
     * @return 是否成功
     */
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Organization organization) {
        boolean result = organizationService.updateOrg(id, organization);
        return result ? R.ok(true) : R.fail(500, "修改失败");
    }

    /**
     * 删除组织（检查是否有子节点）
     * DELETE /api/v1/organization/{id}
     * @param id 组织ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        try {
            boolean result = organizationService.delete(id);
            return result ? R.ok(true) : R.fail(500, "删除失败");
        } catch (RuntimeException e) {
            return R.fail(500, e.getMessage());
        }
    }
}
