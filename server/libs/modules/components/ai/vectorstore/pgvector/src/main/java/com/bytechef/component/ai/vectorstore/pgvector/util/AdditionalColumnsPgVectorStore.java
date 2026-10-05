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

package com.bytechef.component.ai.vectorstore.pgvector.util;

import com.pgvector.PGvector;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link PgVectorStore} that, in addition to the standard {@code id, content, metadata, embedding} columns, writes a
 * fixed set of extra column values to every inserted row. Needed when the vector table has additional (e.g. NOT NULL)
 * columns that the Spring AI implementation does not know about. Each value is cast to the column's actual PostgreSQL
 * type, so string values can be used for uuid, timestamp, numeric, etc. columns.
 *
 * @author Marko Krišković
 */
public class AdditionalColumnsPgVectorStore extends PgVectorStore {

    private static final String COLUMN_TYPES_SQL = """
        SELECT attname, format_type(atttypid, atttypmod) FROM pg_catalog.pg_attribute
        WHERE attrelid = to_regclass(?) AND attnum > 0 AND NOT attisdropped
        """;

    private final Map<String, Object> additionalColumns;
    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper = JsonMapper.builder()
        .addModules(JacksonUtils.instantiateAvailableModules())
        .build();
    private final int maxDocumentBatchSize;
    private final String qualifiedTableName;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AdditionalColumnsPgVectorStore(
        PgVectorStoreBuilder builder, JdbcTemplate jdbcTemplate, String schemaName, String tableName,
        int maxDocumentBatchSize, Map<String, Object> additionalColumns) {

        super(builder);

        this.additionalColumns = new LinkedHashMap<>(additionalColumns);
        this.jdbcTemplate = jdbcTemplate;
        this.maxDocumentBatchSize = maxDocumentBatchSize;
        this.qualifiedTableName = quoteIdentifier(schemaName) + "." + quoteIdentifier(tableName);
    }

    @Override
    public void doAdd(List<Document> documents) {
        if (additionalColumns.isEmpty()) {
            super.doAdd(documents);

            return;
        }

        EmbeddingOptions embeddingOptions = EmbeddingOptions.builder()
            .build();

        List<float[]> embeddings = embeddingModel.embed(documents, embeddingOptions, batchingStrategy);

        String sql = buildInsertSql(getColumnTypes());
        List<Object> additionalValues = additionalColumns.values()
            .stream()
            .map(this::toParameterValue)
            .toList();

        for (int start = 0; start < documents.size(); start += maxDocumentBatchSize) {
            int end = Math.min(start + maxDocumentBatchSize, documents.size());

            insertOrUpdateBatch(sql, documents.subList(start, end), embeddings.subList(start, end), additionalValues);
        }
    }

    private String buildInsertSql(Map<String, String> columnTypes) {
        List<String> columnNames = new ArrayList<>(List.of("id", "content", "metadata", "embedding"));
        List<String> placeholders = new ArrayList<>(List.of("?", "?", "?::jsonb", "?"));

        for (String columnName : additionalColumns.keySet()) {
            String columnType = columnTypes.get(columnName);

            if (columnType == null) {
                throw new IllegalArgumentException(
                    "Column '%s' does not exist in table %s".formatted(columnName, qualifiedTableName));
            }

            columnNames.add(quoteIdentifier(columnName));
            placeholders.add("CAST(? AS " + columnType + ")");
        }

        String updateSet = columnNames.stream()
            .filter(columnName -> !columnName.equals("id"))
            .map(columnName -> columnName + " = EXCLUDED." + columnName)
            .collect(Collectors.joining(", "));

        return "INSERT INTO " + qualifiedTableName + " (" + String.join(", ", columnNames) + ") VALUES (" +
            String.join(", ", placeholders) + ") ON CONFLICT (id) DO UPDATE SET " + updateSet;
    }

    private Map<String, String> getColumnTypes() {
        Map<String, String> columnTypes = new HashMap<>();

        jdbcTemplate.query(
            COLUMN_TYPES_SQL,
            resultSet -> {
                columnTypes.put(resultSet.getString(1), resultSet.getString(2));
            },
            qualifiedTableName);

        return columnTypes;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void insertOrUpdateBatch(
        String sql, List<Document> batch, List<float[]> embeddings, List<Object> additionalValues) {

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement preparedStatement, int index) throws SQLException {
                Document document = batch.get(index);

                StatementCreatorUtils.setParameterValue(
                    preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, UUID.fromString(document.getId()));
                StatementCreatorUtils.setParameterValue(
                    preparedStatement, 2, SqlTypeValue.TYPE_UNKNOWN, document.getText());
                StatementCreatorUtils.setParameterValue(
                    preparedStatement, 3, SqlTypeValue.TYPE_UNKNOWN,
                    jsonMapper.writeValueAsString(document.getMetadata()));
                StatementCreatorUtils.setParameterValue(
                    preparedStatement, 4, SqlTypeValue.TYPE_UNKNOWN, new PGvector(embeddings.get(index)));

                for (int valueIndex = 0; valueIndex < additionalValues.size(); valueIndex++) {
                    StatementCreatorUtils.setParameterValue(
                        preparedStatement, 5 + valueIndex, SqlTypeValue.TYPE_UNKNOWN,
                        additionalValues.get(valueIndex));
                }
            }

            @Override
            public int getBatchSize() {
                return batch.size();
            }
        });
    }

    private Object toParameterValue(Object value) {
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            return jsonMapper.writeValueAsString(value);
        }

        return value;
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
