/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.knowledgebase.remote.client;

import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.KnowledgeBaseDocumentsResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.dto.DocumentStatusUpdate;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
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
public class RemoteKnowledgeBaseDocumentServiceClient extends AbstractRemoteKnowledgeBaseClient
    implements KnowledgeBaseDocumentService {

    public RemoteKnowledgeBaseDocumentServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        super(grpcChannels, objectMapper);
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public KnowledgeBaseDocument getKnowledgeBaseDocument(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<KnowledgeBaseDocument> getKnowledgeBaseDocuments(long knowledgeBaseId) {
        KnowledgeBaseDocumentsResponse knowledgeBaseDocumentsResponse = call(
            () -> getStub().getKnowledgeBaseDocuments(idRequest(knowledgeBaseId)));

        return knowledgeBaseProtoMapper.toKnowledgeBaseDocuments(
            knowledgeBaseDocumentsResponse.getKnowledgeBaseDocuments());
    }

    @Override
    public DocumentStatusUpdate getKnowledgeBaseDocumentStatus(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public KnowledgeBaseDocument saveKnowledgeBaseDocument(KnowledgeBaseDocument knowledgeBaseDocument) {
        throw new UnsupportedOperationException();
    }
}
