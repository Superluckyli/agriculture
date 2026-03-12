package lizhuoer.agri.agri_system.module.task.service.impl;

import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAssignDTO;
import lizhuoer.agri.agri_system.module.task.domain.enums.TaskStatusV2;
import lizhuoer.agri.agri_system.module.task.mapper.AgriTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgriTaskServiceImplAssignTaskTest {

    @Mock
    private ISysUserService userService;
    @Mock
    private AgriTaskMapper agriTaskMapper;

    private AgriTaskServiceImpl service;
    private LoginUser operator;

    @BeforeEach
    void setUp() {
        service = Mockito.spy(new AgriTaskServiceImpl(userService));
        ReflectionTestUtils.setField(service, "baseMapper", agriTaskMapper);
        operator = new LoginUser(1L, "admin", Set.of("ADMIN"));
    }

    @Test
    void assignTaskShouldSucceedForFarmer() {
        TaskAssignDTO dto = assignDto(1L, 3L, null);
        doReturn(pendingAcceptTask(1L)).when(service).getById(1L);
        when(userService.getById(3L)).thenReturn(activeUser(3L));
        when(userService.getRoleKeys(3L)).thenReturn(Set.of("FARMER"));
        when(agriTaskMapper.assignTask(eq(1L), eq(3L), eq(1L), isNull(),
                any(LocalDateTime.class), any(LocalDateTime.class),
                eq(TaskStatusV2.PENDING_ACCEPT),
                eq(TaskStatusV2.PENDING_ACCEPT), eq(0)))
                .thenReturn(1);

        service.assignTask(dto, operator, "trace-assign-1");
    }

    @Test
    void assignTaskShouldSucceedForWorkerWithInProgressStatus() {
        TaskAssignDTO dto = assignDto(1L, 5L, null);
        doReturn(pendingAcceptTask(1L)).when(service).getById(1L);
        when(userService.getById(5L)).thenReturn(activeUser(5L));
        when(userService.getRoleKeys(5L)).thenReturn(Set.of("WORKER"));
        when(agriTaskMapper.assignTask(eq(1L), eq(5L), eq(1L), isNull(),
                any(LocalDateTime.class), any(LocalDateTime.class),
                eq(TaskStatusV2.PENDING_ACCEPT),
                eq(TaskStatusV2.IN_PROGRESS), eq(0)))
                .thenReturn(1);

        service.assignTask(dto, operator, "trace-assign-worker");
    }

    @Test
    void assignTaskShouldFailWhenNotAdmin() {
        TaskAssignDTO dto = assignDto(1L, 3L, null);
        LoginUser farmer = new LoginUser(2L, "farmer1", Set.of("FARMER"));

        assertThrows(RuntimeException.class, () -> service.assignTask(dto, farmer, null));
    }

    @Test
    void assignTaskShouldFailWhenAssigneeIdNull() {
        TaskAssignDTO dto = new TaskAssignDTO();
        dto.setTaskId(1L);

        assertThrows(IllegalArgumentException.class, () -> service.assignTask(dto, operator, null));
    }

    private static TaskAssignDTO assignDto(Long taskId, Long assigneeId, String remark) {
        TaskAssignDTO dto = new TaskAssignDTO();
        dto.setTaskId(taskId);
        dto.setAssigneeId(assigneeId);
        dto.setRemark(remark);
        return dto;
    }

    private static AgriTask pendingAcceptTask(Long taskId) {
        AgriTask task = new AgriTask();
        task.setTaskId(taskId);
        task.setStatusV2(TaskStatusV2.PENDING_ACCEPT);
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
