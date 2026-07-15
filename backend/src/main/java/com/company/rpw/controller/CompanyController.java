package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.entity.Company;
import com.company.rpw.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公司管理控制器
 */
@RestController
@RequestMapping("/api/v1/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    /**
     * 查询公司列表（支持按orgId筛选）
     * GET /api/v1/company/list
     * @param orgId 所属组织ID（可选）
     * @return 公司列表
     */
    @GetMapping("/list")
    public R<List<Company>> list(@RequestParam(required = false) Long orgId) {

        List<Company> list;
        if (orgId != null) {
            list = companyService.getByOrgId(orgId);
        } else {
            list = companyService.list();
        }

        return R.ok(list);
    }

    /**
     * 根据ID查询公司
     * GET /api/v1/company/{id}
     * @param id 公司ID
     * @return 公司详情
     */
    @GetMapping("/{id}")
    public R<Company> getById(@PathVariable Long id) {
        Company company = companyService.getById(id);
        return R.ok(company);
    }

    /**
     * 新增公司
     * POST /api/v1/company
     * @param company 公司信息
     * @return 是否成功
     */
    @PostMapping
    public R<Boolean> create(@RequestBody Company company) {
        boolean result = companyService.create(company);
        return result ? R.ok(true) : R.fail(500, "新增失败");
    }

    /**
     * 修改公司
     * PUT /api/v1/company/{id}
     * @param id 公司ID
     * @param company 公司信息
     * @return 是否成功
     */
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Company company) {
        boolean result = companyService.updateCompany(id, company);
        return result ? R.ok(true) : R.fail(500, "修改失败");
    }

    /**
     * 删除公司
     * DELETE /api/v1/company/{id}
     * @param id 公司ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        boolean result = companyService.delete(id);
        return result ? R.ok(true) : R.fail(500, "删除失败");
    }
}
