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

import com.bytechef.component.definition.Option;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryConversationOptions;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.H2ChatMemoryRepositoryDialect;
import org.springframework.ai.chat.memory.repository.jdbc.HsqldbChatMemoryRepositoryDialect;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.MysqlChatMemoryRepositoryDialect;
import org.springframework.ai.chat.memory.repository.jdbc.OracleChatMemoryRepositoryDialect;
import org.springframework.ai.chat.memory.repository.jdbc.PostgresChatMemoryRepositoryDialect;
import org.springframework.ai.chat.memory.repository.jdbc.SqlServerChatMemoryRepositoryDialect;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * @author Ivica Cardic
 */
class OrderedJdbcChatMemoryRepositoryTest {

    private static final Instant BASE_TIMESTAMP = Instant.parse("2026-01-01T00:00:00Z");

    private SingleConnectionDataSource dataSource;

    @BeforeEach
    void beforeEach() {
        dataSource = new SingleConnectionDataSource(
            "jdbc:h2:mem:orderedChatMemory;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "", true);
    }

    @AfterEach
    void afterEach() {
        dataSource.destroy();
    }

    @Test
    void testFirstMessagesAreLimitedAndOrderedNewestFirst() {
        ChatMemoryRepository chatMemoryRepository = createRepository(dataSource, "limited_chat_memory");

        saveConversations(chatMemoryRepository, 105);

        List<Option<String>> options = ChatMemoryConversationOptions.getOptions(chatMemoryRepository, null);

        assertThat(options).hasSize(ChatMemoryConversationOptions.MAX_OPTIONS);
        assertThat(options.getFirst()
            .getValue()).isEqualTo("conversation-104");
        assertThat(options.getFirst()
            .getDescription()).contains("first 104");
        assertThat(options.getLast()
            .getValue()).isEqualTo("conversation-005");
    }

    @Test
    void testFirstMessagesAreFilteredBySearchText() {
        ChatMemoryRepository chatMemoryRepository = createRepository(dataSource, "filtered_chat_memory");

        saveConversations(chatMemoryRepository, 30);

        assertThat(ChatMemoryConversationOptions.getOptions(chatMemoryRepository, "CONVERSATION-01"))
            .extracting(Option::getValue)
            .containsExactly(
                "conversation-019", "conversation-018", "conversation-017", "conversation-016", "conversation-015",
                "conversation-014", "conversation-013", "conversation-012", "conversation-011", "conversation-010");
        assertThat(ChatMemoryConversationOptions.getOptions(chatMemoryRepository, "%")).isEmpty();
        assertThat(ChatMemoryConversationOptions.getOptions(chatMemoryRepository, "conversation_01")).isEmpty();
    }

    @Test
    void testGetSelectFirstMessagesSqlPerDialect() {
        assertThat(JdbcChatMemoryUtils.getSelectFirstMessagesSql(new PostgresChatMemoryRepositoryDialect()))
            .contains("MAX(\"timestamp\")", "LIMIT ?");
        assertThat(JdbcChatMemoryUtils.getSelectFirstMessagesSql(new H2ChatMemoryRepositoryDialect()))
            .contains("MAX(\"timestamp\")", "LIMIT ?");
        assertThat(JdbcChatMemoryUtils.getSelectFirstMessagesSql(new MysqlChatMemoryRepositoryDialect()))
            .contains("MAX(`timestamp`)", "LIMIT ?");
        assertThat(JdbcChatMemoryUtils.getSelectFirstMessagesSql(new HsqldbChatMemoryRepositoryDialect()))
            .contains("MAX(timestamp)", "LIMIT ?");
        assertThat(JdbcChatMemoryUtils.getSelectFirstMessagesSql(new OracleChatMemoryRepositoryDialect()))
            .contains("MAX(\"TIMESTAMP\")", "FETCH FIRST ? ROWS ONLY");
        assertThat(JdbcChatMemoryUtils.getSelectFirstMessagesSql(new SqlServerChatMemoryRepositoryDialect()))
            .contains("MAX([timestamp])", "OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY");
    }

    static ChatMemoryRepository createRepository(DataSource dataSource, String table) {
        return JdbcChatMemoryUtils.createChatMemoryRepository(
            dataSource, new JdbcChatMemoryTable(null, table), Map.of());
    }

    static void saveConversations(ChatMemoryRepository chatMemoryRepository, int count) {
        for (int index = 0; index < count; index++) {
            Instant timestamp = BASE_TIMESTAMP.plus(index, ChronoUnit.MINUTES);

            List<Message> messages = List.of(
                UserMessage.builder()
                    .text("first " + index)
                    .metadata(Map.of(JdbcChatMemoryRepository.CONVERSATION_TS, timestamp))
                    .build(),
                AssistantMessage.builder()
                    .content("reply " + index)
                    .properties(Map.of(JdbcChatMemoryRepository.CONVERSATION_TS, timestamp.plusSeconds(1)))
                    .build());

            chatMemoryRepository.saveAll("conversation-%03d".formatted(index), messages);
        }
    }
}
