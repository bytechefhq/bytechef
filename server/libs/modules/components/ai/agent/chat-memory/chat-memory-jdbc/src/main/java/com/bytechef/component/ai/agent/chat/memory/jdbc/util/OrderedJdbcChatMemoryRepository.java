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

import com.bytechef.platform.component.definition.ai.agent.ConversationFirstMessageReader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Wraps a {@link ChatMemoryRepository} and replaces {@link #findConversationIds()} with a query that orders results by
 * the most recent message timestamp (DESC) so callers always see the most active conversations first. Also reads the
 * first message of the most recently active conversations in a single query.
 *
 * @author ByteChef
 */
public class OrderedJdbcChatMemoryRepository implements ChatMemoryRepository, ConversationFirstMessageReader {

    private final ChatMemoryRepository delegate;
    private final JdbcTemplate jdbcTemplate;
    private final String findConversationIdsOrderedSql;
    private final String findFirstMessagesSql;

    @SuppressFBWarnings("EI2")
    public OrderedJdbcChatMemoryRepository(
        ChatMemoryRepository delegate, JdbcTemplate jdbcTemplate, String findConversationIdsOrderedSql,
        String findFirstMessagesSql) {

        this.delegate = delegate;
        this.jdbcTemplate = jdbcTemplate;
        this.findConversationIdsOrderedSql = findConversationIdsOrderedSql;
        this.findFirstMessagesSql = findFirstMessagesSql;
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public List<String> findConversationIds() {
        return jdbcTemplate.queryForList(findConversationIdsOrderedSql, String.class);
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public List<ConversationFirstMessage> findFirstMessages(@Nullable String searchText, int limit) {
        return jdbcTemplate.query(
            findFirstMessagesSql,
            (resultSet, rowNumber) -> new ConversationFirstMessage(resultSet.getString(1), resultSet.getString(2)),
            toContainsPattern(searchText), limit);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return delegate.findByConversationId(conversationId);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        delegate.saveAll(conversationId, messages);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        delegate.deleteByConversationId(conversationId);
    }

    private static String toContainsPattern(@Nullable String searchText) {
        if (searchText == null || searchText.isBlank()) {
            return "%";
        }

        String escapedSearchText = searchText.toLowerCase(Locale.ROOT)
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_");

        return "%" + escapedSearchText + "%";
    }
}
