package lizhuoer.agri.agri_system.module.report.controller;

import jakarta.validation.Valid;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.report.domain.CostAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.DashboardV2VO;
import lizhuoer.agri.agri_system.module.report.domain.ProductionAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAiSummaryRequestDTO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAiStreamEventVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsFilterDTO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsOverviewVO;
import lizhuoer.agri.agri_system.module.report.domain.TaskAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.service.IReportAiSummaryService;
import lizhuoer.agri.agri_system.module.report.service.IReportService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private IReportService reportService;

    @Autowired
    private ObjectProvider<IReportAiSummaryService> reportAiSummaryServiceProvider;

    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        return R.ok(reportService.getDashboardData());
    }

    @GetMapping("/dashboard/v2")
    public R<DashboardV2VO> dashboardV2() {
        return R.ok(reportService.getDashboardV2Data());
    }

    @GetMapping("/analytics/overview")
    public R<ReportAnalyticsOverviewVO> analyticsOverview(ReportAnalyticsFilterDTO filter) {
        return R.ok(ensureOverviewShape(reportService.getAnalyticsOverviewData(filter), filter));
    }

    @GetMapping("/analytics/task")
    public R<TaskAnalyticsVO> taskAnalytics(ReportAnalyticsFilterDTO filter) {
        return R.ok(ensureTaskShape(reportService.getTaskAnalyticsData(filter), filter));
    }

    @GetMapping("/analytics/production")
    public R<ProductionAnalyticsVO> productionAnalytics(ReportAnalyticsFilterDTO filter) {
        return R.ok(ensureProductionShape(reportService.getProductionAnalyticsData(filter), filter));
    }

    @GetMapping("/analytics/cost")
    public R<CostAnalyticsVO> costAnalytics(ReportAnalyticsFilterDTO filter) {
        return R.ok(ensureCostShape(reportService.getCostAnalyticsData(filter), filter));
    }

    @PostMapping(value = "/analytics/ai-summary/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter aiSummaryStream(@Valid @RequestBody ReportAiSummaryRequestDTO request) {
        IReportAiSummaryService reportAiSummaryService = reportAiSummaryServiceProvider.getIfAvailable();
        if (reportAiSummaryService == null) {
            throw new IllegalStateException("AI summary streaming service is not configured");
        }
        SseEmitter emitter = new SseEmitter(60_000L);
        reportAiSummaryService.stream(
                request,
                event -> sendEvent(emitter, event),
                emitter::complete,
                emitter::completeWithError
        );
        return emitter;
    }

    private void sendEvent(SseEmitter emitter, ReportAiStreamEventVO event) {
        try {
            emitter.send(SseEmitter.event().data(event));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to send AI summary stream event", e);
        }
    }

    private ReportAnalyticsOverviewVO ensureOverviewShape(ReportAnalyticsOverviewVO vo, ReportAnalyticsFilterDTO filter) {
        ReportAnalyticsOverviewVO result = vo != null ? vo : new ReportAnalyticsOverviewVO();
        result.setFilterContext(filter);
        ReportAnalyticsOverviewVO.KpisVO kpis = result.getKpis();
        if (kpis == null) {
            kpis = new ReportAnalyticsOverviewVO.KpisVO();
            result.setKpis(kpis);
        }
        kpis.setTaskCompletionRate(zeroIfNull(kpis.getTaskCompletionRate()));
        kpis.setOnTimeExecutionRate(zeroIfNull(kpis.getOnTimeExecutionRate()));
        kpis.setOverdueTaskCount(zeroIfNull(kpis.getOverdueTaskCount()));
        kpis.setActiveBatchCount(zeroIfNull(kpis.getActiveBatchCount()));
        kpis.setOutputAchievementRate(zeroIfNull(kpis.getOutputAchievementRate()));
        kpis.setPurchaseAmount(zeroIfNull(kpis.getPurchaseAmount()));
        kpis.setMaterialCost(zeroIfNull(kpis.getMaterialCost()));
        return result;
    }

    private TaskAnalyticsVO ensureTaskShape(TaskAnalyticsVO vo, ReportAnalyticsFilterDTO filter) {
        TaskAnalyticsVO result = vo != null ? vo : new TaskAnalyticsVO();
        result.setFilterContext(filter);
        TaskAnalyticsVO.TrendVO trend = result.getTrend();
        if (trend == null) {
            trend = new TaskAnalyticsVO.TrendVO();
            result.setTrend(trend);
        }
        trend.setLabels(emptyIfNull(trend.getLabels()));
        trend.setCreated(emptyIfNull(trend.getCreated()));
        trend.setCompleted(emptyIfNull(trend.getCompleted()));
        trend.setOverdue(emptyIfNull(trend.getOverdue()));
        TaskAnalyticsVO.StatusDistributionVO statusDistribution = result.getStatusDistribution();
        if (statusDistribution == null) {
            statusDistribution = new TaskAnalyticsVO.StatusDistributionVO();
            result.setStatusDistribution(statusDistribution);
        }
        statusDistribution.setLabels(emptyIfNull(statusDistribution.getLabels()));
        statusDistribution.setSeries(emptyIfNull(statusDistribution.getSeries()));
        result.setAssigneeRanking(emptyIfNull(result.getAssigneeRanking()));
        result.setAbnormalTasks(emptyIfNull(result.getAbnormalTasks()));
        return result;
    }

    private ProductionAnalyticsVO ensureProductionShape(ProductionAnalyticsVO vo, ReportAnalyticsFilterDTO filter) {
        ProductionAnalyticsVO result = vo != null ? vo : new ProductionAnalyticsVO();
        result.setFilterContext(filter);
        result.setCropDistribution(emptyIfNull(result.getCropDistribution()));
        result.setOutputComparison(emptyIfNull(result.getOutputComparison()));
        ProductionAnalyticsVO.HarvestTrendVO harvestTrend = result.getHarvestTrend();
        if (harvestTrend == null) {
            harvestTrend = new ProductionAnalyticsVO.HarvestTrendVO();
            result.setHarvestTrend(harvestTrend);
        }
        harvestTrend.setLabels(emptyIfNull(harvestTrend.getLabels()));
        harvestTrend.setBatchCount(emptyIfNull(harvestTrend.getBatchCount()));
        harvestTrend.setEstimatedOutput(emptyIfNull(harvestTrend.getEstimatedOutput()));
        result.setRiskBatches(emptyIfNull(result.getRiskBatches()));
        return result;
    }

    private CostAnalyticsVO ensureCostShape(CostAnalyticsVO vo, ReportAnalyticsFilterDTO filter) {
        CostAnalyticsVO result = vo != null ? vo : new CostAnalyticsVO();
        result.setFilterContext(filter);
        CostAnalyticsVO.PurchaseTrendVO purchaseTrend = result.getPurchaseTrend();
        if (purchaseTrend == null) {
            purchaseTrend = new CostAnalyticsVO.PurchaseTrendVO();
            result.setPurchaseTrend(purchaseTrend);
        }
        purchaseTrend.setLabels(emptyIfNull(purchaseTrend.getLabels()));
        purchaseTrend.setAmount(emptyIfNull(purchaseTrend.getAmount()));
        purchaseTrend.setOrderCount(emptyIfNull(purchaseTrend.getOrderCount()));
        result.setMaterialCostTopN(emptyIfNull(result.getMaterialCostTopN()));
        result.setCategoryCostShare(emptyIfNull(result.getCategoryCostShare()));
        result.setAbnormalCostItems(emptyIfNull(result.getAbnormalCostItems()));
        return result;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Integer zeroIfNull(Integer value) {
        return value != null ? value : 0;
    }

    private <T> java.util.List<T> emptyIfNull(java.util.List<T> values) {
        return values != null ? values : Collections.emptyList();
    }
}
