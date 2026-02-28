package lizhuoer.agri.agri_system.module.task.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskAssignDTO {
    private Long taskId;
    private Long executorId;
    private String remark;
}
