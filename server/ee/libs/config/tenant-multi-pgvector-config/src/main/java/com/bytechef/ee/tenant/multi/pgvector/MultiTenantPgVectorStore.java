/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.pgvector;

import static org.springframework.ai.observation.conventions.VectorStoreSimilarityMetric.COSINE;
import static org.springframework.ai.observation.conventions.VectorStoreSimilarityMetric.DOT;
import static org.springframework.ai.observation.conventions.VectorStoreSimilarityMetric.EUCLIDEAN;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.EUCLIDEAN_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.NEGATIVE_INNER_PRODUCT;

import com.bytechef.tenant.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.pgvector.PGvector;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentMetadata;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.observation.conventions.VectorStoreProvider;
import org.springframework.ai.observation.conventions.VectorStoreSimilarityMetric;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.ai.vectorstore.AbstractVectorStoreBuilder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;
import org.springframework.ai.vectorstore.observation.AbstractObservationVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.ai.vectorstore.pgvector.PgVectorFilterExpressionConverter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Multi-tenant PgVectorStore implementation that dynamically resolves the schema from TenantContext.
 *
 * <p>
 * Unlike the standard PgVectorStore which uses a fixed schema name configured at build time, this implementation
 * retrieves the schema name from {@link TenantContext#getCurrentDatabaseSchema(String)} on each operation, enabling
 * true multi-tenant isolation at the database schema level.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MultiTenantPgVectorStore extends AbstractObservationVectorStore implements VectorStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiTenantPgVectorStore.class);

    private static final Map<PgVectorStore.PgDistanceType, VectorStoreSimilarityMetric> SIMILARITY_TYPE_MAPPING =
        Map.of(COSINE_DISTANCE, COSINE, EUCLIDEAN_DISTANCE, EUCLIDEAN, NEGATIVE_INNER_PRODUCT, DOT);
    private static final String VECTORSTORE_SCHEMA_SUFFIX = "vectorstore";

    public final FilterExpressionConverter filterExpressionConverter = new PgVectorFilterExpressionConverter();

    private final String vectorTableName;
    private final JdbcTemplate jdbcTemplate;
    private final PgVectorStore.PgIdType idType;
    private final int dimensions;
    private final PgVectorStore.PgDistanceType distanceType;
    private final ObjectMapper objectMapper;
    private final DocumentRowMapper documentRowMapper;
    private final int maxDocumentBatchSize;

    @SuppressFBWarnings("EI")
    public MultiTenantPgVectorStore(MultiTenantPgVectorStoreBuilder builder) {
        super(builder);

        Assert.notNull(builder.jdbcTemplate, "JdbcTemplate must not be null");

        this.objectMapper = JsonMapper.builder()
            .addModules(JacksonUtils.instantiateAvailableModules())
            .build();
        this.documentRowMapper = new DocumentRowMapper(this.objectMapper);

        String vectorTable = builder.vectorTableName;

        this.vectorTableName = vectorTable.isEmpty() ? PgVectorStore.DEFAULT_TABLE_NAME : vectorTable.trim();

        LOGGER.info(
            "Using the vector table name: {}. Is empty: {}", this.vectorTableName, this.vectorTableName.isEmpty());

        this.idType = builder.idType;
        this.jdbcTemplate = builder.jdbcTemplate;
        this.dimensions = builder.dimensions;
        this.distanceType = builder.distanceType;
        this.maxDocumentBatchSize = builder.maxDocumentBatchSize;
    }

    public static MultiTenantPgVectorStoreBuilder builder(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return new MultiTenantPgVectorStoreBuilder(jdbcTemplate, embeddingModel);
    }

    @Override
    public VectorStoreObservationContext.Builder createObservationContextBuilder(String operationName) {
        return VectorStoreObservationContext.builder(VectorStoreProvider.PG_VECTOR.value(), operationName)
            .collectionName(this.vectorTableName)
            .dimensions(this.embeddingDimensions())
            .namespace(TenantContext.getCurrentDatabaseSchema(VECTORSTORE_SCHEMA_SUFFIX))
            .similarityMetric(getSimilarityMetric());
    }

    @Override
    public void doAdd(List<Document> documents) {
        EmbeddingOptions.Builder builder = EmbeddingOptions.builder();

        List<float[]> embeddings = this.embeddingModel.embed(documents, builder.build(), this.batchingStrategy);

        List<List<Document>> batchedDocuments = batchDocuments(documents);

        batchedDocuments.forEach(batchDocument -> insertOrUpdateBatch(batchDocument, documents, embeddings));
    }

    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC", justification = "Table/schema names are trusted configuration")
    public List<Double> embeddingDistance(String query) {
        return this.jdbcTemplate.query(
            "SELECT embedding " + this.comparisonOperator() + " ? AS distance FROM " + getFullyQualifiedTableName(),
            (rs, rowNum) -> rs.getDouble(DocumentRowMapper.COLUMN_DISTANCE), getQueryEmbedding(query));
    }

    public PgVectorStore.PgDistanceType getDistanceType() {
        return this.distanceType;
    }

    @Override
    public <T> Optional<T> getNativeClient() {
        @SuppressWarnings("unchecked")
        T client = (T) this.jdbcTemplate;
        return Optional.of(client);
    }

    @Override
    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC", justification = "Table/schema names are trusted configuration")
    public void doDelete(List<String> idList) {
        String sql = "DELETE FROM " + getFullyQualifiedTableName() + " WHERE id = ?";

        this.jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                var id = idList.get(i);
                StatementCreatorUtils.setParameterValue(ps, 1, SqlTypeValue.TYPE_UNKNOWN, convertIdToPgType(id));
            }

            @Override
            public int getBatchSize() {
                return idList.size();
            }
        });
    }

    @Override
    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC", justification = "Table/schema names are trusted configuration")
    public List<Document> doSimilaritySearch(SearchRequest request) {
        String nativeFilterExpression = (request.getFilterExpression() != null)
            ? filterExpressionConverter.convertExpression(request.getFilterExpression()) : "";

        String jsonPathFilter = "";

        if (StringUtils.hasText(nativeFilterExpression)) {
            jsonPathFilter = " AND metadata::jsonb @@ '" + nativeFilterExpression + "'::jsonpath ";
        }

        double distance = 1 - request.getSimilarityThreshold();

        PGvector queryEmbedding = getQueryEmbedding(request.getQuery());

        return this.jdbcTemplate.query(
            String.format(
                this.getDistanceType().similaritySearchSqlTemplate, getFullyQualifiedTableName(), jsonPathFilter),
            this.documentRowMapper, queryEmbedding, queryEmbedding, distance, request.getTopK());
    }

    @Override
    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC", justification = "Table/schema names are trusted configuration")
    protected void doDelete(Filter.Expression filterExpression) {
        String nativeFilterExpression = this.filterExpressionConverter.convertExpression(filterExpression);

        String sql = "DELETE FROM " + getFullyQualifiedTableName() + " WHERE metadata::jsonb @@ '" +
            nativeFilterExpression + "'::jsonpath";

        // Execute the delete
        try {
            this.jdbcTemplate.update(sql);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete documents by filter", e);
        }
    }

    private List<List<Document>> batchDocuments(List<Document> documents) {
        List<List<Document>> batches = new ArrayList<>();

        for (int i = 0; i < documents.size(); i += this.maxDocumentBatchSize) {
            batches.add(documents.subList(i, Math.min(i + this.maxDocumentBatchSize, documents.size())));
        }

        return batches;
    }

    private String comparisonOperator() {
        return this.getDistanceType().operator;
    }

    private Object convertIdToPgType(String id) {
        return switch (getIdType()) {
            case UUID -> UUID.fromString(id);
            case TEXT -> id;
            case INTEGER, SERIAL -> Integer.valueOf(id);
            case BIGSERIAL -> Long.valueOf(id);
        };
    }

    private int embeddingDimensions() {
        // The manually set dimensions have precedence over the computed one.
        if (this.dimensions > 0) {
            return this.dimensions;
        }

        try {
            int embeddingDimensions = this.embeddingModel.dimensions();
            if (embeddingDimensions > 0) {
                return embeddingDimensions;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to obtain the embedding dimensions from the embedding model and fall backs to default:"
                + PgVectorStore.OPENAI_EMBEDDING_DIMENSION_SIZE, e);
        }
        return PgVectorStore.OPENAI_EMBEDDING_DIMENSION_SIZE;
    }

    private String getFullyQualifiedTableName() {
        return TenantContext.getCurrentDatabaseSchema(VECTORSTORE_SCHEMA_SUFFIX) + "." + vectorTableName;
    }

    private PgVectorStore.PgIdType getIdType() {
        return this.idType;
    }

    private PGvector getQueryEmbedding(String query) {
        float[] embedding = this.embeddingModel.embed(query);
        return new PGvector(embedding);
    }

    private String getSimilarityMetric() {
        VectorStoreSimilarityMetric metric = SIMILARITY_TYPE_MAPPING.get(this.distanceType);
        return metric != null ? metric.value() : this.distanceType.name();
    }

    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC", justification = "Table/schema names are trusted configuration")
    private void insertOrUpdateBatch(List<Document> batch, List<Document> documents, List<float[]> embeddings) {
        String sql = "INSERT INTO " + getFullyQualifiedTableName()
            + " (id, content, metadata, embedding) VALUES (?, ?, ?::jsonb, ?) " + "ON CONFLICT (id) DO "
            + "UPDATE SET content = ? , metadata = ?::jsonb , embedding = ? ";

        this.jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {

                var document = batch.get(i);
                var id = convertIdToPgType(document.getId());
                var content = document.getText();
                var json = toJson(document.getMetadata());
                var embedding = embeddings.get(documents.indexOf(document));
                var pGvector = new PGvector(embedding);

                StatementCreatorUtils.setParameterValue(ps, 1, SqlTypeValue.TYPE_UNKNOWN, id);
                StatementCreatorUtils.setParameterValue(ps, 2, SqlTypeValue.TYPE_UNKNOWN, content);
                StatementCreatorUtils.setParameterValue(ps, 3, SqlTypeValue.TYPE_UNKNOWN, json);
                StatementCreatorUtils.setParameterValue(ps, 4, SqlTypeValue.TYPE_UNKNOWN, pGvector);
                StatementCreatorUtils.setParameterValue(ps, 5, SqlTypeValue.TYPE_UNKNOWN, content);
                StatementCreatorUtils.setParameterValue(ps, 6, SqlTypeValue.TYPE_UNKNOWN, json);
                StatementCreatorUtils.setParameterValue(ps, 7, SqlTypeValue.TYPE_UNKNOWN, pGvector);
            }

            @Override
            public int getBatchSize() {
                return batch.size();
            }
        });
    }

    private String toJson(Map<String, Object> map) {
        try {
            return this.objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class MultiTenantPgVectorStoreBuilder
        extends AbstractVectorStoreBuilder<MultiTenantPgVectorStoreBuilder> {

        private final JdbcTemplate jdbcTemplate;
        private String vectorTableName = PgVectorStore.DEFAULT_TABLE_NAME;
        private PgVectorStore.PgIdType idType = PgVectorStore.DEFAULT_ID_TYPE;
        private int dimensions = PgVectorStore.INVALID_EMBEDDING_DIMENSION;
        private PgVectorStore.PgDistanceType distanceType = COSINE_DISTANCE;
        private int maxDocumentBatchSize = PgVectorStore.MAX_DOCUMENT_BATCH_SIZE;

        private MultiTenantPgVectorStoreBuilder(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
            super(embeddingModel);

            Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");

            this.jdbcTemplate = jdbcTemplate;
        }

        public MultiTenantPgVectorStoreBuilder vectorTableName(String vectorTableName) {
            this.vectorTableName = vectorTableName;

            return this;
        }

        public MultiTenantPgVectorStoreBuilder idType(PgVectorStore.PgIdType idType) {
            this.idType = idType;

            return this;
        }

        public MultiTenantPgVectorStoreBuilder dimensions(int dimensions) {
            this.dimensions = dimensions;

            return this;
        }

        public MultiTenantPgVectorStoreBuilder distanceType(PgVectorStore.PgDistanceType distanceType) {
            this.distanceType = distanceType;

            return this;
        }

        public MultiTenantPgVectorStoreBuilder maxDocumentBatchSize(int maxDocumentBatchSize) {
            this.maxDocumentBatchSize = maxDocumentBatchSize;

            return this;
        }

        public MultiTenantPgVectorStore build() {
            return new MultiTenantPgVectorStore(this);
        }
    }

    private static class DocumentRowMapper implements RowMapper<Document> {

        private static final String COLUMN_METADATA = "metadata";
        private static final String COLUMN_ID = "id";
        private static final String COLUMN_CONTENT = "content";
        private static final String COLUMN_DISTANCE = "distance";
        private final ObjectMapper objectMapper;

        DocumentRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Document mapRow(ResultSet rs, int rowNum) throws SQLException {
            String id = rs.getString(COLUMN_ID);
            String content = rs.getString(COLUMN_CONTENT);
            PGobject pgMetadata = rs.getObject(COLUMN_METADATA, PGobject.class);
            float distance = rs.getFloat(COLUMN_DISTANCE);

            Map<String, Object> metadata = toMap(pgMetadata);
            metadata.put(DocumentMetadata.DISTANCE.value(), distance);

            return Document.builder()
                .id(id)
                .text(content)
                .metadata(metadata)
                .score(1.0 - distance)
                .build();
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> toMap(PGobject pgObject) {
            String source = pgObject.getValue();

            try {
                return (Map<String, Object>) this.objectMapper.readValue(source, Map.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
