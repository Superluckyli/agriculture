package lizhuoer.agri.agri_system.module.crop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.crop.batch.domain.AgriCropBatch;
import lizhuoer.agri.agri_system.module.crop.batch.mapper.AgriCropBatchMapper;
import lizhuoer.agri.agri_system.module.crop.domain.BaseCropVariety;
import lizhuoer.agri.agri_system.module.crop.mapper.BaseCropVarietyMapper;
import lizhuoer.agri.agri_system.module.crop.service.IBaseCropVarietyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaseCropVarietyServiceImpl extends ServiceImpl<BaseCropVarietyMapper, BaseCropVariety>
        implements IBaseCropVarietyService {

    @Autowired
    private AgriCropBatchMapper cropBatchMapper;

    @Override
    public List<BaseCropVariety> listAll() {
        return list();
    }

    @Override
    public void deleteVarieties(List<Long> ids) {
        for (Long id : ids) {
            long refCount = cropBatchMapper.selectCount(new LambdaQueryWrapper<AgriCropBatch>()
                    .eq(AgriCropBatch::getVarietyId, id));
            if (refCount > 0) {
                BaseCropVariety variety = getById(id);
                String name = variety != null ? variety.getCropName() : String.valueOf(id);
                throw new RuntimeException("品种【" + name + "】已被种植批次引用，无法删除");
            }
        }
        removeBatchByIds(ids);
    }
}
