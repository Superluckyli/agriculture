package lizhuoer.agri.agri_system.module.crop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.crop.domain.CropBatch;
import lizhuoer.agri.agri_system.module.crop.mapper.CropBatchMapper;
import lizhuoer.agri.agri_system.module.crop.service.ICropBatchService;
import org.springframework.stereotype.Service;

@Service
public class CropBatchServiceImpl extends ServiceImpl<CropBatchMapper, CropBatch> implements ICropBatchService {
}
