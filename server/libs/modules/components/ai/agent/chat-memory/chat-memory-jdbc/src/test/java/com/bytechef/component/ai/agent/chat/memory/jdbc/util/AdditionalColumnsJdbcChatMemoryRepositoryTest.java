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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.H2ChatMemoryRepositoryDialect;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.MysqlChatMemoryRepositoryDialect;
import org.springframework.ai.chat.memory.repository.jdbc.OracleChatMemoryRepositoryDialect;
import org.springframework.ai.chat.memory.repository.jdbc.PostgresChatMemoryRepositoryDialect;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * @author Marko Krišković
 */
class AdditionalColumnsJdbcChatMemoryRepositoryTest {

    @Test
    void testBuildInsertMessageSqlPostgres() {
        JdbcChatMemoryTable chatMemoryTable = new JdbcChatMemoryTable("chat", "messages");

        String insertMessageSql = chatMemoryTable.rewriteSql(
            new PostgresChatMemoryRepositoryDialect().getInsertMessageSql());

        assertThat(
            AdditionalColumnsJdbcChatMemoryRepository.buildInsertMessageSql(
                insertMessageSql, List.of("organization_id", "user_id")))
                    .isEqualTo(
                        "INSERT INTO chat.messages (conversation_id, content, type, \"timestamp\", sequence_id, " +
                            "organization_id, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)");
    }

    @Test
    void testBuildInsertMessageSqlMysqlAndOracle() {
        assertThat(
            AdditionalColumnsJdbcChatMemoryRepository.buildInsertMessageSql(
                new MysqlChatMemoryRepositoryDialect().getInsertMessageSql(), List.of("tenant_id")))
                    .isEqualTo(
                        "INSERT INTO SPRING_AI_CHAT_MEMORY (conversation_id, content, type, `timestamp`, " +
                            "sequence_id, tenant_id) VALUES (?, ?, ?, ?, ?, ?)");

        assertThat(
            AdditionalColumnsJdbcChatMemoryRepository.buildInsertMessageSql(
                new OracleChatMemoryRepositoryDialect().getInsertMessageSql(), List.of("TENANT_ID")))
                    .endsWith("SEQUENCE_ID, TENANT_ID) VALUES (?, ?, ?, ?, ?, ?)");
    }

    @Test
    void testBuildInsertMessageSqlRejectsInvalidColumnName() {
        assertThatThrownBy(
            () -> AdditionalColumnsJdbcChatMemoryRepository.buildInsertMessageSql(
                new PostgresChatMemoryRepositoryDialect().getInsertMessageSql(), List.of("x) --")))
                    .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testBuildSelectStoredValuesSql() {
        assertThat(
            AdditionalColumnsJdbcChatMemoryRepository.buildSelectStoredValuesSql(
                new PostgresChatMemoryRepositoryDialect().getSelectMessagesSql(), List.of("organization_id")))
                    .isEqualTo(
                        "SELECT content, type, \"timestamp\", organization_id FROM SPRING_AI_CHAT_MEMORY " +
                            "WHERE conversation_id = ? ORDER BY sequence_id");
    }

    @Test
    void testSaveAllKeepsAdditionalValuesOfStoredMessages() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource(
            "jdbc:h2:mem:chatMemory;DB_CLOSE_DELAY=-1", "sa", "", true);

        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            jdbcTemplate.execute("""
                CREATE TABLE SPRING_AI_CHAT_MEMORY (
                    conversation_id VARCHAR(36) NOT NULL,
                    content LONGVARCHAR NOT NULL,
                    type VARCHAR(10) NOT NULL,
                    timestamp TIMESTAMP NOT NULL,
                    sequence_id BIGINT NOT NULL,
                    tenant_id VARCHAR(10) NOT NULL)
                """);

            createRepository(jdbcTemplate, "first").saveAll(
                "conversation", List.of(new UserMessage("hello"), new AssistantMessage("hi")));

            ChatMemoryRepository secondRepository = createRepository(jdbcTemplate, "second");

            List<Message> messages = new ArrayList<>(secondRepository.findByConversationId("conversation"));

            messages.add(new UserMessage("how are you?"));

            secondRepository.saveAll("conversation", messages);

            assertThat(
                jdbcTemplate.queryForList(
                    "SELECT content, tenant_id FROM SPRING_AI_CHAT_MEMORY ORDER BY sequence_id"))
                        .extracting(row -> row.get("CONTENT") + ":" + row.get("TENANT_ID"))
                        .containsExactly("hello:first", "hi:first", "how are you?:second");
        } finally {
            dataSource.destroy();
        }
    }

    private static ChatMemoryRepository createRepository(JdbcTemplate jdbcTemplate, String tenantId) {
        H2ChatMemoryRepositoryDialect dialect = new H2ChatMemoryRepositoryDialect();

        ChatMemoryRepository chatMemoryRepository = JdbcChatMemoryRepository.builder()
            .jdbcTemplate(jdbcTemplate)
            .dialect(dialect)
            .build();

        return new AdditionalColumnsJdbcChatMemoryRepository(
            chatMemoryRepository, jdbcTemplate, dialect, new JdbcChatMemoryTable(null, "SPRING_AI_CHAT_MEMORY"),
            Map.of("tenant_id", tenantId));
    }
}
