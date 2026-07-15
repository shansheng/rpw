package com.company.rpw.controller.bpm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.rpw.bpm.BpmCurrentUser;
import com.company.rpw.common.PageResult;
import com.company.rpw.common.R;
import com.company.rpw.entity.bpm.BpmUserGroup;
import com.company.rpw.service.bpm.BpmUserGroupService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程用户分组 Controller
 * 路径: /api/v1/bpm/user-group
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bpm/user-group")
@RequiredArgsConstructor
public class BpmUserGroupController {

    private final BpmUserGroupService userGroupService;

    /** 保存/更新请求体（userIds 为数组，入库转为逗号字符串） */
    @Data
    public static class UserGroupSaveReqVO {
        private Long id;
        private String name;
        private String description;
        private List<Long> userIds;
        private Integer status;
        private String remark;
    }

    /** 列表/详情响应（userIds 为数组） */
    @Data
    public static class UserGroupRespVO {
        private Long id;
        private String name;
        private String description;
        private List<Long> userIds;
        private Integer status;
        private String remark;
        private java.time.LocalDateTime createTime;
    }

    private UserGroupRespVO toResp(BpmUserGroup e) {
        UserGroupRespVO v = new UserGroupRespVO();
        v.setId(e.getId());
        v.setName(e.getName());
        v.setDescription(e.getDescription());
        v.setUserIds(parseUserIds(e.getUserIds()));
        v.setStatus(e.getStatus());
        v.setRemark(e.getRemark());
        v.setCreateTime(e.getCreateTime());
        return v;
    }

    private List<Long> parseUserIds(String csv) {
        List<Long> list = new ArrayList<>();
        if (!StringUtils.hasText(csv)) {
            return list;
        }
        for (String s : csv.split(",")) {
            try {
                list.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return list;
    }

    private String toCsv(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    @GetMapping("/page")
    public R<PageResult<UserGroupRespVO>> page(@RequestParam(required = false) Integer pageNo,
                                                @RequestParam(required = false) Integer pageSize,
                                                @RequestParam(required = false) String name,
                                                @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<BpmUserGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), BpmUserGroup::getName, name)
                .eq(status != null, BpmUserGroup::getStatus, status)
                .orderByDesc(BpmUserGroup::getId);
        Page<BpmUserGroup> p = new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize);
        IPage<BpmUserGroup> r = userGroupService.page(p, wrapper);
        List<UserGroupRespVO> list = r.getRecords().stream().map(this::toResp).collect(Collectors.toList());
        return R.ok(PageResult.of(list, r.getTotal()));
    }

    @GetMapping("/get")
    public R<UserGroupRespVO> get(@RequestParam Long id) {
        BpmUserGroup e = userGroupService.getById(id);
        return R.ok(e == null ? null : toResp(e));
    }

    @PostMapping("/create")
    public R<Long> create(@RequestBody UserGroupSaveReqVO req) {
        BpmUserGroup e = new BpmUserGroup();
        e.setName(req.getName());
        e.setDescription(req.getDescription());
        e.setUserIds(toCsv(req.getUserIds()));
        e.setStatus(req.getStatus());
        e.setRemark(req.getRemark());
        userGroupService.save(e);
        return R.ok(e.getId());
    }

    @PutMapping("/update")
    public R<Boolean> update(@RequestBody UserGroupSaveReqVO req) {
        BpmUserGroup e = new BpmUserGroup();
        e.setId(req.getId());
        e.setName(req.getName());
        e.setDescription(req.getDescription());
        e.setUserIds(toCsv(req.getUserIds()));
        e.setStatus(req.getStatus());
        e.setRemark(req.getRemark());
        return R.ok(userGroupService.updateById(e));
    }

    @DeleteMapping("/delete")
    public R<Boolean> delete(@RequestParam Long id) {
        return R.ok(userGroupService.removeById(id));
    }

    @GetMapping("/simple-list")
    public R<List<UserGroupRespVO>> simpleList() {
        List<UserGroupRespVO> list = userGroupService.list().stream().map(this::toResp).collect(Collectors.toList());
        return R.ok(list);
    }
}
