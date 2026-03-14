package lizhuoer.agri.agri_system.module.purchase.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderItemCreateRequest {
    @NotNull(message = "物资ID不能为空")
    private Long materialId;

    @NotNull(message = "采购数量不能为空")
    @Positive(message = "采购数量必须大于0")
    private BigDecimal purchaseQty;

    @NotNull(message = "单价不能为空")
    @Positive(message = "单价必须大于0")
    private BigDecimal unitPrice;

    private String remark;
}
