package lizhuoer.agri.agri_system.module.supplier.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.module.supplier.domain.SupplierInfo;

import java.util.List;

public interface ISupplierInfoService extends IService<SupplierInfo> {

    void addSupplier(SupplierInfo supplier);

    void updateSupplier(SupplierInfo supplier);

    void deleteSupplier(List<Long> ids);

    Page<SupplierInfo> listPage(Page<SupplierInfo> page, String name);

    List<SupplierInfo> listAll();
}
