/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.knowledgebase.remote.client;

import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.Empty;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.KnowledgeBaseResponse;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.KnowledgeBasesResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
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
public class RemoteKnowledgeBaseServiceClient extends AbstractRemoteKnowledgeBaseClient
    implements KnowledgeBaseService {

    public RemoteKnowledgeBaseServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        super(grpcChannels, objectMapper);
    }

    @Override
    public KnowledgeBase createKnowledgeBase(KnowledgeBase knowledgeBase) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteKnowledgeBase(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public KnowledgeBase getKnowledgeBase(Long id) {
        KnowledgeBaseResponse knowledgeBaseResponse = call(() -> getStub().getKnowledgeBase(idRequest(id)));

        return knowledgeBaseProtoMapper.toKnowledgeBase(knowledgeBaseResponse.getKnowledgeBase());
    }

    @Override
    public List<KnowledgeBase> getKnowledgeBases() {
        KnowledgeBasesResponse knowledgeBasesResponse = call(
            () -> getStub().getKnowledgeBases(Empty.getDefaultInstance()));

        return knowledgeBaseProtoMapper.toKnowledgeBases(knowledgeBasesResponse.getKnowledgeBases());
    }

    @Override
    public List<KnowledgeBase> getKnowledgeBases(int environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public KnowledgeBase updateKnowledgeBase(Long id, KnowledgeBase knowledgeBase) {
        throw new UnsupportedOperationException();
    }
}
