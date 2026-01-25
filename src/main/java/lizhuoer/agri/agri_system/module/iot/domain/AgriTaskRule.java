package lizhuoer.agri.agri_system.module.iot.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 智能预警规则
 */
@Data
@TableName("agri_task_rule")
public class AgriTaskRule {
    @TableId(type = IdType.AUTO)
    private Long ruleId;

    private String ruleName;
    private String sensorType;
    private BigDecimal minVal;
    private BigDecimal maxVal;
    private String autoTaskType;
    private Integer priority;
    private Integer isEnable;
}
