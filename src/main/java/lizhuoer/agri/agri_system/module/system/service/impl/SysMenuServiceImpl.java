package lizhuoer.agri.agri_system.module.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.system.domain.SysMenu;
import lizhuoer.agri.agri_system.module.system.mapper.SysMenuMapper;
import lizhuoer.agri.agri_system.module.system.service.ISysMenuService;
import org.springframework.stereotype.Service;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {
}
