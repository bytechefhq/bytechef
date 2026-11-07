/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.knowledgebase.remote.grpc;

import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentLoader;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnClass(name = "com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentLoaderImpl")
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class KnowledgeBaseGrpcServerConfiguration {

    @Bean
    KnowledgeBaseRemoteServiceGrpcServer knowledgeBaseRemoteServiceGrpcServer(
        KnowledgeBaseService knowledgeBaseService, KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService,
        KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade,
        KnowledgeBaseDocumentLoader knowledgeBaseDocumentLoader,
        @Qualifier("knowledgeBasePgVectorStore") VectorStore vectorStore, ObjectMapper objectMapper) {

        return new KnowledgeBaseRemoteServiceGrpcServer(
            knowledgeBaseService, knowledgeBaseDocumentService, knowledgeBaseDocumentTagService,
            knowledgeBaseDocumentChunkFacade, knowledgeBaseDocumentLoader, vectorStore,
            new KnowledgeBaseProtoMapper(objectMapper));
    }
}
