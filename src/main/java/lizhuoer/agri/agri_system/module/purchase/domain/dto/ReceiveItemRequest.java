package lizhuoer.agri.agri_system.module.purchase.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单品收货请求 — 支持分批部分收货
 */
@Data
public class ReceiveItemRequest {
    @NotNull(message = "明细 ID 不能为空")
    private Long itemId;

    @NotNull(message = "收货数量不能为空")
    @DecimalMin(value = "0.001", message = "收货数量必须大于 0")
    private BigDecimal qty;

    private String remark;
}
