package lizhuoer.agri.agri_system.module.purchase.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentCreateRequest {
    @NotNull(message = "付款金额不能为空")
    @Positive(message = "付款金额必须大于0")
    private BigDecimal payAmount;

    @NotBlank(message = "付款方式不能为空")
    private String payMethod;

    private String remark;
}
