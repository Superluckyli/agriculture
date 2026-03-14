package lizhuoer.agri.agri_system.module.material.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInfo;
import lizhuoer.agri.agri_system.module.material.service.IMaterialInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/material/info")
public class MaterialInfoController {

    @Autowired
    private IMaterialInfoService materialInfoService;

    @GetMapping("/list")
    public R<Page<MaterialInfo>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String name, String category, Long supplierId) {
        Page<MaterialInfo> page = new Page<>(pageNum, pageSize);
        return R.ok(materialInfoService.listPage(page, name, category, supplierId));
    }

    @GetMapping("/low-stock")
    public R<List<MaterialInfo>> lowStock() {
        return R.ok(materialInfoService.listLowStock());
    }

    @GetMapping("/all")
    public R<List<MaterialInfo>> all() {
        return R.ok(materialInfoService.listAll());
    }

    @PostMapping
    public R<Void> add(@Valid @RequestBody MaterialInfo info) {
        materialInfoService.addMaterial(info);
        return R.ok();
    }

    @PutMapping
    public R<Void> edit(@Valid @RequestBody MaterialInfo info) {
        materialInfoService.updateMaterial(info);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        materialInfoService.deleteMaterial(Arrays.asList(ids));
        return R.ok();
    }
}
