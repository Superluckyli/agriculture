package lizhuoer.agri.agri_system.module.crop.farmland.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.crop.farmland.domain.AgriFarmland;
import lizhuoer.agri.agri_system.module.crop.farmland.service.IAgriFarmlandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/crop/farmland")
public class AgriFarmlandController {

    @Autowired
    private IAgriFarmlandService farmlandService;

    @GetMapping("/list")
    public R<Page<AgriFarmland>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String name, Integer status) {
        Page<AgriFarmland> page = new Page<>(pageNum, pageSize);
        return R.ok(farmlandService.listPage(page, name, status));
    }

    @GetMapping("/all")
    public R<List<AgriFarmland>> all() {
        return R.ok(farmlandService.listAll());
    }

    @PostMapping
    public R<Void> add(@Valid @RequestBody AgriFarmland farmland) {
        farmlandService.addFarmland(farmland);
        return R.ok();
    }

    @PutMapping
    public R<Void> edit(@Valid @RequestBody AgriFarmland farmland) {
        farmlandService.updateFarmland(farmland);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        farmlandService.deleteFarmland(Arrays.asList(ids));
        return R.ok();
    }
}
