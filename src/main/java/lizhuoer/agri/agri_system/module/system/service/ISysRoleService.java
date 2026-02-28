package lizhuoer.agri.agri_system.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.system.domain.SysRole;

import java.util.List;

public interface ISysRoleService extends IService<SysRole> {

    /**
     * 删除角色（含绑定用户检查）
     * 若任一角色仍有用户绑定则拒绝删除
     */
    R<Void> deleteRoles(List<Long> roleIds);
}
