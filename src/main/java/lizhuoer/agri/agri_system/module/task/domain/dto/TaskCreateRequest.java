package lizhuoer.agri.agri_system.module.task.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskCreateRequest {
    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    @NotNull(message = "批次ID不能为空")
    private Long batchId;

    private String taskType;
    private Integer priority;
    private LocalDateTime deadlineAt;
    private LocalDateTime planTime;
}
