package lizhuoer.agri.agri_system.module.iot.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("iot_warning_event")
public class IotWarningEvent {
    @TableId(type = IdType.AUTO)
    private Long eventId;

    private Long ruleId;
    private Long sensorDataId;
    private Long deviceId;
    private Long farmlandId;
    private Long batchId;
    private String sensorType;
    private BigDecimal triggerValue;
    private LocalDateTime triggeredAt;
    private String handleStatus;
    private String dispatchMode;
    private Long taskId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
