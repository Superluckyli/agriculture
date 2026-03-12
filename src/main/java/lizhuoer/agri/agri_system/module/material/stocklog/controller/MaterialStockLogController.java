package lizhuoer.agri.agri_system.module.material.stocklog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.material.stocklog.domain.MaterialStockLog;
import lizhuoer.agri.agri_system.module.material.stocklog.service.IMaterialStockLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/material/stock-log")
public class MaterialStockLogController {

    @Autowired
    private IMaterialStockLogService stockLogService;

    @GetMapping("/list")
    public R<Page<MaterialStockLog>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Long materialId, String changeType) {
        Page<MaterialStockLog> page = new Page<>(pageNum, pageSize);
        return R.ok(stockLogService.listByMaterialId(materialId, changeType, page));
    }
}
