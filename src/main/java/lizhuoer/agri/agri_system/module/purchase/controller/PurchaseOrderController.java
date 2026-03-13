package lizhuoer.agri.agri_system.module.purchase.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.purchase.domain.PaymentRecord;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrder;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrderItem;
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
    public R<Page<PurchaseOrder>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String status, Long supplierId) {
        Page<PurchaseOrder> page = new Page<>(pageNum, pageSize);
        return R.ok(orderService.listPage(page, supplierId, status));
    }

    @PostMapping
    public R<Void> add(@RequestBody PurchaseOrder order) {
        order.setStatus("draft");
        order.setConfirmedBy(null);
        orderService.save(order);
        return R.ok();
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
        // 禁止客户端篡改受控字段
        order.setStatus(null);
        order.setConfirmedBy(null);
        orderService.updateById(order);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        for (Long id : ids) {
            PurchaseOrder existing = orderService.getById(id);
            if (existing != null && !"draft".equals(existing.getStatus())) {
                throw new RuntimeException("仅草稿状态的采购单可删除, orderId=" + id);
            }
        }
        orderService.removeBatchByIds(Arrays.asList(ids));
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

    // --- 采购明细 ---

    @GetMapping("/{orderId}/items")
    public R<List<PurchaseOrderItem>> items(@PathVariable Long orderId) {
        LambdaQueryWrapper<PurchaseOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrderItem::getPurchaseOrderId, orderId);
        return R.ok(itemService.list(wrapper));
    }

    @PostMapping("/{orderId}/items")
    public R<Void> addItem(@PathVariable Long orderId, @RequestBody PurchaseOrderItem item) {
        item.setPurchaseOrderId(orderId);
        itemService.save(item);
        return R.ok();
    }

    // --- 支付记录 ---

    @GetMapping("/{orderId}/payments")
    public R<List<PaymentRecord>> payments(@PathVariable Long orderId) {
        return R.ok(paymentService.listByOrderId(orderId));
    }

    @PostMapping("/{orderId}/payment")
    public R<PaymentRecord> addPayment(@PathVariable Long orderId, @RequestBody PaymentRecord payment) {
        return R.ok(paymentService.addPayment(orderId, payment.getPayAmount(), payment.getPayMethod()));
    }

    @PostMapping("/{orderId}/receive")
    public R<Void> receive(@PathVariable Long orderId) {
        Long operatorId = LoginUserContext.requireUser().getUserId();
        receiveService.receiveOrder(orderId, operatorId);
        return R.ok();
    }
}
