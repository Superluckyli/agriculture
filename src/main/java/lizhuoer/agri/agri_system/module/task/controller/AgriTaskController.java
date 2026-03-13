package lizhuoer.agri.agri_system.module.task.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAcceptDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAssignDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskRejectDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskUpdateDTO;
import lizhuoer.agri.agri_system.module.task.service.IAgriTaskService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/task")
public class AgriTaskController {

    private final IAgriTaskService taskService;

    public AgriTaskController(IAgriTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/list")
    public R<Page<AgriTask>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String taskName,
            String statusV2,
            Long assigneeId) {
        Page<AgriTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AgriTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(taskName), AgriTask::getTaskName, taskName)
                .eq(StrUtil.isNotBlank(statusV2), AgriTask::getStatusV2, statusV2)
                .eq(assigneeId != null, AgriTask::getAssigneeId, assigneeId)
                .orderByDesc(AgriTask::getPriority)
                .orderByDesc(AgriTask::getCreateTime);
        Page<AgriTask> taskPage = taskService.page(page, wrapper);
        taskService.fillTaskUserNames(taskPage.getRecords());
        return R.ok(taskPage);
    }

    @PostMapping
    public R<Void> add(@RequestBody AgriTask task) {
        LoginUser loginUser = LoginUserContext.get();
        taskService.createTask(task, loginUser);
        return R.ok();
    }

    @PutMapping
    public R<Void> edit(@RequestBody TaskUpdateDTO dto) {
        taskService.updateBasicInfo(dto, LoginUserContext.get());
        return R.ok();
    }

    @PutMapping("/assign")
    public R<Void> assign(@RequestBody TaskAssignDTO dto, HttpServletRequest request) {
        validateAssignRequest(dto);
        LoginUser loginUser = LoginUserContext.requireUser();
        taskService.assignTask(dto, loginUser, request.getHeader("X-Trace-Id"));
        return R.ok();
    }

    @PostMapping("/accept")
    public R<Void> accept(@RequestBody TaskAcceptDTO dto, HttpServletRequest request) {
        LoginUser loginUser = LoginUserContext.requireUser();
        taskService.acceptTask(dto, loginUser, request.getHeader("X-Trace-Id"));
        return R.ok();
    }

    @PostMapping("/reject")
    public R<Void> reject(@RequestBody TaskRejectDTO dto, HttpServletRequest request) {
        LoginUser loginUser = LoginUserContext.requireUser();
        taskService.rejectTask(dto, loginUser, request.getHeader("X-Trace-Id"));
        return R.ok();
    }

    @PostMapping("/{id}/complete")
    public R<Void> complete(@PathVariable Long id, HttpServletRequest request) {
        LoginUser loginUser = LoginUserContext.requireUser();
        taskService.completeTask(id, loginUser, request.getHeader("X-Trace-Id"));
        return R.ok();
    }

    @PutMapping("/{id}/review")
    public R<AgriTask> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean approved = (Boolean) body.get("approved");
        String comment = (String) body.get("comment");
        if (approved == null) {
            throw new IllegalArgumentException("approved 不能为空");
        }
        return taskService.reviewTask(id, approved, comment);
    }

    @PutMapping("/{id}/suspend")
    public R<AgriTask> suspend(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return taskService.suspendTask(id, body.get("reason"));
    }

    @PutMapping("/{id}/resume")
    public R<AgriTask> resume(@PathVariable Long id) {
        return taskService.resumeTask(id);
    }

    @DeleteMapping("/{id}/cancel")
    public R<AgriTask> cancel(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return taskService.cancelTask(id, body.get("reason"));
    }

    @PutMapping("/{id}/reassign")
    public R<AgriTask> reassign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long assigneeId = body.get("assigneeId");
        if (assigneeId == null) {
            throw new IllegalArgumentException("assigneeId 不能为空");
        }
        return taskService.reassignTask(id, assigneeId);
    }

    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        taskService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }

    private void validateAssignRequest(TaskAssignDTO dto) {
        if (dto == null || dto.getTaskId() == null || dto.getAssigneeId() == null) {
            throw new IllegalArgumentException("taskId 和 assigneeId 不能为空");
        }
        if (dto.getTaskId() <= 0 || dto.getAssigneeId() <= 0) {
            throw new IllegalArgumentException("taskId 和 assigneeId 必须大于 0");
        }
    }
}
