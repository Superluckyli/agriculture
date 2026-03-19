package lizhuoer.agri.agri_system.module.purchase.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PurchaseOrderPayRequest {
    @NotBlank(message = "付款方式不能为空")
    private String payMethod;
}
