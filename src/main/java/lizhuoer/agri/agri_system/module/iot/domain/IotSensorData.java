package lizhuoer.agri.agri_system.module.iot.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 传感器数据表
 */
@Data
@TableName("iot_sensor_data")
public class IotSensorData {
    @TableId(type = IdType.AUTO)
    private Long dataId;

    private String plotId;
    private String sensorType; // TEMP, HUMIDITY, PH, LIGHT
    private BigDecimal value;
    private String unit;
    private LocalDateTime createTime;
}
