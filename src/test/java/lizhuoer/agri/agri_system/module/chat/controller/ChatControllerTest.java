package lizhuoer.agri.agri_system.module.chat.controller;

import lizhuoer.agri.agri_system.common.domain.PageResult;
import lizhuoer.agri.agri_system.common.exception.GlobalExceptionHandler;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatConversationSummaryVO;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatMessageVO;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatUserVO;
import lizhuoer.agri.agri_system.module.chat.service.IChatService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerTest {

    private IChatService chatService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        chatService = mock(IChatService.class);

        ChatController controller = new ChatController();
        ReflectionTestUtils.setField(controller, "chatService", chatService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        LoginUserContext.set(new LoginUser(1L, "owner", Set.of("FARM_OWNER")));
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void usersShouldReturnActiveChatUsers() throws Exception {
        ChatUserVO user = new ChatUserVO();
        user.setUserId(2L);
        user.setUsername("worker1");
        user.setRealName("Worker One");
        when(chatService.listAvailableUsers(1L)).thenReturn(List.of(user));

        mockMvc.perform(get("/chat/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].userId").value(2))
                .andExpect(jsonPath("$.data[0].username").value("worker1"));

        verify(chatService).listAvailableUsers(1L);
    }

    @Test
    void conversationsShouldReturnCurrentUserConversationList() throws Exception {
        ChatConversationSummaryVO conversation = new ChatConversationSummaryVO();
        conversation.setConversationId(11L);
        conversation.setPeerUserId(2L);
        conversation.setPeerDisplayName("Worker One");
        conversation.setUnreadCount(3L);
        when(chatService.listConversations(1L)).thenReturn(List.of(conversation));

        mockMvc.perform(get("/chat/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].conversationId").value(11))
                .andExpect(jsonPath("$.data[0].unreadCount").value(3));

        verify(chatService).listConversations(1L);
    }

    @Test
    void directConversationShouldCreateOrReturnConversation() throws Exception {
        ChatConversationSummaryVO conversation = new ChatConversationSummaryVO();
        conversation.setConversationId(12L);
        conversation.setPeerUserId(3L);
        conversation.setPeerDisplayName("Tech User");
        when(chatService.getOrCreateDirectConversation(1L, 3L)).thenReturn(conversation);

        String body = """
                {
                  "targetUserId": 3
                }
                """;

        mockMvc.perform(post("/chat/conversations/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.conversationId").value(12))
                .andExpect(jsonPath("$.data.peerUserId").value(3));

        verify(chatService).getOrCreateDirectConversation(1L, 3L);
    }

    @Test
    void messagesShouldReturnPagedConversationMessages() throws Exception {
        ChatMessageVO message = new ChatMessageVO();
        message.setMessageId(101L);
        message.setConversationId(12L);
        message.setSenderId(1L);
        message.setReceiverId(3L);
        message.setContent("hello");

        PageResult<ChatMessageVO> page = new PageResult<>();
        page.setItems(List.of(message));
        page.setPage(1);
        page.setSize(20);
        page.setTotal(1);

        when(chatService.listMessages(1L, 12L, 1, 20)).thenReturn(page);

        mockMvc.perform(get("/chat/conversations/12/messages?pageNum=1&pageSize=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items[0].messageId").value(101))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(chatService).listMessages(1L, 12L, 1, 20);
    }

    @Test
    void readShouldMarkConversationRead() throws Exception {
        mockMvc.perform(post("/chat/conversations/12/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(chatService).markConversationRead(1L, 12L);
    }

}
