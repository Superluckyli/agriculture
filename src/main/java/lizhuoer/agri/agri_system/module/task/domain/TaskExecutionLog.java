package lizhuoer.agri.agri_system.module.task.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务执行记录
 */
@Data
@TableName("task_execution_log")
public class TaskExecutionLog {
    @TableId(type = IdType.AUTO)
    private Long logId;

    private Long taskId;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private Integer statusSnapshot;
    private String photoUrl;
    private String materialCostJson;
    private String problemDesc;
    private LocalDateTime createTime;
}
