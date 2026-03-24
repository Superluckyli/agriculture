package lizhuoer.agri.agri_system.module.report;

import lizhuoer.agri.agri_system.common.exception.GlobalExceptionHandler;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.report.controller.ReportController;
import lizhuoer.agri.agri_system.module.report.domain.CostAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.DashboardV2VO;
import lizhuoer.agri.agri_system.module.report.domain.ProductionAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAiStreamEventVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsFilterDTO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsOverviewVO;
import lizhuoer.agri.agri_system.module.report.domain.TaskAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.service.IReportAiSummaryService;
import lizhuoer.agri.agri_system.module.report.service.IReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportAiSummaryControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReportController controller = new ReportController();
        ReflectionTestUtils.setField(controller, "reportService", new StubReportService());
        ReflectionTestUtils.setField(controller, "reportAiSummaryService", new StubReportAiSummaryService());
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
    void aiSummaryStreamEndpointStartsAsyncAndEmitsSectionEvents() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/report/analytics/ai-summary/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"currentTab":"task","filters":{"startDate":"2026-03-01","endDate":"2026-03-31"}}
                        """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("section-start")))
                .andExpect(content().string(containsString("done")));
    }

    private static final class StubReportAiSummaryService implements IReportAiSummaryService {
        @Override
        public void stream(lizhuoer.agri.agri_system.module.report.domain.ReportAiSummaryRequestDTO request,
                           java.util.function.Consumer<ReportAiStreamEventVO> eventConsumer) {
            ReportAiStreamEventVO sectionStart = new ReportAiStreamEventVO();
            sectionStart.setType("section-start");
            sectionStart.setSection(request.getCurrentTab());
            eventConsumer.accept(sectionStart);

            ReportAiStreamEventVO done = new ReportAiStreamEventVO();
            done.setType("done");
            done.setSection(request.getCurrentTab());
            eventConsumer.accept(done);
        }
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
            return new ReportAnalyticsOverviewVO();
        }

        @Override
        public TaskAnalyticsVO getTaskAnalyticsData(ReportAnalyticsFilterDTO filter) {
            return new TaskAnalyticsVO();
        }

        @Override
        public ProductionAnalyticsVO getProductionAnalyticsData(ReportAnalyticsFilterDTO filter) {
            return new ProductionAnalyticsVO();
        }

        @Override
        public CostAnalyticsVO getCostAnalyticsData(ReportAnalyticsFilterDTO filter) {
            return new CostAnalyticsVO();
        }
    }
}
