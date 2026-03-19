package lizhuoer.agri.agri_system.module.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lizhuoer.agri.agri_system.module.chat.domain.ChatConversation;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatConversationSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatConversationMapper extends BaseMapper<ChatConversation> {

    @Select("""
            SELECT id, user_a_id AS userAId, user_b_id AS userBId, last_message_id AS lastMessageId,
                   last_message_at AS lastMessageAt, created_at AS createdAt
            FROM chat_conversation
            WHERE user_a_id = #{userAId} AND user_b_id = #{userBId}
            LIMIT 1
            """)
    ChatConversation selectDirectConversation(@Param("userAId") Long userAId, @Param("userBId") Long userBId);

    @Select("""
            SELECT
                c.id AS conversationId,
                CASE WHEN c.user_a_id = #{userId} THEN c.user_b_id ELSE c.user_a_id END AS peerUserId,
                u.username AS peerUsername,
                COALESCE(u.real_name, u.username) AS peerDisplayName,
                m.content AS lastMessagePreview,
                c.last_message_at AS lastMessageAt,
                (
                    SELECT COUNT(1)
                    FROM chat_message cm
                    WHERE cm.conversation_id = c.id
                      AND cm.receiver_id = #{userId}
                      AND cm.id > COALESCE(rs.last_read_message_id, 0)
                ) AS unreadCount
            FROM chat_conversation c
            JOIN sys_user u
              ON u.user_id = CASE WHEN c.user_a_id = #{userId} THEN c.user_b_id ELSE c.user_a_id END
            LEFT JOIN chat_message m ON m.id = c.last_message_id
            LEFT JOIN chat_read_state rs
              ON rs.conversation_id = c.id AND rs.user_id = #{userId}
            WHERE (c.user_a_id = #{userId} OR c.user_b_id = #{userId})
            ORDER BY c.last_message_at DESC, c.id DESC
            """)
    List<ChatConversationSummaryVO> selectConversationSummaries(@Param("userId") Long userId);

    @Select("""
            SELECT
                c.id AS conversationId,
                CASE WHEN c.user_a_id = #{userId} THEN c.user_b_id ELSE c.user_a_id END AS peerUserId,
                u.username AS peerUsername,
                COALESCE(u.real_name, u.username) AS peerDisplayName,
                m.content AS lastMessagePreview,
                c.last_message_at AS lastMessageAt,
                (
                    SELECT COUNT(1)
                    FROM chat_message cm
                    WHERE cm.conversation_id = c.id
                      AND cm.receiver_id = #{userId}
                      AND cm.id > COALESCE(rs.last_read_message_id, 0)
                ) AS unreadCount
            FROM chat_conversation c
            JOIN sys_user u
              ON u.user_id = CASE WHEN c.user_a_id = #{userId} THEN c.user_b_id ELSE c.user_a_id END
            LEFT JOIN chat_message m ON m.id = c.last_message_id
            LEFT JOIN chat_read_state rs
              ON rs.conversation_id = c.id AND rs.user_id = #{userId}
            WHERE c.id = #{conversationId}
              AND (c.user_a_id = #{userId} OR c.user_b_id = #{userId})
            LIMIT 1
            """)
    ChatConversationSummaryVO selectConversationSummaryForUser(@Param("userId") Long userId,
                                                               @Param("conversationId") Long conversationId);
}
