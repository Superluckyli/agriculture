package lizhuoer.agri.agri_system.module.purchase.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.purchase.domain.PaymentRecord;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrder;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrderItem;
import lizhuoer.agri.agri_system.module.purchase.domain.dto.PurchaseOrderCreateRequest;
import lizhuoer.agri.agri_system.module.purchase.domain.dto.PurchaseOrderItemCreateRequest;
import lizhuoer.agri.agri_system.module.purchase.mapper.PaymentRecordMapper;
import lizhuoer.agri.agri_system.module.purchase.mapper.PurchaseOrderItemMapper;
import lizhuoer.agri.agri_system.module.purchase.mapper.PurchaseOrderMapper;
import lizhuoer.agri.agri_system.module.purchase.service.IPurchaseOrderService;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder> implements IPurchaseOrderService {

    @Autowired
    private PurchaseOrderItemMapper purchaseOrderItemMapper;

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder createOrder(PurchaseOrderCreateRequest req) {
        PurchaseOrder order = new PurchaseOrder();
        order.setSupplierId(req.getSupplierId());
        order.setPayMethod(req.getPayMethod());
        order.setRemark(req.getRemark());
        order.setStatus("draft");
        order.setOrderNo("PO-" + System.currentTimeMillis());
        order.setVersion(0);
        order.setCreatedAt(LocalDateTime.now());
        Long userId = LoginUserContext.requireUser().getUserId();
        order.setCreatedBy(userId);
        save(order);

        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderCreateRequest.ItemRequest item : req.getItems()) {
            PurchaseOrderItem poi = new PurchaseOrderItem();
            poi.setPurchaseOrderId(order.getId());
            poi.setMaterialId(item.getMaterialId());
            poi.setPurchaseQty(item.getPurchaseQty());
            poi.setUnitPrice(item.getUnitPrice());
            poi.setLineAmount(item.getPurchaseQty().multiply(item.getUnitPrice()));
            poi.setRemark(item.getRemark());
            purchaseOrderItemMapper.insert(poi);
            total = total.add(poi.getLineAmount());
        }
        // 直接用 SQL 更新 totalAmount，避免乐观锁版本冲突
        baseMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getId, order.getId())
                        .set(PurchaseOrder::getTotalAmount, total));
        return getById(order.getId());
    }

    @Override
    public PurchaseOrder confirmOrder(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("采购单不存在");
        }
        if (!"draft".equals(order.getStatus())) {
            throw new RuntimeException("当前状态不允许此操作，仅草稿状态可确认");
        }
        long itemCount = purchaseOrderItemMapper.selectCount(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getPurchaseOrderId, orderId));
        if (itemCount < 1) {
            throw new RuntimeException("采购单至少需要一条物资明细才能确认");
        }
        int ver = order.getVersion() == null ? 0 : order.getVersion();
        int rows = baseMapper.casUpdateStatus(orderId, "draft", "confirmed", null, ver);
        if (rows == 0) {
            throw new RuntimeException("采购单状态已变化，确认失败（并发冲突）");
        }
        return getById(orderId);
    }

    @Override
    public PurchaseOrder cancelOrder(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("采购单不存在");
        }
        String currentStatus = order.getStatus();
        if (!"draft".equals(currentStatus) && !"confirmed".equals(currentStatus)) {
            throw new RuntimeException("当前状态不允许此操作，仅草稿或已确认状态可取消");
        }
        int ver = order.getVersion() == null ? 0 : order.getVersion();
        int rows = baseMapper.casUpdateStatus(orderId, currentStatus, "cancelled", null, ver);
        if (rows == 0) {
            throw new RuntimeException("采购单状态已变化，取消失败（并发冲突）");
        }
        return getById(orderId);
    }

    private static final Set<String> ALLOWED_PAY_METHODS = Set.of("银行转账", "支付宝", "微信支付", "现金", "其他");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long id, String payMethod, Long operatorId) {
        // 校验付款方式白名单
        if (!ALLOWED_PAY_METHODS.contains(payMethod)) {
            throw new RuntimeException("不支持的付款方式: " + payMethod);
        }

        PurchaseOrder order = getById(id);
        if (order == null) {
            throw new RuntimeException("采购单不存在");
        }
        if (!"confirmed".equals(order.getStatus())) {
            throw new RuntimeException("仅已确认状态的采购单可付款");
        }

        // 计算已付金额和剩余金额，防超付
        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal paidAmount = paymentRecordMapper.sumPaidByOrderId(id);
        BigDecimal remaining = totalAmount.subtract(paidAmount);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("该采购单已全额付款，无需再次付款");
        }

        // CAS 更新采购单状态: confirmed -> paid
        int ver = order.getVersion() == null ? 0 : order.getVersion();
        int rows = baseMapper.casPayOrder(id, payMethod, operatorId, ver);
        if (rows == 0) {
            throw new RuntimeException("采购单状态已变化，付款失败（并发冲突）");
        }

        // 创建付款记录
        PaymentRecord record = new PaymentRecord();
        record.setPurchaseOrderId(id);
        record.setPayAmount(remaining);
        record.setPayMethod(payMethod);
        record.setStatus("paid");
        record.setPayTime(LocalDateTime.now());
        record.setOperatorId(operatorId);
        paymentRecordMapper.insert(record);
    }

    @Override
    public int casUpdateStatus(Long id, String fromStatus, String toStatus, Long confirmedBy, Integer version) {
        return baseMapper.casUpdateStatus(id, fromStatus, toStatus, confirmedBy, version);
    }

    @Override
    public Page<PurchaseOrder> listPage(Page<PurchaseOrder> page, Long supplierId, String status) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(supplierId != null, PurchaseOrder::getSupplierId, supplierId)
                .eq(StrUtil.isNotBlank(status), PurchaseOrder::getStatus, status)
                .orderByDesc(PurchaseOrder::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public void deleteOrders(List<Long> ids) {
        for (Long id : ids) {
            PurchaseOrder order = getById(id);
            if (order == null) {
                throw new RuntimeException("采购单不存在: " + id);
            }
            if (!"draft".equals(order.getStatus())) {
                throw new RuntimeException("采购单【" + order.getOrderNo() + "】仅草稿状态可删除");
            }
            long itemCount = purchaseOrderItemMapper.selectCount(new LambdaQueryWrapper<PurchaseOrderItem>()
                    .eq(PurchaseOrderItem::getPurchaseOrderId, id));
            if (itemCount > 0) {
                throw new RuntimeException("采购单【" + order.getOrderNo() + "】存在订单明细，无法删除");
            }
            long payCount = paymentRecordMapper.selectCount(new LambdaQueryWrapper<PaymentRecord>()
                    .eq(PaymentRecord::getPurchaseOrderId, id));
            if (payCount > 0) {
                throw new RuntimeException("采购单【" + order.getOrderNo() + "】存在付款记录，无法删除");
            }
        }
        removeBatchByIds(ids);
    }

    @Override
    public void recalcTotalAmount(Long orderId) {
        List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getPurchaseOrderId, orderId));
        BigDecimal total = items.stream()
                .map(PurchaseOrderItem::getLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        baseMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getId, orderId)
                        .set(PurchaseOrder::getTotalAmount, total));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceItems(Long orderId, List<PurchaseOrderItemCreateRequest> items) {
        // 删除旧明细
        purchaseOrderItemMapper.delete(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getPurchaseOrderId, orderId));
        // 插入新明细
        for (PurchaseOrderItemCreateRequest req : items) {
            PurchaseOrderItem poi = new PurchaseOrderItem();
            poi.setPurchaseOrderId(orderId);
            poi.setMaterialId(req.getMaterialId());
            poi.setPurchaseQty(req.getPurchaseQty());
            poi.setUnitPrice(req.getUnitPrice());
            poi.setLineAmount(req.getPurchaseQty().multiply(req.getUnitPrice()));
            poi.setRemark(req.getRemark());
            purchaseOrderItemMapper.insert(poi);
        }
        // 重算总额
        recalcTotalAmount(orderId);
    }
}
