package lizhuoer.agri.agri_system.module.task.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAcceptDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAssignDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskRejectDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskUpdateDTO;
import lizhuoer.agri.agri_system.module.task.domain.enums.TaskStatusV2;
import lizhuoer.agri.agri_system.module.task.mapper.AgriTaskMapper;
import lizhuoer.agri.agri_system.module.task.service.IAgriTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgriTaskServiceImpl extends ServiceImpl<AgriTaskMapper, AgriTask> implements IAgriTaskService {
    private static final String ROLE_FARMER = "FARMER";
    private static final String ROLE_WORKER = "WORKER";

    private final ISysUserService userService;

    public AgriTaskServiceImpl(ISysUserService userService) {
        this.userService = userService;
    }

    @Override
    public void createAutoTask(AgriTask task) {
        LocalDateTime now = LocalDateTime.now();
        if (task.getCreateTime() == null) {
            task.setCreateTime(now);
        }
        task.setStatusV2(TaskStatusV2.PENDING_ACCEPT);
        task.setUpdateTime(now);
        task.setVersion(0);
        this.save(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTask(TaskAssignDTO dto, LoginUser operator, String traceId) {
        if (dto == null || dto.getTaskId() == null || dto.getAssigneeId() == null) {
            throw new IllegalArgumentException("taskId 和 assigneeId 不能为空");
        }
        if (dto.getTaskId() <= 0 || dto.getAssigneeId() <= 0) {
            throw new IllegalArgumentException("taskId 和 assigneeId 必须大于 0");
        }
        if (operator == null || !operator.hasAnyRole("ADMIN", "FARM_OWNER")) {
            throw new RuntimeException("仅管理员可派单");
        }

        AgriTask task = mustGetTask(dto.getTaskId());
        String assigneeRoleKey = ensureAssigneeValid(dto.getAssigneeId());

        boolean farmerIdempotent = ROLE_FARMER.equals(assigneeRoleKey)
                && TaskStatusV2.PENDING_ACCEPT.equals(task.getStatusV2())
                && Objects.equals(task.getAssigneeId(), dto.getAssigneeId());
        boolean workerIdempotent = ROLE_WORKER.equals(assigneeRoleKey)
                && TaskStatusV2.IN_PROGRESS.equals(task.getStatusV2())
                && Objects.equals(task.getAssigneeId(), dto.getAssigneeId());
        if (farmerIdempotent || workerIdempotent) {
            return;
        }
        if (!TaskStatusV2.PENDING_ACCEPT.equals(task.getStatusV2())) {
            throw new RuntimeException("仅待接单任务可派单");
        }

        String toStatus = ROLE_WORKER.equals(assigneeRoleKey)
                ? TaskStatusV2.IN_PROGRESS
                : TaskStatusV2.PENDING_ACCEPT;
        LocalDateTime now = LocalDateTime.now();
        int updated = baseMapper.assignTask(
                dto.getTaskId(),
                dto.getAssigneeId(),
                operator.getUserId(),
                dto.getRemark(),
                now,
                now,
                TaskStatusV2.PENDING_ACCEPT,
                toStatus,
                safeVersion(task));

        if (updated == 0) {
            AgriTask latest = this.getById(dto.getTaskId());
            if (latest != null
                    && toStatus.equals(latest.getStatusV2())
                    && Objects.equals(latest.getAssigneeId(), dto.getAssigneeId())) {
                return;
            }
            throw new RuntimeException("任务状态已变化，派单失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptTask(TaskAcceptDTO dto, LoginUser operator, String traceId) {
        if (dto == null || dto.getTaskId() == null) {
            throw new RuntimeException("taskId 不能为空");
        }
        if (operator == null) {
            throw new RuntimeException("仅农户可接单");
        }
        if (operator.hasRole(ROLE_WORKER) && !operator.hasRole(ROLE_FARMER)) {
            throw new RuntimeException("WORKER无需接单/拒单，请直接执行任务");
        }
        if (!operator.hasRole(ROLE_FARMER)) {
            throw new RuntimeException("仅农户可接单");
        }

        AgriTask task = mustGetTask(dto.getTaskId());
        if (TaskStatusV2.IN_PROGRESS.equals(task.getStatusV2())
                && Objects.equals(task.getAcceptBy(), operator.getUserId())) {
            return;
        }
        if (!TaskStatusV2.PENDING_ACCEPT.equals(task.getStatusV2())) {
            throw new RuntimeException("仅待接单任务可接单");
        }
        if (!Objects.equals(task.getAssigneeId(), operator.getUserId())) {
            throw new RuntimeException("仅被派单农户可接单");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = baseMapper.acceptTask(
                dto.getTaskId(),
                operator.getUserId(),
                operator.getUserId(),
                now,
                now,
                TaskStatusV2.PENDING_ACCEPT,
                TaskStatusV2.IN_PROGRESS,
                safeVersion(task));

        if (updated == 0) {
            AgriTask latest = this.getById(dto.getTaskId());
            if (latest != null
                    && TaskStatusV2.IN_PROGRESS.equals(latest.getStatusV2())
                    && Objects.equals(latest.getAcceptBy(), operator.getUserId())) {
                return;
            }
            throw new RuntimeException("任务状态已变化，接单失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTask(TaskRejectDTO dto, LoginUser operator, String traceId) {
        if (dto == null || dto.getTaskId() == null) {
            throw new RuntimeException("taskId 不能为空");
        }
        if (operator == null) {
            throw new RuntimeException("仅农户可拒单");
        }
        if (operator.hasRole(ROLE_WORKER) && !operator.hasRole(ROLE_FARMER)) {
            throw new RuntimeException("WORKER无需接单/拒单，请直接执行任务");
        }
        if (!operator.hasRole(ROLE_FARMER)) {
            throw new RuntimeException("仅农户可拒单");
        }

        AgriTask task = mustGetTask(dto.getTaskId());
        if (TaskStatusV2.PENDING_ACCEPT.equals(task.getStatusV2())
                && Objects.equals(task.getRejectBy(), operator.getUserId())) {
            return;
        }
        if (!TaskStatusV2.PENDING_ACCEPT.equals(task.getStatusV2())) {
            throw new RuntimeException("仅待接单任务可拒单");
        }
        if (!Objects.equals(task.getAssigneeId(), operator.getUserId())) {
            throw new RuntimeException("仅被派单农户可拒单");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = baseMapper.rejectTask(
                dto.getTaskId(),
                operator.getUserId(),
                operator.getUserId(),
                dto.getReason(),
                now,
                now,
                TaskStatusV2.PENDING_ACCEPT,
                TaskStatusV2.REJECTED_REASSIGN,
                safeVersion(task));

        if (updated == 0) {
            AgriTask latest = this.getById(dto.getTaskId());
            if (latest != null
                    && TaskStatusV2.REJECTED_REASSIGN.equals(latest.getStatusV2())
                    && Objects.equals(latest.getRejectBy(), operator.getUserId())) {
                return;
            }
            throw new RuntimeException("任务状态已变化，拒单失败");
        }
    }

    @Override
    public void updateBasicInfo(TaskUpdateDTO dto, LoginUser operator) {
        if (dto == null || dto.getTaskId() == null) {
            throw new RuntimeException("taskId 不能为空");
        }
        AgriTask task = new AgriTask();
        task.setTaskId(dto.getTaskId());
        task.setBatchId(dto.getBatchId());
        task.setTaskName(dto.getTaskName());
        task.setTaskType(dto.getTaskType());
        task.setPriority(dto.getPriority());
        task.setPlanTime(dto.getPlanTime());
        task.setUpdateTime(LocalDateTime.now());
        if (operator != null) {
            task.setUpdateBy(operator.getUserId());
        }
        if (!this.updateById(task)) {
            throw new RuntimeException("任务不存在或更新失败");
        }
    }

    @Override
    public void fillTaskUserNames(List<AgriTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        Set<Long> userIds = tasks.stream()
                .map(AgriTask::getAssigneeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> userNameMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            userNameMap = userService.listByIds(userIds).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(
                            SysUser::getUserId,
                            this::toDisplayName,
                            (left, right) -> left));
        }

        for (AgriTask task : tasks) {
            task.setAssigneeName(resolveUserName(task.getAssigneeId(), userNameMap));
        }
    }

    private AgriTask mustGetTask(Long taskId) {
        AgriTask task = this.getById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        return task;
    }

    private String ensureAssigneeValid(Long assigneeId) {
        SysUser assignee = userService.getById(assigneeId);
        if (assignee == null) {
            throw new RuntimeException("派单执行人不存在");
        }
        if (assignee.getStatus() == null || assignee.getStatus() != 1) {
            throw new RuntimeException("派单执行人已禁用");
        }
        String roleKey = resolveSupportedRoleKey(userService.getRoleKeys(assigneeId));
        if (roleKey == null) {
            throw new RuntimeException("仅支持派给 FARMER/WORKER");
        }
        return roleKey;
    }

    private int safeVersion(AgriTask task) {
        return task.getVersion() == null ? 0 : task.getVersion();
    }

    private String toDisplayName(SysUser user) {
        if (user == null) {
            return null;
        }
        if (StrUtil.isNotBlank(user.getRealName())) {
            return user.getRealName();
        }
        return StrUtil.isBlank(user.getUsername()) ? null : user.getUsername();
    }

    private String resolveUserName(Long userId, Map<Long, String> userNameMap) {
        if (userId == null) {
            return null;
        }
        return userNameMap.get(userId);
    }

    private String resolveSupportedRoleKey(Set<String> roleKeys) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return null;
        }
        if (roleKeys.contains(ROLE_FARMER)) {
            return ROLE_FARMER;
        }
        if (roleKeys.contains(ROLE_WORKER)) {
            return ROLE_WORKER;
        }
        return null;
    }
}
