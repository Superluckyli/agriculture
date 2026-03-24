package lizhuoer.agri.agri_system.module.report;

import lizhuoer.agri.agri_system.module.report.domain.CostAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ProductionAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAiStreamEventVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAiSummaryRequestDTO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsFilterDTO;
import lizhuoer.agri.agri_system.module.report.domain.TaskAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.service.IReportService;
import lizhuoer.agri.agri_system.module.report.service.impl.ReportAiSummaryServiceImpl;
import lizhuoer.agri.agri_system.module.report.service.support.AiModelClient;
import lizhuoer.agri.agri_system.module.report.service.support.ReportAiEvidenceBuilder;
import lizhuoer.agri.agri_system.module.report.service.support.ReportAiPromptBuilder;
import lizhuoer.agri.agri_system.module.report.service.support.ReportAnalyticsContextBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportAiSummaryServiceTest {

    @Mock
    private IReportService reportService;

    @Mock
    private AiModelClient aiModelClient;

    private ReportAiSummaryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportAiSummaryServiceImpl(
                aiModelClient,
                new ReportAnalyticsContextBuilder(reportService),
                new ReportAiPromptBuilder(),
                new ReportAiEvidenceBuilder());
    }

    @Test
    void taskTabUsesTaskAnalyticsOnlyAndEmitsEvidenceAfterSectionText() {
        when(reportService.getTaskAnalyticsData(any())).thenReturn(taskAnalyticsFixture());
        doAnswer(invocation -> {
            Consumer<String> sink = invocation.getArgument(1);
            sink.accept("[SECTION:conclusion]任务执行整体稳定。");
            sink.accept("[SECTION:reason]完成趋势回升。");
            sink.accept("[SECTION:risk]逾期仍集中。");
            sink.accept("[SECTION:attention]建议关注高压执行人。");
            return null;
        }).when(aiModelClient).stream(anyString(), any());

        List<ReportAiStreamEventVO> events = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        service.stream(request("task"), events::add, () -> completed.set(true), errorRef::set);

        verify(reportService).getTaskAnalyticsData(any());
        verify(reportService, never()).getProductionAnalyticsData(any());
        verify(reportService, never()).getCostAnalyticsData(any());
        assertThat(errorRef.get()).isNull();
        assertThat(completed).isTrue();
        assertThat(events).extracting(ReportAiStreamEventVO::getType)
                .contains("evidence", "done");
        assertThat(indexOf(events, "section-chunk", "conclusion"))
                .isLessThan(indexOf(events, "evidence", "conclusion"));
        assertThat(indexOf(events, "section-chunk", "attention"))
                .isLessThan(indexOf(events, "evidence", "attention"));
    }

    @Test
    void productionTabUsesProductionAnalyticsOnly() {
        when(reportService.getProductionAnalyticsData(any())).thenReturn(productionAnalyticsFixture());
        doAnswer(invocation -> {
            Consumer<String> sink = invocation.getArgument(1);
            sink.accept("[SECTION:conclusion]产出表现平稳。[SECTION:reason]目标达成可控。");
            sink.accept("[SECTION:risk]存在低达成率批次。[SECTION:attention]持续观察风险批次。\n");
            return null;
        }).when(aiModelClient).stream(anyString(), any());

        List<ReportAiStreamEventVO> events = new ArrayList<>();
        service.stream(request("production"), events::add, () -> {}, throwable -> {
            throw new AssertionError("unexpected error", throwable);
        });

        verify(reportService).getProductionAnalyticsData(any());
        verify(reportService, never()).getTaskAnalyticsData(any());
        verify(reportService, never()).getCostAnalyticsData(any());
        assertThat(events).extracting(ReportAiStreamEventVO::getType).contains("done");
    }

    @Test
    void costTabUsesCostAnalyticsOnly() {
        when(reportService.getCostAnalyticsData(any())).thenReturn(costAnalyticsFixture());
        doAnswer(invocation -> {
            Consumer<String> sink = invocation.getArgument(1);
            sink.accept("[SECTION:conclusion]成本总体平稳。[SECTION:reason]采购节奏正常。");
            sink.accept("[SECTION:risk]个别物资成本偏高。[SECTION:attention]建议持续观察异常物资。\n");
            return null;
        }).when(aiModelClient).stream(anyString(), any());

        service.stream(request("cost"), event -> {}, () -> {}, throwable -> {
            throw new AssertionError("unexpected error", throwable);
        });

        verify(reportService).getCostAnalyticsData(any());
        verify(reportService, never()).getTaskAnalyticsData(any());
        verify(reportService, never()).getProductionAnalyticsData(any());
    }

    @Test
    void insufficientDataPromptExplicitlyRequiresDataInsufficientWording() {
        when(reportService.getTaskAnalyticsData(any())).thenReturn(new TaskAnalyticsVO());
        doAnswer(invocation -> null).when(aiModelClient).stream(anyString(), any());

        service.stream(request("task"), event -> {}, () -> {}, throwable -> {
            throw new AssertionError("unexpected error", throwable);
        });

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiModelClient).stream(promptCaptor.capture(), any());
        assertThat(promptCaptor.getValue())
                .contains("2026-03-01")
                .contains("2026-03-31")
                .contains("数据不足")
                .contains("关注 / 复核 / 持续观察")
                .contains("[SECTION:conclusion]")
                .contains("避免输出无法从分析数据直接支持的业务动作");
    }

    private int indexOf(List<ReportAiStreamEventVO> events, String type, String section) {
        for (int i = 0; i < events.size(); i++) {
            ReportAiStreamEventVO event = events.get(i);
            if (type.equals(event.getType()) && section.equals(event.getSection())) {
                return i;
            }
        }
        return -1;
    }

    private ReportAiSummaryRequestDTO request(String currentTab) {
        ReportAnalyticsFilterDTO filters = new ReportAnalyticsFilterDTO();
        filters.setStartDate(LocalDate.of(2026, 3, 1));
        filters.setEndDate(LocalDate.of(2026, 3, 31));
        filters.setGranularity("day");

        ReportAiSummaryRequestDTO request = new ReportAiSummaryRequestDTO();
        request.setCurrentTab(currentTab);
        request.setFilters(filters);
        return request;
    }

    private TaskAnalyticsVO taskAnalyticsFixture() {
        TaskAnalyticsVO analytics = new TaskAnalyticsVO();
        TaskAnalyticsVO.TrendVO trend = new TaskAnalyticsVO.TrendVO();
        trend.setLabels(List.of("2026-03-01", "2026-03-02"));
        trend.setCompleted(List.of(5, 8));
        trend.setOverdue(List.of(2, 3));
        analytics.setTrend(trend);

        TaskAnalyticsVO.AbnormalTaskItemVO abnormalTask = new TaskAnalyticsVO.AbnormalTaskItemVO();
        abnormalTask.setTaskName("灌溉巡检");
        analytics.setAbnormalTasks(List.of(abnormalTask));
        return analytics;
    }

    private ProductionAnalyticsVO productionAnalyticsFixture() {
        ProductionAnalyticsVO analytics = new ProductionAnalyticsVO();
        ProductionAnalyticsVO.RiskBatchItemVO riskBatch = new ProductionAnalyticsVO.RiskBatchItemVO();
        riskBatch.setBatchNo("B-2026-01");
        riskBatch.setFarmlandName("一号地块");
        riskBatch.setAchievementRate(new BigDecimal("72.5"));
        analytics.setRiskBatches(List.of(riskBatch));
        return analytics;
    }

    private CostAnalyticsVO costAnalyticsFixture() {
        CostAnalyticsVO analytics = new CostAnalyticsVO();
        CostAnalyticsVO.AbnormalCostItemVO abnormalCost = new CostAnalyticsVO.AbnormalCostItemVO();
        abnormalCost.setMaterialName("生物肥");
        abnormalCost.setSupplierName("绿源供应");
        abnormalCost.setCost(new BigDecimal("1888.50"));
        analytics.setAbnormalCostItems(List.of(abnormalCost));
        return analytics;
    }
}
