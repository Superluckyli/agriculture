package lizhuoer.agri.agri_system.module.task.service.impl;

import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.domain.TaskFlowLog;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAssignDTO;
import lizhuoer.agri.agri_system.module.task.domain.enums.TaskStatus;
import lizhuoer.agri.agri_system.module.task.mapper.AgriTaskMapper;
import lizhuoer.agri.agri_system.module.task.mapper.TaskFlowLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgriTaskServiceImplAssignTaskTest {

    @Mock
    private ISysUserService userService;
    @Mock
    private TaskFlowLogMapper taskFlowLogMapper;
    @Mock
    private AgriTaskMapper agriTaskMapper;

    private AgriTaskServiceImpl service;
    private LoginUser operator;

    @BeforeEach
    void setUp() {
        service = Mockito.spy(new AgriTaskServiceImpl(userService, taskFlowLogMapper));
        ReflectionTestUtils.setField(service, "baseMapper", agriTaskMapper);
        operator = new LoginUser(1L, "admin", Set.of("ADMIN"));
    }

    @Test
    void assignTaskShouldSucceedWithTaskIdAndExecutorId() {
        TaskAssignDTO dto = assignDto(1L, 3L, null);
        doReturn(pendingAssignTask(1L)).when(service).getById(1L);
        when(userService.getById(3L)).thenReturn(activeUser(3L));
        when(userService.getRoleKeys(3L)).thenReturn(Set.of("FARMER"));
        when(agriTaskMapper.assignTask(eq(1L), eq(3L), eq(1L), isNull(),
                any(LocalDateTime.class), any(LocalDateTime.class),
                eq(TaskStatus.PENDING_ASSIGN.getCode()),
                eq(TaskStatus.PENDING_ACCEPT.getCode()), eq(0)))
                .thenReturn(1);

        service.assignTask(dto, operator, "trace-assign-1");

        ArgumentCaptor<TaskFlowLog> logCaptor = ArgumentCaptor.forClass(TaskFlowLog.class);
        verify(taskFlowLogMapper).insert(logCaptor.capture());
        assertEquals(1L, logCaptor.getValue().getTaskId());
        assertEquals(3L, logCaptor.getValue().getTargetUserId());
        assertNull(logCaptor.getValue().getRemark());
    }

    @Test
    void assignTaskShouldSucceedWithRemark() {
        TaskAssignDTO dto = assignDto(1L, 3L, "Assigned by flow");
        doReturn(pendingAssignTask(1L)).when(service).getById(1L);
        when(userService.getById(3L)).thenReturn(activeUser(3L));
        when(userService.getRoleKeys(3L)).thenReturn(Set.of("FARMER"));
        when(agriTaskMapper.assignTask(eq(1L), eq(3L), eq(1L), eq("Assigned by flow"),
                any(LocalDateTime.class), any(LocalDateTime.class),
                eq(TaskStatus.PENDING_ASSIGN.getCode()),
                eq(TaskStatus.PENDING_ACCEPT.getCode()), eq(0)))
                .thenReturn(1);

        service.assignTask(dto, operator, "trace-assign-2");

        ArgumentCaptor<TaskFlowLog> logCaptor = ArgumentCaptor.forClass(TaskFlowLog.class);
        verify(taskFlowLogMapper).insert(logCaptor.capture());
        assertEquals("Assigned by flow", logCaptor.getValue().getRemark());
        assertEquals(3L, logCaptor.getValue().getTargetUserId());
    }

    @Test
    void assignTaskShouldFailWhenExecutorNotExists() {
        TaskAssignDTO dto = assignDto(1L, 99L, null);
        doReturn(pendingAssignTask(1L)).when(service).getById(1L);
        when(userService.getById(99L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.assignTask(dto, operator, "trace-fail-1"));

        assertEquals("派单执行人不存在", ex.getMessage());
        verify(agriTaskMapper, never()).assignTask(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void assignTaskShouldFailWhenExecutorDisabled() {
        TaskAssignDTO dto = assignDto(1L, 99L, null);
        SysUser disabledExecutor = activeUser(99L);
        disabledExecutor.setStatus(0);
        doReturn(pendingAssignTask(1L)).when(service).getById(1L);
        when(userService.getById(99L)).thenReturn(disabledExecutor);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.assignTask(dto, operator, "trace-fail-2"));

        assertEquals("派单执行人已禁用", ex.getMessage());
        verify(agriTaskMapper, never()).assignTask(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void assignTaskShouldFailWhenExecutorRoleInvalid() {
        TaskAssignDTO dto = assignDto(1L, 99L, null);
        doReturn(pendingAssignTask(1L)).when(service).getById(1L);
        when(userService.getById(99L)).thenReturn(activeUser(99L));
        when(userService.getRoleKeys(99L)).thenReturn(Set.of("ADMIN"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.assignTask(dto, operator, "trace-fail-3"));

        assertEquals("仅支持派给 FARMER/WORKER", ex.getMessage());
        verify(agriTaskMapper, never()).assignTask(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void assignTaskShouldSucceedWhenExecutorRoleWorkerAndEnterAccepted() {
        TaskAssignDTO dto = assignDto(1L, 5L, null);
        doReturn(pendingAssignTask(1L)).when(service).getById(1L);
        when(userService.getById(5L)).thenReturn(activeUser(5L));
        when(userService.getRoleKeys(5L)).thenReturn(Set.of("WORKER"));
        when(agriTaskMapper.assignTask(eq(1L), eq(5L), eq(1L), isNull(),
                any(LocalDateTime.class), any(LocalDateTime.class),
                eq(TaskStatus.PENDING_ASSIGN.getCode()),
                eq(TaskStatus.ACCEPTED.getCode()), eq(0)))
                .thenReturn(1);

        service.assignTask(dto, operator, "trace-assign-worker");

        ArgumentCaptor<TaskFlowLog> logCaptor = ArgumentCaptor.forClass(TaskFlowLog.class);
        verify(taskFlowLogMapper).insert(logCaptor.capture());
        assertEquals(1L, logCaptor.getValue().getTaskId());
        assertEquals(TaskStatus.ACCEPTED.getCode(), logCaptor.getValue().getToStatus());
        assertEquals(5L, logCaptor.getValue().getTargetUserId());
    }

    private static TaskAssignDTO assignDto(Long taskId, Long executorId, String remark) {
        TaskAssignDTO dto = new TaskAssignDTO();
        dto.setTaskId(taskId);
        dto.setExecutorId(executorId);
        dto.setRemark(remark);
        return dto;
    }

    private static AgriTask pendingAssignTask(Long taskId) {
        AgriTask task = new AgriTask();
        task.setTaskId(taskId);
        task.setStatus(TaskStatus.PENDING_ASSIGN.getCode());
        task.setVersion(0);
        return task;
    }

    private static SysUser activeUser(Long userId) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setStatus(1);
        return user;
    }
}
