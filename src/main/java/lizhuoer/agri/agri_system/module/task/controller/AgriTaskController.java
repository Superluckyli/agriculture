package lizhuoer.agri.agri_system.module.task.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.service.IAgriTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;

@RestController
@RequestMapping("/task")
public class AgriTaskController {

    @Autowired
    private IAgriTaskService taskService;

    @GetMapping("/list")
    public R<Page<AgriTask>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String taskName,
            Integer status,
            Long executorId) {
        Page<AgriTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AgriTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(taskName), AgriTask::getTaskName, taskName)
                .eq(status != null, AgriTask::getStatus, status)
                .eq(executorId != null, AgriTask::getExecutorId, executorId)
                .orderByDesc(AgriTask::getPriority)
                .orderByDesc(AgriTask::getCreateTime);
        return R.ok(taskService.page(page, wrapper));
    }

    @PostMapping
    public R<Void> add(@RequestBody AgriTask task) {
        if (task.getCreateTime() == null) {
            task.setCreateTime(LocalDateTime.now());
        }
        if (task.getStatus() == null) {
            task.setStatus(0); // 默认待分配
        }
        taskService.save(task);
        return R.ok();
    }

    @PutMapping
    public R<Void> edit(@RequestBody AgriTask task) {
        taskService.updateById(task);
        return R.ok();
    }

    /**
     * 指派任务（快捷接口）
     */
    @PutMapping("/assign")
    public R<Void> assign(@RequestBody AgriTask task) {
        // 前端传 taskId 和 executorId
        if (task.getTaskId() == null || task.getExecutorId() == null) {
            return R.fail("参数不全");
        }
        task.setStatus(1); // 变更为待执行
        taskService.updateById(task);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        taskService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }
}
