package lizhuoer.agri.agri_system.module.crop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.crop.domain.GrowthStageLog;
import lizhuoer.agri.agri_system.module.crop.mapper.GrowthStageLogMapper;
import lizhuoer.agri.agri_system.module.crop.service.IGrowthStageLogService;
import org.springframework.stereotype.Service;

@Service
public class GrowthStageLogServiceImpl extends ServiceImpl<GrowthStageLogMapper, GrowthStageLog>
        implements IGrowthStageLogService {
}
