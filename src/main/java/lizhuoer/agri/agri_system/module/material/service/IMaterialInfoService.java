package lizhuoer.agri.agri_system.module.material.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInfo;

import java.util.List;

public interface IMaterialInfoService extends IService<MaterialInfo> {

    void addMaterial(MaterialInfo info);

    void updateMaterial(MaterialInfo info);

    void deleteMaterial(List<Long> ids);

    Page<MaterialInfo> listPage(Page<MaterialInfo> page, String name, String category, Long supplierId);

    List<MaterialInfo> listLowStock();

    List<MaterialInfo> listAll();
}
