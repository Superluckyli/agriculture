package lizhuoer.agri.agri_system.module.purchase.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PurchaseOrderCreateRequest {

    @NotNull(message = "供应商ID不能为空")
    private Long supplierId;

    private String payMethod;
    private String remark;

    @Valid
    @NotEmpty(message = "采购明细不能为空")
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {

        @NotNull(message = "物料ID不能为空")
        private Long materialId;

        @NotNull(message = "采购数量不能为空")
        private BigDecimal purchaseQty;

        @NotNull(message = "单价不能为空")
        private BigDecimal unitPrice;

        private String remark;
    }
}
