package lizhuoer.agri.agri_system.module.task.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 农事任务
 */
@Data
@TableName("agri_task")
public class AgriTask {
    @TableId(type = IdType.AUTO)
    private Long taskId;

    private Long batchId;
    private String taskName;
    private String taskType;
    private Integer priority;
    private LocalDateTime planTime;
    private Integer status; // 0待分配 1待执行 2进行中 3已完成 4已逾期
    private Long executorId;
    private Long createBy;
    private LocalDateTime createTime;
}
