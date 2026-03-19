package lizhuoer.agri.agri_system.module.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lizhuoer.agri.agri_system.module.chat.domain.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("""
            SELECT id, conversation_id AS conversationId, sender_id AS senderId, receiver_id AS receiverId,
                   content, message_type AS messageType, created_at AS createdAt
            FROM chat_message
            WHERE conversation_id = #{conversationId}
            ORDER BY id DESC
            LIMIT 1
            """)
    ChatMessage selectLatestByConversationId(@Param("conversationId") Long conversationId);
}
