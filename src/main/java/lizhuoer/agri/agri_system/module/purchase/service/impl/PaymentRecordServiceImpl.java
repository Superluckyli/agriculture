package lizhuoer.agri.agri_system.module.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.purchase.domain.PaymentRecord;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrder;
import lizhuoer.agri.agri_system.module.purchase.mapper.PaymentRecordMapper;
import lizhuoer.agri.agri_system.module.purchase.service.IPaymentRecordService;
import lizhuoer.agri.agri_system.module.purchase.service.IPurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class PaymentRecordServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecord> implements IPaymentRecordService {

    private static final Set<String> PAYABLE_STATUSES = Set.of("confirmed", "receiving", "partial_received");

    @Autowired
    private IPurchaseOrderService purchaseOrderService;

    @Override
    public PaymentRecord addPayment(Long orderId, BigDecimal amount, String method) {
        // 1. 金额校验
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("付款金额必须大于0");
        }

        // 2. 订单状态校验
        PurchaseOrder order = purchaseOrderService.getById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("采购单不存在");
        }
        if (!PAYABLE_STATUSES.contains(order.getStatus())) {
            throw new IllegalArgumentException("当前订单状态不允许付款");
        }

        // 3. 累计付款校验
        BigDecimal paid = baseMapper.sumPaidByOrderId(orderId);
        BigDecimal remaining = order.getTotalAmount().subtract(paid);
        if (amount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException("付款金额超出未付余额（剩余 " + remaining + "）");
        }

        // 4. 保存
        PaymentRecord record = new PaymentRecord();
        record.setPurchaseOrderId(orderId);
        record.setPayAmount(amount);
        record.setPayMethod(method);
        record.setStatus("paid");
        record.setPayTime(LocalDateTime.now());
        record.setOperatorId(LoginUserContext.requireUser().getUserId());
        save(record);
        return record;
    }

    @Override
    public List<PaymentRecord> listByOrderId(Long orderId) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRecord::getPurchaseOrderId, orderId)
                .orderByDesc(PaymentRecord::getPayTime);
        return list(wrapper);
    }
}
