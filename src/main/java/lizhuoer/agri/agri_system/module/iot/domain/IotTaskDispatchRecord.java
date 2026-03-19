package lizhuoer.agri.agri_system.module.iot.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("iot_task_dispatch_record")
public class IotTaskDispatchRecord {
    @TableId(type = IdType.AUTO)
    private Long dispatchId;

    private Long eventId;
    private Long ruleId;
    private Long taskId;
    private String dispatchMode;
    private String dispatchStatus;
    private Long operatorId;
    private String aiSummary;
    private String errorMessage;
    private LocalDateTime createdAt;
}
