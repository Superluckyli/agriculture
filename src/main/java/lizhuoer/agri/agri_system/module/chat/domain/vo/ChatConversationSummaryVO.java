package lizhuoer.agri.agri_system.module.chat.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatConversationSummaryVO {
    private Long conversationId;
    private Long peerUserId;
    private String peerUsername;
    private String peerDisplayName;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;
}
