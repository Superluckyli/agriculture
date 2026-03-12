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
    private BigDecimal suggestedQty;
    private BigDecimal actualQty;
    private BigDecimal unitPrice;
    private String deviationReason;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String materialName;
}
