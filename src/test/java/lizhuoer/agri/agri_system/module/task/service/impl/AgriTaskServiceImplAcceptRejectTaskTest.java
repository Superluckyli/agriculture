package lizhuoer.agri.agri_system.module.task.service.impl;

import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAcceptDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskRejectDTO;
import lizhuoer.agri.agri_system.module.task.mapper.AgriTaskMapper;
import lizhuoer.agri.agri_system.module.task.mapper.TaskFlowLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgriTaskServiceImplAcceptRejectTaskTest {

    @Mock
    private ISysUserService userService;
    @Mock
    private TaskFlowLogMapper taskFlowLogMapper;
    @Mock
    private AgriTaskMapper agriTaskMapper;

    private AgriTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AgriTaskServiceImpl(userService, taskFlowLogMapper);
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
        verify(taskFlowLogMapper, never()).insert(any());
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
        verify(taskFlowLogMapper, never()).insert(any());
    }
}
