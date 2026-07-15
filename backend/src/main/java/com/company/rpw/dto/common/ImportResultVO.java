package com.company.rpw.dto.common;

import lombok.Data;

/**
 * 导入结果 VO
 */
@Data
public class ImportResultVO {

    /** 总条数 */
    private Integer totalCount;

    /** 成功条数 */
    private Integer successCount;

    /** 失败条数 */
    private Integer failCount;

    /** 失败详情 */
    private String failDetails;
}
