package lizhuoer.agri.agri_system.module.material.domain;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("material_info")
public class MaterialInfo {
    @TableId(type = IdType.AUTO)
    private Long materialId;

    private Long tenantId;
    private Long orgId;
    @NotBlank(message = "物资名称不能为空")
    private String name;
    private String category;
    private String specification;
    @NotBlank(message = "计量单位不能为空")
    private String unit;
    private BigDecimal currentStock;
    private BigDecimal safeThreshold;
    private BigDecimal suggestPurchaseQty;
    private Long supplierId;

    @TableField(exist = false)
    private String supplierName;

    private BigDecimal unitPrice;
    private Integer status;

    @Version
    private Integer version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
