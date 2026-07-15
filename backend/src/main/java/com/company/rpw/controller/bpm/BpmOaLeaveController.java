package com.company.rpw.controller.bpm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.rpw.bpm.BpmCurrentUser;
import com.company.rpw.common.PageResult;
import com.company.rpw.common.R;
import com.company.rpw.dto.bpm.BpmOaLeaveCreateReqVO;
import com.company.rpw.entity.SysUser;
import com.company.rpw.entity.bpm.BpmOaLeave;
import com.company.rpw.enums.bpm.BpmProcessInstanceStatusEnum;
import com.company.rpw.mapper.SysUserMapper;
import com.company.rpw.service.bpm.BpmOaLeaveService;
import lombok.RequiredArgsConstructor;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OA 请假控制器（完整业务表单 + 流程驱动）
 *
 * <p>创建请假时：保存业务表单 -> 启动 p_Leave 流程实例 -> 自动完成「提交」节点
 * -> 将「审批」节点（START_USER_SELECT 策略，多实例 coll_userList）指派给发起人选择的审批人
 * -> 回写流程实例ID。取消请假时：删除流程实例并将业务状态置为已取消。</p>
 */
@RestController
@RequestMapping("/api/v1/bpm/oa/leave")
@RequiredArgsConstructor
public class BpmOaLeaveController {

    private static final String PROCESS_KEY = "p_Leave";

    private final BpmOaLeaveService bpmOaLeaveService;
    private final SysUserMapper sysUserMapper;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    /**
     * 创建请假申请并启动流程
     * POST /api/v1/bpm/oa/leave/create
     */
    @PostMapping("/create")
    public R<Long> create(@RequestBody BpmOaLeaveCreateReqVO req) {
        String username = BpmCurrentUser.getUsername();

        // 1. 解析当前登录用户ID
        SysUser current = sysUserMapper.selectByUsername(username);
        Long userId = current == null ? null : current.getId();

        // 2. 保存请假业务表单（状态：审批中）
        BpmOaLeave leave = new BpmOaLeave();
        leave.setUserId(userId);
        leave.setType(req.getType());
        leave.setReason(req.getReason());
        leave.setStartTime(req.getStartTime());
        leave.setEndTime(req.getEndTime());
        leave.setStatus(BpmProcessInstanceStatusEnum.RUNNING.getCode());
        leave.setProcessInstanceId("");
        bpmOaLeaveService.save(leave);

        // 3. 收集「发起人自选审批人」-> 多实例集合变量 coll_userList
        List<Long> approverIds = new ArrayList<>();
        if (req.getStartUserSelectAssignees() != null) {
            for (List<Long> ids : req.getStartUserSelectAssignees().values()) {
                if (ids != null) {
                    approverIds.addAll(ids);
                }
            }
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("coll_userList", approverIds);

        // 4. 以当前用户身份启动流程实例（businessKey 绑定请假业务ID，便于详情/流转记录按实例回查业务表单）
        Authentication.setAuthenticatedUserId(username);
        ProcessInstance pi;
        try {
            pi = runtimeService.startProcessInstanceByKey(
                    PROCESS_KEY, String.valueOf(leave.getId()), variables);
        } finally {
            Authentication.setAuthenticatedUserId(null);
        }

        // 5. 自动完成「提交」任务（发起人提交），使流程推进到审批节点
        List<Task> submitTasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        for (Task t : submitTasks) {
            try {
                taskService.setAssignee(t.getId(), username);
            } catch (Exception ignored) {
            }
            taskService.complete(t.getId());
        }

        // 6. 为「审批」节点（多实例）指派发起人选择的审批人
        List<Task> approvalTasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        for (int i = 0; i < approvalTasks.size(); i++) {
            if (i < approverIds.size() && approverIds.get(i) != null) {
                SysUser approver = sysUserMapper.selectById(approverIds.get(i));
                if (approver != null && StringUtils.hasText(approver.getUsername())) {
                    try {
                        taskService.setAssignee(approvalTasks.get(i).getId(), approver.getUsername());
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // 7. 回写流程实例ID
        leave.setProcessInstanceId(pi.getId());
        bpmOaLeaveService.updateById(leave);
        return R.ok(leave.getId());
    }

    /**
     * 取消请假（删除流程实例 + 业务状态置为已取消）
     * DELETE /api/v1/bpm/oa/leave/cancel
     */
    @DeleteMapping("/cancel")
    public R<Boolean> cancel(@RequestParam Long id, @RequestParam String reason) {
        BpmOaLeave leave = bpmOaLeaveService.getById(id);
        if (leave == null) {
            return R.fail("请假记录不存在");
        }
        if (!BpmProcessInstanceStatusEnum.RUNNING.getCode().equals(leave.getStatus())) {
            return R.fail("当前状态不可取消");
        }
        if (StringUtils.hasText(leave.getProcessInstanceId())) {
            runtimeService.deleteProcessInstance(leave.getProcessInstanceId(),
                    "用户取消: " + (reason == null ? "" : reason));
        }
        leave.setStatus(BpmProcessInstanceStatusEnum.CANCEL.getCode());
        bpmOaLeaveService.updateById(leave);
        return R.ok(true);
    }

    /**
     * 根据ID查询请假详情
     * GET /api/v1/bpm/oa/leave/get?id=
     */
    @GetMapping("/get")
    public R<?> get(@RequestParam Long id) {
        return R.ok(bpmOaLeaveService.getById(id));
    }

    /**
     * 根据流程实例ID查询请假详情（供流程详情/流转记录查看业务表单使用）
     * GET /api/v1/bpm/oa/leave/get-by-process-instance-id?processInstanceId=
     */
    @GetMapping("/get-by-process-instance-id")
    public R<?> getByProcessInstanceId(@RequestParam String processInstanceId) {
        if (!StringUtils.hasText(processInstanceId)) {
            return R.fail("流程实例ID不能为空");
        }
        BpmOaLeave leave = bpmOaLeaveService.lambdaQuery()
                .eq(BpmOaLeave::getProcessInstanceId, processInstanceId)
                .last("LIMIT 1")
                .one();
        return R.ok(leave);
    }

    /**
     * 请假分页查询
     * GET /api/v1/bpm/oa/leave/page
     */
    @GetMapping("/page")
    public R<?> page(@RequestParam(required = false) Integer pageNo,
                     @RequestParam(required = false) Integer pageSize,
                     @RequestParam(required = false) String type,
                     @RequestParam(required = false) Integer status,
                     @RequestParam(required = false) String reason) {
        long current = pageNo == null ? 1 : pageNo;
        long size = pageSize == null ? 10 : pageSize;

        LambdaQueryWrapper<BpmOaLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(type != null, BpmOaLeave::getType, type);
        wrapper.eq(status != null, BpmOaLeave::getStatus, status);
        wrapper.like(reason != null, BpmOaLeave::getReason, reason);
        wrapper.orderByDesc(BpmOaLeave::getCreateTime);

        IPage<BpmOaLeave> page = bpmOaLeaveService.page(new Page<>(current, size), wrapper);
        return R.ok(PageResult.of(page.getRecords(), page.getTotal()));
    }
}
