package com.company.rpw.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelAnalysisException;
import com.alibaba.excel.read.listener.PageReadListener;
import com.company.rpw.dto.common.ImportResultVO;
import com.company.rpw.entity.ResourcePlanMaterial;
import com.company.rpw.mapper.DictMapper;
import com.company.rpw.mapper.ResourcePlanMaterialMapper;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel 导入服务
 * 支持 WBS、资源字典、供应商字典、材料计划 的 Excel 导入
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final DictMapper dictMapper;
    private final ResourcePlanMaterialMapper resourcePlanMaterialMapper;

    private static final String TABLE_WBS = "dict_wbs";
    private static final String TABLE_RESOURCE = "dict_resource";
    private static final String TABLE_SUPPLIER = "dict_supplier";
    private static final String TABLE_MATERIAL_PLAN = "resource_plan_material";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 导入 WBS
     */
    public ImportResultVO importWbs(MultipartFile file) {
        DictImportListener listener = new DictImportListener(dictMapper, TABLE_WBS, "WBS编码", "WBS名称");
        return doImport(file, listener, DictRow.class);
    }

    /**
     * 导入资源字典
     */
    public ImportResultVO importResource(MultipartFile file) {
        DictImportListener listener = new DictImportListener(dictMapper, TABLE_RESOURCE, "资源编码", "资源名称");
        return doImport(file, listener, DictRow.class);
    }

    /**
     * 导入供应商字典
     */
    public ImportResultVO importSupplier(MultipartFile file) {
        DictImportListener listener = new DictImportListener(dictMapper, TABLE_SUPPLIER, "供应商编码", "供应商名称");
        return doImport(file, listener, DictRow.class);
    }

    /**
     * 导入材料计划
     */
    public ImportResultVO importMaterialPlan(MultipartFile file) {
        MaterialPlanImportListener listener = new MaterialPlanImportListener(resourcePlanMaterialMapper);
        return doImport(file, listener, MaterialPlanRow.class);
    }

    /**
     * 通用导入执行方法
     */
    private <T> ImportResultVO doImport(MultipartFile file, AnalysisEventListener<T> listener, Class<T> headClass) {
        ImportResultVO result = new ImportResultVO();
        try {
            EasyExcel.read(file.getInputStream(), headClass, listener)
                    .sheet()
                    .doRead();

            result.setTotalCount(listener instanceof DictImportListener
                    ? ((DictImportListener) listener).getTotalCount()
                    : ((MaterialPlanImportListener) listener).getTotalCount());
            result.setSuccessCount(listener instanceof DictImportListener
                    ? ((DictImportListener) listener).getSuccessCount()
                    : ((MaterialPlanImportListener) listener).getSuccessCount());
            result.setFailCount(listener instanceof DictImportListener
                    ? ((DictImportListener) listener).getFailCount()
                    : ((MaterialPlanImportListener) listener).getFailCount());

            List<String> failDetails = listener instanceof DictImportListener
                    ? ((DictImportListener) listener).getFailDetails()
                    : ((MaterialPlanImportListener) listener).getFailDetails();
            result.setFailDetails(String.join("\n", failDetails));

            log.info("导入完成: total={}, success={}, fail={}",
                    result.getTotalCount(), result.getSuccessCount(), result.getFailCount());
        } catch (IOException e) {
            log.error("读取文件失败", e);
            result.setTotalCount(0);
            result.setSuccessCount(0);
            result.setFailCount(0);
            result.setFailDetails("读取文件失败: " + e.getMessage());
        } catch (ExcelAnalysisException e) {
            log.error("解析Excel失败", e);
            result.setTotalCount(0);
            result.setSuccessCount(0);
            result.setFailCount(0);
            result.setFailDetails("解析Excel失败: " + e.getMessage());
        }
        return result;
    }

    // ==================== Excel 行数据模型 ====================

    /**
     * 字典导入行数据模型
     */
    public static class DictRow {
        private String code;
        private String name;
        private Integer sortOrder;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }
    }

    /**
     * 材料计划导入行数据模型
     */
    public static class MaterialPlanRow {
        private Long projectId;
        private String wbsCode;
        private String resourceName;
        private String specification;
        private String unit;
        private java.math.BigDecimal budgetQuantity;
        private String supplierCode;
        private String purchaseSourceCode;
        private String purchaseProgressCode;
        private String shippingProgressCode;
        private String planArrivalDate;
        private String remark;

        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }

        public String getWbsCode() {
            return wbsCode;
        }

        public void setWbsCode(String wbsCode) {
            this.wbsCode = wbsCode;
        }

        public String getResourceName() {
            return resourceName;
        }

        public void setResourceName(String resourceName) {
            this.resourceName = resourceName;
        }

        public String getSpecification() {
            return specification;
        }

        public void setSpecification(String specification) {
            this.specification = specification;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public java.math.BigDecimal getBudgetQuantity() {
            return budgetQuantity;
        }

        public void setBudgetQuantity(java.math.BigDecimal budgetQuantity) {
            this.budgetQuantity = budgetQuantity;
        }

        public String getSupplierCode() {
            return supplierCode;
        }

        public void setSupplierCode(String supplierCode) {
            this.supplierCode = supplierCode;
        }

        public String getPurchaseSourceCode() {
            return purchaseSourceCode;
        }

        public void setPurchaseSourceCode(String purchaseSourceCode) {
            this.purchaseSourceCode = purchaseSourceCode;
        }

        public String getPurchaseProgressCode() {
            return purchaseProgressCode;
        }

        public void setPurchaseProgressCode(String purchaseProgressCode) {
            this.purchaseProgressCode = purchaseProgressCode;
        }

        public String getShippingProgressCode() {
            return shippingProgressCode;
        }

        public void setShippingProgressCode(String shippingProgressCode) {
            this.shippingProgressCode = shippingProgressCode;
        }

        public String getPlanArrivalDate() {
            return planArrivalDate;
        }

        public void setPlanArrivalDate(String planArrivalDate) {
            this.planArrivalDate = planArrivalDate;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    // ==================== EasyExcel 监听器 ====================

    /**
     * 字典导入监听器（通用：dict_wbs, dict_resource, dict_supplier）
     */
    public static class DictImportListener extends AnalysisEventListener<DictRow> {

        private final DictMapper dictMapper;
        private final String tableName;
        private final String codeLabel;
        private final String nameLabel;
        private final List<String> failDetails = new ArrayList<>();
        private int totalCount = 0;
        private int successCount = 0;
        private int failCount = 0;
        private static final int BATCH_SIZE = 100;
        private final List<Map<String, Object>> batchList = new ArrayList<>();

        public DictImportListener(DictMapper dictMapper, String tableName, String codeLabel, String nameLabel) {
            this.dictMapper = dictMapper;
            this.tableName = tableName;
            this.codeLabel = codeLabel;
            this.nameLabel = nameLabel;
        }

        @Override
        public void invoke(DictRow row, AnalysisContext context) {
            totalCount++;
            Integer rowIndex = context.readRowHolder().getRowIndex();

            // 校验必填字段
            if (row.getCode() == null || row.getCode().trim().isEmpty()) {
                failCount++;
                failDetails.add("第" + rowIndex + "行: " + codeLabel + "不能为空");
                return;
            }
            if (row.getName() == null || row.getName().trim().isEmpty()) {
                failCount++;
                failDetails.add("第" + rowIndex + "行: " + nameLabel + "不能为空");
                return;
            }

            try {
                Map<String, Object> data = new HashMap<>();
                data.put("code", row.getCode().trim());
                data.put("name", row.getName().trim());
                if (row.getSortOrder() != null) {
                    data.put("sort_order", row.getSortOrder());
                } else {
                    data.put("sort_order", 0);
                }
                batchList.add(data);

                if (batchList.size() >= BATCH_SIZE) {
                    flushBatch();
                }
                successCount++;
            } catch (Exception e) {
                failCount++;
                successCount--;
                log.error("第{}行处理失败: {}", rowIndex, e.getMessage(), e);
                failDetails.add("第" + rowIndex + "行: " + e.getMessage());
            }
        }

        private void flushBatch() {
            for (Map<String, Object> data : batchList) {
                try {
                    dictMapper.insert(tableName, data);
                } catch (Exception e) {
                    log.error("批量插入失败: {}", e.getMessage(), e);
                }
            }
            batchList.clear();
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            if (!batchList.isEmpty()) {
                flushBatch();
            }
            log.info("{}导入完成: total={}, success={}, fail={}", tableName, totalCount, successCount, failCount);
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailCount() {
            return failCount;
        }

        public List<String> getFailDetails() {
            return failDetails;
        }
    }

    /**
     * 材料计划导入监听器
     */
    public static class MaterialPlanImportListener extends AnalysisEventListener<MaterialPlanRow> {

        private final ResourcePlanMaterialMapper mapper;
        private final List<String> failDetails = new ArrayList<>();
        private int totalCount = 0;
        private int successCount = 0;
        private int failCount = 0;
        private static final int BATCH_SIZE = 100;
        private final List<ResourcePlanMaterial> batchList = new ArrayList<>();

        public MaterialPlanImportListener(ResourcePlanMaterialMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public void invoke(MaterialPlanRow row, AnalysisContext context) {
            totalCount++;
            Integer rowIndex = context.readRowHolder().getRowIndex();

            try {
                // 校验必填字段
                StringBuilder errors = new StringBuilder();
                if (row.getWbsCode() == null || row.getWbsCode().trim().isEmpty()) {
                    errors.append("WBS编码不能为空; ");
                }
                if (row.getResourceName() == null || row.getResourceName().trim().isEmpty()) {
                    errors.append("材料名称不能为空; ");
                }
                if (errors.length() > 0) {
                    failCount++;
                    failDetails.add("第" + rowIndex + "行: " + errors);
                    return;
                }

                ResourcePlanMaterial entity = new ResourcePlanMaterial();
                entity.setProjectId(row.getProjectId());
                entity.setWbsCode(row.getWbsCode());
                entity.setResourceName(row.getResourceName());
                entity.setSpecification(row.getSpecification());
                entity.setUnit(row.getUnit());
                entity.setBudgetQuantity(row.getBudgetQuantity());
                entity.setSupplierCode(row.getSupplierCode());
                entity.setPurchaseSourceCode(row.getPurchaseSourceCode());
                entity.setPurchaseProgressCode(row.getPurchaseProgressCode());
                entity.setShippingProgressCode(row.getShippingProgressCode());
                entity.setRemark(row.getRemark());
                entity.setApprovalStatus("DRAFT");

                // 解析日期
                if (row.getPlanArrivalDate() != null && !row.getPlanArrivalDate().trim().isEmpty()) {
                    try {
                        entity.setPlanArrivalDate(LocalDate.parse(row.getPlanArrivalDate().trim(), DATE_FORMATTER));
                    } catch (DateTimeParseException e) {
                        failCount++;
                        failDetails.add("第" + rowIndex + "行: 计划到场日期格式错误(需要yyyy-MM-dd): " + row.getPlanArrivalDate());
                        return;
                    }
                }

                batchList.add(entity);
                if (batchList.size() >= BATCH_SIZE) {
                    flushBatch();
                }
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("第{}行处理失败: {}", rowIndex, e.getMessage(), e);
                failDetails.add("第" + rowIndex + "行: " + e.getMessage());
            }
        }

        private void flushBatch() {
            for (ResourcePlanMaterial entity : batchList) {
                try {
                    mapper.insert(entity);
                } catch (Exception e) {
                    log.error("批量插入失败: {}", e.getMessage(), e);
                }
            }
            batchList.clear();
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            if (!batchList.isEmpty()) {
                flushBatch();
            }
            log.info("材料计划导入完成: total={}, success={}, fail={}", totalCount, successCount, failCount);
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailCount() {
            return failCount;
        }

        public List<String> getFailDetails() {
            return failDetails;
        }
    }
}
