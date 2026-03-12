package lizhuoer.agri.agri_system.module.crop.farmland.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.module.crop.farmland.domain.AgriFarmland;

import java.util.List;

public interface IAgriFarmlandService extends IService<AgriFarmland> {

    void addFarmland(AgriFarmland farmland);

    void updateFarmland(AgriFarmland farmland);

    void deleteFarmland(List<Long> ids);

    Page<AgriFarmland> listPage(Page<AgriFarmland> page, String name, Integer status);

    List<AgriFarmland> listAll();
}
