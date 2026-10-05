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

package com.bytechef.component.ai.vectorstore.mariadb.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.ai.vectorstore.mariadb.MariaDBVectorStore;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link MariaDBVectorStore} that, in addition to the standard {@code id, content, metadata, embedding} columns, writes
 * a fixed set of extra column values to every inserted row. Values are bound as-is and converted by MariaDB to the
 * column type.
 *
 * @author Marko Krišković
 */
public class AdditionalColumnsMariaDBVectorStore extends MariaDBVectorStore {

    private final Map<String, Object> additionalColumns;
    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper = JsonMapper.builder()
        .addModules(JacksonUtils.instantiateAvailableModules())
        .build();
    private final String qualifiedTableName;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AdditionalColumnsMariaDBVectorStore(
        MariaDBBuilder builder, JdbcTemplate jdbcTemplate, String schemaName, String tableName,
        Map<String, Object> additionalColumns) {

        super(builder);

        this.additionalColumns = new LinkedHashMap<>(additionalColumns);
        this.jdbcTemplate = jdbcTemplate;
        this.qualifiedTableName = schemaName == null
            ? quoteIdentifier(tableName) : quoteIdentifier(schemaName) + "." + quoteIdentifier(tableName);
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

        List<Object> additionalValues = additionalColumns.values()
            .stream()
            .map(this::toParameterValue)
            .toList();

        for (int start = 0; start < documents.size(); start += MAX_DOCUMENT_BATCH_SIZE) {
            int end = Math.min(start + MAX_DOCUMENT_BATCH_SIZE, documents.size());

            insertOrUpdateBatch(documents.subList(start, end), embeddings.subList(start, end), additionalValues);
        }
    }

    private String buildInsertSql() {
        List<String> columnNames = new ArrayList<>(List.of("id", "content", "metadata", "embedding"));

        for (String columnName : additionalColumns.keySet()) {
            columnNames.add(quoteIdentifier(columnName));
        }

        String placeholders = columnNames.stream()
            .map(columnName -> "?")
            .collect(Collectors.joining(", "));

        String updateSet = columnNames.stream()
            .filter(columnName -> !columnName.equals("id"))
            .map(columnName -> columnName + " = VALUES(" + columnName + ")")
            .collect(Collectors.joining(", "));

        return "INSERT INTO " + qualifiedTableName + " (" + String.join(", ", columnNames) + ") VALUES (" +
            placeholders + ") ON DUPLICATE KEY UPDATE " + updateSet;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void insertOrUpdateBatch(List<Document> batch, List<float[]> embeddings, List<Object> additionalValues) {
        jdbcTemplate.batchUpdate(buildInsertSql(), new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement preparedStatement, int index) throws SQLException {
                Document document = batch.get(index);

                preparedStatement.setObject(1, document.getId());
                preparedStatement.setString(2, document.getText());
                preparedStatement.setString(3, jsonMapper.writeValueAsString(document.getMetadata()));
                preparedStatement.setObject(4, embeddings.get(index));

                for (int valueIndex = 0; valueIndex < additionalValues.size(); valueIndex++) {
                    preparedStatement.setObject(5 + valueIndex, additionalValues.get(valueIndex));
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
        return "`" + identifier.replace("`", "``") + "`";
    }
}
