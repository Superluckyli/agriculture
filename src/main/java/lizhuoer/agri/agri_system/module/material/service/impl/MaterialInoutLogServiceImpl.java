package lizhuoer.agri.agri_system.module.material.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.common.exception.GlobalExceptionHandler;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInfo;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInoutLog;
import lizhuoer.agri.agri_system.module.material.mapper.MaterialInoutLogMapper;
import lizhuoer.agri.agri_system.module.material.service.IMaterialInfoService;
import lizhuoer.agri.agri_system.module.material.service.IMaterialInoutLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class MaterialInoutLogServiceImpl extends ServiceImpl<MaterialInoutLogMapper, MaterialInoutLog>
        implements IMaterialInoutLogService {

    @Autowired
    private IMaterialInfoService materialInfoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeInout(MaterialInoutLog log) {
        if (log.getCreateTime() == null) {
            log.setCreateTime(LocalDateTime.now());
        }

        // 1. 获取物资当前信息
        MaterialInfo info = materialInfoService.getById(log.getMaterialId());
        if (info == null) {
            throw new RuntimeException("物资不存在");
        }

        // 2. 计算新库存
        BigDecimal currentStock = info.getStockQuantity() != null ? info.getStockQuantity() : BigDecimal.ZERO;
        BigDecimal change = log.getQuantity();

        if (log.getType() == 1) {
            // 入库
            currentStock = currentStock.add(change);
        } else if (log.getType() == 2) {
            // 出库
            if (currentStock.compareTo(change) < 0) {
                throw new RuntimeException("库存不足，无法出库");
            }
            currentStock = currentStock.subtract(change);
        }

        // 3. 更新库存表
        info.setStockQuantity(currentStock);
        info.setUpdateTime(LocalDateTime.now());
        materialInfoService.updateById(info);

        // 4. 保存流水
        this.save(log);
    }
}
