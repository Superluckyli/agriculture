package lizhuoer.agri.agri_system.module.report.domain;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReportAnalyticsOverviewVO {
    private ReportAnalyticsFilterDTO filterContext;
    private KpisVO kpis;

    @Data
    public static class KpisVO {
        private BigDecimal taskCompletionRate;
        private BigDecimal onTimeExecutionRate;
        private Integer overdueTaskCount;
        private Integer activeBatchCount;
        private BigDecimal outputAchievementRate;
        private BigDecimal purchaseAmount;
        private BigDecimal materialCost;
        private String updatedAt;
    }
}
