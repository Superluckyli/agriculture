package lizhuoer.agri.agri_system.module.report.service.support;

import lizhuoer.agri.agri_system.module.report.domain.CostAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ProductionAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsFilterDTO;
import lizhuoer.agri.agri_system.module.report.domain.TaskAnalyticsVO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ReportAiPromptBuilder {

    public String build(ReportAnalyticsContextBuilder.ReportAnalyticsContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是农业经营报表分析助手。仅可基于下方 analytics 数据生成总结，不要编造、不要引用图表描述。\n")
                .append("当前页签: ").append(context.currentTab()).append('\n')
                .append("筛选窗口: ").append(formatDate(context.filters() != null ? context.filters().getStartDate() : null))
                .append(" 至 ").append(formatDate(context.filters() != null ? context.filters().getEndDate() : null)).append('\n')
                .append("语气限制: 结论和建议只能使用“关注 / 复核 / 持续观察”这类克制表述，不得输出更强烈或指令式语气。\n")
                .append("输出必须严格按以下四段并使用精确段落标记，不要缺段、不要改名、不要额外增加标题：\n")
                .append("[SECTION:conclusion]\n")
                .append("[SECTION:reason]\n")
                .append("[SECTION:risk]\n")
                .append("[SECTION:attention]\n")
                .append("避免输出无法从分析数据直接支持的业务动作，例如新增预算、调整组织编制、采购决策或处罚动作。\n");

        if (context.dataSufficient()) {
            prompt.append("当前数据足够支撑总结，请给出简洁事实判断。\n");
        } else {
            prompt.append("当前分析数据不足，请在结论中明确说明“数据不足”，仅做保守描述，不要延伸业务动作。\n");
        }

        prompt.append("analytics 数据:\n")
                .append(renderAnalytics(context.currentTab(), context.analytics()));
        return prompt.toString();
    }

    private String renderAnalytics(String currentTab, Object analytics) {
        if ("task".equals(currentTab)) {
            return renderTaskAnalytics((TaskAnalyticsVO) analytics);
        }
        if ("production".equals(currentTab)) {
            return renderProductionAnalytics((ProductionAnalyticsVO) analytics);
        }
        if ("cost".equals(currentTab)) {
            return renderCostAnalytics((CostAnalyticsVO) analytics);
        }
        return "-\n";
    }

    private String renderTaskAnalytics(TaskAnalyticsVO analytics) {
        TaskAnalyticsVO.TrendVO trend = analytics != null ? analytics.getTrend() : null;
        return new StringBuilder()
                .append("- completedTrend: ").append(trend != null ? safeList(trend.getCompleted()) : "[]").append('\n')
                .append("- overdueTrend: ").append(trend != null ? safeList(trend.getOverdue()) : "[]").append('\n')
                .append("- abnormalTasks: ").append(analytics != null && analytics.getAbnormalTasks() != null ? analytics.getAbnormalTasks() : List.of()).append('\n')
                .toString();
    }

    private String renderProductionAnalytics(ProductionAnalyticsVO analytics) {
        ProductionAnalyticsVO.HarvestTrendVO trend = analytics != null ? analytics.getHarvestTrend() : null;
        return new StringBuilder()
                .append("- riskBatches: ").append(analytics != null && analytics.getRiskBatches() != null ? analytics.getRiskBatches() : List.of()).append('\n')
                .append("- outputComparison: ").append(analytics != null && analytics.getOutputComparison() != null ? analytics.getOutputComparison() : List.of()).append('\n')
                .append("- harvestBatchCount: ").append(trend != null ? safeList(trend.getBatchCount()) : "[]").append('\n')
                .toString();
    }

    private String renderCostAnalytics(CostAnalyticsVO analytics) {
        CostAnalyticsVO.PurchaseTrendVO trend = analytics != null ? analytics.getPurchaseTrend() : null;
        return new StringBuilder()
                .append("- abnormalCostItems: ").append(analytics != null && analytics.getAbnormalCostItems() != null ? analytics.getAbnormalCostItems() : List.of()).append('\n')
                .append("- materialCostTopN: ").append(analytics != null && analytics.getMaterialCostTopN() != null ? analytics.getMaterialCostTopN() : List.of()).append('\n')
                .append("- purchaseOrderCount: ").append(trend != null ? safeList(trend.getOrderCount()) : "[]").append('\n')
                .toString();
    }

    private String safeList(List<?> values) {
        return values == null ? "[]" : values.toString();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.toString();
    }
}
