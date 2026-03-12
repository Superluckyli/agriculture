package lizhuoer.agri.agri_system.module.purchase.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("purchase_order_item")
public class PurchaseOrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long purchaseOrderId;
    private Long materialId;
    private BigDecimal purchaseQty;
    private BigDecimal receiveQty;
    private BigDecimal unitPrice;
    private BigDecimal lineAmount;
    private String remark;
}
