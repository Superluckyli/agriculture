package lizhuoer.agri.agri_system.module.task.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskUpdateDTO {
    @NotNull(message = "taskId 不能为空")
    private Long taskId;

    private Long batchId;
    private String taskName;
    private String taskType;
    private Integer priority;
    private LocalDateTime planTime;
}
