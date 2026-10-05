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

package com.bytechef.component.ai.vectorstore.oracle.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import oracle.jdbc.OracleType;
import oracle.sql.VECTOR;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.ai.vectorstore.oracle.OracleVectorStore;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link OracleVectorStore} that, in addition to the standard {@code id, content, metadata, embedding} columns, writes
 * a fixed set of extra column values to every inserted row. Column names are used unquoted (like the table name in
 * {@link OracleVectorStore}), so they are case-insensitive; values are bound as-is and converted by Oracle to the
 * column type.
 *
 * @author Marko Krišković
 */
public class AdditionalColumnsOracleVectorStore extends OracleVectorStore {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_$#]*");

    private final Map<String, Object> additionalColumns;
    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper = JsonMapper.builder()
        .addModules(JacksonUtils.instantiateAvailableModules())
        .build();
    private final OracleJsonFactory oracleJsonFactory = new OracleJsonFactory();
    private final String tableName;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AdditionalColumnsOracleVectorStore(
        Builder builder, JdbcTemplate jdbcTemplate, String tableName, Map<String, Object> additionalColumns) {

        super(builder);

        this.additionalColumns = new LinkedHashMap<>(additionalColumns);
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
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

        insertOrUpdate(documents, embeddings, additionalValues);
    }

    private String buildMergeSql() {
        List<String> columnNames = new ArrayList<>(List.of("id", "content", "metadata", "embedding"));

        for (String columnName : additionalColumns.keySet()) {
            columnNames.add(validateIdentifier(columnName));
        }

        String placeholders = columnNames.stream()
            .map(columnName -> "?")
            .collect(Collectors.joining(", "));

        String updateSet = columnNames.stream()
            .filter(columnName -> !columnName.equals("id"))
            .map(columnName -> "target." + columnName + " = source." + columnName)
            .collect(Collectors.joining(", "));

        String insertColumns = columnNames.stream()
            .map(columnName -> "target." + columnName)
            .collect(Collectors.joining(", "));

        String insertValues = columnNames.stream()
            .map(columnName -> "source." + columnName)
            .collect(Collectors.joining(", "));

        return "merge into " + validateIdentifier(tableName) + " target using (values(" + placeholders + ")) source (" +
            String.join(", ", columnNames) + ") on (target.id = source.id) when matched then update set " +
            updateSet + " when not matched then insert (" + insertColumns + ") values (" + insertValues + ")";
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void insertOrUpdate(List<Document> documents, List<float[]> embeddings, List<Object> additionalValues) {
        jdbcTemplate.batchUpdate(buildMergeSql(), new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement preparedStatement, int index) throws SQLException {
                Document document = documents.get(index);

                StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.VARCHAR, document.getId());
                StatementCreatorUtils.setParameterValue(preparedStatement, 2, Types.VARCHAR, document.getText());
                StatementCreatorUtils.setParameterValue(
                    preparedStatement, 3, OracleType.JSON.getVendorTypeNumber(), toOson(document.getMetadata()));
                StatementCreatorUtils.setParameterValue(
                    preparedStatement, 4, OracleType.VECTOR.getVendorTypeNumber(),
                    toVector(embeddings.get(index)));

                for (int valueIndex = 0; valueIndex < additionalValues.size(); valueIndex++) {
                    StatementCreatorUtils.setParameterValue(
                        preparedStatement, 5 + valueIndex, SqlTypeValue.TYPE_UNKNOWN,
                        additionalValues.get(valueIndex));
                }
            }

            @Override
            public int getBatchSize() {
                return documents.size();
            }
        });
    }

    /**
     * Same binary JSON encoding as {@link OracleVectorStore}: only string, integer, float, double and boolean metadata
     * values are written.
     */
    private byte[] toOson(Map<String, Object> metadata) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (OracleJsonGenerator oracleJsonGenerator = oracleJsonFactory.createJsonBinaryGenerator(outputStream)) {
            oracleJsonGenerator.writeStartObject();

            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                Object value = entry.getValue();

                if (value instanceof String string) {
                    oracleJsonGenerator.write(entry.getKey(), string);
                } else if (value instanceof Integer integer) {
                    oracleJsonGenerator.write(entry.getKey(), integer);
                } else if (value instanceof Float floatValue) {
                    oracleJsonGenerator.write(entry.getKey(), floatValue);
                } else if (value instanceof Double doubleValue) {
                    oracleJsonGenerator.write(entry.getKey(), doubleValue);
                } else if (value instanceof Boolean booleanValue) {
                    oracleJsonGenerator.write(entry.getKey(), booleanValue);
                }
            }

            oracleJsonGenerator.writeEnd();
        }

        return outputStream.toByteArray();
    }

    private Object toParameterValue(Object value) {
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            return jsonMapper.writeValueAsString(value);
        }

        return value;
    }

    private static VECTOR toVector(float[] embedding) throws SQLException {
        double[] values = new double[embedding.length];

        for (int index = 0; index < embedding.length; index++) {
            values[index] = embedding[index];
        }

        return VECTOR.ofFloat64Values(values);
    }

    private static String validateIdentifier(String identifier) {
        Matcher matcher = IDENTIFIER_PATTERN.matcher(identifier);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid Oracle identifier: " + identifier);
        }

        return identifier;
    }
}
