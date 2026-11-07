/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.knowledgebase.remote.client;

import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.DocumentIdResponse;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.LoadDocumentRequest;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.UpdateDocumentRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentLoader;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteKnowledgeBaseDocumentLoaderClient extends AbstractRemoteKnowledgeBaseClient
    implements KnowledgeBaseDocumentLoader {

    public RemoteKnowledgeBaseDocumentLoaderClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        super(grpcChannels, objectMapper);
    }

    @Override
    public long load(
        long knowledgeBaseId, @Nullable Long knowledgeBaseDocumentId, String documentName, List<Chunk> chunks,
        Map<String, Object> additionalMetadata) {

        LoadDocumentRequest.Builder builder = LoadDocumentRequest.newBuilder()
            .setKnowledgeBaseId(knowledgeBaseId)
            .setDocumentName(documentName)
            .setChunks(knowledgeBaseProtoMapper.write(chunks))
            .setAdditionalMetadata(knowledgeBaseProtoMapper.write(additionalMetadata));

        if (knowledgeBaseDocumentId != null) {
            builder.setKnowledgeBaseDocumentId(knowledgeBaseDocumentId);
        }

        DocumentIdResponse documentIdResponse = call(() -> getStub().loadDocument(builder.build()));

        return documentIdResponse.getKnowledgeBaseDocumentId();
    }

    @Override
    public long update(
        long knowledgeBaseId, @Nullable Long knowledgeBaseDocumentId, @Nullable Long knowledgeBaseDocumentChunkId,
        List<Chunk> chunks, Map<String, Object> additionalMetadata) {

        UpdateDocumentRequest.Builder builder = UpdateDocumentRequest.newBuilder()
            .setKnowledgeBaseId(knowledgeBaseId)
            .setChunks(knowledgeBaseProtoMapper.write(chunks))
            .setAdditionalMetadata(knowledgeBaseProtoMapper.write(additionalMetadata));

        if (knowledgeBaseDocumentId != null) {
            builder.setKnowledgeBaseDocumentId(knowledgeBaseDocumentId);
        }

        if (knowledgeBaseDocumentChunkId != null) {
            builder.setKnowledgeBaseDocumentChunkId(knowledgeBaseDocumentChunkId);
        }

        DocumentIdResponse documentIdResponse = call(() -> getStub().updateDocument(builder.build()));

        return documentIdResponse.getKnowledgeBaseDocumentId();
    }
}
