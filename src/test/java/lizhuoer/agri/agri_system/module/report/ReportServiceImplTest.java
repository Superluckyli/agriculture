package lizhuoer.agri.agri_system.module.report;

import lizhuoer.agri.agri_system.module.crop.batch.mapper.AgriCropBatchMapper;
import lizhuoer.agri.agri_system.module.material.mapper.MaterialInfoMapper;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsFilterDTO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsOverviewVO;
import lizhuoer.agri.agri_system.module.report.domain.TaskAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.mapper.ReportMapper;
import lizhuoer.agri.agri_system.module.report.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private AgriCropBatchMapper cropBatchMapper;

    @Mock
    private MaterialInfoMapper materialInfoMapper;

    @InjectMocks
    private ReportServiceImpl service;

    @Test
    void buildsOverviewKpisFromMapperResults() {
        when(reportMapper.countAnalyticsTaskSummary(any(), any(), any(), any(), any()))
                .thenReturn(row(
                        "assignedCount", 10,
                        "completedCount", 8,
                        "onTimeCompletedCount", 6,
                        "overdueCount", 2));
        when(reportMapper.countAnalyticsActiveBatchSummary(any(), any()))
                .thenReturn(row("activeBatchCount", 5));
        when(reportMapper.sumAnalyticsOutputSummary(any(), any()))
                .thenReturn(row(
                        "targetTotal", new BigDecimal("100"),
                        "actualTotal", new BigDecimal("85")));
        when(reportMapper.sumAnalyticsCostSummary(any(), any(), any(), any()))
                .thenReturn(row(
                        "purchaseAmount", new BigDecimal("1200"),
                        "materialCost", new BigDecimal("680")));

        ReportAnalyticsOverviewVO result = service.getAnalyticsOverviewData(filter());

        assertThat(result.getFilterContext().getStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(result.getKpis().getTaskCompletionRate()).isEqualByComparingTo("80.0");
        assertThat(result.getKpis().getOnTimeExecutionRate()).isEqualByComparingTo("75.0");
        assertThat(result.getKpis().getOverdueTaskCount()).isEqualTo(2);
        assertThat(result.getKpis().getActiveBatchCount()).isEqualTo(5);
        assertThat(result.getKpis().getOutputAchievementRate()).isEqualByComparingTo("85.0");
        assertThat(result.getKpis().getPurchaseAmount()).isEqualByComparingTo("1200");
        assertThat(result.getKpis().getMaterialCost()).isEqualByComparingTo("680");
        assertThat(result.getKpis().getUpdatedAt()).isNotBlank();
    }

    @Test
    void buildsTaskAnalyticsDataFromMapperResults() {
        when(reportMapper.countTaskCreatedTrend(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(
                        row("label", "2026-03-01", "count", 3),
                        row("label", "2026-03-02", "count", 1)));
        when(reportMapper.countTaskCompletedTrend(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(row("label", "2026-03-02", "count", 2)));
        when(reportMapper.countTaskOverdueTrend(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(row("label", "2026-03-01", "count", 1)));
        when(reportMapper.countTaskStatusDistribution(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(
                        row("label", "2026-03-01", "status", "created", "count", 2),
                        row("label", "2026-03-01", "status", "completed", "count", 1),
                        row("label", "2026-03-02", "status", "in_progress", "count", 1)));
        when(reportMapper.listTaskAssigneeRanking(any(), any(), any(), any(), any()))
                .thenReturn(List.of(row(
                        "assigneeId", 4L,
                        "assigneeName", "王工人",
                        "assignedCount", 4,
                        "completedCount", 3,
                        "onTimeCompletedCount", 2,
                        "overdueCount", 1)));
        when(reportMapper.listAbnormalTasks(any(), any(), any(), any(), any()))
                .thenReturn(List.of(row(
                        "taskId", 11L,
                        "taskName", "旱地排水沟疏通",
                        "assigneeName", "赵工人",
                        "statusV2", "overdue",
                        "deadlineAt", "2026-02-05 18:00:00",
                        "riskLevel", "LOW",
                        "overdueDays", 3)));

        TaskAnalyticsVO result = service.getTaskAnalyticsData(filter());

        assertThat(result.getTrend().getLabels()).containsExactly("2026-03-01", "2026-03-02");
        assertThat(result.getTrend().getCreated()).containsExactly(3, 1);
        assertThat(result.getTrend().getCompleted()).containsExactly(0, 2);
        assertThat(result.getTrend().getOverdue()).containsExactly(1, 0);

        Map<String, List<Integer>> seriesByStatus = result.getStatusDistribution().getSeries().stream()
                .collect(Collectors.toMap(TaskAnalyticsVO.StatusSeriesItemVO::getName, TaskAnalyticsVO.StatusSeriesItemVO::getData));
        assertThat(result.getStatusDistribution().getLabels()).containsExactly("2026-03-01", "2026-03-02");
        assertThat(seriesByStatus.get("created")).containsExactly(2, 0);
        assertThat(seriesByStatus.get("completed")).containsExactly(1, 0);
        assertThat(seriesByStatus.get("in_progress")).containsExactly(0, 1);

        assertThat(result.getAssigneeRanking()).hasSize(1);
        assertThat(result.getAssigneeRanking().get(0).getAssigneeName()).isEqualTo("王工人");
        assertThat(result.getAssigneeRanking().get(0).getCompletionRate()).isEqualByComparingTo("75.0");
        assertThat(result.getAssigneeRanking().get(0).getOnTimeRate()).isEqualByComparingTo("66.7");
        assertThat(result.getAssigneeRanking().get(0).getOverdueRate()).isEqualByComparingTo("25.0");

        assertThat(result.getAbnormalTasks()).hasSize(1);
        assertThat(result.getAbnormalTasks().get(0).getTaskName()).isEqualTo("旱地排水沟疏通");
        assertThat(result.getAbnormalTasks().get(0).getOverdueDays()).isEqualTo(3);
    }

    private ReportAnalyticsFilterDTO filter() {
        ReportAnalyticsFilterDTO filter = new ReportAnalyticsFilterDTO();
        filter.setStartDate(LocalDate.of(2026, 3, 1));
        filter.setEndDate(LocalDate.of(2026, 3, 31));
        filter.setGranularity("day");
        return filter;
    }

    private Map<String, Object> row(Object... pairs) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            row.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return row;
    }
}
