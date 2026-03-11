package lizhuoer.agri.agri_system.module.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户表
 */
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long userId;

    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String realName;
    private String phone;
    private String deptName;
    private Integer status;
    private LocalDateTime createTime;

    /** 角色名列表（非数据库字段） */
    @TableField(exist = false)
    private List<String> roleNames;

    /** 角色ID列表（非数据库字段） */
    @TableField(exist = false)
    private List<Long> roleIds;
}
