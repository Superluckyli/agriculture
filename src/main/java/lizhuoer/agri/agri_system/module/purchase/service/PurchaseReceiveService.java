package lizhuoer.agri.agri_system.module.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInfo;
import lizhuoer.agri.agri_system.module.material.mapper.MaterialInfoMapper;
import lizhuoer.agri.agri_system.module.material.stocklog.domain.MaterialStockLog;
import lizhuoer.agri.agri_system.module.material.stocklog.service.IMaterialStockLogService;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrder;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrderItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PurchaseReceiveService {
    private static final int MAX_RETRY = 3;

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

    @Transactional(rollbackFor = Exception.class)
    public void receiveOrder(Long orderId, Long operatorId) {
        PurchaseOrder order = orderService.getById(orderId);
        if (order == null) {
            throw new RuntimeException("采购单不存在");
        }
        if (!"confirmed".equals(order.getStatus())) {
            throw new RuntimeException("仅已确认采购单可收货");
        }

        LambdaQueryWrapper<PurchaseOrderItem> qw = new LambdaQueryWrapper<>();
        qw.eq(PurchaseOrderItem::getPurchaseOrderId, orderId);
        List<PurchaseOrderItem> items = itemService.list(qw);

        LocalDateTime now = LocalDateTime.now();

        for (PurchaseOrderItem item : items) {
            BigDecimal remaining = item.getPurchaseQty().subtract(
                    item.getReceiveQty() != null ? item.getReceiveQty() : BigDecimal.ZERO);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Optimistic lock add stock
            boolean added = false;
            for (int i = 0; i < MAX_RETRY; i++) {
                MaterialInfo mat = materialInfoMapper.selectById(item.getMaterialId());
                if (mat == null) {
                    throw new RuntimeException("物资不存在: " + item.getMaterialId());
                }
                int ver = mat.getVersion() == null ? 0 : mat.getVersion();
                int rows = materialInfoMapper.addStock(item.getMaterialId(), remaining, ver);
                if (rows > 0) {
                    MaterialStockLog sl = new MaterialStockLog();
                    sl.setMaterialId(item.getMaterialId());
                    sl.setChangeType("IN");
                    sl.setQty(remaining);
                    sl.setBeforeStock(mat.getCurrentStock());
                    sl.setAfterStock(mat.getCurrentStock().add(remaining));
                    sl.setRelatedType("purchase");
                    sl.setRelatedId(orderId);
                    sl.setOperatorId(operatorId);
                    sl.setCreatedAt(now);
                    stockLogService.save(sl);
                    added = true;
                    break;
                }
                // 乐观锁冲突退避
                try { Thread.sleep(20); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("库存操作被中断", e);
                }
            }
            if (!added) {
                throw new RuntimeException("库存更新并发冲突，请重试");
            }

            // Update receive qty
            item.setReceiveQty(item.getPurchaseQty());
            itemService.updateById(item);
        }

        int ver = order.getVersion() == null ? 0 : order.getVersion();
        int rows = orderService.casUpdateStatus(
                orderId, "confirmed", "completed", operatorId, ver);
        if (rows == 0) {
            throw new RuntimeException("采购单状态已变化，收货失败（并发冲突）");
        }
    }
}
