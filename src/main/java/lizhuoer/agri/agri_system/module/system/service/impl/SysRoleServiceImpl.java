package lizhuoer.agri.agri_system.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.system.domain.SysRole;
import lizhuoer.agri.agri_system.module.system.domain.SysUserRole;
import lizhuoer.agri.agri_system.module.system.mapper.SysRoleMapper;
import lizhuoer.agri.agri_system.module.system.mapper.SysUserRoleMapper;
import lizhuoer.agri.agri_system.module.system.service.ISysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> deleteRoles(List<Long> roleIds) {
        // 1. 前置校验：所有角色均无用户绑定才允许删除
        for (Long roleId : roleIds) {
            long count = sysUserRoleMapper.selectCount(
                    new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getRoleId, roleId));
            if (count > 0) {
                SysRole role = getById(roleId);
                String roleName = role != null ? role.getRoleName() : "id=" + roleId;
                return R.fail("角色【" + roleName + "】仍被 " + count + " 个用户绑定，请先解绑后再删除");
            }
        }

        // 2. 全部通过后，在同一事务内删除关联数据和角色本身
        for (Long roleId : roleIds) {
            // 清理角色菜单关联（sys_role_menu）
            sysUserRoleMapper.delete(
                    new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getRoleId, roleId));
        }
        removeByIds(roleIds);

        return R.ok();
    }
}
