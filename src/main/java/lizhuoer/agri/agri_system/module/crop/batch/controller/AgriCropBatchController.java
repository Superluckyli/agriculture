package lizhuoer.agri.agri_system.module.crop.batch.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.crop.batch.domain.AgriCropBatch;
import lizhuoer.agri.agri_system.module.crop.batch.service.IAgriCropBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/crop/batch")
public class AgriCropBatchController {

    @Autowired
    private IAgriCropBatchService batchService;

    @GetMapping("/list")
    public R<Page<AgriCropBatch>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String batchNo, String status, Long farmlandId) {
        Page<AgriCropBatch> page = new Page<>(pageNum, pageSize);
        return R.ok(batchService.listPage(page, batchNo, status, farmlandId));
    }

    @PostMapping
    public R<Void> add(@RequestBody AgriCropBatch batch) {
        batchService.createBatch(batch);
        return R.ok();
    }

    @PutMapping
    public R<Void> edit(@RequestBody AgriCropBatch batch) {
        batchService.updateBatch(batch);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        batchService.deleteBatch(Arrays.asList(ids));
        return R.ok();
    }

    @PutMapping("/{id}/start")
    public R<Void> start(@PathVariable Long id) {
        batchService.startBatch(id);
        return R.ok();
    }

    @PutMapping("/{id}/pause")
    public R<Void> pause(@PathVariable Long id) {
        batchService.pauseBatch(id);
        return R.ok();
    }

    @PutMapping("/{id}/harvest")
    public R<Void> harvest(@PathVariable Long id) {
        batchService.harvestBatch(id);
        return R.ok();
    }

    @PutMapping("/{id}/abandon")
    public R<Void> abandon(@PathVariable Long id, @RequestParam(required = false) String reason) {
        batchService.abandonBatch(id, reason);
        return R.ok();
    }

    @PutMapping("/{id}/archive")
    public R<Void> archive(@PathVariable Long id) {
        batchService.archiveBatch(id);
        return R.ok();
    }
}
