package lizhuoer.agri.agri_system.module.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lizhuoer.agri.agri_system.module.purchase.domain.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM payment_record WHERE purchase_order_id = #{orderId} AND status = 'paid'")
    BigDecimal sumPaidByOrderId(Long orderId);
}
