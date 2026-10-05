/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.ai.agent.chat.memory.jdbc.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.repository.jdbc.PostgresChatMemoryRepositoryDialect;

/**
 * @author Marko Krišković
 */
class JdbcChatMemoryTableTest {

    private static final String POSTGRES_SCHEMA_SCRIPT = """
        CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (conversation_id VARCHAR(36) NOT NULL);
        CREATE INDEX IF NOT EXISTS SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX
        ON SPRING_AI_CHAT_MEMORY(conversation_id, "timestamp");
        """;

    @Test
    void testDefaultTableLeavesSqlUnchanged() {
        JdbcChatMemoryTable chatMemoryTable = new JdbcChatMemoryTable(null, "spring_ai_chat_memory");

        assertThat(chatMemoryTable.isDefault()).isTrue();
        assertThat(chatMemoryTable.rewriteSchemaScript(POSTGRES_SCHEMA_SCRIPT)).isEqualTo(POSTGRES_SCHEMA_SCRIPT);
    }

    @Test
    void testRewriteSqlWithSchemaAndTable() {
        JdbcChatMemoryTable chatMemoryTable = new JdbcChatMemoryTable("chat", "messages");

        TableAwareChatMemoryRepositoryDialect dialect = new TableAwareChatMemoryRepositoryDialect(
            new PostgresChatMemoryRepositoryDialect(), chatMemoryTable);

        assertThat(dialect.getInsertMessageSql()).startsWith("INSERT INTO chat.messages (");
        assertThat(dialect.getSelectMessagesSql()).contains("FROM chat.messages WHERE");
        assertThat(dialect.getSelectConversationIdsSql()).isEqualTo(
            "SELECT DISTINCT conversation_id FROM chat.messages");
        assertThat(dialect.getDeleteMessagesSql()).isEqualTo("DELETE FROM chat.messages WHERE conversation_id = ?");
    }

    @Test
    void testRewriteSchemaScript() {
        JdbcChatMemoryTable chatMemoryTable = new JdbcChatMemoryTable("chat", "messages");

        assertThat(chatMemoryTable.rewriteSchemaScript(POSTGRES_SCHEMA_SCRIPT)).isEqualTo("""
            CREATE TABLE IF NOT EXISTS chat.messages (conversation_id VARCHAR(36) NOT NULL);
            CREATE INDEX IF NOT EXISTS messages_CONVERSATION_ID_TIMESTAMP_IDX
            ON chat.messages(conversation_id, "timestamp");
            """);
    }

    @Test
    void testInvalidIdentifierIsRejected() {
        assertThatThrownBy(() -> new JdbcChatMemoryTable(null, "messages; DROP TABLE users"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JdbcChatMemoryTable("\"chat\"", "messages"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
