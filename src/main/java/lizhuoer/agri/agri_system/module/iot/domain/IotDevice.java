package lizhuoer.agri.agri_system.module.iot.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("iot_device")
public class IotDevice {
    @TableId(type = IdType.AUTO)
    private Long deviceId;

    private String deviceCode;
    private String deviceName;
    private String sourceType;
    private String deviceStatus;
    private LocalDateTime lastReportedAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
