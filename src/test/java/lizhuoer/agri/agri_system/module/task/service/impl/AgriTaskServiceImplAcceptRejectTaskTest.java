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
    @Mock
    private lizhuoer.agri.agri_system.module.task.material.service.IAgriTaskMaterialService taskMaterialService;
    @Mock
    private lizhuoer.agri.agri_system.module.task.log.service.IAgriTaskLogService taskLogService;
    @Mock
    private lizhuoer.agri.agri_system.module.material.mapper.MaterialInfoMapper materialInfoMapper;
    @Mock
    private lizhuoer.agri.agri_system.module.material.stocklog.service.IMaterialStockLogService stockLogService;
    @Mock
    private lizhuoer.agri.agri_system.module.purchase.service.IPurchaseOrderService purchaseOrderService;
    @Mock
    private lizhuoer.agri.agri_system.module.purchase.service.IPurchaseOrderItemService purchaseOrderItemService;

    private AgriTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        service = Mockito.spy(new AgriTaskServiceImpl(userService, taskMaterialService, taskLogService, materialInfoMapper, stockLogService, purchaseOrderService, purchaseOrderItemService));
        ReflectionTestUtils.setField(service, "baseMapper", agriTaskMapper);
    }

    @Test
    void acceptTaskShouldFailWhenOperatorNotWorker() {
        TaskAcceptDTO dto = new TaskAcceptDTO();
        dto.setTaskId(1L);
        LoginUser farmOwner = new LoginUser(2L, "farm_owner", Set.of("FARM_OWNER"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.acceptTask(dto, farmOwner, "trace-accept"));

        assertEquals("仅工人可接单", ex.getMessage());
        verify(agriTaskMapper, never()).acceptTask(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectTaskShouldFailWhenOperatorNotWorker() {
        TaskRejectDTO dto = new TaskRejectDTO();
        dto.setTaskId(1L);
        dto.setReason("no need");
        LoginUser farmOwner = new LoginUser(2L, "farm_owner", Set.of("FARM_OWNER"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.rejectTask(dto, farmOwner, "trace-reject"));

        assertEquals("仅工人可拒单", ex.getMessage());
        verify(agriTaskMapper, never()).rejectTask(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void acceptTaskShouldFailWhenStatusNotPendingAccept() {
        TaskAcceptDTO dto = new TaskAcceptDTO();
        dto.setTaskId(1L);
        LoginUser worker = new LoginUser(4L, "worker1", Set.of("WORKER"));

        AgriTask task = new AgriTask();
        task.setTaskId(1L);
        task.setStatusV2(TaskStatusV2.IN_PROGRESS);
        task.setAssigneeId(4L);
        task.setVersion(0);
        doReturn(task).when(service).getById(1L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.acceptTask(dto, worker, null));

        assertEquals("仅待接单任务可接单", ex.getMessage());
    }

    @Test
    void rejectTaskShouldSucceedForWorkerWithPendingAcceptStatus() {
        TaskRejectDTO dto = new TaskRejectDTO();
        dto.setTaskId(1L);
        dto.setReason("resource unavailable");
        LoginUser worker = new LoginUser(4L, "worker1", Set.of("WORKER"));

        AgriTask task = new AgriTask();
        task.setTaskId(1L);
        task.setStatusV2(TaskStatusV2.PENDING_ACCEPT);
        task.setAssigneeId(4L);
        task.setVersion(0);
        doReturn(task).when(service).getById(1L);

        Mockito.when(agriTaskMapper.rejectTask(
                any(), any(), any(), any(), any(), any(),
                any(), any(), any())).thenReturn(1);

        service.rejectTask(dto, worker, null);
    }
}
