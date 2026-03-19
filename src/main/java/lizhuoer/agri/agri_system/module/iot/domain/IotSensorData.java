package lizhuoer.agri.agri_system.module.iot.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("iot_sensor_data")
public class IotSensorData {
    @TableId(type = IdType.AUTO)
    private Long dataId;

    private Long deviceId;
    private Long farmlandId;
    private String plotId;
    private String sensorType;

    @JsonProperty("value")
    private BigDecimal sensorValue;

    private String unit;
    private String sourceType;
    private String qualityStatus;

    @TableField("reported_at")
    private LocalDateTime createTime;
    private LocalDateTime createdAt;
}
