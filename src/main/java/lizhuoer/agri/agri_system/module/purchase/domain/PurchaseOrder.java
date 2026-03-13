package lizhuoer.agri.agri_system.module.purchase.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("purchase_order")
public class PurchaseOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long orgId;
    private String orderNo;
    private String status;
    private Long supplierId;
    private BigDecimal totalAmount;
    private String payMethod;
    private String remark;
    private Long createdBy;
    private Long confirmedBy;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
