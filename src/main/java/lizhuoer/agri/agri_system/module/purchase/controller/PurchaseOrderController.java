package lizhuoer.agri.agri_system.module.purchase.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lizhuoer.agri.agri_system.common.domain.PageResult;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.purchase.domain.PaymentRecord;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrder;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrderItem;
import lizhuoer.agri.agri_system.module.purchase.domain.dto.PaymentCreateRequest;
import lizhuoer.agri.agri_system.module.purchase.domain.dto.PurchaseOrderCreateRequest;
import lizhuoer.agri.agri_system.module.purchase.domain.dto.PurchaseOrderPayRequest;
import lizhuoer.agri.agri_system.module.purchase.domain.dto.PurchaseOrderItemCreateRequest;
import lizhuoer.agri.agri_system.module.purchase.domain.dto.ReceiveItemRequest;
import lizhuoer.agri.agri_system.module.purchase.service.IPaymentRecordService;
import lizhuoer.agri.agri_system.module.purchase.service.IPurchaseOrderItemService;
import lizhuoer.agri.agri_system.module.purchase.service.IPurchaseOrderService;
import lizhuoer.agri.agri_system.module.purchase.service.PurchaseReceiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/purchase")
public class PurchaseOrderController {

    @Autowired
    private IPurchaseOrderService orderService;
    @Autowired
    private IPurchaseOrderItemService itemService;
    @Autowired
    private IPaymentRecordService paymentService;
    @Autowired
    private PurchaseReceiveService receiveService;

    @GetMapping("/list")
    public R<PageResult<PurchaseOrder>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String status, Long supplierId) {
        Page<PurchaseOrder> page = new Page<>(pageNum, pageSize);
        return R.ok(PageResult.from(orderService.listPage(page, supplierId, status)));
    }

    @PostMapping
    public R<PurchaseOrder> add(@Valid @RequestBody PurchaseOrderCreateRequest req) {
        return R.ok(orderService.createOrder(req));
    }

    @PutMapping
    public R<Void> edit(@RequestBody PurchaseOrder order) {
        if (order.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        PurchaseOrder existing = orderService.getById(order.getId());
        if (existing == null) {
            throw new RuntimeException("采购单不存在");
        }
        if (!"draft".equals(existing.getStatus())) {
            throw new RuntimeException("仅草稿状态的采购单可编辑");
        }
        order.setStatus(null);
        order.setConfirmedBy(null);
        orderService.updateById(order);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        orderService.deleteOrders(Arrays.asList(ids));
        return R.ok();
    }

    @PutMapping("/{id}/confirm")
    public R<PurchaseOrder> confirm(@PathVariable Long id) {
        return R.ok(orderService.confirmOrder(id));
    }

    @PutMapping("/{id}/cancel")
    public R<PurchaseOrder> cancel(@PathVariable Long id) {
        return R.ok(orderService.cancelOrder(id));
    }

    @PutMapping("/{id}/pay")
    public R<Void> pay(@PathVariable Long id, @Valid @RequestBody PurchaseOrderPayRequest req) {
        Long operatorId = LoginUserContext.requireUser().getUserId();
        orderService.payOrder(id, req.getPayMethod(), operatorId);
        return R.ok();
    }

    // --- 采购明细 ---

    @GetMapping("/{orderId}/items")
    public R<List<PurchaseOrderItem>> items(@PathVariable Long orderId) {
        LambdaQueryWrapper<PurchaseOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrderItem::getPurchaseOrderId, orderId);
        return R.ok(itemService.list(wrapper));
    }

    @PostMapping("/{orderId}/items")
    public R<Void> addItem(@PathVariable Long orderId,
                           @Valid @RequestBody PurchaseOrderItemCreateRequest req) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseOrderId(orderId);
        item.setMaterialId(req.getMaterialId());
        item.setPurchaseQty(req.getPurchaseQty());
        item.setUnitPrice(req.getUnitPrice());
        item.setLineAmount(req.getPurchaseQty().multiply(req.getUnitPrice()));
        item.setRemark(req.getRemark());
        itemService.save(item);
        // 重算订单总金额
        orderService.recalcTotalAmount(orderId);
        return R.ok();
    }

    @PutMapping("/{orderId}/items")
    public R<Void> replaceItems(@PathVariable Long orderId,
                                @Valid @RequestBody List<PurchaseOrderItemCreateRequest> items) {
        PurchaseOrder existing = orderService.getById(orderId);
        if (existing == null) {
            throw new RuntimeException("采购单不存在");
        }
        if (!"draft".equals(existing.getStatus())) {
            throw new RuntimeException("仅草稿状态可编辑明细");
        }
        orderService.replaceItems(orderId, items);
        return R.ok();
    }

    // --- 支付记录 ---

    @GetMapping("/{orderId}/payments")
    public R<List<PaymentRecord>> payments(@PathVariable Long orderId) {
        return R.ok(paymentService.listByOrderId(orderId));
    }

    @PostMapping("/{orderId}/payment")
    public R<PaymentRecord> addPayment(@PathVariable Long orderId,
                                       @Valid @RequestBody PaymentCreateRequest req) {
        return R.ok(paymentService.addPayment(orderId, req.getPayAmount(), req.getPayMethod()));
    }

    @PostMapping("/{orderId}/receive")
    public R<Void> receive(@PathVariable Long orderId) {
        Long operatorId = LoginUserContext.requireUser().getUserId();
        receiveService.receiveOrder(orderId, operatorId);
        return R.ok();
    }

    /**
     * 单品分批收货 — 对指定明细行收入指定数量
     */
    @PostMapping("/{orderId}/receive-item")
    public R<Void> receiveItem(@PathVariable Long orderId,
                                @Valid @RequestBody ReceiveItemRequest req) {
        Long operatorId = LoginUserContext.requireUser().getUserId();
        receiveService.receiveItem(orderId, req, operatorId);
        return R.ok();
    }
}
