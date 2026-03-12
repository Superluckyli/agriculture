package lizhuoer.agri.agri_system.module.task.material.service;

import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.task.material.domain.AgriTaskMaterial;

import java.math.BigDecimal;
import java.util.List;

public interface IAgriTaskMaterialService extends IService<AgriTaskMaterial> {

    R<List<AgriTaskMaterial>> addMaterials(Long taskId, List<AgriTaskMaterial> items);

    R<AgriTaskMaterial> updateActualQty(Long id, BigDecimal actualQty, String deviationReason);

    R<List<AgriTaskMaterial>> listByTaskId(Long taskId);
}
