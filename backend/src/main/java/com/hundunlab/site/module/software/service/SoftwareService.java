package com.hundunlab.site.module.software.service;

import com.hundunlab.site.module.software.pojo.VO.SoftwareDetailVO;
import com.hundunlab.site.module.software.pojo.VO.SoftwareSummaryVO;

import java.util.List;

/**
 * 软件展示（只读、匿名可见）。
 * <p>
 * 后台的增删改属于 admin 模块的职责，这里不掺进来。
 */
public interface SoftwareService {

    /**
     * 已发布软件列表，按 sort_order 升序
     */
    List<SoftwareSummaryVO> listPublished();

    /**
     * 软件详情页聚合数据
     *
     * @param slug URL 标识
     * @throws com.hundunlab.site.common.exception.MyException.BizException 软件不存在或未发布
     */
    SoftwareDetailVO getDetail(String slug);
}
