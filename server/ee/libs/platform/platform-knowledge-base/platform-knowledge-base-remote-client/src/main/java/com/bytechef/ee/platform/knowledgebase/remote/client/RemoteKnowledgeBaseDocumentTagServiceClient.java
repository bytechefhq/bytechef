/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.knowledgebase.remote.client;

import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.Empty;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.TagNamesResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteKnowledgeBaseDocumentTagServiceClient extends AbstractRemoteKnowledgeBaseClient
    implements KnowledgeBaseDocumentTagService {

    public RemoteKnowledgeBaseDocumentTagServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        super(grpcChannels, objectMapper);
    }

    @Override
    public List<String> getAllTagNames() {
        TagNamesResponse tagNamesResponse = call(() -> getStub().getAllTagNames(Empty.getDefaultInstance()));

        return List.copyOf(tagNamesResponse.getTagNamesList());
    }

    @Override
    public List<String> getTagNamesByKnowledgeBaseId(Long knowledgeBaseId) {
        TagNamesResponse tagNamesResponse = call(
            () -> getStub().getTagNamesByKnowledgeBaseId(idRequest(knowledgeBaseId)));

        return List.copyOf(tagNamesResponse.getTagNamesList());
    }

    @Override
    public Map<Long, List<String>> getTagNamesByKnowledgeBaseDocumentId(Long knowledgeBaseId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, List<String>> getTagNamesByKnowledgeBaseDocumentName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateTagNames(long knowledgeBaseDocumentId, List<String> tagNames) {
        throw new UnsupportedOperationException();
    }
}
