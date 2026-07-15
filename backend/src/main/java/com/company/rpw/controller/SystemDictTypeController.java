package com.company.rpw.controller;

import com.company.rpw.common.PageResult;
import com.company.rpw.common.R;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 系统字典类型 Controller（精简版）
 *
 * <p>本系统未引入 yudao 的 sys_dict_type 表，字典类型与 {@link SystemDictDataController} 中的
 * 字典数据共用内存常量。字典数据负责「某个类型下的具体键值」，本控制器负责「类型元信息列表」，
 * 供前端的表单设计器（form-create 的字典选择器）、字典管理页等 {@code getSimpleDictTypeList}
 * 调用，以渲染字典类型下拉。</p>
 *
 * <p>返回结构对齐前端 {@code SystemDictTypeApi.DictType}：id / name / type / status / remark / createTime。
 * 其中 {@code type} 必须与 {@link SystemDictDataController} 中字典数据的 dictType 键保持一致。</p>
 */
@RestController
@RequestMapping("/api/v1/system/dict-type")
public class SystemDictTypeController {

    /** 自增 id 生成器（仅内存，重启后重置） */
    private static final AtomicLong ID_SEQ = new AtomicLong(1);
    /** 字典类型内存存储 */
    private static final Map<Long, DictTypeVO> STORE = new ConcurrentHashMap<>();

    static {
        add("common_status", "系统状态");
        add("bpm_model_type", "工作流模型类型");
        add("bpm_model_form_type", "工作流模型表单类型");
        add("infra_boolean_string", "是否");
        add("system_menu_type", "系统菜单类型");
        add("system_role_type", "系统角色类型");
        add("system_data_scope", "数据权限范围");
        add("bpm_oa_leave_type", "请假类型");
        add("bpm_process_instance_status", "工作流流程实例状态");
        add("bpm_task_status", "工作流任务状态");
        add("bpm_task_candidate_strategy", "工作流任务候选人策略");
        add("bpm_process_listener_type", "工作流流程监听器类型");
        add("bpm_process_listener_value_type", "工作流流程监听器值类型");
        add("bpm_comment_type", "工作流审批评论类型");
    }

    /**
     * 精简字典类型列表（前端表单设计器字典选择器、字典管理页下拉使用）
     * GET /api/v1/system/dict-type/list-all-simple
     */
    @GetMapping("/list-all-simple")
    public R<List<DictTypeVO>> listAllSimple() {
        return R.ok(allSorted());
    }

    /**
     * 分页查询字典类型
     * GET /api/v1/system/dict-type/page
     */
    @GetMapping("/page")
    public R<PageResult<DictTypeVO>> page(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type) {
        int no = pageNo == null ? 1 : pageNo;
        int size = pageSize == null ? 10 : pageSize;
        List<DictTypeVO> filtered = allSorted().stream()
                .filter(v -> name == null || v.getName().contains(name))
                .filter(v -> type == null || v.getType().contains(type))
                .collect(Collectors.toList());
        long total = filtered.size();
        int from = Math.min((no - 1) * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return R.ok(PageResult.of(filtered.subList(from, to), total));
    }

    /**
     * 根据ID查询字典类型
     * GET /api/v1/system/dict-type/get
     */
    @GetMapping("/get")
    public R<DictTypeVO> get(@RequestParam Long id) {
        DictTypeVO vo = STORE.get(id);
        if (vo == null) {
            return R.fail("字典类型不存在");
        }
        return R.ok(vo);
    }

    /**
     * 新增字典类型（内存态，重启后重置；生产应接 sys_dict_type 表）
     */
    @PostMapping("/create")
    public R<Long> create(@RequestBody DictTypeVO body) {
        DictTypeVO vo = new DictTypeVO();
        vo.setId(ID_SEQ.getAndIncrement());
        vo.setName(body.getName());
        vo.setType(body.getType());
        vo.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        vo.setRemark(body.getRemark());
        vo.setCreateTime("2024-01-01 00:00:00");
        STORE.put(vo.getId(), vo);
        return R.ok(vo.getId());
    }

    /**
     * 修改字典类型
     */
    @PutMapping("/update")
    public R<Boolean> update(@RequestBody DictTypeVO body) {
        DictTypeVO vo = STORE.get(body.getId());
        if (vo == null) {
            return R.fail("字典类型不存在");
        }
        vo.setName(body.getName());
        vo.setType(body.getType());
        vo.setStatus(body.getStatus());
        vo.setRemark(body.getRemark());
        return R.ok(true);
    }

    /**
     * 删除字典类型
     */
    @DeleteMapping("/delete")
    public R<Boolean> delete(@RequestParam Long id) {
        return R.ok(STORE.remove(id) != null);
    }

    private static void add(String type, String name) {
        DictTypeVO vo = new DictTypeVO();
        vo.setId(ID_SEQ.getAndIncrement());
        vo.setType(type);
        vo.setName(name);
        vo.setStatus(1);
        vo.setRemark("");
        vo.setCreateTime("2024-01-01 00:00:00");
        STORE.put(vo.getId(), vo);
    }

    private static List<DictTypeVO> allSorted() {
        return new ArrayList<>(STORE.values()).stream()
                .sorted(Comparator.comparing(DictTypeVO::getId))
                .collect(Collectors.toList());
    }

    /** 字典类型视图对象，对齐前端 SystemDictTypeApi.DictType */
    public static class DictTypeVO {
        private Long id;
        private String name;
        private String type;
        private Integer status;
        private String remark;
        private String createTime;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public String getCreateTime() {
            return createTime;
        }

        public void setCreateTime(String createTime) {
            this.createTime = createTime;
        }
    }
}
