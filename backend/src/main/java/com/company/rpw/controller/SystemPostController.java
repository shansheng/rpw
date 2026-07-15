package com.company.rpw.controller;

import com.company.rpw.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 系统岗位 Controller（适配 yudao-ui-admin-vben 的 bpm 流程用户任务「候选人-岗位」选择器）
 * 后端当前未实现岗位管理（无 sys_post 表），此处返回空列表，保证前端初始化不报错。
 * 如需岗位数据，可后续补充 sys_post 表 + SysPost 实体后在此查询返回。
 */
@RestController
@RequestMapping("/api/v1/system/post")
@RequiredArgsConstructor
public class SystemPostController {

    /**
     * 岗位精简列表（供流程用户任务「候选人-岗位」下拉选择）
     */
    @GetMapping("/simple-list")
    public R<List<Map<String, Object>>> simpleList() {
        return R.ok(Collections.emptyList());
    }
}
