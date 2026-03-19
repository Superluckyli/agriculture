CREATE TABLE IF NOT EXISTS chat_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_a_id BIGINT NOT NULL,
    user_b_id BIGINT NOT NULL,
    last_message_id BIGINT,
    last_message_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_chat_conversation_pair UNIQUE (user_a_id, user_b_id),
    INDEX idx_chat_conversation_user_a (user_a_id, last_message_at),
    INDEX idx_chat_conversation_user_b (user_b_id, last_message_at)
);

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    message_type VARCHAR(16) NOT NULL DEFAULT 'text',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_message_conversation (conversation_id, id),
    INDEX idx_chat_message_receiver (receiver_id, id)
);

CREATE TABLE IF NOT EXISTS chat_read_state (
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_read_message_id BIGINT,
    last_read_at DATETIME,
    PRIMARY KEY (conversation_id, user_id),
    INDEX idx_chat_read_state_user (user_id, last_read_at)
);
