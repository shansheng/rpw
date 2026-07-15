package com.company.rpw.controller;

import com.company.rpw.common.PageResult;
import com.company.rpw.common.R;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 系统字典数据 Controller（精简版）
 *
 * <p>本系统未引入 yudao 的 sys_dict_type / sys_dict_data 表，字典数据以内存常量提供，
 * 仅覆盖前端 BPM 等模块所需的字典类型。前端 {@code getSimpleDictDataList} 在路由守卫中
 * 调用本接口的 {@code /simple-list} 加载字典缓存，供各页面的下拉/单选渲染使用。</p>
 *
 * <p>返回结构对齐前端 {@code SystemDictDataApi.DictData}：dictType / label / value / status / sort 等，
 * 其中 value 为字符串（前端按 valueType 再转 number / boolean）。</p>
 *
 * <p>字典管理页（views/system/dict）的左侧字典类型列表由 {@code SystemDictTypeController} 提供，
 * 右侧字典数据列表由本控制器的 {@code /page} 提供（同样走内存常量，与 {@code /simple-list} 同源）。
 * 新增/修改/删除均为内存态，重启后重置——生产应接 sys_dict_data 表。</p>
 */
@RestController
@RequestMapping("/api/v1/system/dict-data")
public class SystemDictDataController {

    /** 字典数据内存存储（可变，支持管理页的增删改） */
    private static final List<Map<String, Object>> DICT_DATA =
            new ArrayList<>(buildDictData());
    /** 自增 id 生成器（仅内存，重启后重置） */
    private static final AtomicLong DATA_ID_SEQ =
            new AtomicLong(DICT_DATA.size() + 1);

    /**
     * 精简字典数据列表（路由守卫加载字典缓存用）
     * GET /api/v1/system/dict-data/simple-list
     */
    @GetMapping("/simple-list")
    public R<List<Map<String, Object>>> simpleList() {
        return R.ok(DICT_DATA);
    }

