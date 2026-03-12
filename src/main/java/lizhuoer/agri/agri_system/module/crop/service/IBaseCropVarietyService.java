package lizhuoer.agri.agri_system.module.crop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.module.crop.domain.BaseCropVariety;

import java.util.List;

public interface IBaseCropVarietyService extends IService<BaseCropVariety> {

    List<BaseCropVariety> listAll();
}
