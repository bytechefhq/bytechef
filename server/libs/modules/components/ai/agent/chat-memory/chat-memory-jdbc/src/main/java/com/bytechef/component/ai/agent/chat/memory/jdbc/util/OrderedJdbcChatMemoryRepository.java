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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.ai.chat.memory.repository.jdbc.PostgresChatMemoryRepositoryDialect;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Wraps a {@link ChatMemoryRepository} and:
 * <ul>
 * <li>Replaces {@link #findConversationIds()} with a query ordered by most-recent timestamp DESC.</li>
 * <li>Serializes {@link AssistantMessage#getToolCalls()} and {@link ToolResponseMessage#getResponses()} as JSON in the
 * {@code content} column so that tool-call sequences survive a DB round-trip. Spring AI's built-in
 * {@code JdbcChatMemoryRepository} discards this information (always stores {@code ""} for both message types).</li>
 * <li>Filters out broken tool-call sequences on read, preventing 400 errors from the LLM API.</li>
 * </ul>
 *
 * @author ByteChef
 */
public class OrderedJdbcChatMemoryRepository implements ChatMemoryRepository {

    private static final String TOOL_CALLS_JSON_PREFIX = "{\"_btc_tc\":";
    private static final String TOOL_RESPONSES_JSON_PREFIX = "{\"_btc_tr\":";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final TypeReference<List<AssistantMessage.ToolCall>> TOOL_CALL_LIST_TYPE =
        new TypeReference<>() {};
    private static final TypeReference<List<ToolResponseMessage.ToolResponse>> TOOL_RESPONSE_LIST_TYPE =
        new TypeReference<>() {};

    private final ChatMemoryRepository delegate;
    private final JdbcTemplate jdbcTemplate;
    private final String findConversationIdsOrderedSql;
    private final JdbcChatMemoryRepositoryDialect dialect;

    @SuppressFBWarnings("EI2")
    public OrderedJdbcChatMemoryRepository(
        ChatMemoryRepository delegate, JdbcTemplate jdbcTemplate, String findConversationIdsOrderedSql) {

        this.delegate = delegate;
        this.jdbcTemplate = jdbcTemplate;
        this.findConversationIdsOrderedSql = findConversationIdsOrderedSql;

        DataSource dataSource = jdbcTemplate.getDataSource();

        this.dialect = dataSource != null
            ? JdbcChatMemoryRepositoryDialect.from(dataSource)
            : new PostgresChatMemoryRepositoryDialect();
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public List<String> findConversationIds() {
        return jdbcTemplate.queryForList(findConversationIdsOrderedSql, String.class);
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public List<Message> findByConversationId(String conversationId) {
        List<Message> messages = jdbcTemplate.query(
            dialect.getSelectMessagesSql(),
            (rs, rowNum) -> decodeMessage(rs.getString(1), rs.getString(2)),
            conversationId);

        return filterBrokenToolCallSequences(messages);
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void saveAll(String conversationId, List<Message> messages) {
        delegate.deleteByConversationId(conversationId);

        if (messages.isEmpty()) {
            return;
        }

        AtomicLong sequenceId = new AtomicLong(Instant.now()
            .getEpochSecond());

        jdbcTemplate.batchUpdate(dialect.getInsertMessageSql(), new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Message message = messages.get(i);

                ps.setString(1, conversationId);
                ps.setString(2, encodeContent(message));
                ps.setString(3, message.getMessageType()
                    .name());
                ps.setTimestamp(4, new Timestamp(sequenceId.getAndIncrement() * 1000L));
            }

            @Override
            public int getBatchSize() {
                return messages.size();
            }
        });
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        delegate.deleteByConversationId(conversationId);
    }

    /**
     * Filters out broken tool-call sequences that cannot be sent to an LLM. Spring AI's
     * {@code JdbcChatMemoryRepository} cannot reconstruct {@code ToolResponseMessage} objects with a valid
     * {@code toolCallId} — it always creates empty-responses instances. This causes 400 errors from the LLM API.
     * <p>
     * This filter removes:
     * <ul>
     * <li>An {@link AssistantMessage} with tool calls followed by one or more empty {@link ToolResponseMessage} objects
     * (broken cross-turn sequences from old storage).</li>
     * <li>Orphaned {@link ToolResponseMessage} objects (empty or non-empty) not immediately preceded by an
     * {@link AssistantMessage} with tool calls. This covers both stale data and sequences truncated by the message
     * window.</li>
     * </ul>
     * Within-turn safety: the {@link AssistantMessage} with tool calls is saved before its {@link ToolResponseMessage},
     * so mid-turn reads contain only the former and it is preserved intact.
     */
    public static List<Message> filterBrokenToolCallSequences(List<Message> messages) {
        List<Message> result = new ArrayList<>(messages.size());

        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);

            if (message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
                int j = i + 1;

                while (j < messages.size() && messages.get(j) instanceof ToolResponseMessage toolResponse
                    && toolResponse.getResponses()
                        .isEmpty()) {
                    j++;
                }

                if (j > i + 1) {
                    i = j - 1;

                    continue;
                }
            } else if (message instanceof ToolResponseMessage
                && (result.isEmpty()
                    || !(result.get(result.size() - 1) instanceof AssistantMessage precedingMsg
                        && precedingMsg.hasToolCalls()))) {
                // Covers stale empty-response records from old storage AND window-truncated orphans where
                // the AssistantMessage was cut off by the message window.
                continue;
            }

            result.add(message);
        }

        return result;
    }

    static String encodeContent(Message message) {
        if (message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
            try {
                return TOOL_CALLS_JSON_PREFIX + OBJECT_MAPPER.writeValueAsString(assistantMessage.getToolCalls());
            } catch (JsonProcessingException e) {
                return message.getText() != null ? message.getText() : "";
            }
        }

        if (message instanceof ToolResponseMessage toolResponseMessage
            && !toolResponseMessage.getResponses()
                .isEmpty()) {
            try {
                return TOOL_RESPONSES_JSON_PREFIX
                    + OBJECT_MAPPER.writeValueAsString(toolResponseMessage.getResponses());
            } catch (JsonProcessingException e) {
                return "";
            }
        }

        return message.getText() != null ? message.getText() : "";
    }

    static Message decodeMessage(@Nullable String content, String type) {
        MessageType messageType;

        try {
            messageType = MessageType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return new UserMessage(content != null ? content : "");
        }

        return switch (messageType) {
            case USER -> new UserMessage(content != null ? content : "");
            case SYSTEM -> new SystemMessage(content != null ? content : "");
            case ASSISTANT -> decodeAssistantMessage(content);
            case TOOL -> decodeToolResponseMessage(content);
        };
    }

    private static Message decodeAssistantMessage(@Nullable String content) {
        if (content != null && content.startsWith(TOOL_CALLS_JSON_PREFIX)) {
            try {
                List<AssistantMessage.ToolCall> toolCalls =
                    OBJECT_MAPPER.readValue(content.substring(TOOL_CALLS_JSON_PREFIX.length()), TOOL_CALL_LIST_TYPE);

                return AssistantMessage.builder()
                    .content("")
                    .toolCalls(toolCalls)
                    .build();
            } catch (JsonProcessingException e) {
                return new AssistantMessage(content);
            }
        }

        return new AssistantMessage(content != null ? content : "");
    }

    private static Message decodeToolResponseMessage(@Nullable String content) {
        if (content != null && content.startsWith(TOOL_RESPONSES_JSON_PREFIX)) {
            try {
                List<ToolResponseMessage.ToolResponse> responses =
                    OBJECT_MAPPER.readValue(
                        content.substring(TOOL_RESPONSES_JSON_PREFIX.length()), TOOL_RESPONSE_LIST_TYPE);

                return ToolResponseMessage.builder()
                    .responses(responses)
                    .build();
            } catch (JsonProcessingException e) {
                return ToolResponseMessage.builder()
                    .responses(List.of())
                    .build();
            }
        }

        return ToolResponseMessage.builder()
            .responses(List.of())
            .build();
    }
}
