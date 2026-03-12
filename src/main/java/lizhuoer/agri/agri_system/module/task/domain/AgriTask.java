package lizhuoer.agri.agri_system.module.task.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agri_task")
public class AgriTask {
    @TableId(type = IdType.AUTO)
    private Long taskId;

    private Long tenantId;
    private Long orgId;
    private Long batchId;
    private String taskNo;
    private String taskName;
    private String taskType;
    private String taskSource;
    private String riskLevel;
    private Integer needReview;
    private Integer priority;
    private LocalDateTime planTime;
    private LocalDateTime deadlineAt;

    /** V1 唯一状态字段: pending_review/pending_accept/in_progress/completed/
     *  rejected_reassign/rejected_review/suspended/overdue/cancelled */
    private String statusV2;

    private Long assigneeId;
    private LocalDateTime assignTime;
    private Long assignBy;
    private String assignRemark;
    private Long reviewerUserId;

    private LocalDateTime acceptTime;
    private Long acceptBy;
    private LocalDateTime completedAt;

    private LocalDateTime rejectTime;
    private Long rejectBy;
    private String rejectReason;
    private String rejectReasonType;

    private String suspendReason;
    private String cancelReason;

    private String suggestAction;
    private String precautionNote;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    @Version
    private Integer version;

    @TableField(exist = false)
    private String assigneeName;
}
