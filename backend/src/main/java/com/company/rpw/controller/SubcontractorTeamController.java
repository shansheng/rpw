package com.company.rpw.controller;

import com.company.rpw.common.R;
import com.company.rpw.entity.SubcontractorTeam;
import com.company.rpw.service.SubcontractorTeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分包队伍统计控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/subcontractor-team")
@RequiredArgsConstructor
public class SubcontractorTeamController {

    private final SubcontractorTeamService subcontractorTeamService;

    /**
     * 查询列表（支持按项目ID、专业工程筛选）
     * GET /api/v1/subcontractor-team/list
     */
    @GetMapping("/list")
    public R<List<SubcontractorTeam>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String professionalEngineering) {
        log.info("查询分包队伍统计列表, projectId: {}, professionalEngineering: {}", projectId, professionalEngineering);
        return R.ok(subcontractorTeamService.list(projectId, professionalEngineering));
    }

    /**
     * 根据ID查询
     * GET /api/v1/subcontractor-team/{id}
     */
    @GetMapping("/{id}")
    public R<SubcontractorTeam> getById(@PathVariable Long id) {
        log.info("查询分包队伍统计详情, id: {}", id);
        return R.ok(subcontractorTeamService.getById(id));
    }

    /**
     * 新增
     * POST /api/v1/subcontractor-team
     */
    @PostMapping
    public R<Boolean> create(@RequestBody SubcontractorTeam entity) {
        log.info("新增分包队伍统计: {}", entity.getSubcontractName());
        return subcontractorTeamService.create(entity) ? R.ok(true) : R.fail(500, "新增失败");
    }

    /**
     * 修改
     * PUT /api/v1/subcontractor-team/{id}
     */
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody SubcontractorTeam entity) {
        log.info("修改分包队伍统计, id: {}", id);
        return subcontractorTeamService.update(id, entity) ? R.ok(true) : R.fail(500, "修改失败");
    }

    /**
     * 删除
     * DELETE /api/v1/subcontractor-team/{id}
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        log.info("删除分包队伍统计, id: {}", id);
        return subcontractorTeamService.delete(id) ? R.ok(true) : R.fail(500, "删除失败");
    }
}
