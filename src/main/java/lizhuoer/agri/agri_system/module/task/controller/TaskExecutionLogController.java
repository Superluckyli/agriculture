package lizhuoer.agri.agri_system.module.task.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.task.domain.TaskExecutionLog;
import lizhuoer.agri.agri_system.module.task.service.ITaskExecutionLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/task/log")
public class TaskExecutionLogController {

    @Autowired
    private ITaskExecutionLogService logService;

    @GetMapping("/list")
    public R<Page<TaskExecutionLog>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Long taskId) {
        Page<TaskExecutionLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TaskExecutionLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(taskId != null, TaskExecutionLog::getTaskId, taskId)
                .orderByDesc(TaskExecutionLog::getCreateTime);
        return R.ok(logService.page(page, wrapper));
    }

    @PostMapping
    public R<Void> add(@RequestBody TaskExecutionLog log) {
        logService.submitLog(log);
        return R.ok();
    }
}
