package lizhuoer.agri.agri_system.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("""
            SELECT r.role_key
            FROM sys_user_role ur
            INNER JOIN sys_role r ON ur.role_id = r.role_id
            WHERE ur.user_id = #{userId}
            """)
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);
}
