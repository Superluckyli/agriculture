package lizhuoer.agri.agri_system.module.report.service;

import lizhuoer.agri.agri_system.module.report.domain.ChartDataVO;
import lizhuoer.agri.agri_system.module.report.domain.DashboardV2VO;

import java.util.Map;

public interface IReportService {
    /**
     * 获取大屏首页数据聚合
     */
    Map<String, Object> getDashboardData();

    /**
     * Dashboard V2 聚合数据
     */
    DashboardV2VO getDashboardV2Data();
}
