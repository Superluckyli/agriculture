package lizhuoer.agri.agri_system.module.report;

import lizhuoer.agri.agri_system.common.exception.GlobalExceptionHandler;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.report.controller.ReportController;
import lizhuoer.agri.agri_system.module.report.domain.CostAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.DashboardV2VO;
import lizhuoer.agri.agri_system.module.report.domain.ProductionAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsFilterDTO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsOverviewVO;
import lizhuoer.agri.agri_system.module.report.domain.TaskAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.service.IReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportAnalyticsControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReportController controller = new ReportController();
        ReflectionTestUtils.setField(controller, "reportService", new StubReportService());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        LoginUserContext.set(new LoginUser(1L, "admin", Set.of("ADMIN")));
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void overviewEndpointReturnsFrozenContractShape() throws Exception {
        mockMvc.perform(get("/report/analytics/overview")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.filterContext.startDate").value("2026-03-01"))
                .andExpect(jsonPath("$.data.filterContext.endDate").value("2026-03-31"))
                .andExpect(jsonPath("$.data.kpis.taskCompletionRate").exists())
                .andExpect(jsonPath("$.data.kpis.materialCost").exists());
    }

    @Test
    void taskEndpointReturnsFrozenContractShape() throws Exception {
        mockMvc.perform(get("/report/analytics/task")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31")
                        .param("granularity", "week"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.filterContext.granularity").value("week"))
                .andExpect(jsonPath("$.data.trend.labels").isArray())
                .andExpect(jsonPath("$.data.statusDistribution.labels").isArray())
                .andExpect(jsonPath("$.data.assigneeRanking").isArray())
                .andExpect(jsonPath("$.data.abnormalTasks").isArray());
    }

    @Test
    void productionEndpointReturnsFrozenContractShape() throws Exception {
        mockMvc.perform(get("/report/analytics/production")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31")
                        .param("varietyId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.filterContext.varietyId").value(2))
                .andExpect(jsonPath("$.data.cropDistribution").isArray())
                .andExpect(jsonPath("$.data.outputComparison").isArray())
                .andExpect(jsonPath("$.data.harvestTrend.labels").isArray())
                .andExpect(jsonPath("$.data.riskBatches").isArray());
    }

    @Test
    void costEndpointReturnsFrozenContractShape() throws Exception {
        mockMvc.perform(get("/report/analytics/cost")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31")
                        .param("supplierId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.filterContext.supplierId").value(9))
                .andExpect(jsonPath("$.data.purchaseTrend.labels").isArray())
                .andExpect(jsonPath("$.data.materialCostTopN").isArray())
                .andExpect(jsonPath("$.data.categoryCostShare").isArray())
                .andExpect(jsonPath("$.data.abnormalCostItems").isArray());
    }

    private static final class StubReportService implements IReportService {
        @Override
        public Map<String, Object> getDashboardData() {
            return Map.of();
        }

        @Override
        public DashboardV2VO getDashboardV2Data() {
            return new DashboardV2VO();
        }

        @Override
        public ReportAnalyticsOverviewVO getAnalyticsOverviewData(ReportAnalyticsFilterDTO filter) {
            ReportAnalyticsOverviewVO vo = new ReportAnalyticsOverviewVO();
            vo.setFilterContext(filter);
            ReportAnalyticsOverviewVO.KpisVO kpis = new ReportAnalyticsOverviewVO.KpisVO();
            kpis.setTaskCompletionRate(BigDecimal.ZERO);
            kpis.setOnTimeExecutionRate(BigDecimal.ZERO);
            kpis.setOverdueTaskCount(0);
            kpis.setActiveBatchCount(0);
            kpis.setOutputAchievementRate(BigDecimal.ZERO);
            kpis.setPurchaseAmount(BigDecimal.ZERO);
            kpis.setMaterialCost(BigDecimal.ZERO);
            vo.setKpis(kpis);
            return vo;
        }

        @Override
        public TaskAnalyticsVO getTaskAnalyticsData(ReportAnalyticsFilterDTO filter) {
            TaskAnalyticsVO vo = new TaskAnalyticsVO();
            vo.setFilterContext(filter);

            TaskAnalyticsVO.TrendVO trend = new TaskAnalyticsVO.TrendVO();
            trend.setLabels(java.util.Collections.emptyList());
            trend.setCreated(java.util.Collections.emptyList());
            trend.setCompleted(java.util.Collections.emptyList());
            trend.setOverdue(java.util.Collections.emptyList());
            vo.setTrend(trend);

            TaskAnalyticsVO.StatusDistributionVO statusDistribution = new TaskAnalyticsVO.StatusDistributionVO();
            statusDistribution.setLabels(java.util.Collections.emptyList());
            statusDistribution.setSeries(java.util.Collections.emptyList());
            vo.setStatusDistribution(statusDistribution);
            vo.setAssigneeRanking(java.util.Collections.emptyList());
            vo.setAbnormalTasks(java.util.Collections.emptyList());
            return vo;
        }

        @Override
        public ProductionAnalyticsVO getProductionAnalyticsData(ReportAnalyticsFilterDTO filter) {
            ProductionAnalyticsVO vo = new ProductionAnalyticsVO();
            vo.setFilterContext(filter);
            vo.setCropDistribution(java.util.Collections.emptyList());
            vo.setOutputComparison(java.util.Collections.emptyList());

            ProductionAnalyticsVO.HarvestTrendVO harvestTrend = new ProductionAnalyticsVO.HarvestTrendVO();
            harvestTrend.setLabels(java.util.Collections.emptyList());
            harvestTrend.setBatchCount(java.util.Collections.emptyList());
            harvestTrend.setEstimatedOutput(java.util.Collections.emptyList());
            vo.setHarvestTrend(harvestTrend);
            vo.setRiskBatches(java.util.Collections.emptyList());
            return vo;
        }

        @Override
        public CostAnalyticsVO getCostAnalyticsData(ReportAnalyticsFilterDTO filter) {
            CostAnalyticsVO vo = new CostAnalyticsVO();
            vo.setFilterContext(filter);

            CostAnalyticsVO.PurchaseTrendVO purchaseTrend = new CostAnalyticsVO.PurchaseTrendVO();
            purchaseTrend.setLabels(java.util.Collections.emptyList());
            purchaseTrend.setAmount(java.util.Collections.emptyList());
            purchaseTrend.setOrderCount(java.util.Collections.emptyList());
            vo.setPurchaseTrend(purchaseTrend);
            vo.setMaterialCostTopN(java.util.Collections.emptyList());
            vo.setCategoryCostShare(java.util.Collections.emptyList());
            vo.setAbnormalCostItems(java.util.Collections.emptyList());
            return vo;
        }
    }
}