    /**
     * 分页查询字典数据（字典管理页右侧列表用）
     * GET /api/v1/system/dict-data/page
     */
    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) String label) {
        int no = pageNo == null ? 1 : pageNo;
        int size = pageSize == null ? 10 : pageSize;
        List<Map<String, Object>> filtered = DICT_DATA.stream()
                .filter(m -> dictType == null || dictType.equals(m.get("dictType")))
                .filter(m -> label == null || ((String) m.get("label")).contains(label))
                .collect(Collectors.toList());
        long total = filtered.size();
        int from = Math.min((no - 1) * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return R.ok(PageResult.of(filtered.subList(from, to), total));
    }

    /**
     * 根据ID查询字典数据
     * GET /api/v1/system/dict-data/get
     */
    @GetMapping("/get")
    public R<Map<String, Object>> get(@RequestParam Long id) {
        return DICT_DATA.stream()
                .filter(m -> id.equals(m.get("id")))
                .findFirst()
                .map(R::ok)
                .orElse(R.fail("字典数据不存在"));
    }

    /**
     * 新增字典数据（内存态，重启后重置；生产应接 sys_dict_data 表）
     * POST /api/v1/system/dict-data/create
     */
    @PostMapping("/create")
    public R<Long> create(@RequestBody Map<String, Object> body) {
        Map<String, Object> m = new LinkedHashMap<>();
        long id = DATA_ID_SEQ.getAndIncrement();
        m.put("id", id);
        m.put("dictType", body.getOrDefault("dictType", ""));
        m.put("label", body.getOrDefault("label", ""));
        m.put("value", String.valueOf(body.getOrDefault("value", "")));
        m.put("status", body.getOrDefault("status", 1));
        m.put("sort", body.getOrDefault("sort", DICT_DATA.size() + 1));
        m.put("colorType", body.getOrDefault("colorType", ""));
        m.put("cssClass", body.getOrDefault("cssClass", ""));
        m.put("remark", body.getOrDefault("remark", ""));
        m.put("createTime", "2024-01-01 00:00:00");
        DICT_DATA.add(m);
        return R.ok(id);
    }

    /**
     * 修改字典数据
     * PUT /api/v1/system/dict-data/update
     */
    @PutMapping("/update")
    public R<Boolean> update(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        return DICT_DATA.stream()
                .filter(m -> id.equals(m.get("id")))
                .findFirst()
                .map(m -> {
                    m.put("dictType", body.getOrDefault("dictType", m.get("dictType")));
                    m.put("label", body.getOrDefault("label", m.get("label")));
                    m.put("value", String.valueOf(body.getOrDefault("value", m.get("value"))));
                    m.put("status", body.getOrDefault("status", m.get("status")));
                    m.put("sort", body.getOrDefault("sort", m.get("sort")));
                    m.put("colorType", body.getOrDefault("colorType", m.get("colorType")));
                    m.put("cssClass", body.getOrDefault("cssClass", m.get("cssClass")));
                    m.put("remark", body.getOrDefault("remark", m.get("remark")));
                    return R.ok(true);
                })
                .orElse(R.fail("字典数据不存在"));
    }

    /**
     * 删除字典数据
     * DELETE /api/v1/system/dict-data/delete
     */
    @DeleteMapping("/delete")
    public R<Boolean> delete(@RequestParam Long id) {
        boolean removed = DICT_DATA.removeIf(m -> id.equals(m.get("id")));
        return R.ok(removed);
    }

    /**
     * 批量删除字典数据
     * DELETE /api/v1/system/dict-data/delete-list
     */
    @DeleteMapping("/delete-list")
    public R<Boolean> deleteList(@RequestParam String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
        boolean removed = DICT_DATA.removeIf(m -> idList.contains((Long) m.get("id")));
        return R.ok(removed);
    }

    private static List<Map<String, Object>> buildDictData() {
        List<Map<String, Object>> list = new ArrayList<>();
        // 流程模型类型：value 对齐前端 BpmModelType 常量（BPMN=10 / SIMPLE=20）
        add(list, "bpm_model_type", "BPMN 流程", "10", 1);
        add(list, "bpm_model_type", "SIMPLE 流程", "20", 2);
        // 流程模型表单类型：value 对齐前端 BpmModelFormType 常量（NORMAL=10 流程表单 / CUSTOM=20 业务表单）
        add(list, "bpm_model_form_type", "流程表单", "10", 1);
        add(list, "bpm_model_form_type", "业务表单", "20", 2);
        // 是否可见（布尔字符串）
        add(list, "infra_boolean_string", "是", "true", 1);
        add(list, "infra_boolean_string", "否", "false", 2);
        // 通用状态：1=启用 0=禁用（与 sys_role/sys_menu/sys_dept 的 status 字段一致；
        // 前端 CommonStatusEnum 在本 fork 中 ENABLE=0 为反置，但显示由本字典驱动，须以 DB 约定为准）
        add(list, "common_status", "启用", "1", 1);
        add(list, "common_status", "禁用", "0", 2);
        // 菜单类型：1目录 2菜单 3按钮（对齐 SystemMenuTypeEnum）
        add(list, "system_menu_type", "目录", "1", 1);
        add(list, "system_menu_type", "菜单", "2", 2);
        add(list, "system_menu_type", "按钮", "3", 3);
        // 角色类型：1内置角色 2自定义角色（对齐 SystemRoleTypeEnum，与 sys_role.type 一致）
        add(list, "system_role_type", "内置角色", "1", 1);
        add(list, "system_role_type", "自定义角色", "2", 2);
        // 用户性别：1男 2女（对齐 SysUser.sex 字段）
        add(list, "system_user_sex", "男", "1", 1);
        add(list, "system_user_sex", "女", "2", 2);
        // 数据权限范围：1全部 2指定部门 3部门 4部门及以下 5仅本人（对齐 SystemDataScopeEnum）
        add(list, "system_data_scope", "全部数据权限", "1", 1);
        add(list, "system_data_scope", "指定部门数据权限", "2", 2);
        add(list, "system_data_scope", "部门数据权限", "3", 3);
        add(list, "system_data_scope", "部门及以下数据权限", "4", 4);
        add(list, "system_data_scope", "仅本人数据权限", "5", 5);
        // 请假类型：对齐 BpmOaLeaveTypeEnum（事假/病假/年假/婚假/产假），value 为字符串
        add(list, "bpm_oa_leave_type", "事假", "1", 1);
        add(list, "bpm_oa_leave_type", "病假", "2", 2);
        add(list, "bpm_oa_leave_type", "年假", "3", 3);
        add(list, "bpm_oa_leave_type", "婚假", "4", 4);
        add(list, "bpm_oa_leave_type", "产假", "5", 5);
        // 流程实例状态：value 对齐后端 BpmProcessInstanceStatusEnum（1审批中/2审批通过/3已取消/4审批不通过）
        // 注意：前端 BpmProcessInstanceStatus 常量把 3/4 写成「审批不通过/已取消」，与后端枚举相反；
        // 业务表实际存储以本后端枚举为准，故字典取值以本处定义（存储真相）为准，避免审批/取消显示错乱。
        add(list, "bpm_process_instance_status", "审批中", "1", 1);
        add(list, "bpm_process_instance_status", "审批通过", "2", 2);
        add(list, "bpm_process_instance_status", "已取消", "3", 3);
        add(list, "bpm_process_instance_status", "审批不通过", "4", 4);
        // 任务状态：value 对齐 yudao BpmTaskStatusEnum（1审批中/2审批通过/3已取消/4审批不通过）
        add(list, "bpm_task_status", "审批中", "1", 1);
        add(list, "bpm_task_status", "审批通过", "2", 2);
        add(list, "bpm_task_status", "已取消", "3", 3);
        add(list, "bpm_task_status", "审批不通过", "4", 4);
        // 任务候选人策略：value 对齐 yudao BpmTaskCandidateStrategyEnum
        // （本系统请假流程「审批」节点使用 USER=35）
        add(list, "bpm_task_candidate_strategy", "流程发起人", "10", 1);
        add(list, "bpm_task_candidate_strategy", "角色", "20", 2);
        add(list, "bpm_task_candidate_strategy", "部门成员", "30", 3);
        add(list, "bpm_task_candidate_strategy", "部门领导", "31", 4);
        add(list, "bpm_task_candidate_strategy", "岗位", "32", 5);
        add(list, "bpm_task_candidate_strategy", "用户", "35", 6);
        add(list, "bpm_task_candidate_strategy", "用户组", "40", 7);
        add(list, "bpm_task_candidate_strategy", "表达式", "50", 8);
        add(list, "bpm_task_candidate_strategy", "直属上级", "60", 9);
        add(list, "bpm_task_candidate_strategy", "表单内用户字段", "70", 10);
        // 流程监听器类型：value 对齐 yudao BpmProcessListenerTypeEnum（1流程/2活动/3任务）
        add(list, "bpm_process_listener_type", "流程", "1", 1);
        add(list, "bpm_process_listener_type", "活动", "2", 2);
        add(list, "bpm_process_listener_type", "任务", "3", 3);
        // 流程监听器值类型：value 对齐 yudao BpmProcessListenerValueTypeEnum（1类/2代理表达式/3表达式）
        add(list, "bpm_process_listener_value_type", "类", "1", 1);
        add(list, "bpm_process_listener_value_type", "代理表达式", "2", 2);
        add(list, "bpm_process_listener_value_type", "表达式", "3", 3);
        // 审批评论类型：value 对齐 yudao BpmCommentTypeEnum
        add(list, "bpm_comment_type", "发起", "1", 1);
        add(list, "bpm_comment_type", "提交", "2", 2);
        add(list, "bpm_comment_type", "通过", "3", 3);
        add(list, "bpm_comment_type", "驳回", "4", 4);
        add(list, "bpm_comment_type", "取消", "5", 5);
        add(list, "bpm_comment_type", "转办", "6", 6);
        add(list, "bpm_comment_type", "委派", "7", 7);
        add(list, "bpm_comment_type", "加签", "8", 8);
        add(list, "bpm_comment_type", "减签", "9", 9);
        add(list, "bpm_comment_type", "跳过", "10", 10);
        return list;
    }

    private static void add(List<Map<String, Object>> list, String dictType, String label,
                            String value, int sort) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", (long) (list.size() + 1));
        m.put("dictType", dictType);
        m.put("label", label);
        m.put("value", value);
        m.put("status", 1);
        m.put("sort", sort);
        m.put("colorType", "");
        m.put("cssClass", "");
        m.put("remark", "");
        m.put("createTime", "2024-01-01 00:00:00");
        list.add(m);
    }
}
