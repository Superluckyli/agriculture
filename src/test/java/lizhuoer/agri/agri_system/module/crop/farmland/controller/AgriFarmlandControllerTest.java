package lizhuoer.agri.agri_system.module.crop.farmland.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.exception.GlobalExceptionHandler;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.crop.farmland.domain.AgriFarmland;
import lizhuoer.agri.agri_system.module.crop.farmland.service.IAgriFarmlandService;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgriFarmlandControllerTest {

    private IAgriFarmlandService farmlandService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        farmlandService = mock(IAgriFarmlandService.class);
        AgriFarmlandController controller = new AgriFarmlandController();
        ReflectionTestUtils.setField(controller, "farmlandService", farmlandService);
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
    void listShouldReturnPageWithDefaultParams() throws Exception {
        Page<AgriFarmland> page = new Page<>(1, 10);
        page.setRecords(List.of(sampleFarmland(1L, "Farm-A")));
        page.setTotal(1);
        when(farmlandService.listPage(any(Page.class), isNull(), isNull())).thenReturn(page);

        mockMvc.perform(get("/crop/farmland/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void listShouldFilterByName() throws Exception {
        Page<AgriFarmland> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(farmlandService.listPage(any(Page.class), eq("east"), isNull())).thenReturn(page);

        mockMvc.perform(get("/crop/farmland/list").param("name", "east"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(farmlandService).listPage(any(Page.class), eq("east"), isNull());
    }

    @Test
    void allShouldReturnFullList() throws Exception {
        when(farmlandService.listAll()).thenReturn(List.of(sampleFarmland(1L, "Farm-A"), sampleFarmland(2L, "Farm-B")));

        mockMvc.perform(get("/crop/farmland/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void addShouldSucceedWithValidBody() throws Exception {
        String body = """
                {
                  "name": "East Field",
                  "code": "EF-001",
                  "area": 15.5,
                  "location": "East zone",
                  "status": 1
                }
                """;

        mockMvc.perform(post("/crop/farmland")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(farmlandService).addFarmland(any(AgriFarmland.class));
    }

    @Test
    void editShouldSucceedWithValidBody() throws Exception {
        String body = """
                {
                  "id": 1,
                  "name": "East Field Updated",
                  "code": "EF-001",
                  "area": 20.0,
                  "status": 1
                }
                """;

        mockMvc.perform(put("/crop/farmland")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(farmlandService).updateFarmland(any(AgriFarmland.class));
    }

    @Test
    void removeShouldSucceedWithIds() throws Exception {
        mockMvc.perform(delete("/crop/farmland/1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(farmlandService).deleteFarmland(any());
    }

    private static AgriFarmland sampleFarmland(Long id, String name) {
        AgriFarmland f = new AgriFarmland();
        f.setId(id);
        f.setName(name);
        f.setCode("F-" + id);
        f.setArea(BigDecimal.valueOf(10));
        f.setStatus(1);
        return f;
    }
}
