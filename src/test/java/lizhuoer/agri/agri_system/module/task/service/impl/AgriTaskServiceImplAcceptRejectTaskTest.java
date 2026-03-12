package lizhuoer.agri.agri_system.module.task.service.impl;

import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAcceptDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskRejectDTO;
import lizhuoer.agri.agri_system.module.task.domain.enums.TaskStatusV2;
import lizhuoer.agri.agri_system.module.task.mapper.AgriTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgriTaskServiceImplAcceptRejectTaskTest {

    @Mock
    private ISysUserService userService;
    @Mock
    private AgriTaskMapper agriTaskMapper;

    private AgriTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        service = Mockito.spy(new AgriTaskServiceImpl(userService));
        ReflectionTestUtils.setField(service, "baseMapper", agriTaskMapper);
    }

    @Test
    void acceptTaskShouldFailWhenOperatorIsWorker() {
        TaskAcceptDTO dto = new TaskAcceptDTO();
        dto.setTaskId(1L);
        LoginUser worker = new LoginUser(6L, "worker1", Set.of("WORKER"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.acceptTask(dto, worker, "trace-accept-worker"));

        assertEquals("WORKER无需接单/拒单，请直接执行任务", ex.getMessage());
        verify(agriTaskMapper, never()).acceptTask(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectTaskShouldFailWhenOperatorIsWorker() {
        TaskRejectDTO dto = new TaskRejectDTO();
        dto.setTaskId(1L);
        dto.setReason("no need");
        LoginUser worker = new LoginUser(6L, "worker1", Set.of("WORKER"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.rejectTask(dto, worker, "trace-reject-worker"));

        assertEquals("WORKER无需接单/拒单，请直接执行任务", ex.getMessage());
        verify(agriTaskMapper, never()).rejectTask(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void acceptTaskShouldFailWhenStatusNotPendingAccept() {
        TaskAcceptDTO dto = new TaskAcceptDTO();
        dto.setTaskId(1L);
        LoginUser farmer = new LoginUser(3L, "farmer1", Set.of("FARMER"));

        AgriTask task = new AgriTask();
        task.setTaskId(1L);
        task.setStatusV2(TaskStatusV2.IN_PROGRESS);
        task.setAssigneeId(3L);
        task.setVersion(0);
        doReturn(task).when(service).getById(1L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.acceptTask(dto, farmer, null));

        assertEquals("仅待接单任务可接单", ex.getMessage());
    }

    @Test
    void rejectTaskShouldSucceedForFarmerWithPendingAcceptStatus() {
        TaskRejectDTO dto = new TaskRejectDTO();
        dto.setTaskId(1L);
        dto.setReason("resource unavailable");
        LoginUser farmer = new LoginUser(3L, "farmer1", Set.of("FARMER"));

        AgriTask task = new AgriTask();
        task.setTaskId(1L);
        task.setStatusV2(TaskStatusV2.PENDING_ACCEPT);
        task.setAssigneeId(3L);
        task.setVersion(0);
        doReturn(task).when(service).getById(1L);

        // rejectTask routes to rejected_reassign by default (no reject_reason_type set)
        org.mockito.Mockito.when(agriTaskMapper.rejectTask(
                any(), any(), any(), any(), any(), any(),
                any(), any(), any())).thenReturn(1);

        service.rejectTask(dto, farmer, null);
    }
}
