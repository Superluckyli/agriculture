package lizhuoer.agri.agri_system.module.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.domain.TaskExecutionLog;
import lizhuoer.agri.agri_system.module.task.mapper.TaskExecutionLogMapper;
import lizhuoer.agri.agri_system.module.task.service.IAgriTaskService;
import lizhuoer.agri.agri_system.module.task.service.ITaskExecutionLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TaskExecutionLogServiceImpl extends ServiceImpl<TaskExecutionLogMapper, TaskExecutionLog>
        implements ITaskExecutionLogService {

    @Autowired
    private IAgriTaskService taskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitLog(TaskExecutionLog log) {
        if (log.getCreateTime() == null) {
            log.setCreateTime(LocalDateTime.now());
        }
        this.save(log);

        // 同步更新任务主表的状态
        if (log.getStatusSnapshot() != null) {
            AgriTask task = new AgriTask();
            task.setTaskId(log.getTaskId());
            task.setStatus(log.getStatusSnapshot()); // 例如更新为 3已完成
            taskService.updateById(task);
        }
    }
}
