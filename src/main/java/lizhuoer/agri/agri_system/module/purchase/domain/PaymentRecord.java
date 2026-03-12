package lizhuoer.agri.agri_system.module.purchase.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_record")
public class PaymentRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long purchaseOrderId;
    private String payMethod;
    private BigDecimal payAmount;
    private String status;
    private LocalDateTime payTime;
    private Long operatorId;
    private String remark;
}
