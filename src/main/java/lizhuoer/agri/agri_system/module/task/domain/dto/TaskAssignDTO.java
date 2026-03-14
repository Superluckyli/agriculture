package lizhuoer.agri.agri_system.module.task.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskAssignDTO {
    @NotNull(message = "taskId 不能为空")
    @Positive(message = "taskId 必须大于 0")
    private Long taskId;

    @NotNull(message = "assigneeId 不能为空")
    @Positive(message = "assigneeId 必须大于 0")
    private Long assigneeId;

    private String remark;
}
