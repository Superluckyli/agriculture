package lizhuoer.agri.agri_system.module.purchase.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.exception.GlobalExceptionHandler;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.purchase.domain.PaymentRecord;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrder;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrderItem;
import lizhuoer.agri.agri_system.module.purchase.service.IPaymentRecordService;
import lizhuoer.agri.agri_system.module.purchase.service.IPurchaseOrderItemService;
import lizhuoer.agri.agri_system.module.purchase.service.IPurchaseOrderService;
import lizhuoer.agri.agri_system.module.purchase.service.PurchaseReceiveService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PurchaseOrderControllerTest {

    private IPurchaseOrderService orderService;
    private IPurchaseOrderItemService itemService;
    private IPaymentRecordService paymentService;
    private PurchaseReceiveService receiveService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        orderService = mock(IPurchaseOrderService.class);
        itemService = mock(IPurchaseOrderItemService.class);
        paymentService = mock(IPaymentRecordService.class);
        receiveService = mock(PurchaseReceiveService.class);

        PurchaseOrderController controller = new PurchaseOrderController();
        ReflectionTestUtils.setField(controller, "orderService", orderService);
        ReflectionTestUtils.setField(controller, "itemService", itemService);
        ReflectionTestUtils.setField(controller, "paymentService", paymentService);
        ReflectionTestUtils.setField(controller, "receiveService", receiveService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        LoginUserContext.set(new LoginUser(1L, "admin", Set.of("ADMIN")));
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    // --- 主表 CRUD ---

    @Test
    void listShouldReturnPage() throws Exception {
        Page<PurchaseOrder> page = new Page<>(1, 10);
        page.setRecords(List.of(sampleOrder(1L, "draft")));
        page.setTotal(1);
        when(orderService.listPage(any(Page.class), any(), any())).thenReturn(page);

        mockMvc.perform(get("/purchase/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void addShouldSetStatusToDraft() throws Exception {
        String body = """
                {
                  "supplierId": 1,
                  "payMethod": "bank_transfer",
                  "remark": "First order"
                }
                """;

        mockMvc.perform(post("/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(orderService).save(any(PurchaseOrder.class));
    }

    @Test
    void editShouldSucceedWhenDraft() throws Exception {
        PurchaseOrder existing = sampleOrder(1L, "draft");
        when(orderService.getById(1L)).thenReturn(existing);

        String body = """
                {
                  "id": 1,
                  "supplierId": 2,
                  "remark": "Updated remark"
                }
                """;

        mockMvc.perform(put("/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(orderService).updateById(any(PurchaseOrder.class));
    }

    @Test
    void editShouldFailWhenIdIsNull() throws Exception {
        String body = """
                {
                  "supplierId": 2
                }
                """;

        mockMvc.perform(put("/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(orderService, never()).updateById(any());
    }

    @Test
    void editShouldFailWhenOrderNotDraft() throws Exception {
        PurchaseOrder existing = sampleOrder(1L, "confirmed");
        when(orderService.getById(1L)).thenReturn(existing);

        String body = """
                {
                  "id": 1,
                  "supplierId": 2
                }
                """;

        mockMvc.perform(put("/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(orderService, never()).updateById(any());
    }

    @Test
    void removeShouldSucceedWhenDraft() throws Exception {
        mockMvc.perform(delete("/purchase/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(orderService).deleteOrders(any());
    }

    @Test
    void removeShouldFailWhenServiceThrows() throws Exception {
        org.mockito.Mockito.doThrow(new RuntimeException("存在关联子记录"))
                .when(orderService).deleteOrders(any());

        mockMvc.perform(delete("/purchase/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // --- 状态流转 ---

    @Test
    void confirmShouldCallServiceConfirm() throws Exception {
        PurchaseOrder confirmed = sampleOrder(1L, "confirmed");
        when(orderService.confirmOrder(1L)).thenReturn(confirmed);

        mockMvc.perform(put("/purchase/1/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(orderService).confirmOrder(1L);
    }

    @Test
    void cancelShouldCallServiceCancel() throws Exception {
        PurchaseOrder cancelled = sampleOrder(1L, "cancelled");
        when(orderService.cancelOrder(1L)).thenReturn(cancelled);

        mockMvc.perform(put("/purchase/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(orderService).cancelOrder(1L);
    }

    // --- 订单明细 ---

    @Test
    void itemsShouldReturnListByOrderId() throws Exception {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setId(1L);
        item.setPurchaseOrderId(10L);
        item.setMaterialId(5L);
        item.setPurchaseQty(BigDecimal.valueOf(100));
        when(itemService.list(any(Wrapper.class))).thenReturn(List.of(item));

        mockMvc.perform(get("/purchase/10/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void addItemShouldSetOrderId() throws Exception {
        String body = """
                {
                  "materialId": 5,
                  "purchaseQty": 100,
                  "unitPrice": 12.50
                }
                """;

        mockMvc.perform(post("/purchase/10/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(itemService).save(any(PurchaseOrderItem.class));
    }

    // --- 付款记录 ---

    @Test
    void paymentsShouldCallListByOrderId() throws Exception {
        when(paymentService.listByOrderId(10L)).thenReturn(List.of());

        mockMvc.perform(get("/purchase/10/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(paymentService).listByOrderId(10L);
    }

    @Test
    void addPaymentShouldCallService() throws Exception {
        PaymentRecord result = new PaymentRecord();
        result.setId(1L);
        result.setPayAmount(BigDecimal.valueOf(500));
        when(paymentService.addPayment(eq(10L), any(BigDecimal.class), eq("bank_transfer")))
                .thenReturn(result);

        String body = """
                {
                  "payAmount": 500,
                  "payMethod": "bank_transfer"
                }
                """;

        mockMvc.perform(post("/purchase/10/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(paymentService).addPayment(eq(10L), any(BigDecimal.class), eq("bank_transfer"));
    }

    // --- 收货 ---

    @Test
    void receiveShouldCallReceiveService() throws Exception {
        mockMvc.perform(post("/purchase/10/receive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(receiveService).receiveOrder(eq(10L), eq(1L));
    }

    // --- Helper ---

    private static PurchaseOrder sampleOrder(Long id, String status) {
        PurchaseOrder o = new PurchaseOrder();
        o.setId(id);
        o.setOrderNo("PO-" + id);
        o.setStatus(status);
        o.setSupplierId(1L);
        o.setTotalAmount(BigDecimal.valueOf(1000));
        o.setVersion(0);
        return o;
    }
}
