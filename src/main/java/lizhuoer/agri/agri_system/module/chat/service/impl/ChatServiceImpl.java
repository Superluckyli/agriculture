package lizhuoer.agri.agri_system.module.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lizhuoer.agri.agri_system.common.domain.PageResult;
import lizhuoer.agri.agri_system.module.chat.domain.ChatConversation;
import lizhuoer.agri.agri_system.module.chat.domain.ChatMessage;
import lizhuoer.agri.agri_system.module.chat.domain.ChatReadState;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatConversationSummaryVO;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatMessageVO;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatUserVO;
import lizhuoer.agri.agri_system.module.chat.mapper.ChatConversationMapper;
import lizhuoer.agri.agri_system.module.chat.mapper.ChatMessageMapper;
import lizhuoer.agri.agri_system.module.chat.mapper.ChatReadStateMapper;
import lizhuoer.agri.agri_system.module.chat.service.IChatService;
import lizhuoer.agri.agri_system.module.chat.ws.ChatEventPublisher;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatServiceImpl implements IChatService {

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatReadStateMapper readStateMapper;
    private final ISysUserService userService;
    private final ChatEventPublisher eventPublisher;

    public ChatServiceImpl(ChatConversationMapper conversationMapper,
            ChatMessageMapper messageMapper,
            ChatReadStateMapper readStateMapper,
            ISysUserService userService,
            ChatEventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.readStateMapper = readStateMapper;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    // 获取所有用户
    @Override
    public List<ChatUserVO> listAvailableUsers(Long currentUserId) {
        return userService.list(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .ne(currentUserId != null, SysUser::getUserId, currentUserId)
                .orderByAsc(SysUser::getRealName)
                .orderByAsc(SysUser::getUsername))
                .stream()
                .map(this::toChatUser)
                .toList();
    }

    // 获取所有会话
    @Override
    public List<ChatConversationSummaryVO> listConversations(Long currentUserId) {
        return conversationMapper.selectConversationSummaries(currentUserId);
    }

    // 获取消息列表

    @Override
    public PageResult<ChatMessageVO> listMessages(Long currentUserId, Long conversationId, int pageNum, int pageSize) {
        requireConversationMember(currentUserId, conversationId);
        Page<ChatMessage> page = messageMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getConversationId, conversationId)
                        .orderByDesc(ChatMessage::getId));
        PageResult<ChatMessageVO> result = new PageResult<>();
        result.setItems(page.getRecords().stream().map(this::toMessageVO).toList());
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        return result;
    }

    // 获取或创建直接会话
    @Override
    public ChatConversationSummaryVO getOrCreateDirectConversation(Long currentUserId, Long targetUserId) {
        if (currentUserId == null || targetUserId == null) {
            throw new IllegalArgumentException("用户不能为空");
        }
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("不能与自己聊天");
        }

        SysUser targetUser = requireActiveUser(targetUserId);
        Long userAId = Math.min(currentUserId, targetUserId);
        Long userBId = Math.max(currentUserId, targetUserId);

        ChatConversation existing = conversationMapper.selectDirectConversation(userAId, userBId);
        if (existing != null) {
            return requireConversationSummary(currentUserId, existing.getId());
        }

        ChatConversation conversation = new ChatConversation();
        conversation.setUserAId(userAId);
        conversation.setUserBId(userBId);
        try {
            conversationMapper.insert(conversation);
        } catch (DuplicateKeyException duplicateKeyException) {
            ChatConversation concurrent = conversationMapper.selectDirectConversation(userAId, userBId);
            if (concurrent != null) {
                return requireConversationSummary(currentUserId, concurrent.getId());
            }
            throw duplicateKeyException;
        }

        ChatConversationSummaryVO summary = requireConversationSummary(currentUserId, conversation.getId());
        if (summary.getPeerDisplayName() == null) {
            summary.setPeerDisplayName(displayNameOf(targetUser));
            summary.setPeerUsername(targetUser.getUsername());
            summary.setPeerUserId(targetUserId);
            summary.setUnreadCount(0L);
        }
        return summary;
    }

    // 标记会话中信息已读
    @Override
    @Transactional
    public void markConversationRead(Long currentUserId, Long conversationId) {
        // 校验用户是否在会话中
        ChatConversation conversation = requireConversationMember(currentUserId, conversationId);
        // 获取会话中的最新消息
        ChatMessage latestMessage = messageMapper.selectLatestByConversationId(conversation.getId());
        // 如果没有消息，直接返回
        if (latestMessage == null) {
            // 此时会话中没有消息，将未读消息数量置为0
            ChatConversationSummaryVO summary = requireConversationSummary(currentUserId, conversationId);
            // 调用发布器，更新会话信息
            eventPublisher.publishConversationUpdate(currentUserId, summary);
            // 掉用发布器，同步已读状态
            eventPublisher.publishReadSync(currentUserId, conversationId);
            return;
        }

        ChatReadState existing = readStateMapper.selectByConversationAndUser(conversationId, currentUserId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            ChatReadState state = new ChatReadState();
            state.setConversationId(conversationId);
            state.setUserId(currentUserId);
            state.setLastReadMessageId(latestMessage.getId());
            state.setLastReadAt(now);
            readStateMapper.insert(state);
        } else {
            ChatReadState update = new ChatReadState();
            update.setLastReadMessageId(latestMessage.getId());
            update.setLastReadAt(now);
            readStateMapper.update(update, new LambdaUpdateWrapper<ChatReadState>()
                    .eq(ChatReadState::getConversationId, conversationId)
                    .eq(ChatReadState::getUserId, currentUserId));
        }
        // 包含未读消息数量unreadCount
        ChatConversationSummaryVO summary = requireConversationSummary(currentUserId, conversationId);
        eventPublisher.publishConversationUpdate(currentUserId, summary);
        eventPublisher.publishReadSync(currentUserId, conversationId);
    }

    // 发送消息
    @Override
    @Transactional
    public ChatMessageVO sendMessage(Long currentUserId, Long conversationId, String content) {
        ChatConversation conversation = requireConversationMember(currentUserId, conversationId);
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        if (normalized.length() > 1000) {
            throw new IllegalArgumentException("消息长度不能超过1000字符");
        }

        Long receiverId = conversation.getUserAId().equals(currentUserId)
                ? conversation.getUserBId()
                : conversation.getUserAId();

        ChatMessage message = new ChatMessage();
        LocalDateTime now = LocalDateTime.now();
        message.setConversationId(conversationId);
        message.setSenderId(currentUserId);
        message.setReceiverId(receiverId);
        message.setContent(normalized);
        message.setMessageType("text");
        message.setCreatedAt(now);
        messageMapper.insert(message);

        ChatConversation updateConversation = new ChatConversation();
        updateConversation.setId(conversationId);
        updateConversation.setLastMessageId(message.getId());
        updateConversation.setLastMessageAt(message.getCreatedAt());
        conversationMapper.update(updateConversation, new LambdaUpdateWrapper<ChatConversation>()
                .eq(ChatConversation::getId, conversationId)
                .and(wrapper -> wrapper
                        .isNull(ChatConversation::getLastMessageId)
                        .or()
                        .lt(ChatConversation::getLastMessageId, message.getId())));

        ChatMessageVO messageVO = toMessageVO(message);
        eventPublisher.publishMessage(currentUserId, receiverId, messageVO);
        eventPublisher.publishConversationUpdate(currentUserId,
                requireConversationSummary(currentUserId, conversationId));
        eventPublisher.publishConversationUpdate(receiverId, requireConversationSummary(receiverId, conversationId));
        return messageVO;
    }

    // 校验用户是否属于会话
    private ChatConversation requireConversationMember(Long userId, Long conversationId) {
        ChatConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (!userId.equals(conversation.getUserAId()) && !userId.equals(conversation.getUserBId())) {
            throw new IllegalArgumentException("当前用户不属于该会话");
        }
        return conversation;
    }

    // 校验用户是否激活
    private SysUser requireActiveUser(Long userId) {
        SysUser user = userService.getById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new IllegalArgumentException("目标用户不存在或已禁用");
        }
        return user;
    }

    // 校验会话摘要
    private ChatConversationSummaryVO requireConversationSummary(Long userId, Long conversationId) {
        ChatConversationSummaryVO summary = conversationMapper.selectConversationSummaryForUser(userId, conversationId);
        if (summary == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (summary.getUnreadCount() == null) {
            summary.setUnreadCount(0L);
        }
        return summary;
    }

    // 转换为 ChatUserVO
    private ChatUserVO toChatUser(SysUser user) {
        ChatUserVO vo = new ChatUserVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setDeptName(user.getDeptName());
        return vo;
    }

    // 获取显示名称
    private String displayNameOf(SysUser user) {
        return user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : user.getUsername();
    }

    // 转换为 ChatMessageVO
    private ChatMessageVO toMessageVO(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setMessageId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setSenderId(message.getSenderId());
        vo.setReceiverId(message.getReceiverId());
        vo.setContent(message.getContent());
        vo.setMessageType(message.getMessageType());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }
}
