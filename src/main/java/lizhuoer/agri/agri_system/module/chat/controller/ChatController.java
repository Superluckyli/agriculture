package lizhuoer.agri.agri_system.module.chat.controller;

import jakarta.validation.Valid;
import lizhuoer.agri.agri_system.common.domain.PageResult;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.chat.domain.dto.DirectConversationRequest;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatConversationSummaryVO;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatMessageVO;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatUserVO;
import lizhuoer.agri.agri_system.module.chat.service.IChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private IChatService chatService;

    @GetMapping("/users")
    public R<List<ChatUserVO>> users() {
        Long userId = LoginUserContext.requireUser().getUserId();
        return R.ok(chatService.listAvailableUsers(userId));
    }

    @GetMapping("/conversations")
    public R<List<ChatConversationSummaryVO>> conversations() {
        Long userId = LoginUserContext.requireUser().getUserId();
        return R.ok(chatService.listConversations(userId));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public R<PageResult<ChatMessageVO>> messages(@PathVariable Long conversationId,
                                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = LoginUserContext.requireUser().getUserId();
        return R.ok(chatService.listMessages(userId, conversationId, pageNum, pageSize));
    }

    @PostMapping("/conversations/direct")
    public R<ChatConversationSummaryVO> directConversation(@Valid @RequestBody DirectConversationRequest request) {
        Long userId = LoginUserContext.requireUser().getUserId();
        return R.ok(chatService.getOrCreateDirectConversation(userId, request.getTargetUserId()));
    }

    @PostMapping("/conversations/{conversationId}/read")
    public R<Void> read(@PathVariable Long conversationId) {
        Long userId = LoginUserContext.requireUser().getUserId();
        chatService.markConversationRead(userId, conversationId);
        return R.ok();
    }
}
