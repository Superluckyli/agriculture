package lizhuoer.agri.agri_system.module.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计日志
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog {
    @TableId(type = IdType.AUTO)
    private Long logId;

    private Long userId;
    private String username;
    private String module;
    private String action;
    private String target;
    private String ip;
    private String method;
    private String uri;
    private Integer status;
    private Long duration;
    private LocalDateTime createTime;
}
