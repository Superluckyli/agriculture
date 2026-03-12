package lizhuoer.agri.agri_system.module.material.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
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
    private String name;
    private String category;
    private String specification;
    private String unit;
    private BigDecimal currentStock;
    private BigDecimal safeThreshold;
    private BigDecimal suggestPurchaseQty;
    private Long supplierId;
    private BigDecimal unitPrice;
    private Integer status;

    @Version
    private Integer version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
