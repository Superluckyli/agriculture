package lizhuoer.agri.agri_system.module.task.service.impl;

import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.mapper.TaskFlowLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgriTaskServiceImplListUserNameTest {

    @Mock
    private ISysUserService userService;

    @Mock
    private TaskFlowLogMapper taskFlowLogMapper;

    private AgriTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AgriTaskServiceImpl(userService, taskFlowLogMapper);
    }

    @Test
    void fillTaskUserNamesShouldBatchLoadAndFillNames() {
        AgriTask first = new AgriTask();
        first.setExecutorId(3L);
        first.setAssigneeId(5L);

        AgriTask second = new AgriTask();
        second.setExecutorId(4L);
        second.setAssigneeId(null);

        when(userService.listByIds(argThat(ids -> ids.containsAll(Set.of(3L, 4L, 5L)) && ids.size() == 3)))
                .thenReturn(List.of(
                        user(3L, "Alice", "alice"),
                        user(4L, "   ", "user4"),
                        user(5L, null, "user5")));
        when(userService.getRoleKeys(3L)).thenReturn(Set.of("FARMER"));
        when(userService.getRoleKeys(4L)).thenReturn(Set.of("WORKER"));

        service.fillTaskUserNames(List.of(first, second));

        assertEquals("Alice", first.getExecutorName());
        assertEquals("user5", first.getAssigneeName());
        assertEquals("user4", second.getExecutorName());
        assertNull(second.getAssigneeName());
        assertEquals("FARMER", first.getExecutorRoleKey());
        assertEquals("农户", first.getExecutorRoleName());
        assertEquals("WORKER", second.getExecutorRoleKey());
        assertEquals("工人", second.getExecutorRoleName());
        verify(userService, times(1)).listByIds(anyCollection());
        verify(userService, never()).getById(anyLong());
    }

    @Test
    void fillTaskUserNamesShouldKeepNullWhenUserDeleted() {
        AgriTask task = new AgriTask();
        task.setExecutorId(99L);

        when(userService.listByIds(anyCollection())).thenReturn(List.of());
        when(userService.getRoleKeys(99L)).thenReturn(Set.of());

        service.fillTaskUserNames(List.of(task));

        assertNull(task.getExecutorName());
        assertNull(task.getAssigneeName());
        assertNull(task.getExecutorRoleKey());
        assertNull(task.getExecutorRoleName());
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
