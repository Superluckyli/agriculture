package lizhuoer.agri.agri_system.module.iot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.exception.GlobalExceptionHandler;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.iot.domain.AgriTaskRule;
import lizhuoer.agri.agri_system.module.iot.service.IAgriTaskRuleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgriTaskRuleControllerTest {

    private IAgriTaskRuleService ruleService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ruleService = mock(IAgriTaskRuleService.class);
        AgriTaskRuleController controller = new AgriTaskRuleController();
        ReflectionTestUtils.setField(controller, "ruleService", ruleService);
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
    void listShouldReturnPage() throws Exception {
        Page<AgriTaskRule> page = new Page<>(1, 10);
        page.setRecords(List.of(sampleRule(1L)));
        page.setTotal(1);
        when(ruleService.page(any(Page.class))).thenReturn(page);

        mockMvc.perform(get("/iot/rule/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void addShouldSucceed() throws Exception {
        String body = """
                { "ruleName": "温度预警", "sensorType": "TEMP", "minVal": 5.0, "maxVal": 40.0, "isEnable": 1 }
                """;

        mockMvc.perform(post("/iot/rule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(ruleService).save(any(AgriTaskRule.class));
    }

    @Test
    void editShouldSucceed() throws Exception {
        String body = """
                { "ruleId": 1, "ruleName": "温度预警", "sensorType": "TEMP", "maxVal": 45.0 }
                """;

        mockMvc.perform(put("/iot/rule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(ruleService).updateById(any(AgriTaskRule.class));
    }

    @Test
    void removeShouldSucceed() throws Exception {
        when(ruleService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/iot/rule/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(ruleService).removeById(eq(1L));
    }

    private static AgriTaskRule sampleRule(Long id) {
        AgriTaskRule r = new AgriTaskRule();
        r.setRuleId(id);
        r.setSensorType("TEMP");
        r.setMinVal(BigDecimal.valueOf(5));
        r.setMaxVal(BigDecimal.valueOf(40));
        r.setIsEnable(1);
        return r;
    }
}
