package lizhuoer.agri.agri_system.module.iot.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("agri_task_rule")
public class AgriTaskRule {
    @TableId(type = IdType.AUTO)
    private Long ruleId;

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    @NotBlank(message = "传感器类型不能为空")
    private String sensorType;

    private String triggerCondition;

    @TableField("min_value")
    private BigDecimal minVal;

    @TableField("max_value")
    private BigDecimal maxVal;

    private String createMode;

    @TableField("task_type")
    private String autoTaskType;

    @TableField("task_priority")
    private Integer priority;

    @TableField("is_enabled")
    private Integer isEnable;

    @TableField("dispatch_cooldown_minutes")
    private Integer cooldownMinutes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
