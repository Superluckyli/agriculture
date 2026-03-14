package lizhuoer.agri.agri_system.module.crop.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.exception.GlobalExceptionHandler;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.crop.domain.BaseCropVariety;
import lizhuoer.agri.agri_system.module.crop.service.IBaseCropVarietyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BaseCropVarietyControllerTest {

    private IBaseCropVarietyService varietyService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        varietyService = mock(IBaseCropVarietyService.class);
        BaseCropVarietyController controller = new BaseCropVarietyController();
        ReflectionTestUtils.setField(controller, "varietyService", varietyService);
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
        Page<BaseCropVariety> page = new Page<>(1, 10);
        page.setRecords(List.of(sampleVariety(1L, "Rice")));
        page.setTotal(1);
        when(varietyService.page(any(Page.class), any(Wrapper.class))).thenReturn(page);

        mockMvc.perform(get("/crop/variety/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void allShouldReturnFullList() throws Exception {
        when(varietyService.listAll()).thenReturn(List.of(sampleVariety(1L, "Rice"), sampleVariety(2L, "Wheat")));

        mockMvc.perform(get("/crop/variety/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void addShouldSucceed() throws Exception {
        String body = """
                { "cropName": "Corn", "growthCycleDays": 120 }
                """;

        mockMvc.perform(post("/crop/variety")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(varietyService).save(any(BaseCropVariety.class));
    }

    @Test
    void editShouldSucceed() throws Exception {
        String body = """
                { "varietyId": 1, "cropName": "Corn Updated" }
                """;

        mockMvc.perform(put("/crop/variety")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(varietyService).updateById(any(BaseCropVariety.class));
    }

    @Test
    void removeShouldSucceed() throws Exception {
        mockMvc.perform(delete("/crop/variety/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(varietyService).deleteVarieties(any());
    }

    private static BaseCropVariety sampleVariety(Long id, String name) {
        BaseCropVariety v = new BaseCropVariety();
        v.setVarietyId(id);
        v.setCropName(name);
        return v;
    }
}
