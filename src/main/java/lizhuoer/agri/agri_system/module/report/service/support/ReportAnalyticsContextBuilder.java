package lizhuoer.agri.agri_system.module.report.service.support;

import lizhuoer.agri.agri_system.module.report.domain.CostAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ProductionAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAiSummaryRequestDTO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsFilterDTO;
import lizhuoer.agri.agri_system.module.report.domain.TaskAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.service.IReportService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Component
public class ReportAnalyticsContextBuilder {

    private final IReportService reportService;

    public ReportAnalyticsContextBuilder(IReportService reportService) {
        this.reportService = reportService;
    }

    public ReportAnalyticsContext build(ReportAiSummaryRequestDTO request) {
        ReportAnalyticsFilterDTO filters = request.getFilters();
        Object analytics = switch (request.getCurrentTab()) {
            case "task" -> reportService.getTaskAnalyticsData(filters);
            case "production" -> reportService.getProductionAnalyticsData(filters);
            case "cost" -> reportService.getCostAnalyticsData(filters);
            default -> throw new IllegalArgumentException("Unsupported currentTab");
        };
        return new ReportAnalyticsContext(request.getCurrentTab(), filters, analytics, isDataSufficient(request.getCurrentTab(), analytics));
    }

    private boolean isDataSufficient(String currentTab, Object analytics) {
        return switch (currentTab) {
            case "task" -> isTaskDataSufficient((TaskAnalyticsVO) analytics);
            case "production" -> isProductionDataSufficient((ProductionAnalyticsVO) analytics);
            case "cost" -> isCostDataSufficient((CostAnalyticsVO) analytics);
            default -> false;
        };
    }

    private boolean isTaskDataSufficient(TaskAnalyticsVO analytics) {
        if (analytics == null) {
            return false;
        }
        TaskAnalyticsVO.TrendVO trend = analytics.getTrend();
        return hasAnyPositive(trend != null ? trend.getCreated() : null)
                || hasAnyPositive(trend != null ? trend.getCompleted() : null)
                || hasAnyPositive(trend != null ? trend.getOverdue() : null)
                || hasElements(analytics.getAssigneeRanking())
                || hasElements(analytics.getAbnormalTasks());
    }

    private boolean isProductionDataSufficient(ProductionAnalyticsVO analytics) {
        if (analytics == null) {
            return false;
        }
        ProductionAnalyticsVO.HarvestTrendVO trend = analytics.getHarvestTrend();
        return hasElements(analytics.getCropDistribution())
                || hasElements(analytics.getOutputComparison())
                || hasElements(analytics.getRiskBatches())
                || hasAnyPositive(trend != null ? trend.getBatchCount() : null)
                || hasAnyPositiveDecimal(trend != null ? trend.getEstimatedOutput() : null);
    }

    private boolean isCostDataSufficient(CostAnalyticsVO analytics) {
        if (analytics == null) {
            return false;
        }
        CostAnalyticsVO.PurchaseTrendVO trend = analytics.getPurchaseTrend();
        return hasElements(analytics.getMaterialCostTopN())
                || hasElements(analytics.getCategoryCostShare())
                || hasElements(analytics.getAbnormalCostItems())
                || hasAnyPositive(trend != null ? trend.getOrderCount() : null)
                || hasAnyPositiveDecimal(trend != null ? trend.getAmount() : null);
    }

    private boolean hasElements(List<?> items) {
        return items != null && !items.isEmpty();
    }

    private boolean hasAnyPositive(List<Integer> values) {
        return values != null && values.stream().filter(Objects::nonNull).anyMatch(value -> value > 0);
    }

    private boolean hasAnyPositiveDecimal(List<BigDecimal> values) {
        return values != null && values.stream().filter(Objects::nonNull).anyMatch(value -> value.compareTo(BigDecimal.ZERO) > 0);
    }

    public record ReportAnalyticsContext(String currentTab,
                                         ReportAnalyticsFilterDTO filters,
                                         Object analytics,
                                         boolean dataSufficient) {
    }
}
