package lizhuoer.agri.agri_system.module.report.service.support;

import lizhuoer.agri.agri_system.module.report.domain.CostAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ProductionAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAiEvidenceItemVO;
import lizhuoer.agri.agri_system.module.report.domain.TaskAnalyticsVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class ReportAiEvidenceBuilder {

    public List<ReportAiEvidenceItemVO> build(String currentTab, String section, Object analytics) {
        if ("task".equals(currentTab) && "conclusion".equals(section) && analytics instanceof TaskAnalyticsVO taskAnalytics) {
            return buildTaskConclusion(taskAnalytics);
        }
        if ("production".equals(currentTab) && "risk".equals(section) && analytics instanceof ProductionAnalyticsVO productionAnalytics) {
            return buildProductionRisk(productionAnalytics);
        }
        if ("cost".equals(currentTab) && "attention".equals(section) && analytics instanceof CostAnalyticsVO costAnalytics) {
            return buildCostAttention(costAnalytics);
        }
        return List.of();
    }

    private List<ReportAiEvidenceItemVO> buildTaskConclusion(TaskAnalyticsVO analytics) {
        TaskAnalyticsVO.TrendVO trend = analytics.getTrend();
        List<TaskAnalyticsVO.AbnormalTaskItemVO> abnormalTasks = analytics.getAbnormalTasks();
        return List.of(
                item("已完成任务", Integer.toString(lastInt(trend != null ? trend.getCompleted() : null))),
                item("逾期任务", Integer.toString(lastInt(trend != null ? trend.getOverdue() : null))),
                item("异常任务", firstTaskName(abnormalTasks))
        );
    }

    private List<ReportAiEvidenceItemVO> buildProductionRisk(ProductionAnalyticsVO analytics) {
        ProductionAnalyticsVO.RiskBatchItemVO riskBatch = firstRiskBatch(analytics.getRiskBatches());
        return List.of(
                item("风险批次", riskBatch != null ? defaultString(riskBatch.getBatchNo()) : "-"),
                item("所属地块", riskBatch != null ? defaultString(riskBatch.getFarmlandName()) : "-"),
                item("达成率", formatPercent(riskBatch != null ? riskBatch.getAchievementRate() : null))
        );
    }

    private List<ReportAiEvidenceItemVO> buildCostAttention(CostAnalyticsVO analytics) {
        CostAnalyticsVO.AbnormalCostItemVO abnormalCost = firstAbnormalCost(analytics.getAbnormalCostItems());
        return List.of(
                item("异常物资", abnormalCost != null ? defaultString(abnormalCost.getMaterialName()) : "-"),
                item("供应商", abnormalCost != null ? defaultString(abnormalCost.getSupplierName()) : "-"),
                item("异常成本", formatDecimal(abnormalCost != null ? abnormalCost.getCost() : null))
        );
    }

    private TaskAnalyticsVO.AbnormalTaskItemVO firstAbnormalTask(List<TaskAnalyticsVO.AbnormalTaskItemVO> abnormalTasks) {
        return abnormalTasks == null || abnormalTasks.isEmpty() ? null : abnormalTasks.get(0);
    }

    private ProductionAnalyticsVO.RiskBatchItemVO firstRiskBatch(List<ProductionAnalyticsVO.RiskBatchItemVO> riskBatches) {
        return riskBatches == null || riskBatches.isEmpty() ? null : riskBatches.get(0);
    }

    private CostAnalyticsVO.AbnormalCostItemVO firstAbnormalCost(List<CostAnalyticsVO.AbnormalCostItemVO> abnormalCosts) {
        return abnormalCosts == null || abnormalCosts.isEmpty() ? null : abnormalCosts.get(0);
    }

    private String firstTaskName(List<TaskAnalyticsVO.AbnormalTaskItemVO> abnormalTasks) {
        TaskAnalyticsVO.AbnormalTaskItemVO task = firstAbnormalTask(abnormalTasks);
        return task == null ? "-" : defaultString(task.getTaskName());
    }

    private int lastInt(List<Integer> values) {
        return values == null || values.isEmpty() || values.get(values.size() - 1) == null
                ? 0
                : values.get(values.size() - 1);
    }

    private String formatPercent(BigDecimal value) {
        return value == null ? "0%" : value.stripTrailingZeros().toPlainString() + "%";
    }

    private String formatDecimal(BigDecimal value) {
        return value == null ? "0" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String defaultString(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private ReportAiEvidenceItemVO item(String label, String value) {
        ReportAiEvidenceItemVO item = new ReportAiEvidenceItemVO();
        item.setLabel(label);
        item.setValue(value);
        return item;
    }
}
