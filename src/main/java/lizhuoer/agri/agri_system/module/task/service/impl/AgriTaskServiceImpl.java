package lizhuoer.agri.agri_system.module.task.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.domain.TaskFlowLog;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAcceptDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAssignDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskRejectDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskUpdateDTO;
import lizhuoer.agri.agri_system.module.task.domain.enums.TaskStatus;
import lizhuoer.agri.agri_system.module.task.mapper.AgriTaskMapper;
import lizhuoer.agri.agri_system.module.task.mapper.TaskFlowLogMapper;
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
import java.util.stream.Stream;

@Service
public class AgriTaskServiceImpl extends ServiceImpl<AgriTaskMapper, AgriTask> implements IAgriTaskService {
    private static final String ROLE_FARMER = "FARMER";
    private static final String ROLE_WORKER = "WORKER";

    private final ISysUserService userService;
    private final TaskFlowLogMapper taskFlowLogMapper;

    public AgriTaskServiceImpl(ISysUserService userService, TaskFlowLogMapper taskFlowLogMapper) {
        this.userService = userService;
        this.taskFlowLogMapper = taskFlowLogMapper;
    }

    @Override
    public void createAutoTask(AgriTask task) {
        LocalDateTime now = LocalDateTime.now();
        if (task.getCreateTime() == null) {
            task.setCreateTime(now);
        }
        task.setStatus(TaskStatus.PENDING_ASSIGN.getCode());
        task.setUpdateTime(now);
        task.setVersion(0);
        this.save(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTask(TaskAssignDTO dto, LoginUser operator, String traceId) {
        if (dto == null || dto.getTaskId() == null || dto.getExecutorId() == null) {
            throw new IllegalArgumentException("taskId 和 executorId 不能为空");
        }
        if (dto.getTaskId() <= 0 || dto.getExecutorId() <= 0) {
            throw new IllegalArgumentException("taskId 和 executorId 必须大于 0");
        }
        if (operator == null || !operator.hasAnyRole("ADMIN", "FARM_OWNER")) {
            throw new RuntimeException("仅管理员可派单");
        }

        AgriTask task = mustGetTask(dto.getTaskId());
        String executorRoleKey = ensureExecutorValid(dto.getExecutorId());
        boolean farmerAssignIdempotent = ROLE_FARMER.equals(executorRoleKey)
                && Objects.equals(task.getStatus(), TaskStatus.PENDING_ACCEPT.getCode())
                && Objects.equals(task.getAssigneeId(), dto.getExecutorId());
        boolean workerAssignIdempotent = ROLE_WORKER.equals(executorRoleKey)
                && Objects.equals(task.getStatus(), TaskStatus.ACCEPTED.getCode())
                && Objects.equals(task.getAssigneeId(), dto.getExecutorId());
        if (farmerAssignIdempotent || workerAssignIdempotent) {
            return;
        }
        if (!Objects.equals(task.getStatus(), TaskStatus.PENDING_ASSIGN.getCode())) {
            throw new RuntimeException("仅待分配任务可派单");
        }

        int toStatus = ROLE_WORKER.equals(executorRoleKey)
                ? TaskStatus.ACCEPTED.getCode()
                : TaskStatus.PENDING_ACCEPT.getCode();
        LocalDateTime now = LocalDateTime.now();
        int updated = baseMapper.assignTask(
                dto.getTaskId(),
                dto.getExecutorId(),
                operator.getUserId(),
                dto.getRemark(),
                now,
                now,
                TaskStatus.PENDING_ASSIGN.getCode(),
                toStatus,
                safeVersion(task));

        if (updated == 0) {
            AgriTask latest = this.getById(dto.getTaskId());
            if (latest != null
                    && Objects.equals(latest.getStatus(), toStatus)
                    && Objects.equals(latest.getAssigneeId(), dto.getExecutorId())) {
                return;
            }
            throw new RuntimeException("任务状态已变化，派单失败");
        }

        insertFlowLog(dto.getTaskId(), "assign",
                TaskStatus.PENDING_ASSIGN.getCode(), toStatus,
                operator.getUserId(), dto.getExecutorId(), dto.getRemark(), traceId, now);
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
        if (Objects.equals(task.getStatus(), TaskStatus.ACCEPTED.getCode())
                && Objects.equals(task.getAcceptBy(), operator.getUserId())) {
            return;
        }
        if (!Objects.equals(task.getStatus(), TaskStatus.PENDING_ACCEPT.getCode())) {
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
                TaskStatus.PENDING_ACCEPT.getCode(),
                TaskStatus.ACCEPTED.getCode(),
                safeVersion(task));

        if (updated == 0) {
            AgriTask latest = this.getById(dto.getTaskId());
            if (latest != null
                    && Objects.equals(latest.getStatus(), TaskStatus.ACCEPTED.getCode())
                    && Objects.equals(latest.getAcceptBy(), operator.getUserId())) {
                return;
            }
            throw new RuntimeException("任务状态已变化，接单失败");
        }

        insertFlowLog(dto.getTaskId(), "accept",
                TaskStatus.PENDING_ACCEPT.getCode(), TaskStatus.ACCEPTED.getCode(),
                operator.getUserId(), operator.getUserId(), null, traceId, now);
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
        if (Objects.equals(task.getStatus(), TaskStatus.PENDING_ASSIGN.getCode())
                && Objects.equals(task.getRejectBy(), operator.getUserId())) {
            return;
        }
        if (!Objects.equals(task.getStatus(), TaskStatus.PENDING_ACCEPT.getCode())) {
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
                TaskStatus.PENDING_ACCEPT.getCode(),
                TaskStatus.PENDING_ASSIGN.getCode(),
                safeVersion(task));

        if (updated == 0) {
            AgriTask latest = this.getById(dto.getTaskId());
            if (latest != null
                    && Objects.equals(latest.getStatus(), TaskStatus.PENDING_ASSIGN.getCode())
                    && Objects.equals(latest.getRejectBy(), operator.getUserId())) {
                return;
            }
            throw new RuntimeException("任务状态已变化，拒单失败");
        }

        insertFlowLog(dto.getTaskId(), "reject",
                TaskStatus.PENDING_ACCEPT.getCode(), TaskStatus.PENDING_ASSIGN.getCode(),
                operator.getUserId(), operator.getUserId(), dto.getReason(), traceId, now);
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
                .flatMap(task -> Stream.of(task.getExecutorId(), task.getAssigneeId()))
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

        Set<Long> executorIds = tasks.stream()
                .map(AgriTask::getExecutorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> executorRoleMap = Collections.emptyMap();
        if (!executorIds.isEmpty()) {
            executorRoleMap = new java.util.HashMap<>();
            for (Long executorId : executorIds) {
                String roleKey = resolveExecutorRoleKeyByUserId(executorId);
                if (roleKey != null) {
                    executorRoleMap.put(executorId, roleKey);
                }
            }
        }

        for (AgriTask task : tasks) {
            task.setExecutorName(resolveUserName(task.getExecutorId(), userNameMap));
            task.setAssigneeName(resolveUserName(task.getAssigneeId(), userNameMap));

            String executorRoleKey = resolveUserName(task.getExecutorId(), executorRoleMap);
            task.setExecutorRoleKey(executorRoleKey);
            task.setExecutorRoleName(resolveRoleName(executorRoleKey));
        }
    }

    private AgriTask mustGetTask(Long taskId) {
        AgriTask task = this.getById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        return task;
    }

    private String ensureExecutorValid(Long executorId) {
        SysUser executor = userService.getById(executorId);
        if (executor == null) {
            throw new RuntimeException("派单执行人不存在");
        }
        if (executor.getStatus() == null || executor.getStatus() != 1) {
            throw new RuntimeException("派单执行人已禁用");
        }
        String roleKey = resolveSupportedRoleKey(userService.getRoleKeys(executorId));
        if (roleKey == null) {
            throw new RuntimeException("仅支持派给 FARMER/WORKER");
        }
        return roleKey;
    }

    private int safeVersion(AgriTask task) {
        return task.getVersion() == null ? 0 : task.getVersion();
    }

    private void insertFlowLog(Long taskId, String action, Integer fromStatus, Integer toStatus,
            Long operatorId, Long targetUserId, String remark, String traceId, LocalDateTime now) {
        TaskFlowLog log = new TaskFlowLog();
        log.setTaskId(taskId);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(operatorId);
        log.setTargetUserId(targetUserId);
        log.setRemark(StrUtil.emptyToDefault(remark, null));
        log.setTraceId(StrUtil.emptyToDefault(traceId, null));
        log.setCreateTime(now);
        taskFlowLogMapper.insert(log);
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

    private String resolveExecutorRoleKeyByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return resolveSupportedRoleKey(userService.getRoleKeys(userId));
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

    private String resolveRoleName(String roleKey) {
        if (ROLE_FARMER.equals(roleKey)) {
            return "农户";
        }
        if (ROLE_WORKER.equals(roleKey)) {
            return "工人";
        }
        return null;
    }
}
