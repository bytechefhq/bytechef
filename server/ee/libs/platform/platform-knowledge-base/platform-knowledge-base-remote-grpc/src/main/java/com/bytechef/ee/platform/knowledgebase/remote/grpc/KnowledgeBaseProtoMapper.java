/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.knowledgebase.remote.grpc;

import com.bytechef.ee.remote.grpc.JsonBytes;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentLoader.Chunk;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class KnowledgeBaseProtoMapper {

    private static final TypeReference<List<Chunk>> CHUNKS_TYPE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<List<VectorDocument>> DOCUMENTS_TYPE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<KnowledgeBase> KNOWLEDGE_BASE_TYPE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<List<KnowledgeBaseDocumentChunk>> KNOWLEDGE_BASE_DOCUMENT_CHUNKS_TYPE_REFERENCE =
        new TypeReference<>() {};
    private static final TypeReference<List<KnowledgeBaseDocument>> KNOWLEDGE_BASE_DOCUMENTS_TYPE_REFERENCE =
        new TypeReference<>() {};
    private static final TypeReference<List<KnowledgeBase>> KNOWLEDGE_BASES_TYPE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> METADATA_TYPE_REFERENCE = new TypeReference<>() {};

    private final FilterExpressionCodec filterExpressionCodec;
    private final JsonBytes jsonBytes;

    public KnowledgeBaseProtoMapper(ObjectMapper objectMapper) {
        this.filterExpressionCodec = new FilterExpressionCodec(objectMapper);
        this.jsonBytes = new JsonBytes(objectMapper);
    }

    public List<Chunk> toChunks(ByteString bytes) {
        return orEmpty(jsonBytes.read(bytes, CHUNKS_TYPE_REFERENCE));
    }

    public List<Document> toDocuments(ByteString bytes) {
        return orEmpty(jsonBytes.read(bytes, DOCUMENTS_TYPE_REFERENCE)).stream()
            .map(VectorDocument::toDocument)
            .toList();
    }

    public Filter.Expression toFilterExpression(ByteString bytes) {
        return filterExpressionCodec.read(bytes);
    }

    public KnowledgeBase toKnowledgeBase(ByteString bytes) {
        return Objects.requireNonNull(jsonBytes.read(bytes, KNOWLEDGE_BASE_TYPE_REFERENCE), "knowledgeBase");
    }

    public List<KnowledgeBaseDocumentChunk> toKnowledgeBaseDocumentChunks(ByteString bytes) {
        return orEmpty(jsonBytes.read(bytes, KNOWLEDGE_BASE_DOCUMENT_CHUNKS_TYPE_REFERENCE));
    }

    public List<KnowledgeBaseDocument> toKnowledgeBaseDocuments(ByteString bytes) {
        return orEmpty(jsonBytes.read(bytes, KNOWLEDGE_BASE_DOCUMENTS_TYPE_REFERENCE));
    }

    public List<KnowledgeBase> toKnowledgeBases(ByteString bytes) {
        return orEmpty(jsonBytes.read(bytes, KNOWLEDGE_BASES_TYPE_REFERENCE));
    }

    public Map<String, Object> toMetadata(ByteString bytes) {
        Map<String, Object> metadata = jsonBytes.read(bytes, METADATA_TYPE_REFERENCE);

        return metadata == null ? Map.of() : metadata;
    }

    public ByteString write(Object value) {
        return jsonBytes.write(value);
    }

    public ByteString writeDocuments(List<Document> documents) {
        return jsonBytes.write(
            documents.stream()
                .map(VectorDocument::of)
                .toList());
    }

    public ByteString writeFilterExpression(Filter.Expression filterExpression) {
        return filterExpressionCodec.write(filterExpression);
    }

    private static <T> List<T> orEmpty(@Nullable List<T> list) {
        return list == null ? List.of() : list;
    }

    private record VectorDocument(
        String id, @Nullable String text, @Nullable Map<String, Object> metadata, @Nullable Double score) {

        static VectorDocument of(Document document) {
            return new VectorDocument(
                document.getId(), document.getText(), new HashMap<>(document.getMetadata()), document.getScore());
        }

        Document toDocument() {
            return Document.builder()
                .id(id)
                .text(text)
                .metadata(metadata == null ? Map.of() : metadata)
                .score(score)
                .build();
        }
    }
}
