package lizhuoer.agri.agri_system.module.crop.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.crop.domain.GrowthStageLog;
import lizhuoer.agri.agri_system.module.crop.service.IGrowthStageLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/crop/growth-log")
public class GrowthStageLogController {

    @Autowired
    private IGrowthStageLogService logService;

    /**
     * 获取某批次的所有生长记录
     */
    @GetMapping("/list/{batchId}")
    public R<List<GrowthStageLog>> list(@PathVariable Long batchId) {
        LambdaQueryWrapper<GrowthStageLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GrowthStageLog::getBatchId, batchId)
                .orderByAsc(GrowthStageLog::getLogDate);
        return R.ok(logService.list(wrapper));
    }

    @PostMapping
    public R<Void> add(@RequestBody GrowthStageLog log) {
        if (log.getLogDate() == null) {
            log.setLogDate(LocalDateTime.now());
        }
        logService.save(log);
        return R.ok();
    }
}
