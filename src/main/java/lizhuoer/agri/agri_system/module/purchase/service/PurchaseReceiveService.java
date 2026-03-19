package lizhuoer.agri.agri_system.module.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lizhuoer.agri.agri_system.common.exception.BusinessException;
import lizhuoer.agri.agri_system.common.exception.ErrorCode;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInfo;
import lizhuoer.agri.agri_system.module.material.mapper.MaterialInfoMapper;
import lizhuoer.agri.agri_system.module.material.stocklog.domain.MaterialStockLog;
import lizhuoer.agri.agri_system.module.material.stocklog.service.IMaterialStockLogService;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrder;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrderItem;
import lizhuoer.agri.agri_system.module.purchase.domain.dto.ReceiveItemRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 采购收货服务 — 支持整单收货和单品分批收货
 */
@Service
public class PurchaseReceiveService {
    private static final int MAX_RETRY = 3;
    private static final Set<String> RECEIVABLE = Set.of("paid", "partial_received", "receiving");

    private final IPurchaseOrderService orderService;
    private final IPurchaseOrderItemService itemService;
    private final MaterialInfoMapper materialInfoMapper;
    private final IMaterialStockLogService stockLogService;

    public PurchaseReceiveService(IPurchaseOrderService orderService,
            IPurchaseOrderItemService itemService,
            MaterialInfoMapper materialInfoMapper,
            IMaterialStockLogService stockLogService) {
        this.orderService = orderService;
        this.itemService = itemService;
        this.materialInfoMapper = materialInfoMapper;
        this.stockLogService = stockLogService;
    }

    /**
     * 单品分批收货 — 对指定明细行收入 qty 数量
     * <p>
     * 状态流转: confirmed → partial_received → completed
     */
    @Transactional(rollbackFor = Exception.class)
    public void receiveItem(Long orderId, ReceiveItemRequest req, Long operatorId) {
        PurchaseOrder order = orderService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购单不存在");
        }
        if (!RECEIVABLE.contains(order.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "当前状态不允许收货，仅已确认/部分收货的订单可操作");
        }

        PurchaseOrderItem item = itemService.getById(req.getItemId());
        if (item == null || !orderId.equals(item.getPurchaseOrderId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收货明细不存在或不属于该采购单");
        }

        BigDecimal currentReceived = item.getReceiveQty() != null ? item.getReceiveQty() : BigDecimal.ZERO;
        BigDecimal remaining = item.getPurchaseQty().subtract(currentReceived);
        if (req.getQty().compareTo(remaining) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "收货数量超出剩余可收数量 (" + remaining + ")");
        }

        // 乐观锁入库
        addStockWithRetry(item.getMaterialId(), req.getQty(), orderId, operatorId);

        // 更新明细已收数量
        item.setReceiveQty(currentReceived.add(req.getQty()));
        itemService.updateById(item);

        // 根据所有明细收货情况决定订单状态
        updateOrderStatus(orderId, order, operatorId);
    }

    /**
     * 整单收货 (兼容旧接口) — 一次性收满全部剩余数量
     */
    @Transactional(rollbackFor = Exception.class)
    public void receiveOrder(Long orderId, Long operatorId) {
        PurchaseOrder order = orderService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购单不存在");
        }
        if (!RECEIVABLE.contains(order.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "仅已确认/部分收货的采购单可收货");
        }

        List<PurchaseOrderItem> items = itemService.list(
                new LambdaQueryWrapper<PurchaseOrderItem>()
                        .eq(PurchaseOrderItem::getPurchaseOrderId, orderId));

        for (PurchaseOrderItem item : items) {
            BigDecimal currentReceived = item.getReceiveQty() != null ? item.getReceiveQty() : BigDecimal.ZERO;
            BigDecimal remaining = item.getPurchaseQty().subtract(currentReceived);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            addStockWithRetry(item.getMaterialId(), remaining, orderId, operatorId);

            item.setReceiveQty(item.getPurchaseQty());
            itemService.updateById(item);
        }

        // 整单收货后全部收齐 → completed
        updateOrderStatus(orderId, order, operatorId);
    }

    /**
     * 根据明细收货进度更新订单状态:
     * - 全部收齐 → completed
     * - 部分收货 → partial_received
     */
    private void updateOrderStatus(Long orderId, PurchaseOrder order, Long operatorId) {
        List<PurchaseOrderItem> allItems = itemService.list(
                new LambdaQueryWrapper<PurchaseOrderItem>()
                        .eq(PurchaseOrderItem::getPurchaseOrderId, orderId));

        boolean allReceived = allItems.stream().allMatch(i -> {
            BigDecimal received = i.getReceiveQty() != null ? i.getReceiveQty() : BigDecimal.ZERO;
            return received.compareTo(i.getPurchaseQty()) >= 0;
        });

        String targetStatus = allReceived ? "completed" : "partial_received";
        if (!targetStatus.equals(order.getStatus())) {
            int ver = order.getVersion() != null ? order.getVersion() : 0;
            int rows = orderService.casUpdateStatus(orderId, order.getStatus(), targetStatus, operatorId, ver);
            if (rows == 0) {
                throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_FAIL, "采购单状态已变化，请重试");
            }
        }
    }

    private void addStockWithRetry(Long materialId, BigDecimal qty, Long orderId, Long operatorId) {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < MAX_RETRY; i++) {
            MaterialInfo mat = materialInfoMapper.selectById(materialId);
            if (mat == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "物资不存在: " + materialId);
            }
            int ver = mat.getVersion() != null ? mat.getVersion() : 0;
            int rows = materialInfoMapper.addStock(materialId, qty, ver);
            if (rows > 0) {
                MaterialStockLog sl = new MaterialStockLog();
                sl.setMaterialId(materialId);
                sl.setChangeType("IN");
                sl.setQty(qty);
                sl.setBeforeStock(mat.getCurrentStock());
                sl.setAfterStock(mat.getCurrentStock().add(qty));
                sl.setRelatedType("purchase");
                sl.setRelatedId(orderId);
                sl.setOperatorId(operatorId);
                sl.setCreatedAt(now);
                stockLogService.save(sl);
                return;
            }
            try { Thread.sleep(20); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("库存操作被中断", e);
            }
        }
        throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_FAIL, "库存更新并发冲突，请重试");
    }
}
