package lizhuoer.agri.agri_system.module.report;

import lizhuoer.agri.agri_system.module.report.domain.CostAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ProductionAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAiEvidenceItemVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAiStreamEventVO;
import lizhuoer.agri.agri_system.module.report.domain.TaskAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.service.support.ReportAiEvidenceBuilder;
import lizhuoer.agri.agri_system.module.report.service.support.ReportAiSectionStreamParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportAiSectionStreamParserTest {

    @Test
    void parserHandlesHardMarkerInSingleChunk() {
        List<ReportAiStreamEventVO> events = new ArrayList<>();
        ReportAiSectionStreamParser parser = new ReportAiSectionStreamParser(events::add);

        parser.accept("[SECTION:risk]存在产量偏低风险。");
        parser.finish();

        assertThat(events).extracting(ReportAiStreamEventVO::getType)
                .containsExactly("section-start", "section-chunk", "done");
        assertThat(events).extracting(ReportAiStreamEventVO::getSection)
                .containsExactly("risk", "risk", null);
        assertThat(events).extracting(ReportAiStreamEventVO::getSummary)
                .containsExactly(null, "存在产量偏低风险。", null);
    }

    @Test
    void parserRoutesChunkTextToTheCurrentSectionAcrossSplitMarkers() {
        List<ReportAiStreamEventVO> events = new ArrayList<>();
        ReportAiSectionStreamParser parser = new ReportAiSectionStreamParser(events::add);

        parser.accept("[SECTION:concl");
        parser.accept("usion]任务整体表现稳定。[SECTION:reason]主要因为完成趋势回升。");
        parser.finish();

        assertThat(events).extracting(ReportAiStreamEventVO::getType)
                .containsExactly("section-start", "section-chunk", "section-start", "section-chunk", "done");
        assertThat(events).extracting(ReportAiStreamEventVO::getSection)
                .containsExactly("conclusion", "conclusion", "reason", "reason", null);
        assertThat(events).extracting(ReportAiStreamEventVO::getSummary)
                .containsExactly(null, "任务整体表现稳定。", null, "主要因为完成趋势回升。", null);
    }

    @Test
    void parserStopsRoutingTextWhenUnsupportedMarkerAppearsAfterValidSection() {
        List<ReportAiStreamEventVO> events = new ArrayList<>();
        ReportAiSectionStreamParser parser = new ReportAiSectionStreamParser(events::add);

        parser.accept("[SECTION:conclusion]已完成总结。[SECTION:unknown]这段文本不应落到上一节。");
        parser.finish();

        assertThat(events).extracting(ReportAiStreamEventVO::getType)
                .containsExactly("section-start", "section-chunk", "done");
        assertThat(events).extracting(ReportAiStreamEventVO::getSection)
                .containsExactly("conclusion", "conclusion", null);
        assertThat(events).extracting(ReportAiStreamEventVO::getSummary)
                .containsExactly(null, "已完成总结。", null);
    }

    @Test
    void evidenceBuilderReturnsDeterministicEvidenceForSupportedTabSectionPairs() {
        ReportAiEvidenceBuilder builder = new ReportAiEvidenceBuilder();

        TaskAnalyticsVO taskAnalytics = new TaskAnalyticsVO();
        TaskAnalyticsVO.TrendVO taskTrend = new TaskAnalyticsVO.TrendVO();
        taskTrend.setCompleted(List.of(3, 5, 8));
        taskTrend.setOverdue(List.of(2, 1, 0));
        taskAnalytics.setTrend(taskTrend);
        TaskAnalyticsVO.AbnormalTaskItemVO abnormalTask = new TaskAnalyticsVO.AbnormalTaskItemVO();
        abnormalTask.setTaskName("灌溉巡检");
        abnormalTask.setOverdueDays(2);
        taskAnalytics.setAbnormalTasks(List.of(abnormalTask));

        ProductionAnalyticsVO productionAnalytics = new ProductionAnalyticsVO();
        ProductionAnalyticsVO.RiskBatchItemVO riskBatch = new ProductionAnalyticsVO.RiskBatchItemVO();
        riskBatch.setBatchNo("B-2026-01");
        riskBatch.setFarmlandName("一号地块");
        riskBatch.setAchievementRate(new BigDecimal("72.5"));
        productionAnalytics.setRiskBatches(List.of(riskBatch));

        CostAnalyticsVO costAnalytics = new CostAnalyticsVO();
        CostAnalyticsVO.AbnormalCostItemVO abnormalCost = new CostAnalyticsVO.AbnormalCostItemVO();
        abnormalCost.setMaterialName("生物肥");
        abnormalCost.setSupplierName("绿源供应");
        abnormalCost.setCost(new BigDecimal("1888.50"));
        costAnalytics.setAbnormalCostItems(List.of(abnormalCost));

        assertThat(builder.build("task", "conclusion", taskAnalytics))
                .extracting(ReportAiEvidenceItemVO::getLabel, ReportAiEvidenceItemVO::getValue)
                .containsExactly(
                        tuple("已完成任务", "8"),
                        tuple("逾期任务", "0"),
                        tuple("异常任务", "灌溉巡检")
                );

        assertThat(builder.build("production", "risk", productionAnalytics))
                .extracting(ReportAiEvidenceItemVO::getLabel, ReportAiEvidenceItemVO::getValue)
                .containsExactly(
                        tuple("风险批次", "B-2026-01"),
                        tuple("所属地块", "一号地块"),
                        tuple("达成率", "72.5%")
                );

        assertThat(builder.build("cost", "attention", costAnalytics))
                .extracting(ReportAiEvidenceItemVO::getLabel, ReportAiEvidenceItemVO::getValue)
                .containsExactly(
                        tuple("异常物资", "生物肥"),
                        tuple("供应商", "绿源供应"),
                        tuple("异常成本", "1888.50")
                );
    }

    @Test
    void evidenceBuilderReturnsStableFallbackEvidenceForEmptyOrNullAnalytics() {
        ReportAiEvidenceBuilder builder = new ReportAiEvidenceBuilder();

        assertThat(builder.build("task", "conclusion", new TaskAnalyticsVO()))
                .extracting(ReportAiEvidenceItemVO::getLabel, ReportAiEvidenceItemVO::getValue)
                .containsExactly(
                        tuple("已完成任务", "0"),
                        tuple("逾期任务", "0"),
                        tuple("异常任务", "-")
                );

        assertThat(builder.build("production", "risk", null))
                .extracting(ReportAiEvidenceItemVO::getLabel, ReportAiEvidenceItemVO::getValue)
                .containsExactly(
                        tuple("风险批次", "-"),
                        tuple("所属地块", "-"),
                        tuple("达成率", "0%")
                );

        assertThat(builder.build("cost", "attention", new CostAnalyticsVO()))
                .extracting(ReportAiEvidenceItemVO::getLabel, ReportAiEvidenceItemVO::getValue)
                .containsExactly(
                        tuple("异常物资", "-"),
                        tuple("供应商", "-"),
                        tuple("异常成本", "0")
                );
    }

    private static org.assertj.core.groups.Tuple tuple(String label, String value) {
        return org.assertj.core.groups.Tuple.tuple(label, value);
    }
}
