package lizhuoer.agri.agri_system.module.report.controller;

import lizhuoer.agri.agri_system.common.exception.GlobalExceptionHandler;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.report.service.IReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportControllerTest {

    private IReportService reportService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reportService = mock(IReportService.class);
        ReportController controller = new ReportController();
        ReflectionTestUtils.setField(controller, "reportService", reportService);
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
    void dashboardShouldReturnAggregatedData() throws Exception {
        Map<String, Object> dashboardData = Map.of(
                "cropDistribution", List.of(Map.of("name", "Rice", "value", 5)),
                "taskTrend", Map.of("xAxis", List.of("2026-03-08"), "series", List.of(3)),
                "envMonitor", List.of(Map.of("sensorType", "TEMP", "avgVal", 25.5))
        );
        when(reportService.getDashboardData()).thenReturn(dashboardData);

        mockMvc.perform(get("/report/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.cropDistribution").isArray())
                .andExpect(jsonPath("$.data.taskTrend").isMap())
                .andExpect(jsonPath("$.data.envMonitor").isArray());

        verify(reportService).getDashboardData();
    }

    @Test
    void dashboardShouldHandleEmptyData() throws Exception {
        when(reportService.getDashboardData()).thenReturn(Map.of());

        mockMvc.perform(get("/report/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isMap());
    }
}
