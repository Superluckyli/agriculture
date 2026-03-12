package lizhuoer.agri.agri_system.module.crop.farmland.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.crop.batch.domain.AgriCropBatch;
import lizhuoer.agri.agri_system.module.crop.batch.mapper.AgriCropBatchMapper;
import lizhuoer.agri.agri_system.module.crop.farmland.domain.AgriFarmland;
import lizhuoer.agri.agri_system.module.crop.farmland.mapper.AgriFarmlandMapper;
import lizhuoer.agri.agri_system.module.crop.farmland.service.IAgriFarmlandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgriFarmlandServiceImpl extends ServiceImpl<AgriFarmlandMapper, AgriFarmland> implements IAgriFarmlandService {

    @Autowired
    private AgriCropBatchMapper cropBatchMapper;

    @Override
    public void addFarmland(AgriFarmland farmland) {
        long count = count(new LambdaQueryWrapper<AgriFarmland>()
                .eq(AgriFarmland::getName, farmland.getName()));
        if (count > 0) {
            throw new RuntimeException("农田名称已存在");
        }
        save(farmland);
    }

    @Override
    public void updateFarmland(AgriFarmland farmland) {
        long count = count(new LambdaQueryWrapper<AgriFarmland>()
                .eq(AgriFarmland::getName, farmland.getName())
                .ne(AgriFarmland::getId, farmland.getId()));
        if (count > 0) {
            throw new RuntimeException("农田名称已存在");
        }
        updateById(farmland);
    }

    @Override
    public void deleteFarmland(List<Long> ids) {
        Long refCount = cropBatchMapper.selectCount(new LambdaQueryWrapper<AgriCropBatch>()
                .in(AgriCropBatch::getFarmlandId, ids));
        if (refCount > 0) {
            throw new RuntimeException("所选农田已被作物批次引用，无法删除");
        }
        removeBatchByIds(ids);
    }

    @Override
    public Page<AgriFarmland> listPage(Page<AgriFarmland> page, String name, Integer status) {
        LambdaQueryWrapper<AgriFarmland> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), AgriFarmland::getName, name)
                .eq(status != null, AgriFarmland::getStatus, status)
                .orderByDesc(AgriFarmland::getId);
        return page(page, wrapper);
    }

    @Override
    public List<AgriFarmland> listAll() {
        return list();
    }
}
