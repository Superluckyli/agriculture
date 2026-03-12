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

@Service
public class PaymentRecordServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecord> implements IPaymentRecordService {

    @Autowired
    private IPurchaseOrderService purchaseOrderService;

    @Override
    public PaymentRecord addPayment(Long orderId, BigDecimal amount, String method) {
        PurchaseOrder order = purchaseOrderService.getById(orderId);
        if (order == null) {
            throw new RuntimeException("采购单不存在");
        }
        if (!"confirmed".equals(order.getStatus())) {
            throw new RuntimeException("当前状态不允许支付，仅已确认的采购单可添加支付记录");
        }

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
