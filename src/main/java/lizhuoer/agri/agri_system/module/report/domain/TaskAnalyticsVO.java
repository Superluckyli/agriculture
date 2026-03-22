package lizhuoer.agri.agri_system.module.report.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TaskAnalyticsVO {
    private ReportAnalyticsFilterDTO filterContext;
    private TrendVO trend;
    private StatusDistributionVO statusDistribution;
    private List<AssigneeRankingItemVO> assigneeRanking;
    private List<AbnormalTaskItemVO> abnormalTasks;

    @Data
    public static class TrendVO {
        private List<String> labels;
        private List<Integer> created;
        private List<Integer> completed;
        private List<Integer> overdue;
    }

    @Data
    public static class StatusDistributionVO {
        private List<String> labels;
        private List<StatusSeriesItemVO> series;
    }

    @Data
    public static class StatusSeriesItemVO {
        private String name;
        private List<Integer> data;
    }

    @Data
    public static class AssigneeRankingItemVO {
        private Long assigneeId;
        private String assigneeName;
        private Integer assignedCount;
        private Integer completedCount;
        private BigDecimal completionRate;
        private BigDecimal onTimeRate;
        private BigDecimal overdueRate;
    }

    @Data
    public static class AbnormalTaskItemVO {
        private Long taskId;
        private String taskName;
        private String assigneeName;
        private String statusV2;
        private String deadlineAt;
        private String riskLevel;
        private Integer overdueDays;
    }
}
