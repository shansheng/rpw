package com.company.rpw.controller;

import com.company.rpw.common.PageResult;
import com.company.rpw.common.R;
import com.company.rpw.dto.subcontract.SubcontractChangeCreateReqVO;
import com.company.rpw.entity.ResourcePlanSubcontractChange;
import com.company.rpw.entity.ResourcePlanSubcontractChangeDetail;
import com.company.rpw.service.ResourcePlanSubcontractChangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分包计划变更控制器（主从结构 + p_fbchange 流程驱动）
 */
@RestController
@RequestMapping("/api/v1/resource-plan/subcontract-change")
@RequiredArgsConstructor
public class ResourcePlanSubcontractChangeController {

    private final ResourcePlanSubcontractChangeService service;

    /**
     * 创建分包计划变更并启动 p_fbchange 流程
     * POST /api/v1/resource-plan/subcontract-change/create
     */
    @PostMapping("/create")
    public R<Long> create(@RequestBody SubcontractChangeCreateReqVO req) {
        if (req.getPlanId() == null) {
            return R.fail(400, "请先选择分包计划");
        }
        Long id = service.create(req);
        return R.ok(id);
    }

    /**
     * 取消变更（删除流程实例 + 状态置为已取消）
     * DELETE /api/v1/resource-plan/subcontract-change/cancel
     */
    @DeleteMapping("/cancel")
    public R<Boolean> cancel(@RequestParam Long id,
                             @RequestParam(required = false) String reason) {
        boolean ok = service.cancel(id, reason);
        return ok ? R.ok(true) : R.fail(400, "当前状态不可取消或记录不存在");
    }

    /**
     * 根据ID查询变更详情
     * GET /api/v1/resource-plan/subcontract-change/get?id=
     */
    @GetMapping("/get")
    public R<?> get(@RequestParam Long id) {
        return R.ok(service.getById(id));
    }

    /**
     * 查询变更明细
     * GET /api/v1/resource-plan/subcontract-change/details?changeId=
     */
    @GetMapping("/details")
    public R<?> details(@RequestParam Long changeId) {
        List<ResourcePlanSubcontractChangeDetail> list = service.getDetails(changeId);
        return R.ok(list);
    }

    /**
     * 变更分页查询
     * GET /api/v1/resource-plan/subcontract-change/page
     */
    @GetMapping("/page")
    public R<?> page(@RequestParam(required = false) Integer pageNo,
                     @RequestParam(required = false) Integer pageSize,
                     @RequestParam(required = false) Long projectId,
                     @RequestParam(required = false) String subcontractName,
                     @RequestParam(required = false) String status) {
        PageResult<ResourcePlanSubcontractChange> result =
                service.page(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize,
                        projectId, subcontractName, status);
        return R.ok(result);
    }
}
