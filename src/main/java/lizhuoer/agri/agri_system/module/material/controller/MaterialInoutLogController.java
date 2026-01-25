package lizhuoer.agri.agri_system.module.material.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInoutLog;
import lizhuoer.agri.agri_system.module.material.service.IMaterialInoutLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/material/log")
public class MaterialInoutLogController {

    @Autowired
    private IMaterialInoutLogService inoutLogService;

    @GetMapping("/list")
    public R<Page<MaterialInoutLog>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Long materialId) {
        Page<MaterialInoutLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaterialInoutLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(materialId != null, MaterialInoutLog::getMaterialId, materialId)
                .orderByDesc(MaterialInoutLog::getCreateTime);
        return R.ok(inoutLogService.page(page, wrapper));
    }

    /**
     * 办理入库/出库
     */
    @PostMapping("/execute")
    public R<Void> execute(@RequestBody MaterialInoutLog log) {
        try {
            inoutLogService.executeInout(log);
            return R.ok();
        } catch (RuntimeException e) {
            return R.fail(e.getMessage());
        }
    }
}
