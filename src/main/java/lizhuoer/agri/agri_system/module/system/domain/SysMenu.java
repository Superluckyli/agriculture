package lizhuoer.agri.agri_system.module.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 菜单表
 */
@Data
@TableName("sys_menu")
public class SysMenu {
    @TableId(type = IdType.AUTO)
    private Long menuId;

    private Long parentId;
    private String menuName;
    private String path;
    private String perms;
    private Integer type;
    private Integer orderNum;
}
