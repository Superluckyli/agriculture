package lizhuoer.agri.agri_system.module.task.log.service;

import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.module.task.log.domain.AgriTaskLog;

import java.util.List;

public interface IAgriTaskLogService extends IService<AgriTaskLog> {

    /** 添加任务日志 */
    void addLog(Long taskId, String action, Long operatorId, String content);

    /** 按任务ID查询日志（含操作人姓名），按 createdAt 倒序 */
    List<AgriTaskLog> listByTaskId(Long taskId);
}
