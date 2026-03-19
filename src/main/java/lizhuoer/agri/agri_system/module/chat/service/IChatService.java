package lizhuoer.agri.agri_system.module.chat.service;

import lizhuoer.agri.agri_system.common.domain.PageResult;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatConversationSummaryVO;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatMessageVO;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatUserVO;

import java.util.List;

public interface IChatService {
    List<ChatUserVO> listAvailableUsers(Long currentUserId);

    List<ChatConversationSummaryVO> listConversations(Long currentUserId);

    PageResult<ChatMessageVO> listMessages(Long currentUserId, Long conversationId, int pageNum, int pageSize);

    ChatConversationSummaryVO getOrCreateDirectConversation(Long currentUserId, Long targetUserId);

    void markConversationRead(Long currentUserId, Long conversationId);

    ChatMessageVO sendMessage(Long currentUserId, Long conversationId, String content);
}
