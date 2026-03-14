package lizhuoer.agri.agri_system.module.purchase.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderCreateRequest {
    @NotNull(message = "供应商ID不能为空")
    private Long supplierId;

    private BigDecimal totalAmount;
    private String payMethod;
    private String remark;
}
