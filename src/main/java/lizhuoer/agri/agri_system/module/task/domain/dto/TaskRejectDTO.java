package lizhuoer.agri.agri_system.module.task.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskRejectDTO {
    @NotNull(message = "taskId 不能为空")
    private Long taskId;

    private String reason;
}
