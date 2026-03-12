package lizhuoer.agri.agri_system.module.supplier.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.supplier.domain.SupplierInfo;
import lizhuoer.agri.agri_system.module.supplier.service.ISupplierInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/supplier")
public class SupplierInfoController {

    @Autowired
    private ISupplierInfoService supplierService;

    @GetMapping("/list")
    public R<Page<SupplierInfo>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String name) {
        Page<SupplierInfo> page = new Page<>(pageNum, pageSize);
        return R.ok(supplierService.listPage(page, name));
    }

    @GetMapping("/all")
    public R<List<SupplierInfo>> all() {
        return R.ok(supplierService.listAll());
    }

    @PostMapping
    public R<Void> add(@RequestBody SupplierInfo supplier) {
        supplierService.addSupplier(supplier);
        return R.ok();
    }

    @PutMapping
    public R<Void> edit(@RequestBody SupplierInfo supplier) {
        supplierService.updateSupplier(supplier);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        supplierService.deleteSupplier(Arrays.asList(ids));
        return R.ok();
    }
}
