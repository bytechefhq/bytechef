/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQueryFilter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link ContextStoreSemanticSearchService}. Routes both Mode 2 (pure semantic) and Mode 3
 * (hybrid) queries through Spring AI's {@link VectorStore#similaritySearch(SearchRequest)} with
 * {@link Filter.Expression} metadata predicates — no custom SQL is issued against the vector-store table.
 *
 * <p>
 * Bean registration is gated on {@link EmbeddingModel} and {@link VectorStore} presence; the
 * {@code contextStorePgVectorStore} bean is registered in both single-tenant and multi-tenant deployments, so this
 * service is available in either mode. Callers that want graceful degradation must resolve via
 * {@link org.springframework.beans.factory.ObjectProvider}.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@ConditionalOnBean({
    EmbeddingModel.class, VectorStore.class
})
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI2")
class ContextStoreSemanticSearchServiceImpl implements ContextStoreSemanticSearchService {

    private static final Logger log = LoggerFactory.getLogger(ContextStoreSemanticSearchServiceImpl.class);

    private static final String METADATA_RECORD_ID = "recordId";
    private static final String METADATA_SOURCE_ID = "sourceId";
    private static final String METADATA_DISTANCE = "distance";

    private final VectorStore vectorStore;
    private final ContextStoreQueryService contextStoreQueryService;
    private final ContextStoreRecordService contextStoreRecordService;

    ContextStoreSemanticSearchServiceImpl(
        @Qualifier("contextStorePgVectorStore") VectorStore vectorStore,
        ContextStoreQueryService contextStoreQueryService, ContextStoreRecordService contextStoreRecordService) {

        this.vectorStore = vectorStore;
        this.contextStoreQueryService = contextStoreQueryService;
        this.contextStoreRecordService = contextStoreRecordService;
    }

    @Override
    public List<SemanticHit> searchSemantic(long sourceId, String queryText, int k) {
        Filter.Expression filterExpression = baseFilter(sourceId);

        return runSimilaritySearch(queryText, k, filterExpression);
    }

    @Override
    public List<SemanticHit> searchSemantic(long sourceId, String queryText, int k, ContextStoreQueryFilter prefilter) {
        if (prefilter == null) {
            return searchSemantic(sourceId, queryText, k);
        }

        List<Long> candidateIds = contextStoreQueryService.searchRecordIds(sourceId, prefilter);

        if (candidateIds.isEmpty()) {
            return List.of();
        }

        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op combined = builder.and(
            builder.eq(METADATA_SOURCE_ID, sourceId),
            builder.in(METADATA_RECORD_ID, candidateIds.toArray()));

        return runSimilaritySearch(queryText, k, combined.build());
    }

    private Filter.Expression baseFilter(long sourceId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        return builder.eq(METADATA_SOURCE_ID, sourceId)
            .build();
    }

    private List<SemanticHit> runSimilaritySearch(String queryText, int k, Filter.Expression filterExpression) {
        SearchRequest searchRequest = SearchRequest.builder()
            .query(queryText)
            .topK(k)
            .filterExpression(filterExpression)
            .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        List<Long> rankedRecordIds = new ArrayList<>(documents.size());

        for (Document document : documents) {
            Long recordId = coerceRecordId(document.getMetadata()
                .get(METADATA_RECORD_ID));

            if (recordId != null) {
                rankedRecordIds.add(recordId);
            }
        }

        if (rankedRecordIds.isEmpty()) {
            return List.of();
        }

        Map<Long, ContextStoreRecord> recordsById = hydrateRecords(rankedRecordIds);

        List<SemanticHit> hits = new ArrayList<>(documents.size());

        for (Document document : documents) {
            Long recordId = coerceRecordId(document.getMetadata()
                .get(METADATA_RECORD_ID));

            if (recordId == null) {
                continue;
            }

            ContextStoreRecord record = recordsById.get(recordId);

            if (record == null) {
                // Record was tombstoned/hard-deleted between embedding pass and this search — benign race.
                log.debug(
                    "Context Store semantic search skipped recordId={} — record no longer present", recordId);

                continue;
            }

            hits.add(new SemanticHit(record, computeSimilarityScore(document)));
        }

        return hits;
    }

    private Map<Long, ContextStoreRecord> hydrateRecords(List<Long> recordIds) {
        List<ContextStoreRecord> records = contextStoreRecordService.findAllById(recordIds);

        Map<Long, ContextStoreRecord> recordsById = new HashMap<>(records.size());

        for (ContextStoreRecord record : records) {
            recordsById.put(record.getId(), record);
        }

        return recordsById;
    }

    private static double computeSimilarityScore(Document document) {
        Object distanceValue = document.getMetadata()
            .get(METADATA_DISTANCE);

        if (distanceValue instanceof Number number) {
            return 1.0 - number.doubleValue();
        }

        return Double.NaN;
    }

    @Nullable
    private static Long coerceRecordId(@Nullable Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String string && !string.isEmpty()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }
}
