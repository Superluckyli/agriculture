package lizhuoer.agri.agri_system.module.task.log.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agri_task_log")
public class AgriTaskLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long batchId;
    private String action;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private Long targetUserId;
    private String growthNote;
    private String imageUrls;
    private String abnormalNote;
    private String remark;
    private String traceId;
    private LocalDateTime createdAt;

    /** 操作人姓名（非数据库字段） */
    @TableField(exist = false)
    private String operatorName;
}
