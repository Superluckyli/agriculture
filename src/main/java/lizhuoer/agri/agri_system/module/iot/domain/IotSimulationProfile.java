package lizhuoer.agri.agri_system.module.iot.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("iot_simulation_profile")
public class IotSimulationProfile {
    @TableId(type = IdType.AUTO)
    private Long profileId;

    private Long deviceId;
    private String sensorType;
    private BigDecimal baseValue;
    private BigDecimal fluctuationRange;
    private BigDecimal warningValue;
    private BigDecimal warningProbability;
    private Integer intervalSeconds;
    private Integer isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
