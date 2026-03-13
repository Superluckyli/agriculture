package lizhuoer.agri.agri_system.module.crop.batch.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.crop.batch.domain.AgriCropBatch;
import lizhuoer.agri.agri_system.module.crop.batch.mapper.AgriCropBatchMapper;
import lizhuoer.agri.agri_system.module.crop.batch.service.IAgriCropBatchService;
import lizhuoer.agri.agri_system.module.crop.domain.BaseCropVariety;
import lizhuoer.agri.agri_system.module.crop.farmland.domain.AgriFarmland;
import lizhuoer.agri.agri_system.module.crop.farmland.mapper.AgriFarmlandMapper;
import lizhuoer.agri.agri_system.module.crop.mapper.BaseCropVarietyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AgriCropBatchServiceImpl extends ServiceImpl<AgriCropBatchMapper, AgriCropBatch> implements IAgriCropBatchService {

    @Autowired
    private AgriFarmlandMapper farmlandMapper;

    @Autowired
    private BaseCropVarietyMapper varietyMapper;

    // State transition matrix: status -> allowed next statuses
    private static final Map<String, Set<String>> STATE_TRANSITIONS = new HashMap<>();
    static {
        STATE_TRANSITIONS.put("not_started", Set.of("in_progress", "abandoned"));
        STATE_TRANSITIONS.put("in_progress", Set.of("paused", "harvested", "abandoned"));
        STATE_TRANSITIONS.put("paused", Set.of("in_progress", "abandoned"));
        STATE_TRANSITIONS.put("harvested", Set.of("archived"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBatch(AgriCropBatch batch) {
        // Validate farmland exists
        AgriFarmland farmland = farmlandMapper.selectById(batch.getFarmlandId());
        if (farmland == null) {
            throw new RuntimeException("农田不存在");
        }
        // Validate variety exists (if provided)
        if (batch.getVarietyId() != null) {
            BaseCropVariety variety = varietyMapper.selectById(batch.getVarietyId());
            if (variety == null) {
                throw new RuntimeException("作物品种不存在");
            }
            batch.setCropVariety(variety.getCropName());
        }
        batch.setStatus("not_started");
        save(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBatch(AgriCropBatch batch) {
        if (batch.getFarmlandId() != null) {
            AgriFarmland farmland = farmlandMapper.selectById(batch.getFarmlandId());
            if (farmland == null) {
                throw new RuntimeException("农田不存在");
            }
        }
        if (batch.getVarietyId() != null) {
            BaseCropVariety variety = varietyMapper.selectById(batch.getVarietyId());
            if (variety == null) {
                throw new RuntimeException("作物品种不存在");
            }
            batch.setCropVariety(variety.getCropName());
        }
        updateById(batch);
    }

    @Override
    public void deleteBatch(List<Long> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public Page<AgriCropBatch> listPage(Page<AgriCropBatch> page, String batchNo, String status, Long farmlandId) {
        return baseMapper.selectPageWithNames(page, batchNo, status, farmlandId);
    }

    @Override
    public void startBatch(Long id) {
        transitState(id, "in_progress");
    }

    @Override
    public void pauseBatch(Long id) {
        transitState(id, "paused");
    }

    @Override
    public void harvestBatch(Long id) {
        transitState(id, "harvested");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void abandonBatch(Long id, String reason) {
        AgriCropBatch batch = getById(id);
        if (batch == null) {
            throw new RuntimeException("批次不存在");
        }
        validateTransition(batch.getStatus(), "abandoned");
        batch.setStatus("abandoned");
        batch.setAbandonReason(reason);
        updateById(batch);
    }

    @Override
    public void archiveBatch(Long id) {
        transitState(id, "archived");
    }

    private void transitState(Long id, String targetStatus) {
        AgriCropBatch batch = getById(id);
        if (batch == null) {
            throw new RuntimeException("批次不存在");
        }
        validateTransition(batch.getStatus(), targetStatus);
        batch.setStatus(targetStatus);
        updateById(batch);
    }

    private void validateTransition(String currentStatus, String targetStatus) {
        Set<String> allowed = STATE_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(targetStatus)) {
            throw new RuntimeException("非法状态转换: " + currentStatus + " -> " + targetStatus);
        }
    }
}
