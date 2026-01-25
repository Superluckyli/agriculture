package lizhuoer.agri.agri_system.module.material.service;

import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInoutLog;

public interface IMaterialInoutLogService extends IService<MaterialInoutLog> {
    /**
     * 执行入库或出库
     */
    void executeInout(MaterialInoutLog log);
}
