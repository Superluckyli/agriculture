package lizhuoer.agri.agri_system.module.material.stocklog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.module.material.stocklog.domain.MaterialStockLog;

public interface IMaterialStockLogService extends IService<MaterialStockLog> {

    Page<MaterialStockLog> listByMaterialId(Long materialId, String changeType, Page<MaterialStockLog> page);
}
