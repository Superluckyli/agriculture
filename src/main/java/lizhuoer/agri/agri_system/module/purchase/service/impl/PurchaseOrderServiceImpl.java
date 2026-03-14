package lizhuoer.agri.agri_system.module.purchase.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.purchase.domain.PaymentRecord;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrder;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrderItem;
import lizhuoer.agri.agri_system.module.purchase.mapper.PaymentRecordMapper;
import lizhuoer.agri.agri_system.module.purchase.mapper.PurchaseOrderItemMapper;
import lizhuoer.agri.agri_system.module.purchase.mapper.PurchaseOrderMapper;
import lizhuoer.agri.agri_system.module.purchase.service.IPurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder> implements IPurchaseOrderService {

    @Autowired
    private PurchaseOrderItemMapper purchaseOrderItemMapper;

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Override
    public PurchaseOrder confirmOrder(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("采购单不存在");
        }
        if (!"draft".equals(order.getStatus())) {
            throw new RuntimeException("当前状态不允许此操作，仅草稿状态可确认");
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
        if (!"draft".equals(order.getStatus())) {
            throw new RuntimeException("当前状态不允许此操作，仅草稿状态可取消");
        }
        int ver = order.getVersion() == null ? 0 : order.getVersion();
        int rows = baseMapper.casUpdateStatus(orderId, "draft", "cancelled", null, ver);
        if (rows == 0) {
            throw new RuntimeException("采购单状态已变化，取消失败（并发冲突）");
        }
        return getById(orderId);
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
}
