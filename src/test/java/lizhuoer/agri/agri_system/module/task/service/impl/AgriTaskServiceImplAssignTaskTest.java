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
    private LoginUser operator;

    @BeforeEach
    void setUp() {
        service = Mockito.spy(new AgriTaskServiceImpl(userService, taskMaterialService, taskLogService, materialInfoMapper, stockLogService, purchaseOrderService, purchaseOrderItemService));
        ReflectionTestUtils.setField(service, "baseMapper", agriTaskMapper);
        operator = new LoginUser(1L, "admin", Set.of("ADMIN"));
    }

    @Test
    void assignTaskShouldSucceedForWorker() {
        TaskAssignDTO dto = assignDto(1L, 4L, null);
        doReturn(pendingAcceptTask(1L)).when(service).getById(1L);
        when(userService.getById(4L)).thenReturn(activeUser(4L));
        when(userService.getRoleKeys(4L)).thenReturn(Set.of("WORKER"));
        when(agriTaskMapper.assignTask(eq(1L), eq(4L), eq(1L), isNull(),
                any(LocalDateTime.class), any(LocalDateTime.class),
                eq(TaskStatusV2.PENDING_ACCEPT),
                eq(TaskStatusV2.PENDING_ACCEPT), eq(0)))
                .thenReturn(1);

        service.assignTask(dto, operator, "trace-assign-1");
    }

    @Test
    void assignTaskShouldFailWhenNotAdminOrFarmOwner() {
        TaskAssignDTO dto = assignDto(1L, 4L, null);
        LoginUser worker = new LoginUser(4L, "worker1", Set.of("WORKER"));

        assertThrows(RuntimeException.class, () -> service.assignTask(dto, worker, null));
    }

    @Test
    void assignTaskShouldFailWhenAssigneeIdNull() {
        TaskAssignDTO dto = new TaskAssignDTO();
        dto.setTaskId(1L);

        assertThrows(IllegalArgumentException.class, () -> service.assignTask(dto, operator, null));
    }

    @Test
    void assignTaskShouldFailWhenAssigneeNotWorkerRole() {
        TaskAssignDTO dto = assignDto(1L, 2L, null);
        doReturn(pendingAcceptTask(1L)).when(service).getById(1L);
        when(userService.getById(2L)).thenReturn(activeUser(2L));
        when(userService.getRoleKeys(2L)).thenReturn(Set.of("FARM_OWNER"));

        assertThrows(RuntimeException.class, () -> service.assignTask(dto, operator, null));
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
