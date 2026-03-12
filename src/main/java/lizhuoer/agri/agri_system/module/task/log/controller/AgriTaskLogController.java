package lizhuoer.agri.agri_system.module.task.log.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.task.log.domain.AgriTaskLog;
import lizhuoer.agri.agri_system.module.task.log.service.IAgriTaskLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task/log")
public class AgriTaskLogController {

    @Autowired
    private IAgriTaskLogService taskLogService;

    @GetMapping("/{taskId}")
    public R<List<AgriTaskLog>> listByTask(@PathVariable Long taskId) {
        return R.ok(taskLogService.listByTaskId(taskId));
    }

    @GetMapping("/list")
    public R<Page<AgriTaskLog>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Long taskId, String action) {
        Page<AgriTaskLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AgriTaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(taskId != null, AgriTaskLog::getTaskId, taskId)
                .eq(action != null, AgriTaskLog::getAction, action)
                .orderByDesc(AgriTaskLog::getCreatedAt);
        return R.ok(taskLogService.page(page, wrapper));
    }

    @PostMapping
    public R<Void> add(@RequestBody AgriTaskLog log) {
        taskLogService.save(log);
        return R.ok();
    }

    @PostMapping("/add")
    public R<Void> addLog(@RequestParam Long taskId,
                          @RequestParam String action,
                          @RequestParam(required = false) String content) {
        Long operatorId = LoginUserContext.requireUser().getUserId();
        taskLogService.addLog(taskId, action, operatorId, content);
        return R.ok();
    }
}
