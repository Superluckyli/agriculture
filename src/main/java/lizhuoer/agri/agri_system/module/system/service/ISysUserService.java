package lizhuoer.agri.agri_system.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;

import java.util.Set;

public interface ISysUserService extends IService<SysUser> {
    Set<String> getRoleKeys(Long userId);
}
