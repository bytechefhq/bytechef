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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Wraps a {@link ChatMemoryRepository} and replaces {@link #findConversationIds()} with a query that orders results by
 * the most recent message timestamp (DESC) so callers always see the most active conversations first.
 *
 * @author ByteChef
 */
public class OrderedJdbcChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMemoryRepository delegate;
    private final JdbcTemplate jdbcTemplate;
    private final String findConversationIdsOrderedSql;

    @SuppressFBWarnings("EI2")
    public OrderedJdbcChatMemoryRepository(
        ChatMemoryRepository delegate, JdbcTemplate jdbcTemplate, String findConversationIdsOrderedSql) {

        this.delegate = delegate;
        this.jdbcTemplate = jdbcTemplate;
        this.findConversationIdsOrderedSql = findConversationIdsOrderedSql;
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public List<String> findConversationIds() {
        return jdbcTemplate.queryForList(findConversationIdsOrderedSql, String.class);
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
}
