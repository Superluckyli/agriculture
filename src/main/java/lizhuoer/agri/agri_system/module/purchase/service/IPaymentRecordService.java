package lizhuoer.agri.agri_system.module.purchase.service;

import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.module.purchase.domain.PaymentRecord;

import java.math.BigDecimal;
import java.util.List;

public interface IPaymentRecordService extends IService<PaymentRecord> {

    /** 添加支付记录，校验采购单存在且状态为 confirmed */
    PaymentRecord addPayment(Long orderId, BigDecimal amount, String method);

    /** 按采购单ID查询支付记录，按 payTime 倒序 */
    List<PaymentRecord> listByOrderId(Long orderId);
}
