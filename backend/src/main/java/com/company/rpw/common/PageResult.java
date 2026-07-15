package com.company.rpw.common;

import lombok.Data;

import java.util.List;

/**
 * 分页结果封装，匹配前端 vxe-table 期望的 {list, total} 结构。
 */
@Data
public class PageResult<T> {

    /** 当前页数据列表 */
    private List<T> list;

    /** 总记录数 */
    private Long total;

    public PageResult() {
    }

    public PageResult(List<T> list, Long total) {
        this.list = list;
        this.total = total;
    }

    public static <T> PageResult<T> of(List<T> list, long total) {
        return new PageResult<>(list, total);
    }
}
