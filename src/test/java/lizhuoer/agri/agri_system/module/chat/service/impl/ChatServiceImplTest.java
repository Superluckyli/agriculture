package lizhuoer.agri.agri_system.module.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lizhuoer.agri.agri_system.module.chat.domain.ChatConversation;
import lizhuoer.agri.agri_system.module.chat.domain.ChatMessage;
import lizhuoer.agri.agri_system.module.chat.domain.ChatReadState;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatConversationSummaryVO;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatMessageVO;
import lizhuoer.agri.agri_system.module.chat.mapper.ChatConversationMapper;
import lizhuoer.agri.agri_system.module.chat.mapper.ChatMessageMapper;
import lizhuoer.agri.agri_system.module.chat.mapper.ChatReadStateMapper;
import lizhuoer.agri.agri_system.module.chat.ws.ChatEventPublisher;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceImplTest {

    private ChatConversationMapper conversationMapper;
    private ChatMessageMapper messageMapper;
    private ChatReadStateMapper readStateMapper;
    private ISysUserService userService;
    private ChatEventPublisher eventPublisher;
    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        conversationMapper = mock(ChatConversationMapper.class);
        messageMapper = mock(ChatMessageMapper.class);
        readStateMapper = mock(ChatReadStateMapper.class);
        userService = mock(ISysUserService.class);
        eventPublisher = mock(ChatEventPublisher.class);
        chatService = new ChatServiceImpl(
                conversationMapper,
                messageMapper,
                readStateMapper,
                userService,
                eventPublisher,
                new ObjectMapper()
        );
    }

    @Test
    void getOrCreateDirectConversationShouldReuseExistingPair() {
        SysUser target = activeUser(5L, "worker5");
        ChatConversation existing = new ChatConversation();
        existing.setId(10L);
        existing.setUserAId(2L);
        existing.setUserBId(5L);

        ChatConversationSummaryVO summary = new ChatConversationSummaryVO();
        summary.setConversationId(10L);
        summary.setPeerUserId(5L);
        summary.setPeerDisplayName("worker5");

        when(userService.getById(5L)).thenReturn(target);
        when(conversationMapper.selectDirectConversation(2L, 5L)).thenReturn(existing);
        when(conversationMapper.selectConversationSummaryForUser(2L, 10L)).thenReturn(summary);

        ChatConversationSummaryVO result = chatService.getOrCreateDirectConversation(2L, 5L);

        assertEquals(10L, result.getConversationId());
        verify(conversationMapper, never()).insert(any());
    }

    @Test
    void getOrCreateDirectConversationShouldCreateOrderedPairWhenMissing() {
        SysUser target = activeUser(9L, "worker9");
        ChatConversationSummaryVO summary = new ChatConversationSummaryVO();
        summary.setConversationId(11L);
        summary.setPeerUserId(9L);
        summary.setPeerDisplayName("worker9");

        when(userService.getById(9L)).thenReturn(target);
        when(conversationMapper.selectDirectConversation(3L, 9L)).thenReturn(null);
        doAnswer(invocation -> {
            ChatConversation conversation = invocation.getArgument(0);
            conversation.setId(11L);
            return 1;
        }).when(conversationMapper).insert(any(ChatConversation.class));
        when(conversationMapper.selectConversationSummaryForUser(3L, 11L)).thenReturn(summary);

        ChatConversationSummaryVO result = chatService.getOrCreateDirectConversation(3L, 9L);

        assertEquals(11L, result.getConversationId());
        verify(conversationMapper).insert(argThat(conversation ->
                conversation.getUserAId().equals(3L) && conversation.getUserBId().equals(9L)
        ));
    }

    @Test
    void getOrCreateDirectConversationShouldRetryLookupWhenInsertHitsDuplicateKey() {
        SysUser target = activeUser(9L, "worker9");
        ChatConversation existing = new ChatConversation();
        existing.setId(15L);
        existing.setUserAId(3L);
        existing.setUserBId(9L);

        ChatConversationSummaryVO summary = new ChatConversationSummaryVO();
        summary.setConversationId(15L);
        summary.setPeerUserId(9L);

        when(userService.getById(9L)).thenReturn(target);
        when(conversationMapper.selectDirectConversation(3L, 9L)).thenReturn(null, existing);
        doAnswer(invocation -> {
            throw new DuplicateKeyException("duplicate");
        }).when(conversationMapper).insert(any(ChatConversation.class));
        when(conversationMapper.selectConversationSummaryForUser(3L, 15L)).thenReturn(summary);

        ChatConversationSummaryVO result = chatService.getOrCreateDirectConversation(3L, 9L);

        assertEquals(15L, result.getConversationId());
    }

    @Test
    void sendMessageShouldPersistMessageUpdateConversationAndPublish() {
        ChatConversation conversation = new ChatConversation();
        conversation.setId(12L);
        conversation.setUserAId(1L);
        conversation.setUserBId(7L);

        ChatConversationSummaryVO senderSummary = new ChatConversationSummaryVO();
        senderSummary.setConversationId(12L);
        senderSummary.setPeerUserId(7L);

        ChatConversationSummaryVO receiverSummary = new ChatConversationSummaryVO();
        receiverSummary.setConversationId(12L);
        receiverSummary.setPeerUserId(1L);

        when(conversationMapper.selectById(12L)).thenReturn(conversation);
        doAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(100L);
            return 1;
        }).when(messageMapper).insert(any(ChatMessage.class));
        when(conversationMapper.selectConversationSummaryForUser(1L, 12L)).thenReturn(senderSummary);
        when(conversationMapper.selectConversationSummaryForUser(7L, 12L)).thenReturn(receiverSummary);

        ChatMessageVO result = chatService.sendMessage(1L, 12L, "hello");

        assertEquals(100L, result.getMessageId());
        assertEquals(7L, result.getReceiverId());
        assertNotNull(result.getCreatedAt());
        verify(messageMapper).insert(argThat(message ->
                message.getConversationId().equals(12L)
                        && message.getSenderId().equals(1L)
                        && message.getReceiverId().equals(7L)
                        && message.getContent().equals("hello")
        ));
        verify(conversationMapper).update(argThat(updated ->
                        updated.getId().equals(12L) && updated.getLastMessageId().equals(100L) && updated.getLastMessageAt() != null
                ),
                any());
        verify(eventPublisher).publishMessage(eq(1L), eq(7L), any(ChatMessageVO.class));
        verify(eventPublisher).publishConversationUpdate(1L, senderSummary);
        verify(eventPublisher).publishConversationUpdate(7L, receiverSummary);
    }

    @Test
    void markConversationReadShouldUpdateReadCursorToLatestMessage() {
        ChatConversation conversation = new ChatConversation();
        conversation.setId(12L);
        conversation.setUserAId(1L);
        conversation.setUserBId(7L);

        ChatMessage latest = new ChatMessage();
        latest.setId(200L);
        latest.setConversationId(12L);

        ChatReadState existing = new ChatReadState();
        existing.setConversationId(12L);
        existing.setUserId(1L);
        existing.setLastReadMessageId(150L);

        ChatConversationSummaryVO summary = new ChatConversationSummaryVO();
        summary.setConversationId(12L);
        summary.setUnreadCount(0L);

        when(conversationMapper.selectById(12L)).thenReturn(conversation);
        when(messageMapper.selectLatestByConversationId(12L)).thenReturn(latest);
        when(readStateMapper.selectByConversationAndUser(12L, 1L)).thenReturn(existing);
        when(conversationMapper.selectConversationSummaryForUser(1L, 12L)).thenReturn(summary);

        chatService.markConversationRead(1L, 12L);

        verify(readStateMapper).update(any(ChatReadState.class), any());
        verify(eventPublisher).publishConversationUpdate(1L, summary);
        verify(eventPublisher).publishReadSync(1L, 12L);
    }

    private static SysUser activeUser(Long userId, String username) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUsername(username);
        user.setStatus(1);
        return user;
    }
}
