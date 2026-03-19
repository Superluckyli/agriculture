package lizhuoer.agri.agri_system.module.chat.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_read_state")
public class ChatReadState {
    private Long conversationId;
    private Long userId;
    private Long lastReadMessageId;
    private LocalDateTime lastReadAt;
}
