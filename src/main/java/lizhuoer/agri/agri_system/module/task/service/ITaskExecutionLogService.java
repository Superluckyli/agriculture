package lizhuoer.agri.agri_system.module.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.module.task.domain.TaskExecutionLog;

public interface ITaskExecutionLogService extends IService<TaskExecutionLog> {
    /**
     * 提交执行记录（会自动更新任务状态）
     */
    void submitLog(TaskExecutionLog log);
}
