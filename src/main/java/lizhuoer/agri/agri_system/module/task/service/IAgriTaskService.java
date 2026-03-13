package lizhuoer.agri.agri_system.module.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAcceptDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAssignDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskRejectDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskUpdateDTO;

import java.util.List;

public interface IAgriTaskService extends IService<AgriTask> {
    /**
     * Auto-create task from IoT rule.
     */
    void createAutoTask(AgriTask task);

    /**
     * 手动创建任务（含日志记录）。
     */
    void createTask(AgriTask task, LoginUser operator);

    void assignTask(TaskAssignDTO dto, LoginUser operator, String traceId);

    void acceptTask(TaskAcceptDTO dto, LoginUser operator, String traceId);

    void rejectTask(TaskRejectDTO dto, LoginUser operator, String traceId);

    void completeTask(Long taskId, LoginUser operator, String traceId);

    void updateBasicInfo(TaskUpdateDTO dto, LoginUser operator);

    /**
     * Fill task executor/assignee readable names in batch to avoid N+1 lookups.
     */
    void fillTaskUserNames(List<AgriTask> tasks);

    R<AgriTask> reviewTask(Long taskId, boolean approved, String comment);

    R<AgriTask> suspendTask(Long taskId, String reason);

    R<AgriTask> resumeTask(Long taskId);

    R<AgriTask> cancelTask(Long taskId, String reason);

    R<AgriTask> reassignTask(Long taskId, Long newAssigneeId);
}
