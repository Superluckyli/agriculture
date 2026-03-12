package lizhuoer.agri.agri_system.module.material.stocklog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.material.stocklog.domain.MaterialStockLog;
import lizhuoer.agri.agri_system.module.material.stocklog.mapper.MaterialStockLogMapper;
import lizhuoer.agri.agri_system.module.material.stocklog.service.IMaterialStockLogService;
import org.springframework.stereotype.Service;

@Service
public class MaterialStockLogServiceImpl extends ServiceImpl<MaterialStockLogMapper, MaterialStockLog>
        implements IMaterialStockLogService {

    @Override
    public Page<MaterialStockLog> listByMaterialId(Long materialId, String changeType, Page<MaterialStockLog> page) {
        LambdaQueryWrapper<MaterialStockLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(materialId != null, MaterialStockLog::getMaterialId, materialId)
                .eq(changeType != null, MaterialStockLog::getChangeType, changeType)
                .orderByDesc(MaterialStockLog::getCreatedAt);
        return page(page, wrapper);
    }
}
