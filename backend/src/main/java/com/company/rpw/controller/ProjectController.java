package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.entity.Project;
import com.company.rpw.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 查询项目列表（支持按companyId、status筛选）
     * GET /api/v1/project/list
     * @param companyId 所属公司ID（可选）
     * @param status 状态（可选）
     * @return 项目列表
     */
    @GetMapping("/list")
    public R<List<Project>> list(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Integer status) {
        log.info("查询项目列表, companyId: {}, status: {}", companyId, status);

        List<Project> list;
        if (companyId != null) {
            list = projectService.getByCompanyId(companyId);
        } else if (status != null) {
            list = projectService.getByStatus(status);
        } else {
            list = projectService.list();
        }

        return R.ok(list);
    }

    /**
     * 根据ID查询项目
     * GET /api/v1/project/{id}
     * @param id 项目ID
     * @return 项目详情
     */
    @GetMapping("/{id}")
    public R<Project> getById(@PathVariable Long id) {
        log.info("查询项目详情, id: {}", id);
        Project project = projectService.getById(id);
        return R.ok(project);
    }

    /**
     * 新增项目
     * POST /api/v1/project
     * @param project 项目信息
     * @return 是否成功
     */
    @PostMapping
    public R<Boolean> create(@RequestBody Project project) {
        log.info("新增项目: {}", project.getProjectName());
        boolean result = projectService.create(project);
        return result ? R.ok(true) : R.fail(500, "新增失败");
    }

    /**
     * 修改项目
     * PUT /api/v1/project/{id}
     * @param id 项目ID
     * @param project 项目信息
     * @return 是否成功
     */
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Project project) {
        log.info("修改项目, id: {}", id);
        boolean result = projectService.updateProject(id, project);
        return result ? R.ok(true) : R.fail(500, "修改失败");
    }

    /**
     * 删除项目
     * DELETE /api/v1/project/{id}
     * @param id 项目ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        log.info("删除项目, id: {}", id);
        boolean result = projectService.delete(id);
        return result ? R.ok(true) : R.fail(500, "删除失败");
    }

    /**
     * 更新项目状态
     * PUT /api/v1/project/{id}/status
     * @param id 项目ID
     * @param status 状态：1进行中 2已完工 3已暂停
     * @return 是否成功
     */
    @PutMapping("/{id}/status")
    public R<Boolean> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        log.info("更新项目状态, id: {}, status: {}", id, status);
        boolean result = projectService.updateStatus(id, status);
        return result ? R.ok(true) : R.fail(500, "状态更新失败");
    }
}
