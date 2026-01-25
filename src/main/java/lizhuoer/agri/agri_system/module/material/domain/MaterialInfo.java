package lizhuoer.agri.agri_system.module.material.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物资档案与库存
 */
@Data
@TableName("material_info")
public class MaterialInfo {
    @TableId(type = IdType.AUTO)
    private Long materialId;

    private String name;
    private String category; // 种子, 化肥, 农药, 工具
    private BigDecimal price;
    private BigDecimal stockQuantity;
    private String unit;
    private LocalDateTime updateTime;
}
