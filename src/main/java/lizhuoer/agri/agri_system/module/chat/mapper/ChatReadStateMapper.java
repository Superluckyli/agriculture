package lizhuoer.agri.agri_system.module.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lizhuoer.agri.agri_system.module.chat.domain.ChatReadState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChatReadStateMapper extends BaseMapper<ChatReadState> {

    @Select("""
            SELECT conversation_id AS conversationId, user_id AS userId,
                   last_read_message_id AS lastReadMessageId, last_read_at AS lastReadAt
            FROM chat_read_state
            WHERE conversation_id = #{conversationId} AND user_id = #{userId}
            LIMIT 1
            """)
    ChatReadState selectByConversationAndUser(@Param("conversationId") Long conversationId,
                                              @Param("userId") Long userId);
}
