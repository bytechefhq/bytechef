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
import java.util.List;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Ivica Cardic
 */
@Testcontainers
class OrderedJdbcChatMemoryRepositoryIntTest {

    @Container
    private static final MySQLContainer<?> MY_SQL_CONTAINER = new MySQLContainer<>("mysql:8.4");

    @Container
    private static final PostgreSQLContainer<?> POSTGRE_SQL_CONTAINER = new PostgreSQLContainer<>(
        "postgres:16-alpine");

    @ParameterizedTest(autoCloseArguments = false)
    @MethodSource("containers")
    void testFirstMessagesAreLimitedAndOrderedNewestFirst(JdbcDatabaseContainer<?> container) {
        ChatMemoryRepository chatMemoryRepository = OrderedJdbcChatMemoryRepositoryTest.createRepository(
            createDataSource(container), "limited_chat_memory");

        OrderedJdbcChatMemoryRepositoryTest.saveConversations(chatMemoryRepository, 105);

        List<Option<String>> options = ChatMemoryConversationOptions.getOptions(chatMemoryRepository, null);

        assertThat(options).hasSize(ChatMemoryConversationOptions.MAX_OPTIONS);
        assertThat(options.getFirst()
            .getValue()).isEqualTo("conversation-104");
        assertThat(options.getFirst()
            .getDescription()).contains("first 104");
        assertThat(options.getLast()
            .getValue()).isEqualTo("conversation-005");
        assertThat(options.getLast()
            .getDescription()).contains("first 5");
    }

    @ParameterizedTest(autoCloseArguments = false)
    @MethodSource("containers")
    void testFirstMessagesAreFilteredBySearchText(JdbcDatabaseContainer<?> container) {
        ChatMemoryRepository chatMemoryRepository = OrderedJdbcChatMemoryRepositoryTest.createRepository(
            createDataSource(container), "filtered_chat_memory");

        OrderedJdbcChatMemoryRepositoryTest.saveConversations(chatMemoryRepository, 30);

        assertThat(ChatMemoryConversationOptions.getOptions(chatMemoryRepository, "CONVERSATION-02"))
            .extracting(Option::getValue)
            .containsExactly(
                "conversation-029", "conversation-028", "conversation-027", "conversation-026", "conversation-025",
                "conversation-024", "conversation-023", "conversation-022", "conversation-021", "conversation-020");
        assertThat(ChatMemoryConversationOptions.getOptions(chatMemoryRepository, "%")).isEmpty();
        assertThat(ChatMemoryConversationOptions.getOptions(chatMemoryRepository, "conversation_02")).isEmpty();
        assertThat(ChatMemoryConversationOptions.getOptions(chatMemoryRepository, "o'clock")).isEmpty();
    }

    static Stream<JdbcDatabaseContainer<?>> containers() {
        return Stream.of(MY_SQL_CONTAINER, POSTGRE_SQL_CONTAINER);
    }

    private static DataSource createDataSource(JdbcDatabaseContainer<?> container) {
        return new DriverManagerDataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword());
    }
}
