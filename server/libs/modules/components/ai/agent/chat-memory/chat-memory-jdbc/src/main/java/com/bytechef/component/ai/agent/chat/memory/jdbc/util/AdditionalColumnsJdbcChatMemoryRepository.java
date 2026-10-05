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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.ai.chat.memory.repository.jdbc.PostgresChatMemoryRepositoryDialect;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Wraps a {@link JdbcChatMemoryRepository} and replaces {@link #saveAll(String, List)} with an insert that also writes
 * a fixed set of extra column values to every message row, for chat memory tables with additional (e.g. NOT NULL)
 * columns. Otherwise behaves like {@link JdbcChatMemoryRepository#saveAll(String, List)}: tool call messages are
 * skipped, and the conversation is deleted and re-inserted in one transaction, preserving message timestamps.
 * <p>
 * Because of that delete-and-reinsert, the additional column values of messages that were already stored are read
 * before the delete and written back unchanged; only messages new to the table get the currently configured values.
 * Stored messages are matched by type, content and timestamp (which Spring AI carries in the message metadata). Column
 * names are used unquoted, so they are case-insensitive.
 *
 * @author Marko Krišković
 */
public final class AdditionalColumnsJdbcChatMemoryRepository implements ChatMemoryRepository {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final int additionalColumnCount;
    private final List<Object> additionalValues;
    private final ChatMemoryRepository delegate;
    private final String insertMessageSql;
    private final String selectStoredValuesSql;
    private final JdbcTemplate jdbcTemplate;
    private final boolean postgres;
    private final TransactionTemplate transactionTemplate;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AdditionalColumnsJdbcChatMemoryRepository(
        ChatMemoryRepository delegate, JdbcTemplate jdbcTemplate, JdbcChatMemoryRepositoryDialect dialect,
        JdbcChatMemoryTable chatMemoryTable, Map<String, Object> additionalColumns) {

        Map<String, Object> orderedAdditionalColumns = new LinkedHashMap<>(additionalColumns);

        List<String> columnNames = List.copyOf(orderedAdditionalColumns.keySet());

        this.additionalColumnCount = columnNames.size();
        this.additionalValues = new ArrayList<>(orderedAdditionalColumns.values());
        this.delegate = delegate;
        this.insertMessageSql = buildInsertMessageSql(
            chatMemoryTable.rewriteSql(dialect.getInsertMessageSql()), columnNames);
        this.selectStoredValuesSql = buildSelectStoredValuesSql(
            chatMemoryTable.rewriteSql(dialect.getSelectMessagesSql()), columnNames);
        this.jdbcTemplate = jdbcTemplate;
        this.postgres = dialect instanceof PostgresChatMemoryRepositoryDialect;

        DataSource dataSource = Objects.requireNonNull(jdbcTemplate.getDataSource(), "dataSource");

        this.transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Override
    public List<String> findConversationIds() {
        return delegate.findConversationIds();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return delegate.findByConversationId(conversationId);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        List<Message> persistableMessages = messages.stream()
            .filter(message -> !(message instanceof ToolResponseMessage) &&
                !(message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()))
            .toList();

        transactionTemplate.executeWithoutResult(status -> {
            Map<StoredMessageKey, Deque<List<Object>>> storedValues = findStoredValues(conversationId);

            List<List<Object>> messageValues = persistableMessages.stream()
                .map(message -> getMessageValues(message, storedValues))
                .toList();

            delegate.deleteByConversationId(conversationId);

            insertMessages(conversationId, persistableMessages, messageValues);
        });
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private Map<StoredMessageKey, Deque<List<Object>>> findStoredValues(String conversationId) {
        Map<StoredMessageKey, Deque<List<Object>>> storedValues = new HashMap<>();

        jdbcTemplate.query(
            selectStoredValuesSql,
            resultSet -> {
                Timestamp timestamp = resultSet.getTimestamp(3);

                if (timestamp == null) {
                    return;
                }

                StoredMessageKey storedMessageKey = new StoredMessageKey(
                    resultSet.getString(2), resultSet.getString(1), timestamp.toInstant());

                List<Object> values = new ArrayList<>(additionalColumnCount);

                for (int columnIndex = 0; columnIndex < additionalColumnCount; columnIndex++) {
                    values.add(resultSet.getObject(4 + columnIndex));
                }

                storedValues.computeIfAbsent(storedMessageKey, key -> new ArrayDeque<>())
                    .add(values);
            },
            conversationId);

        return storedValues;
    }

    /**
     * Returns the additional column values previously stored for the message, or the configured values if the message
     * is new to the table.
     */
    private List<Object> getMessageValues(Message message, Map<StoredMessageKey, Deque<List<Object>>> storedValues) {
        Map<String, Object> metadata = message.getMetadata();

        if (metadata.get(JdbcChatMemoryRepository.CONVERSATION_TS) instanceof Instant timestamp) {
            MessageType messageType = message.getMessageType();

            Deque<List<Object>> values = storedValues.get(
                new StoredMessageKey(messageType.name(), message.getText(), timestamp));

            if (values != null && !values.isEmpty()) {
                return values.poll();
            }
        }

        return additionalValues;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void insertMessages(
        String conversationId, List<Message> persistableMessages, List<List<Object>> messageValues) {

        jdbcTemplate.batchUpdate(insertMessageSql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement preparedStatement, int index) throws SQLException {
                Message message = persistableMessages.get(index);

                MessageType messageType = message.getMessageType();

                Map<String, Object> metadata = message.getMetadata();

                Object messageTimestamp = metadata.get(JdbcChatMemoryRepository.CONVERSATION_TS);

                Instant timestamp = messageTimestamp instanceof Instant instant ? instant : Instant.now();

                preparedStatement.setString(1, conversationId);
                preparedStatement.setString(2, message.getText());
                preparedStatement.setString(3, messageType.name());
                preparedStatement.setTimestamp(4, Timestamp.from(timestamp));
                preparedStatement.setLong(5, index);

                List<Object> values = messageValues.get(index);

                for (int valueIndex = 0; valueIndex < values.size(); valueIndex++) {
                    setAdditionalValue(preparedStatement, 6 + valueIndex, values.get(valueIndex));
                }
            }

            @Override
            public int getBatchSize() {
                return persistableMessages.size();
            }
        });
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        delegate.deleteByConversationId(conversationId);
    }

    /**
     * Appends the additional columns and their placeholders to the dialect's
     * {@code INSERT INTO table (columns) VALUES (placeholders)} statement.
     */
    static String buildInsertMessageSql(String insertMessageSql, List<String> columnNames) {
        int valuesIndex = insertMessageSql.lastIndexOf(") VALUES (");
        int endIndex = insertMessageSql.lastIndexOf(')');

        if (valuesIndex < 0 || endIndex <= valuesIndex) {
            throw new IllegalStateException("Unsupported chat memory insert statement: " + insertMessageSql);
        }

        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();

        for (String columnName : columnNames) {
            Matcher matcher = IDENTIFIER_PATTERN.matcher(columnName);

            if (!matcher.matches()) {
                throw new IllegalArgumentException(
                    "Invalid column name '" + columnName + "': only letters, digits and underscores are allowed");
            }

            columns.append(", ")
                .append(columnName);
            placeholders.append(", ?");
        }

        return insertMessageSql.substring(0, valuesIndex) + columns +
            insertMessageSql.substring(valuesIndex, endIndex) + placeholders + insertMessageSql.substring(endIndex);
    }

    /**
     * Appends the additional columns to the dialect's {@code SELECT content, type, timestamp FROM ...} statement.
     */
    static String buildSelectStoredValuesSql(String selectMessagesSql, List<String> columnNames) {
        int fromIndex = selectMessagesSql.indexOf(" FROM ");

        if (fromIndex < 0) {
            throw new IllegalStateException("Unsupported chat memory select statement: " + selectMessagesSql);
        }

        return selectMessagesSql.substring(0, fromIndex) + ", " + String.join(", ", columnNames) +
            selectMessagesSql.substring(fromIndex);
    }

    private void setAdditionalValue(PreparedStatement preparedStatement, int parameterIndex, Object value)
        throws SQLException {

        if (postgres && value instanceof String) {
            // Sent untyped so PostgreSQL converts the string to the column type (uuid, timestamptz, ...).
            preparedStatement.setObject(parameterIndex, value, Types.OTHER);
        } else {
            preparedStatement.setObject(parameterIndex, value);
        }
    }

    private record StoredMessageKey(String type, String content, Instant timestamp) {
    }
}
