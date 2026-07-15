package com.company.rpw.controller.bpm;

import com.company.rpw.bpm.BpmCurrentUser;
import com.company.rpw.bpm.BpmUserHelper;
import com.company.rpw.bpm.BpmUserInfo;
import com.company.rpw.common.R;
import com.company.rpw.entity.bpm.BpmComment;
import com.company.rpw.service.bpm.BpmCommentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.flowable.engine.HistoryService;
import org.flowable.task.api.history.HistoricTaskInstance;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程评论 Controller
 * 路径: /api/v1/bpm/comment
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bpm/comment")
@RequiredArgsConstructor
public class BpmCommentController {

    private final BpmCommentService commentService;
    private final BpmUserHelper userHelper;
    private final HistoryService historyService;

    @Data
    public static class CommentVO {
        private Long id;
        private String taskId;
        private Map<String, Object> task;
        private String processInstanceId;
        private Integer type;
        private String message;
        private Long createTime;
        private BpmUserInfo user;
    }

    @GetMapping("/list-by-process-instance-id")
    public R<List<CommentVO>> listByProcessInstanceId(@RequestParam String processInstanceId) {
        List<BpmComment> list = commentService.lambdaQuery()
                .eq(BpmComment::getProcessInstanceId, processInstanceId)
                .orderByAsc(BpmComment::getId).list();
        return R.ok(list.stream().map(this::toVO).collect(Collectors.toList()));
    }

    @PostMapping("/create")
    public R<Void> create(@RequestBody Map<String, Object> body) {
        String taskId = body.get("taskId") == null ? null : String.valueOf(body.get("taskId"));
        String message = (String) body.get("message");
        BpmComment c = new BpmComment();
        c.setTaskId(taskId);
        c.setMessage(message);
        c.setType(1);
        c.setUserId(toLong(body.get("userId")));
        if (taskId != null) {
            HistoricTaskInstance ht = historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
            if (ht != null) c.setProcessInstanceId(ht.getProcessInstanceId());
        }
        commentService.save(c);
        return R.ok();
    }

    private CommentVO toVO(BpmComment c) {
        CommentVO v = new CommentVO();
        v.setId(c.getId());
        v.setTaskId(c.getTaskId());
        v.setProcessInstanceId(c.getProcessInstanceId());
        v.setType(c.getType());
        v.setMessage(c.getMessage());
        v.setCreateTime(c.getCreateTime() == null ? null : c.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        v.setUser(userHelper.buildInfo(c.getUserId()));
        if (c.getTaskId() != null) {
            HistoricTaskInstance ht = historyService.createHistoricTaskInstanceQuery().taskId(c.getTaskId()).singleResult();
            if (ht != null) {
                Map<String, Object> tm = new HashMap<>();
                tm.put("id", ht.getId());
                tm.put("name", ht.getName());
                tm.put("taskDefinitionKey", ht.getTaskDefinitionKey());
                v.setTask(tm);
            }
        }
        return v;
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }
}
