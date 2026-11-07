/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.knowledgebase.remote.client;

import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.KnowledgeBaseDocumentChunksResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteKnowledgeBaseDocumentChunkFacadeClient extends AbstractRemoteKnowledgeBaseClient
    implements KnowledgeBaseDocumentChunkFacade {

    public RemoteKnowledgeBaseDocumentChunkFacadeClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        super(grpcChannels, objectMapper);
    }

    @Override
    public List<KnowledgeBaseDocumentChunk> getKnowledgeBaseDocumentChunksByDocumentId(
        long knowledgeBaseDocumentId) {

        KnowledgeBaseDocumentChunksResponse knowledgeBaseDocumentChunksResponse = call(
            () -> getStub().getKnowledgeBaseDocumentChunksByDocumentId(idRequest(knowledgeBaseDocumentId)));

        return knowledgeBaseProtoMapper.toKnowledgeBaseDocumentChunks(
            knowledgeBaseDocumentChunksResponse.getKnowledgeBaseDocumentChunks());
    }

    @Override
    public List<KnowledgeBaseDocumentChunk> getKnowledgeBaseDocumentChunksByDocumentIdWithoutContent(
        long knowledgeBaseDocumentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public KnowledgeBaseDocumentChunk updateKnowledgeBaseDocumentChunk(long id, String content) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteKnowledgeBaseDocumentChunk(long id) {
        throw new UnsupportedOperationException();
    }
}
