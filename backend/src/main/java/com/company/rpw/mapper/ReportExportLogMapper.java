package com.company.rpw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.rpw.entity.ReportExportLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报表导出记录 Mapper 接口
 */
@Mapper
public interface ReportExportLogMapper extends BaseMapper<ReportExportLog> {
}
