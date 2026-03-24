package lizhuoer.agri.agri_system.module.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lizhuoer.agri.agri_system.common.exception.GlobalExceptionHandler;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.report.controller.ReportController;
import lizhuoer.agri.agri_system.module.report.domain.CostAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.DashboardV2VO;
import lizhuoer.agri.agri_system.module.report.domain.ProductionAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAiStreamEventVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAiSummaryRequestDTO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsFilterDTO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsOverviewVO;
import lizhuoer.agri.agri_system.module.report.domain.TaskAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.service.IReportAiSummaryService;
import lizhuoer.agri.agri_system.module.report.service.IReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportAiSummaryControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReportController controller = new ReportController();
        ReflectionTestUtils.setField(controller, "reportService", new StubReportService());
        ReflectionTestUtils.setField(controller, "reportAiSummaryServiceProvider",
                providerFor(new StubReportAiSummaryService()));
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
    void aiSummaryStreamEndpointStartsAsyncAndEmitsTypedEvents() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/report/analytics/ai-summary/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"currentTab":"task","filters":{"startDate":"2026-03-01","endDate":"2026-03-31"}}
                        """))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult response = mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        List<JsonNode> events = parseDataEvents(response.getResponse().getContentAsString());
        assertThat(events).hasSize(2);
        assertThat(events.get(0).path("type").asText()).isEqualTo("section-start");
        assertThat(events.get(0).path("section").asText()).isEqualTo("task");
        assertThat(events.get(0).path("summary").isMissingNode()).isFalse();
        assertThat(events.get(0).path("summary").isNull()).isTrue();
        assertThat(events.get(0).path("evidence").isMissingNode()).isFalse();
        assertThat(events.get(0).path("evidence").isNull()).isTrue();
        assertThat(events.get(1).path("type").asText()).isEqualTo("done");
        assertThat(events.get(1).path("section").asText()).isEqualTo("task");
        assertThat(events.get(1).path("summary").isNull()).isTrue();
        assertThat(events.get(1).path("evidence").isNull()).isTrue();
    }

    @Test
    void aiSummaryStreamRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/report/analytics/ai-summary/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"currentTab":" ","filters":null}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.msg", containsString("currentTab")))
                .andExpect(jsonPath("$.msg", containsString("filters")));
    }

    @Test
    void aiSummaryStreamRejectsUnsupportedCurrentTab() throws Exception {
        mockMvc.perform(post("/report/analytics/ai-summary/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"currentTab":"overview","filters":{"startDate":"2026-03-01","endDate":"2026-03-31"}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg", containsString("currentTab")));
    }

    @Test
    void aiSummaryStreamFailsWhenServiceIsUnavailable() throws Exception {
        ReportController controller = new ReportController();
        ReflectionTestUtils.setField(controller, "reportService", new StubReportService());
        ReflectionTestUtils.setField(controller, "reportAiSummaryServiceProvider", providerFor(null));
        MockMvc missingServiceMockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        missingServiceMockMvc.perform(post("/report/analytics/ai-summary/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"currentTab":"task","filters":{"startDate":"2026-03-01","endDate":"2026-03-31"}}
                        """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.msg", containsString("系统异常")));
    }

    @Test
    void aiSummaryStreamEmitsFinalEvidenceAndDoneBeforeTransportClosure() throws Exception {
        ReportController controller = new ReportController();
        ReflectionTestUtils.setField(controller, "reportService", new StubReportService());
        ReflectionTestUtils.setField(controller, "reportAiSummaryServiceProvider",
                providerFor(new OrderedStubReportAiSummaryService()));
        MockMvc orderedMockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        MvcResult mvcResult = orderedMockMvc.perform(post("/report/analytics/ai-summary/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"currentTab":"task","filters":{"startDate":"2026-03-01","endDate":"2026-03-31"}}
                        """))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult response = orderedMockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        List<JsonNode> events = parseDataEvents(response.getResponse().getContentAsString());
        assertThat(events).extracting(event -> event.path("type").asText())
                .containsExactly("section-start", "evidence", "done");
        assertThat(events.get(1).path("section").asText()).isEqualTo("task");
        assertThat(events.get(1).path("evidence").isArray()).isTrue();
        assertThat(events.get(2).path("type").asText()).isEqualTo("done");
    }

    private List<JsonNode> parseDataEvents(String body) {
        return body.lines()
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring("data:".length()).trim())
                .map(this::readJson)
                .collect(Collectors.toList());
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new AssertionError("Failed to parse SSE payload: " + json, e);
        }
    }

    private ObjectProvider<IReportAiSummaryService> providerFor(IReportAiSummaryService service) {
        return new ObjectProvider<>() {
            @Override
            public IReportAiSummaryService getObject(Object... args) {
                return service;
            }

            @Override
            public IReportAiSummaryService getIfAvailable() {
                return service;
            }

            @Override
            public IReportAiSummaryService getIfUnique() {
                return service;
            }

            @Override
            public IReportAiSummaryService getObject() {
                return service;
            }
        };
    }

    private static final class StubReportAiSummaryService implements IReportAiSummaryService {
        @Override
        public void stream(ReportAiSummaryRequestDTO request,
                           Consumer<ReportAiStreamEventVO> eventConsumer,
                           Runnable completionCallback,
                           Consumer<Throwable> errorCallback) {
            ReportAiStreamEventVO sectionStart = new ReportAiStreamEventVO();
            sectionStart.setType("section-start");
            sectionStart.setSection(request.getCurrentTab());
            eventConsumer.accept(sectionStart);

            ReportAiStreamEventVO done = new ReportAiStreamEventVO();
            done.setType("done");
            done.setSection(request.getCurrentTab());
            eventConsumer.accept(done);
            completionCallback.run();
        }
    }

    private static final class OrderedStubReportAiSummaryService implements IReportAiSummaryService {
        @Override
        public void stream(ReportAiSummaryRequestDTO request,
                           Consumer<ReportAiStreamEventVO> eventConsumer,
                           Runnable completionCallback,
                           Consumer<Throwable> errorCallback) {
            ReportAiStreamEventVO sectionStart = new ReportAiStreamEventVO();
            sectionStart.setType("section-start");
            sectionStart.setSection(request.getCurrentTab());
            eventConsumer.accept(sectionStart);

            ReportAiStreamEventVO evidence = new ReportAiStreamEventVO();
            evidence.setType("evidence");
            evidence.setSection(request.getCurrentTab());
            evidence.setEvidence(List.of());
            eventConsumer.accept(evidence);

            ReportAiStreamEventVO done = new ReportAiStreamEventVO();
            done.setType("done");
            done.setSection(request.getCurrentTab());
            eventConsumer.accept(done);

            completionCallback.run();
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
