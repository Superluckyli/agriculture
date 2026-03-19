package lizhuoer.agri.agri_system.module.chat.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageVO {
    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private String messageType;
    private LocalDateTime createdAt;
}
