package lizhuoer.agri.agri_system.module.task.material.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("agri_task_material")
public class AgriTaskMaterial {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long materialId;
    // 建议采购数量
    private BigDecimal suggestedQty;
    // 实际采购数量
    private BigDecimal actualQty;
    // 建议采购单价
    private BigDecimal unitPrice;
    // 偏差原因
    private String deviationReason;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String materialName;
}
