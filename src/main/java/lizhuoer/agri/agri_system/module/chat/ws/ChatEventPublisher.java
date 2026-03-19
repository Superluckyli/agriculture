package lizhuoer.agri.agri_system.module.chat.ws;

import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatConversationSummaryVO;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatMessageVO;

public interface ChatEventPublisher {
    void publishMessage(Long senderId, Long receiverId, ChatMessageVO message);

    void publishConversationUpdate(Long userId, ChatConversationSummaryVO summary);

    void publishReadSync(Long userId, Long conversationId);

    void publishError(Long userId, String message);
}
