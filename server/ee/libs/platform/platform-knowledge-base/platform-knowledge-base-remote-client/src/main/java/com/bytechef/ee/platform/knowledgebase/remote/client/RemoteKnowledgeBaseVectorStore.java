/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.knowledgebase.remote.client;

import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.DeleteDocumentsByIdsRequest;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.DocumentsRequest;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.DocumentsResponse;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.FilterRequest;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.SimilaritySearchRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RemoteKnowledgeBaseVectorStore extends AbstractRemoteKnowledgeBaseClient implements VectorStore {

    public RemoteKnowledgeBaseVectorStore(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        super(grpcChannels, objectMapper);
    }

    @Override
    public void add(List<Document> documents) {
        call(
            () -> getStub().addDocuments(
                DocumentsRequest.newBuilder()
                    .setDocuments(knowledgeBaseProtoMapper.writeDocuments(documents))
                    .build()));
    }

    @Override
    public void delete(List<String> idList) {
        call(
            () -> getStub().deleteDocumentsByIds(
                DeleteDocumentsByIdsRequest.newBuilder()
                    .addAllIds(idList)
                    .build()));
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        call(
            () -> getStub().deleteDocumentsByFilter(
                FilterRequest.newBuilder()
                    .setFilter(knowledgeBaseProtoMapper.writeFilterExpression(filterExpression))
                    .build()));
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        SimilaritySearchRequest.Builder builder = SimilaritySearchRequest.newBuilder()
            .setQuery(request.getQuery())
            .setTopK(request.getTopK())
            .setSimilarityThreshold(request.getSimilarityThreshold());

        Filter.Expression filterExpression = request.getFilterExpression();

        if (filterExpression != null) {
            builder.setFilter(knowledgeBaseProtoMapper.writeFilterExpression(filterExpression));
        }

        DocumentsResponse documentsResponse = call(() -> getStub().similaritySearch(builder.build()));

        return knowledgeBaseProtoMapper.toDocuments(documentsResponse.getDocuments());
    }
}
