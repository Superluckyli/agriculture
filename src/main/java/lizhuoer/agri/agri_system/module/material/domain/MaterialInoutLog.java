package lizhuoer.agri.agri_system.module.material.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物资出入库流水
 */
@Data
@TableName("material_inout_log")
public class MaterialInoutLog {
    @TableId(type = IdType.AUTO)
    private Long logId;

    private Long materialId;
    private Integer type; // 1入库 2出库
    private BigDecimal quantity;
    private Long relatedTaskId;
    private String remark;
    private LocalDateTime createTime;
}
