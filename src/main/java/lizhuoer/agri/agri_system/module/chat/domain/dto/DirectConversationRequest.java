package lizhuoer.agri.agri_system.module.chat.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DirectConversationRequest {
    @NotNull(message = "targetUserId 不能为空")
    private Long targetUserId;
}
