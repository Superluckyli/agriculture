package lizhuoer.agri.agri_system.module.purchase.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrder;
import lizhuoer.agri.agri_system.module.purchase.mapper.PurchaseOrderMapper;
import lizhuoer.agri.agri_system.module.purchase.service.IPurchaseOrderService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder> implements IPurchaseOrderService {

    @Override
    public PurchaseOrder confirmOrder(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("采购单不存在");
        }
        if (!"draft".equals(order.getStatus())) {
            throw new RuntimeException("当前状态不允许此操作，仅草稿状态可确认");
        }
        order.setStatus("confirmed");
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);
        return order;
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
        order.setStatus("cancelled");
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);
        return order;
    }

    @Override
    public Page<PurchaseOrder> listPage(Page<PurchaseOrder> page, Long supplierId, String status) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(supplierId != null, PurchaseOrder::getSupplierId, supplierId)
                .eq(StrUtil.isNotBlank(status), PurchaseOrder::getStatus, status)
                .orderByDesc(PurchaseOrder::getCreatedAt);
        return page(page, wrapper);
    }
}
