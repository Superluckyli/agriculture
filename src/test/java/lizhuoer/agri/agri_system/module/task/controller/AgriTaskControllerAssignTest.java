package lizhuoer.agri.agri_system.module.task.controller;

import lizhuoer.agri.agri_system.common.exception.GlobalExceptionHandler;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAssignDTO;
import lizhuoer.agri.agri_system.module.task.service.IAgriTaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgriTaskControllerAssignTest {

    private IAgriTaskService taskService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        taskService = mock(IAgriTaskService.class);
        AgriTaskController controller = new AgriTaskController(taskService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        LoginUserContext.set(new LoginUser(1L, "admin", Set.of("ADMIN")));
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void assignShouldSucceedWithTaskIdAndExecutorId() throws Exception {
        String body = """
                {
                  "taskId": 1,
                  "executorId": 3
                }
                """;

        mockMvc.perform(put("/task/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TaskAssignDTO> dtoCaptor = ArgumentCaptor.forClass(TaskAssignDTO.class);
        verify(taskService).assignTask(dtoCaptor.capture(), any(LoginUser.class), isNull());
        assertEquals(1L, dtoCaptor.getValue().getTaskId());
        assertEquals(3L, dtoCaptor.getValue().getExecutorId());
        assertNull(dtoCaptor.getValue().getRemark());
    }

    @Test
    void assignShouldSucceedWithRemark() throws Exception {
        String body = """
                {
                  "taskId": 1,
                  "executorId": 3,
                  "remark": "Dispatch by admin"
                }
                """;

        mockMvc.perform(put("/task/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TaskAssignDTO> dtoCaptor = ArgumentCaptor.forClass(TaskAssignDTO.class);
        verify(taskService).assignTask(dtoCaptor.capture(), any(LoginUser.class), isNull());
        assertEquals("Dispatch by admin", dtoCaptor.getValue().getRemark());
    }

    @Test
    void assignShouldFailWhenTaskIdMissing() throws Exception {
        String body = """
                {
                  "executorId": 3
                }
                """;

        mockMvc.perform(put("/task/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("taskId 和 executorId 不能为空"));

        verify(taskService, never()).assignTask(any(), any(), any());
    }

    @Test
    void assignShouldFailWhenExecutorIdMissing() throws Exception {
        String body = """
                {
                  "taskId": 1
                }
                """;

        mockMvc.perform(put("/task/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("taskId 和 executorId 不能为空"));

        verify(taskService, never()).assignTask(any(), any(), any());
    }

    @Test
    void assignShouldFailWhenOnlyFarmerUserIdProvided() throws Exception {
        String body = """
                {
                  "taskId": 1,
                  "farmerUserId": 3
                }
                """;

        mockMvc.perform(put("/task/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("taskId 和 executorId 不能为空"));

        verify(taskService, never()).assignTask(any(), any(), any());
    }
}
