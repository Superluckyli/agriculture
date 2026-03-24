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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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
        ArgumentCaptorHolder.prompt = null;
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
            Consumer<String> onDelta = invocation.getArgument(1);
            Runnable onComplete = invocation.getArgument(2);
            onDelta.accept("[SECTION:conclusion]任务执行整体稳定。");
            onDelta.accept("[SECTION:reason]完成趋势回升。");
            onDelta.accept("[SECTION:risk]逾期仍集中。");
            onDelta.accept("[SECTION:attention]建议关注高压执行人。");
            onComplete.run();
            return null;
        }).when(aiModelClient).stream(anyString(), any(), any(), any());

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
            Consumer<String> onDelta = invocation.getArgument(1);
            Runnable onComplete = invocation.getArgument(2);
            onDelta.accept("[SECTION:conclusion]产出表现平稳。[SECTION:reason]目标达成可控。");
            onDelta.accept("[SECTION:risk]存在低达成率批次。[SECTION:attention]持续观察风险批次。\n");
            onComplete.run();
            return null;
        }).when(aiModelClient).stream(anyString(), any(), any(), any());

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
            Consumer<String> onDelta = invocation.getArgument(1);
            Runnable onComplete = invocation.getArgument(2);
            onDelta.accept("[SECTION:conclusion]成本总体平稳。[SECTION:reason]采购节奏正常。");
            onDelta.accept("[SECTION:risk]个别物资成本偏高。[SECTION:attention]建议持续观察异常物资。\n");
            onComplete.run();
            return null;
        }).when(aiModelClient).stream(anyString(), any(), any(), any());

        service.stream(request("cost"), event -> {}, () -> {}, throwable -> {
            throw new AssertionError("unexpected error", throwable);
        });

        verify(reportService).getCostAnalyticsData(any());
        verify(reportService, never()).getTaskAnalyticsData(any());
        verify(reportService, never()).getProductionAnalyticsData(any());
    }

    @Test
    void completionDoesNotFireUntilProviderSignalsStreamCompletion() {
        when(reportService.getTaskAnalyticsData(any())).thenReturn(taskAnalyticsFixture());
        AtomicReference<Runnable> onCompleteRef = new AtomicReference<>();
        AtomicReference<Consumer<Throwable>> onErrorRef = new AtomicReference<>();
        doAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(1);
            onCompleteRef.set(invocation.getArgument(2));
            onErrorRef.set(invocation.getArgument(3));
            onDelta.accept("[SECTION:conclusion]任务执行整体稳定。");
            return null;
        }).when(aiModelClient).stream(anyString(), any(), any(), any());

        List<ReportAiStreamEventVO> events = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        service.stream(request("task"), events::add, () -> completed.set(true), errorRef::set);

        assertThat(completed).isFalse();
        assertThat(errorRef.get()).isNull();
        assertThat(events).extracting(ReportAiStreamEventVO::getType).doesNotContain("done", "evidence");

        onCompleteRef.get().run();

        assertThat(completed).isTrue();
        assertThat(errorRef.get()).isNull();
        assertThat(events).extracting(ReportAiStreamEventVO::getType).contains("evidence", "done");

        onErrorRef.get().accept(new IllegalStateException("late error"));
        assertThat(errorRef.get()).isNull();
    }

    @Test
    void providerErrorCallbackTerminatesWithoutCompletion() {
        when(reportService.getTaskAnalyticsData(any())).thenReturn(taskAnalyticsFixture());
        IllegalStateException providerFailure = new IllegalStateException("provider failed");
        doAnswer(invocation -> {
            Consumer<Throwable> onError = invocation.getArgument(3);
            onError.accept(providerFailure);
            return null;
        }).when(aiModelClient).stream(anyString(), any(), any(), any());

        List<ReportAiStreamEventVO> events = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        service.stream(request("task"), events::add, () -> completed.set(true), errorRef::set);

        assertThat(completed).isFalse();
        assertThat(errorRef.get()).isSameAs(providerFailure);
        assertThat(events).isEmpty();
    }

    @Test
    void providerCompletionThenSynchronousThrowOnlyTerminatesOnce() {
        when(reportService.getTaskAnalyticsData(any())).thenReturn(taskAnalyticsFixture());
        RuntimeException thrown = new RuntimeException("after complete");
        AtomicInteger completionCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        doAnswer(invocation -> {
            Runnable onComplete = invocation.getArgument(2);
            onComplete.run();
            throw thrown;
        }).when(aiModelClient).stream(anyString(), any(), any(), any());

        service.stream(
                request("task"),
                event -> {},
                completionCount::incrementAndGet,
                throwable -> {
                    errorCount.incrementAndGet();
                    errorRef.set(throwable);
                }
        );

        assertThat(completionCount.get()).isEqualTo(1);
        assertThat(errorCount.get()).isZero();
        assertThat(errorRef.get()).isNull();
    }

    @Test
    void downstreamConsumerThrowDuringParserFinishRoutesErrorOnce() {
        when(reportService.getTaskAnalyticsData(any())).thenReturn(taskAnalyticsFixture());
        AtomicInteger completionCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        List<String> eventTypes = new ArrayList<>();
        doAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(1);
            Runnable onComplete = invocation.getArgument(2);
            Consumer<Throwable> onError = invocation.getArgument(3);
            onDelta.accept("[SECTION:conclusion]收尾文本");
            onComplete.run();
            onDelta.accept("[SECTION:reason]late delta should be ignored");
            onError.accept(new IllegalStateException("late error should be ignored"));
            return null;
        }).when(aiModelClient).stream(anyString(), any(), any(), any());

        service.stream(
                request("task"),
                event -> {
                    eventTypes.add(event.getType());
                    if ("section-chunk".equals(event.getType()) && "收尾文本".equals(event.getSummary())) {
                        throw new IllegalStateException("flush failure");
                    }
                },
                completionCount::incrementAndGet,
                throwable -> {
                    errorCount.incrementAndGet();
                    errorRef.set(throwable);
                }
        );

        assertThat(completionCount.get()).isZero();
        assertThat(errorCount.get()).isEqualTo(1);
        assertThat(errorRef.get()).hasMessage("flush failure");
        assertThat(eventTypes).containsExactly("section-start", "section-chunk");
    }

    @Test
    void completionCallbackThrowDoesNotLeakDoneAndRoutesErrorOnce() {
        when(reportService.getTaskAnalyticsData(any())).thenReturn(taskAnalyticsFixture());
        List<ReportAiStreamEventVO> events = new ArrayList<>();
        AtomicInteger errorCount = new AtomicInteger();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        doAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(1);
            Runnable onComplete = invocation.getArgument(2);
            Consumer<Throwable> onError = invocation.getArgument(3);
            onDelta.accept("[SECTION:conclusion]完成前内容");
            onComplete.run();
            onError.accept(new IllegalStateException("late error should be ignored"));
            return null;
        }).when(aiModelClient).stream(anyString(), any(), any(), any());

        service.stream(
                request("task"),
                events::add,
                () -> {
                    throw new IllegalStateException("completion failed");
                },
                throwable -> {
                    errorCount.incrementAndGet();
                    errorRef.set(throwable);
                }
        );

        assertThat(errorCount.get()).isEqualTo(1);
        assertThat(errorRef.get()).hasMessage("completion failed");
        assertThat(events).extracting(ReportAiStreamEventVO::getType)
                .containsExactly("section-start", "section-chunk");
    }

    @Test
    void lateDeltasAfterOnCompleteDoNotLeakPostTerminalEvents() {
        when(reportService.getTaskAnalyticsData(any())).thenReturn(taskAnalyticsFixture());
        List<ReportAiStreamEventVO> events = new ArrayList<>();
        AtomicInteger completionCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();
        doAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(1);
            Runnable onComplete = invocation.getArgument(2);
            onDelta.accept("[SECTION:conclusion]先完成。\n");
            onComplete.run();
            onDelta.accept("[SECTION:risk]不应再输出");
            return null;
        }).when(aiModelClient).stream(anyString(), any(), any(), any());

        service.stream(
                request("task"),
                events::add,
                completionCount::incrementAndGet,
                throwable -> errorCount.incrementAndGet()
        );

        assertThat(completionCount.get()).isEqualTo(1);
        assertThat(errorCount.get()).isZero();
        assertThat(events).extracting(ReportAiStreamEventVO::getType)
                .containsExactly("section-start", "section-chunk", "evidence", "done");
    }

    @Test
    void lateDeltasAfterOnErrorDoNotLeakPostTerminalEvents() {
        when(reportService.getTaskAnalyticsData(any())).thenReturn(taskAnalyticsFixture());
        List<ReportAiStreamEventVO> events = new ArrayList<>();
        AtomicInteger completionCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        doAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(1);
            Consumer<Throwable> onError = invocation.getArgument(3);
            onDelta.accept("[SECTION:conclusion]未终止前文本");
            onError.accept(new IllegalStateException("provider failed"));
            onDelta.accept("[SECTION:risk]不应再输出");
            return null;
        }).when(aiModelClient).stream(anyString(), any(), any(), any());

        service.stream(
                request("task"),
                events::add,
                completionCount::incrementAndGet,
                throwable -> {
                    errorCount.incrementAndGet();
                    errorRef.set(throwable);
                }
        );

        assertThat(completionCount.get()).isZero();
        assertThat(errorCount.get()).isEqualTo(1);
        assertThat(errorRef.get()).hasMessage("provider failed");
        assertThat(events).extracting(ReportAiStreamEventVO::getType)
                .containsExactly("section-start", "section-chunk");
    }

    @Test
    void providerSynchronousThrowTriggersErrorCallback() {
        when(reportService.getTaskAnalyticsData(any())).thenReturn(taskAnalyticsFixture());
        IllegalArgumentException thrown = new IllegalArgumentException("boom");
        doThrow(thrown).when(aiModelClient).stream(anyString(), any(), any(), any());

        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        service.stream(request("task"), event -> {}, () -> completed.set(true), errorRef::set);

        assertThat(completed).isFalse();
        assertThat(errorRef.get()).isSameAs(thrown);
    }

    @Test
    void emptySectionStillEmitsEvidenceOnCompletion() {
        when(reportService.getTaskAnalyticsData(any())).thenReturn(taskAnalyticsFixture());
        doAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(1);
            Runnable onComplete = invocation.getArgument(2);
            onDelta.accept("[SECTION:conclusion][SECTION:reason]原因说明。\n");
            onComplete.run();
            return null;
        }).when(aiModelClient).stream(anyString(), any(), any(), any());

        List<ReportAiStreamEventVO> events = new ArrayList<>();
        service.stream(request("task"), events::add, () -> {}, throwable -> {
            throw new AssertionError("unexpected error", throwable);
        });

        assertThat(events).extracting(ReportAiStreamEventVO::getType, ReportAiStreamEventVO::getSection)
                .contains(org.assertj.core.groups.Tuple.tuple("evidence", "conclusion"))
                .contains(org.assertj.core.groups.Tuple.tuple("evidence", "reason"));
    }

    @Test
    void dataSufficiencyPromptStaysAlignedWithRenderedTaskAnalytics() {
        TaskAnalyticsVO analytics = new TaskAnalyticsVO();
        TaskAnalyticsVO.AssigneeRankingItemVO assignee = new TaskAnalyticsVO.AssigneeRankingItemVO();
        assignee.setAssigneeName("王工人");
        assignee.setAssignedCount(4);
        analytics.setAssigneeRanking(List.of(assignee));
        analytics.setFilterContext(defaultedFilterContext());
        when(reportService.getTaskAnalyticsData(any())).thenReturn(analytics);
        doAnswer(invocation -> {
            ArgumentCaptorHolder.prompt = invocation.getArgument(0);
            return null;
        }).when(aiModelClient).stream(anyString(), any(), any(), any());

        service.stream(requestWithNullDates("task"), event -> {}, () -> {}, throwable -> {
            throw new AssertionError("unexpected error", throwable);
        });

        assertThat(ArgumentCaptorHolder.prompt)
                .contains("assigneeRanking")
                .contains("王工人")
                .contains(defaultedFilterContext().getStartDate().toString())
                .contains(defaultedFilterContext().getEndDate().toString())
                .contains("当前数据足够支撑总结")
                .doesNotContain("当前分析数据不足");
    }

    @Test
    void productionPromptAlignmentUsesProductionDataAndNormalizedFilters() {
        ProductionAnalyticsVO analytics = productionAnalyticsFixture();
        analytics.setFilterContext(defaultedFilterContext());
        when(reportService.getProductionAnalyticsData(any())).thenReturn(analytics);
        doAnswer(invocation -> {
            ArgumentCaptorHolder.prompt = invocation.getArgument(0);
            return null;
        }).when(aiModelClient).stream(anyString(), any(), any(), any());

        service.stream(requestWithNullDates("production"), event -> {}, () -> {}, throwable -> {
            throw new AssertionError("unexpected error", throwable);
        });

        assertThat(ArgumentCaptorHolder.prompt)
                .contains("cropDistribution")
                .contains("水稻")
                .contains(defaultedFilterContext().getStartDate().toString())
                .contains(defaultedFilterContext().getEndDate().toString())
                .contains("当前数据足够支撑总结");
    }

    @Test
    void insufficientDataPromptExplicitlyRequiresDataInsufficientWording() {
        TaskAnalyticsVO analytics = new TaskAnalyticsVO();
        analytics.setFilterContext(defaultedFilterContext());
        when(reportService.getTaskAnalyticsData(any())).thenReturn(analytics);
        doAnswer(invocation -> {
            ArgumentCaptorHolder.prompt = invocation.getArgument(0);
            return null;
        }).when(aiModelClient).stream(anyString(), any(), any(), any());

        service.stream(requestWithNullDates("task"), event -> {}, () -> {}, throwable -> {
            throw new AssertionError("unexpected error", throwable);
        });

        assertThat(ArgumentCaptorHolder.prompt)
                .contains(defaultedFilterContext().getStartDate().toString())
                .contains(defaultedFilterContext().getEndDate().toString())
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

    private ReportAiSummaryRequestDTO requestWithNullDates(String currentTab) {
        ReportAnalyticsFilterDTO filters = new ReportAnalyticsFilterDTO();
        filters.setGranularity(null);
        ReportAiSummaryRequestDTO request = new ReportAiSummaryRequestDTO();
        request.setCurrentTab(currentTab);
        request.setFilters(filters);
        return request;
    }

    private ReportAnalyticsFilterDTO defaultedFilterContext() {
        ReportAnalyticsFilterDTO filter = new ReportAnalyticsFilterDTO();
        filter.setStartDate(LocalDate.now().minusDays(29));
        filter.setEndDate(LocalDate.now());
        filter.setGranularity("day");
        return filter;
    }

    private TaskAnalyticsVO taskAnalyticsFixture() {
        TaskAnalyticsVO analytics = new TaskAnalyticsVO();
        TaskAnalyticsVO.TrendVO trend = new TaskAnalyticsVO.TrendVO();
        trend.setLabels(List.of("2026-03-01", "2026-03-02"));
        trend.setCreated(List.of(7, 9));
        trend.setCompleted(List.of(5, 8));
        trend.setOverdue(List.of(2, 3));
        analytics.setTrend(trend);
        analytics.setFilterContext(request("task").getFilters());

        TaskAnalyticsVO.AssigneeRankingItemVO assignee = new TaskAnalyticsVO.AssigneeRankingItemVO();
        assignee.setAssigneeName("李工人");
        assignee.setAssignedCount(10);
        analytics.setAssigneeRanking(List.of(assignee));

        TaskAnalyticsVO.AbnormalTaskItemVO abnormalTask = new TaskAnalyticsVO.AbnormalTaskItemVO();
        abnormalTask.setTaskName("灌溉巡检");
        analytics.setAbnormalTasks(List.of(abnormalTask));
        return analytics;
    }

    private ProductionAnalyticsVO productionAnalyticsFixture() {
        ProductionAnalyticsVO analytics = new ProductionAnalyticsVO();
        ProductionAnalyticsVO.CropDistributionItemVO crop = new ProductionAnalyticsVO.CropDistributionItemVO();
        crop.setCropVariety("水稻");
        crop.setBatchCount(2);
        analytics.setCropDistribution(List.of(crop));
        analytics.setFilterContext(request("production").getFilters());

        ProductionAnalyticsVO.RiskBatchItemVO riskBatch = new ProductionAnalyticsVO.RiskBatchItemVO();
        riskBatch.setBatchNo("B-2026-01");
        riskBatch.setFarmlandName("一号地块");
        riskBatch.setAchievementRate(new BigDecimal("72.5"));
        analytics.setRiskBatches(List.of(riskBatch));
        return analytics;
    }

    private CostAnalyticsVO costAnalyticsFixture() {
        CostAnalyticsVO analytics = new CostAnalyticsVO();
        CostAnalyticsVO.PurchaseTrendVO trend = new CostAnalyticsVO.PurchaseTrendVO();
        trend.setOrderCount(List.of(1, 2));
        trend.setAmount(List.of(new BigDecimal("88.00"), new BigDecimal("188.50")));
        analytics.setPurchaseTrend(trend);
        analytics.setFilterContext(request("cost").getFilters());

        CostAnalyticsVO.AbnormalCostItemVO abnormalCost = new CostAnalyticsVO.AbnormalCostItemVO();
        abnormalCost.setMaterialName("生物肥");
        abnormalCost.setSupplierName("绿源供应");
        abnormalCost.setCost(new BigDecimal("1888.50"));
        analytics.setAbnormalCostItems(List.of(abnormalCost));
        return analytics;
    }

    private static final class ArgumentCaptorHolder {
        private static String prompt;
    }
}
