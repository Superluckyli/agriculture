package lizhuoer.agri.agri_system.module.material.stocklog.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("material_stock_log")
public class MaterialStockLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private String changeType;
    private BigDecimal qty;
    private BigDecimal beforeStock;
    private BigDecimal afterStock;
    private String relatedType;
    private Long relatedId;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
