package lizhuoer.agri.agri_system.module.task.log.controller;

import lizhuoer.agri.agri_system.common.exception.GlobalExceptionHandler;
import lizhuoer.agri.agri_system.module.task.log.domain.TaskLogImageUploadVO;
import lizhuoer.agri.agri_system.module.task.log.service.IAgriTaskLogService;
import lizhuoer.agri.agri_system.module.task.log.service.TaskLogImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgriTaskLogUploadControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AgriTaskLogController controller = new AgriTaskLogController();
        ReflectionTestUtils.setField(controller, "taskLogService", mock(IAgriTaskLogService.class));
        TaskLogImageStorageService storageService = mock(TaskLogImageStorageService.class);
        when(storageService.store(any())).thenAnswer(invocation -> {
            MultipartFile file = invocation.getArgument(0);
            if ("text/plain".equals(file.getContentType()) || file.isEmpty()) {
                throw new IllegalArgumentException("仅支持 jpg、jpeg、png、webp 图片");
            }
            return new TaskLogImageUploadVO("/uploads/task-log/2026/03/24/test.jpg", "field.jpg");
        });
        ReflectionTestUtils.setField(controller, "taskLogImageStorageService", storageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void uploadImageReturnsUrlPayload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "field.jpg", "image/jpeg", "fake-image".getBytes());

        mockMvc.perform(multipart("/task/log/upload-image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").exists())
                .andExpect(jsonPath("$.data.name").value("field.jpg"));
    }

    @Test
    void uploadImageReturnsBadRequestForUnsupportedUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "field.jpg", "text/plain", "not-image".getBytes());

        mockMvc.perform(multipart("/task/log/upload-image").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("仅支持 jpg、jpeg、png、webp 图片"));
    }
}
