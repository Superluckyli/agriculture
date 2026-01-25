package lizhuoer.agri.agri_system.module.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;

public interface IAgriTaskService extends IService<AgriTask> {
    /**
     * 自动创建任务 (供 IoT 模块调用)
     */
    void createAutoTask(AgriTask task);
}
