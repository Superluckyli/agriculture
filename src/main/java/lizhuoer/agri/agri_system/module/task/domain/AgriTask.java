package lizhuoer.agri.agri_system.module.task.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Farm task.
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

    /**
     * 0 pending_assign, 1 pending_accept, 2 accepted, 3 completed, 4 overdue, 5 rejected.
     */
    private Integer status;

    /**
     * Legacy field. Kept for compatibility with existing list/query.
     */
    private Long executorId;

    /**
     * Readable executor display name. Not persisted.
     */
    @TableField(exist = false)
    private String executorName;

    /**
     * Executor role key for UI display. Not persisted.
     */
    @TableField(exist = false)
    private String executorRoleKey;

    /**
     * Executor role name for UI display. Not persisted.
     */
    @TableField(exist = false)
    private String executorRoleName;

    /**
     * Current assigned farmer.
     */
    private Long assigneeId;

    /**
     * Readable assignee display name. Not persisted.
     */
    @TableField(exist = false)
    private String assigneeName;

    private LocalDateTime assignTime;
    private Long assignBy;
    private String assignRemark;

    private LocalDateTime acceptTime;
    private Long acceptBy;

    private LocalDateTime rejectTime;
    private Long rejectBy;
    private String rejectReason;

    private Long createBy;
    private LocalDateTime createTime;

    private Long updateBy;
    private LocalDateTime updateTime;

    private Integer version;
}
