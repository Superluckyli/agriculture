package lizhuoer.agri.agri_system.module.crop.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.crop.domain.CropBatch;
import lizhuoer.agri.agri_system.module.crop.service.ICropBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/crop/batch")
public class CropBatchController {

    @Autowired
    private ICropBatchService batchService;

    @GetMapping("/list")
    public R<Page<CropBatch>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String plotId,
            String currentStage) {
        Page<CropBatch> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CropBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(plotId), CropBatch::getPlotId, plotId)
                .eq(StrUtil.isNotBlank(currentStage), CropBatch::getCurrentStage, currentStage)
                .eq(CropBatch::getIsActive, 1) // 默认只查活跃的
                .orderByDesc(CropBatch::getSowingDate);
        return R.ok(batchService.page(page, wrapper));
    }

    @PostMapping
    public R<Void> add(@RequestBody CropBatch batch) {
        batchService.save(batch);
        return R.ok();
    }

    @PutMapping
    public R<Void> edit(@RequestBody CropBatch batch) {
        batchService.updateById(batch);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        batchService.removeBatchByIds(Arrays.asList(ids));
        // TODO: 真正的业务逻辑可能不需要物理删除，而是逻辑删除或者归档
        return R.ok();
    }
}
