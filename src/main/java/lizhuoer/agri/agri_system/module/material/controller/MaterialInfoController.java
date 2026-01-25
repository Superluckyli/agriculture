package lizhuoer.agri.agri_system.module.material.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInfo;
import lizhuoer.agri.agri_system.module.material.service.IMaterialInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;

@RestController
@RequestMapping("/material/info")
public class MaterialInfoController {

    @Autowired
    private IMaterialInfoService materialInfoService;

    @GetMapping("/list")
    public R<Page<MaterialInfo>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String name,
            String category) {
        Page<MaterialInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaterialInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), MaterialInfo::getName, name)
                .eq(StrUtil.isNotBlank(category), MaterialInfo::getCategory, category)
                .orderByDesc(MaterialInfo::getMaterialId);
        return R.ok(materialInfoService.page(page, wrapper));
    }

    @PostMapping
    public R<Void> add(@RequestBody MaterialInfo info) {
        info.setUpdateTime(LocalDateTime.now());
        materialInfoService.save(info);
        return R.ok();
    }

    @PutMapping
    public R<Void> edit(@RequestBody MaterialInfo info) {
        info.setUpdateTime(LocalDateTime.now());
        materialInfoService.updateById(info);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        materialInfoService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }
}
