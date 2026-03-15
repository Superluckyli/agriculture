package lizhuoer.agri.agri_system.module.material.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存调整申请
 */
@Data
@TableName("stock_adjustment")
public class StockAdjustment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long materialId;
    /** INCREASE / DECREASE / SET */
    private String adjustType;
    private BigDecimal qty;
    private String reason;
    /** pending / approved / rejected */
    private String status;
    private Long applicantId;
    private Long reviewerId;
    private String reviewRemark;
    private LocalDateTime reviewTime;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String materialName;
}
