package lizhuoer.agri.agri_system.module.supplier.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.exception.GlobalExceptionHandler;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.supplier.domain.SupplierInfo;
import lizhuoer.agri.agri_system.module.supplier.service.ISupplierInfoService;
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

class SupplierInfoControllerTest {

    private ISupplierInfoService supplierService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        supplierService = mock(ISupplierInfoService.class);
        SupplierInfoController controller = new SupplierInfoController();
        ReflectionTestUtils.setField(controller, "supplierService", supplierService);
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
        Page<SupplierInfo> page = new Page<>(1, 10);
        page.setRecords(List.of(sampleSupplier(1L, "Supplier-A")));
        page.setTotal(1);
        when(supplierService.listPage(any(Page.class), isNull())).thenReturn(page);

        mockMvc.perform(get("/supplier/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void listShouldFilterByName() throws Exception {
        Page<SupplierInfo> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(supplierService.listPage(any(Page.class), eq("green"))).thenReturn(page);

        mockMvc.perform(get("/supplier/list").param("name", "green"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(supplierService).listPage(any(Page.class), eq("green"));
    }

    @Test
    void allShouldReturnFullList() throws Exception {
        when(supplierService.listAll()).thenReturn(List.of(
                sampleSupplier(1L, "Supplier-A"),
                sampleSupplier(2L, "Supplier-B")));

        mockMvc.perform(get("/supplier/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void addShouldSucceed() throws Exception {
        String body = """
                {
                  "name": "Green Supply Co.",
                  "contactName": "Zhang San",
                  "phone": "13800138000",
                  "address": "No.1 Farm Road",
                  "status": 1
                }
                """;

        mockMvc.perform(post("/supplier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(supplierService).addSupplier(any(SupplierInfo.class));
    }

    @Test
    void editShouldSucceed() throws Exception {
        String body = """
                {
                  "id": 1,
                  "name": "Green Supply Co. Updated",
                  "contactName": "Li Si",
                  "phone": "13900139000",
                  "status": 1
                }
                """;

        mockMvc.perform(put("/supplier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(supplierService).updateSupplier(any(SupplierInfo.class));
    }

    @Test
    void removeShouldSucceed() throws Exception {
        mockMvc.perform(delete("/supplier/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(supplierService).deleteSupplier(any());
    }

    private static SupplierInfo sampleSupplier(Long id, String name) {
        SupplierInfo s = new SupplierInfo();
        s.setId(id);
        s.setName(name);
        s.setContactName("Contact-" + id);
        s.setPhone("1380013800" + id);
        s.setStatus(1);
        return s;
    }
}
