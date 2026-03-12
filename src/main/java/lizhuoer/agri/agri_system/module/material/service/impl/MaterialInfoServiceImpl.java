package lizhuoer.agri.agri_system.module.material.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInfo;
import lizhuoer.agri.agri_system.module.material.mapper.MaterialInfoMapper;
import lizhuoer.agri.agri_system.module.material.service.IMaterialInfoService;
import lizhuoer.agri.agri_system.module.supplier.domain.SupplierInfo;
import lizhuoer.agri.agri_system.module.supplier.mapper.SupplierInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MaterialInfoServiceImpl extends ServiceImpl<MaterialInfoMapper, MaterialInfo>
        implements IMaterialInfoService {

    @Autowired
    private SupplierInfoMapper supplierInfoMapper;

    @Override
    public void addMaterial(MaterialInfo info) {
        long count = count(new LambdaQueryWrapper<MaterialInfo>()
                .eq(MaterialInfo::getName, info.getName()));
        if (count > 0) {
            throw new RuntimeException("物资名称已存在");
        }
        if (info.getSupplierId() != null) {
            SupplierInfo supplier = supplierInfoMapper.selectById(info.getSupplierId());
            if (supplier == null) {
                throw new RuntimeException("供应商不存在");
            }
        }
        save(info);
    }

    @Override
    public void updateMaterial(MaterialInfo info) {
        long count = count(new LambdaQueryWrapper<MaterialInfo>()
                .eq(MaterialInfo::getName, info.getName())
                .ne(MaterialInfo::getMaterialId, info.getMaterialId()));
        if (count > 0) {
            throw new RuntimeException("物资名称已存在");
        }
        if (info.getSupplierId() != null) {
            SupplierInfo supplier = supplierInfoMapper.selectById(info.getSupplierId());
            if (supplier == null) {
                throw new RuntimeException("供应商不存在");
            }
        }
        boolean updated = updateById(info);
        if (!updated) {
            throw new RuntimeException("更新失败，数据已被其他人修改，请刷新后重试");
        }
    }

    @Override
    public void deleteMaterial(List<Long> ids) {
        for (Long id : ids) {
            MaterialInfo material = getById(id);
            if (material == null) {
                throw new RuntimeException("物资不存在: " + id);
            }
            if (material.getCurrentStock() != null
                    && material.getCurrentStock().compareTo(BigDecimal.ZERO) != 0) {
                throw new RuntimeException("物资【" + material.getName() + "】库存不为零，无法删除");
            }
        }
        removeBatchByIds(ids);
    }

    @Override
    public Page<MaterialInfo> listPage(Page<MaterialInfo> page, String name, String category, Long supplierId) {
        LambdaQueryWrapper<MaterialInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), MaterialInfo::getName, name)
                .eq(StrUtil.isNotBlank(category), MaterialInfo::getCategory, category)
                .eq(supplierId != null, MaterialInfo::getSupplierId, supplierId)
                .orderByDesc(MaterialInfo::getMaterialId);
        Page<MaterialInfo> result = page(page, wrapper);
        fillSupplierName(result.getRecords());
        return result;
    }

    @Override
    public List<MaterialInfo> listLowStock() {
        List<MaterialInfo> list = list(new LambdaQueryWrapper<MaterialInfo>()
                .apply("current_stock <= safe_threshold")
                .orderByAsc(MaterialInfo::getCurrentStock));
        fillSupplierName(list);
        return list;
    }

    @Override
    public List<MaterialInfo> listAll() {
        return list(new LambdaQueryWrapper<MaterialInfo>()
                .orderByAsc(MaterialInfo::getName));
    }

    private void fillSupplierName(List<MaterialInfo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> supplierIds = records.stream()
                .map(MaterialInfo::getSupplierId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (supplierIds.isEmpty()) {
            return;
        }
        List<SupplierInfo> suppliers = supplierInfoMapper.selectBatchIds(supplierIds);
        Map<Long, String> nameMap = suppliers.stream()
                .collect(Collectors.toMap(SupplierInfo::getId, SupplierInfo::getName));
        for (MaterialInfo m : records) {
            if (m.getSupplierId() != null) {
                m.setSupplierName(nameMap.get(m.getSupplierId()));
            }
        }
    }
}
