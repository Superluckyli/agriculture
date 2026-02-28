package lizhuoer.agri.agri_system.module.task.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_flow_log")
public class TaskFlowLog {
    @TableId(type = IdType.AUTO)
    private Long logId;

    private Long taskId;
    private String action;
    private Integer fromStatus;
    private Integer toStatus;

    private Long operatorId;
    private Long targetUserId;

    private String remark;
    private String traceId;
    private LocalDateTime createTime;
}
