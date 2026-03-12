package lizhuoer.agri.agri_system.module.crop.batch.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.module.crop.batch.domain.AgriCropBatch;

import java.util.List;

public interface IAgriCropBatchService extends IService<AgriCropBatch> {

    void createBatch(AgriCropBatch batch);

    void updateBatch(AgriCropBatch batch);

    void deleteBatch(List<Long> ids);

    Page<AgriCropBatch> listPage(Page<AgriCropBatch> page, String batchNo, String status, Long farmlandId);

    void startBatch(Long id);

    void pauseBatch(Long id);

    void harvestBatch(Long id);

    void abandonBatch(Long id, String reason);

    void archiveBatch(Long id);
}
