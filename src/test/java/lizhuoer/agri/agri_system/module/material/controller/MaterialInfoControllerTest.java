package lizhuoer.agri.agri_system.module.material.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.exception.GlobalExceptionHandler;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInfo;
import lizhuoer.agri.agri_system.module.material.service.IMaterialInfoService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaterialInfoControllerTest {

    private IMaterialInfoService materialInfoService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        materialInfoService = mock(IMaterialInfoService.class);
        MaterialInfoController controller = new MaterialInfoController();
        ReflectionTestUtils.setField(controller, "materialInfoService", materialInfoService);
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
        Page<MaterialInfo> page = new Page<>(1, 10);
        page.setRecords(List.of(sampleMaterial(1L, "Fertilizer-A")));
        page.setTotal(1);
        when(materialInfoService.listPage(any(Page.class), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/material/info/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void lowStockShouldReturnList() throws Exception {
        when(materialInfoService.listLowStock()).thenReturn(List.of(sampleMaterial(1L, "Low-Stock-Item")));

        mockMvc.perform(get("/material/info/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void allShouldReturnFullList() throws Exception {
        when(materialInfoService.listAll()).thenReturn(List.of(sampleMaterial(1L, "M-A"), sampleMaterial(2L, "M-B")));

        mockMvc.perform(get("/material/info/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void addShouldSucceed() throws Exception {
        String body = """
                { "name": "New Fertilizer", "category": "Fertilizer", "unit": "kg" }
                """;

        mockMvc.perform(post("/material/info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(materialInfoService).addMaterial(any(MaterialInfo.class));
    }

    @Test
    void editShouldSucceed() throws Exception {
        String body = """
                { "materialId": 1, "name": "Updated Fertilizer", "unit": "kg" }
                """;

        mockMvc.perform(put("/material/info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(materialInfoService).updateMaterial(any(MaterialInfo.class));
    }

    @Test
    void removeShouldSucceed() throws Exception {
        mockMvc.perform(delete("/material/info/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(materialInfoService).deleteMaterial(any());
    }

    private static MaterialInfo sampleMaterial(Long id, String name) {
        MaterialInfo m = new MaterialInfo();
        m.setMaterialId(id);
        m.setName(name);
        m.setCurrentStock(BigDecimal.valueOf(100));
        return m;
    }
}
