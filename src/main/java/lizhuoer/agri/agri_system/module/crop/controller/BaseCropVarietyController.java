package lizhuoer.agri.agri_system.module.crop.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lizhuoer.agri.agri_system.common.domain.PageResult;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.crop.domain.BaseCropVariety;
import lizhuoer.agri.agri_system.module.crop.service.IBaseCropVarietyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/crop/variety")
public class BaseCropVarietyController {

    @Autowired
    private IBaseCropVarietyService varietyService;

    @GetMapping("/list")
    public R<PageResult<BaseCropVariety>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String cropName) {
        Page<BaseCropVariety> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BaseCropVariety> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(cropName), BaseCropVariety::getCropName, cropName)
                .orderByDesc(BaseCropVariety::getCreateTime);
        return R.ok(PageResult.from(varietyService.page(page, wrapper)));
    }

    @GetMapping("/all")
    public R<List<BaseCropVariety>> all() {
        return R.ok(varietyService.listAll());
    }

    @PostMapping
    @CacheEvict(value = "crop_varieties", allEntries = true)
    public R<Void> add(@Valid @RequestBody BaseCropVariety variety) {
        variety.setCreateTime(null);
        varietyService.save(variety);
        return R.ok();
    }

    @PutMapping
    @CacheEvict(value = "crop_varieties", allEntries = true)
    public R<Void> edit(@Valid @RequestBody BaseCropVariety variety) {
        varietyService.updateById(variety);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        varietyService.deleteVarieties(Arrays.asList(ids));
        return R.ok();
    }
}
