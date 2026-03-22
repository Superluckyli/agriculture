package lizhuoer.agri.agri_system.module.report.service;

import lizhuoer.agri.agri_system.module.report.domain.CostAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.DashboardV2VO;
import lizhuoer.agri.agri_system.module.report.domain.ProductionAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsFilterDTO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsOverviewVO;
import lizhuoer.agri.agri_system.module.report.domain.TaskAnalyticsVO;

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

    /**
     * 报表分析总览（V1 占位实现）
     */
    ReportAnalyticsOverviewVO getAnalyticsOverviewData(ReportAnalyticsFilterDTO filter);

    /**
     * 任务运营分析（V1 占位实现）
     */
    TaskAnalyticsVO getTaskAnalyticsData(ReportAnalyticsFilterDTO filter);

    /**
     * 种植产出分析（V1 占位实现）
     */
    ProductionAnalyticsVO getProductionAnalyticsData(ReportAnalyticsFilterDTO filter);

    /**
     * 成本采购分析（V1 占位实现）
     */
    CostAnalyticsVO getCostAnalyticsData(ReportAnalyticsFilterDTO filter);
}
