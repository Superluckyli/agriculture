package lizhuoer.agri.agri_system.module.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.mapper.SysUserMapper;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {
    @Override
    public Set<String> getRoleKeys(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        java.util.List<String> roleKeys = baseMapper.selectRoleKeysByUserId(userId);
        if (roleKeys == null) {
            return Collections.emptySet();
        }
        return roleKeys.stream()
                .filter(roleKey -> roleKey != null && !roleKey.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }
}
