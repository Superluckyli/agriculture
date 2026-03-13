package lizhuoer.agri.agri_system.module.supplier.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInfo;
import lizhuoer.agri.agri_system.module.material.mapper.MaterialInfoMapper;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrder;
import lizhuoer.agri.agri_system.module.purchase.mapper.PurchaseOrderMapper;
import lizhuoer.agri.agri_system.module.supplier.domain.SupplierInfo;
import lizhuoer.agri.agri_system.module.supplier.mapper.SupplierInfoMapper;
import lizhuoer.agri.agri_system.module.supplier.service.ISupplierInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierInfoServiceImpl extends ServiceImpl<SupplierInfoMapper, SupplierInfo> implements ISupplierInfoService {

    @Autowired
    private MaterialInfoMapper materialInfoMapper;

    @Autowired
    private PurchaseOrderMapper purchaseOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSupplier(SupplierInfo supplier) {
        long count = count(new LambdaQueryWrapper<SupplierInfo>()
                .eq(SupplierInfo::getName, supplier.getName()));
        if (count > 0) {
            throw new RuntimeException("供应商名称已存在");
        }
        save(supplier);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupplier(SupplierInfo supplier) {
        long count = count(new LambdaQueryWrapper<SupplierInfo>()
                .eq(SupplierInfo::getName, supplier.getName())
                .ne(SupplierInfo::getId, supplier.getId()));
        if (count > 0) {
            throw new RuntimeException("供应商名称已存在");
        }
        updateById(supplier);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplier(List<Long> ids) {
        Long materialRef = materialInfoMapper.selectCount(new LambdaQueryWrapper<MaterialInfo>()
                .in(MaterialInfo::getSupplierId, ids));
        if (materialRef > 0) {
            throw new RuntimeException("所选供应商已被物资信息引用，无法删除");
        }
        Long orderRef = purchaseOrderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>()
                .in(PurchaseOrder::getSupplierId, ids));
        if (orderRef > 0) {
            throw new RuntimeException("所选供应商已被采购订单引用，无法删除");
        }
        removeBatchByIds(ids);
    }

    @Override
    public Page<SupplierInfo> listPage(Page<SupplierInfo> page, String name) {
        LambdaQueryWrapper<SupplierInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), SupplierInfo::getName, name)
                .orderByDesc(SupplierInfo::getId);
        return page(page, wrapper);
    }

    @Override
    public List<SupplierInfo> listAll() {
        return list();
    }
}
