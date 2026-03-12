package lizhuoer.agri.agri_system.module.task.service.impl;

import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgriTaskServiceImplListUserNameTest {

    @Mock
    private ISysUserService userService;
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
        service = new AgriTaskServiceImpl(userService, taskMaterialService, taskLogService, materialInfoMapper, stockLogService, purchaseOrderService, purchaseOrderItemService);
    }

    @Test
    void fillTaskUserNamesShouldBatchLoadAndFillAssigneeName() {
        AgriTask first = new AgriTask();
        first.setAssigneeId(5L);

        AgriTask second = new AgriTask();
        second.setAssigneeId(null);

        when(userService.listByIds(anyCollection()))
                .thenReturn(List.of(user(5L, null, "user5")));

        service.fillTaskUserNames(List.of(first, second));

        assertEquals("user5", first.getAssigneeName());
        assertNull(second.getAssigneeName());
        verify(userService, times(1)).listByIds(anyCollection());
        verify(userService, never()).getById(anyLong());
    }

    @Test
    void fillTaskUserNamesShouldKeepNullWhenUserDeleted() {
        AgriTask task = new AgriTask();
        task.setAssigneeId(99L);

        when(userService.listByIds(anyCollection())).thenReturn(List.of());

        service.fillTaskUserNames(List.of(task));

        assertNull(task.getAssigneeName());
    }

    @Test
    void fillTaskUserNamesShouldSkipWhenTaskListEmpty() {
        service.fillTaskUserNames(List.of());

        verify(userService, never()).listByIds(anyCollection());
    }

    private static SysUser user(Long userId, String realName, String username) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setRealName(realName);
        user.setUsername(username);
        return user;
    }
}
