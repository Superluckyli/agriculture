package lizhuoer.agri.agri_system.module.iot.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("iot_device_binding")
public class IotDeviceBinding {
    @TableId(type = IdType.AUTO)
    private Long bindingId;

    private Long deviceId;
    private Long farmlandId;
    private Integer isActive;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
