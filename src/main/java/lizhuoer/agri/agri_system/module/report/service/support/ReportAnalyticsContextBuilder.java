package lizhuoer.agri.agri_system.module.report.service.support;

import lizhuoer.agri.agri_system.module.report.domain.CostAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ProductionAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAiSummaryRequestDTO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsFilterDTO;
import lizhuoer.agri.agri_system.module.report.domain.TaskAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.service.IReportService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Component
public class ReportAnalyticsContextBuilder {

    private final IReportService reportService;
    private final Clock clock;

    public ReportAnalyticsContextBuilder(IReportService reportService) {
        this(reportService, Clock.systemDefaultZone());
    }

    public ReportAnalyticsContextBuilder(IReportService reportService, Clock clock) {
        this.reportService = reportService;
        this.clock = clock;
    }

    public ReportAnalyticsContext build(ReportAiSummaryRequestDTO request) {
        ReportAnalyticsFilterDTO requestedFilters = request.getFilters();
        Object analytics = switch (request.getCurrentTab()) {
            case "task" -> reportService.getTaskAnalyticsData(requestedFilters);
            case "production" -> reportService.getProductionAnalyticsData(requestedFilters);
            case "cost" -> reportService.getCostAnalyticsData(requestedFilters);
            default -> throw new IllegalArgumentException("Unsupported currentTab");
        };
        ReportAnalyticsFilterDTO effectiveFilters = resolveEffectiveFilters(request.getCurrentTab(), requestedFilters, analytics);
        return new ReportAnalyticsContext(request.getCurrentTab(), effectiveFilters, analytics, isDataSufficient(request.getCurrentTab(), analytics));
    }

    private ReportAnalyticsFilterDTO resolveEffectiveFilters(String currentTab,
                                                             ReportAnalyticsFilterDTO requestedFilters,
                                                             Object analytics) {
        ReportAnalyticsFilterDTO filterContext = switch (currentTab) {
            case "task" -> analytics instanceof TaskAnalyticsVO taskAnalytics ? taskAnalytics.getFilterContext() : null;
            case "production" -> analytics instanceof ProductionAnalyticsVO productionAnalytics ? productionAnalytics.getFilterContext() : null;
            case "cost" -> analytics instanceof CostAnalyticsVO costAnalytics ? costAnalytics.getFilterContext() : null;
            default -> null;
        };
        return filterContext != null ? filterContext : normalizeFilters(requestedFilters);
    }

    private ReportAnalyticsFilterDTO normalizeFilters(ReportAnalyticsFilterDTO filter) {
        ReportAnalyticsFilterDTO normalized = new ReportAnalyticsFilterDTO();
        LocalDate today = LocalDate.now(clock);
        normalized.setStartDate(filter != null && filter.getStartDate() != null ? filter.getStartDate() : today.minusDays(29));
        normalized.setEndDate(filter != null && filter.getEndDate() != null ? filter.getEndDate() : today);
        normalized.setGranularity(filter != null && filter.getGranularity() != null && !filter.getGranularity().isBlank()
                ? filter.getGranularity() : "day");
        if (filter != null) {
            normalized.setFarmlandId(filter.getFarmlandId());
            normalized.setVarietyId(filter.getVarietyId());
            normalized.setAssigneeId(filter.getAssigneeId());
            normalized.setMaterialCategory(filter.getMaterialCategory());
            normalized.setSupplierId(filter.getSupplierId());
        }
        return normalized;
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
