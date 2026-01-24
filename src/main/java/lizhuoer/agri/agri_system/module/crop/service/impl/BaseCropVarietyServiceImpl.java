package lizhuoer.agri.agri_system.module.crop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.crop.domain.BaseCropVariety;
import lizhuoer.agri.agri_system.module.crop.mapper.BaseCropVarietyMapper;
import lizhuoer.agri.agri_system.module.crop.service.IBaseCropVarietyService;
import org.springframework.stereotype.Service;

@Service
public class BaseCropVarietyServiceImpl extends ServiceImpl<BaseCropVarietyMapper, BaseCropVariety>
        implements IBaseCropVarietyService {
}
