package lizhuoer.agri.agri_system.module.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.domain.TaskExecutionLog;
import lizhuoer.agri.agri_system.module.task.domain.enums.TaskStatus;
import lizhuoer.agri.agri_system.module.task.mapper.TaskExecutionLogMapper;
import lizhuoer.agri.agri_system.module.task.service.IAgriTaskService;
import lizhuoer.agri.agri_system.module.task.service.ITaskExecutionLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TaskExecutionLogServiceImpl extends ServiceImpl<TaskExecutionLogMapper, TaskExecutionLog>
        implements ITaskExecutionLogService {

    private final IAgriTaskService taskService;

    public TaskExecutionLogServiceImpl(IAgriTaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitLog(TaskExecutionLog log) {
        if (log == null || log.getTaskId() == null) {
            throw new RuntimeException("taskId 不能为空");
        }
        AgriTask task = taskService.getById(log.getTaskId());
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        if (task.getStatus() == null || task.getStatus() < TaskStatus.ACCEPTED.getCode()) {
            throw new RuntimeException("任务未接单，不能提交执行日志");
        }

        if (log.getCreateTime() == null) {
            log.setCreateTime(LocalDateTime.now());
        }
        this.save(log);

        if (log.getStatusSnapshot() != null) {
            if (log.getStatusSnapshot() < TaskStatus.ACCEPTED.getCode()) {
                throw new RuntimeException("执行日志不能把任务状态回写为未接单");
            }
            AgriTask update = new AgriTask();
            update.setTaskId(log.getTaskId());
            update.setStatus(log.getStatusSnapshot());
            update.setUpdateTime(LocalDateTime.now());
            taskService.updateById(update);
        }
    }
}
