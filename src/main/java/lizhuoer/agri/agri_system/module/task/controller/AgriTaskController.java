package lizhuoer.agri.agri_system.module.task.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lizhuoer.agri.agri_system.common.domain.PageResult;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.common.security.AuditLog;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.common.security.RequirePermission;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAcceptDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAssignDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskCreateRequest;
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
    public R<PageResult<AgriTask>> list(@RequestParam(defaultValue = "1") Integer pageNum,
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
        return R.ok(PageResult.from(taskPage));
    }

    @PostMapping
    public R<Void> add(@Valid @RequestBody TaskCreateRequest req) {
        LoginUser loginUser = LoginUserContext.get();
        AgriTask task = new AgriTask();
        task.setTaskName(req.getTaskName());
        task.setBatchId(req.getBatchId());
        task.setTaskType(req.getTaskType());
        task.setPriority(req.getPriority());
        task.setDeadlineAt(req.getDeadlineAt());
        task.setPlanTime(req.getPlanTime());
        taskService.createTask(task, loginUser);
        return R.ok();
    }

    @PutMapping
    public R<Void> edit(@Valid @RequestBody TaskUpdateDTO dto) {
        taskService.updateBasicInfo(dto, LoginUserContext.get());
        return R.ok();
    }

    @PutMapping("/assign")
    @RequirePermission(roles = {"ADMIN", "FARM_OWNER", "TECHNICIAN"})
    @AuditLog(module = "任务管理", action = "ASSIGN", target = "任务")
    public R<Void> assign(@Valid @RequestBody TaskAssignDTO dto, HttpServletRequest request) {
        LoginUser loginUser = LoginUserContext.requireUser();
        taskService.assignTask(dto, loginUser, request.getHeader("X-Trace-Id"));
        return R.ok();
    }

    @PutMapping("/{id}/accept")
    public R<Void> accept(@PathVariable Long id, HttpServletRequest request) {
        LoginUser loginUser = LoginUserContext.requireUser();
        TaskAcceptDTO dto = new TaskAcceptDTO();
        dto.setTaskId(id);
        taskService.acceptTask(dto, loginUser, request.getHeader("X-Trace-Id"));
        return R.ok();
    }

    @PutMapping("/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @Valid @RequestBody TaskRejectDTO dto, HttpServletRequest request) {
        LoginUser loginUser = LoginUserContext.requireUser();
        dto.setTaskId(id);
        taskService.rejectTask(dto, loginUser, request.getHeader("X-Trace-Id"));
        return R.ok();
    }

    @PutMapping("/{id}/complete")
    public R<Void> complete(@PathVariable Long id, HttpServletRequest request) {
        LoginUser loginUser = LoginUserContext.requireUser();
        taskService.completeTask(id, loginUser, request.getHeader("X-Trace-Id"));
        return R.ok();
    }

    @PutMapping("/{id}/review")
    @RequirePermission(roles = {"ADMIN", "FARM_OWNER", "MANAGER", "TECHNICIAN"})
    @AuditLog(module = "任务管理", action = "REVIEW", target = "任务")
    public R<AgriTask> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean approved = (Boolean) body.get("approved");
        String comment = (String) body.get("comment");
        if (approved == null) {
            throw new IllegalArgumentException("approved 不能为空");
        }
        return taskService.reviewTask(id, approved, comment);
    }

    @PutMapping("/{id}/reassign")
    @RequirePermission(roles = {"ADMIN", "FARM_OWNER"})
    public R<AgriTask> reassign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long assigneeId = body.get("assigneeId");
        if (assigneeId == null) {
            throw new IllegalArgumentException("assigneeId 不能为空");
        }
        return taskService.reassignTask(id, assigneeId);
    }

    @DeleteMapping("/{ids}")
    @RequirePermission(roles = {"ADMIN", "FARM_OWNER"})
    @AuditLog(module = "任务管理", action = "DELETE", target = "任务")
    public R<Void> remove(@PathVariable Long[] ids) {
        taskService.deleteTasks(Arrays.asList(ids));
        return R.ok();
    }

}
