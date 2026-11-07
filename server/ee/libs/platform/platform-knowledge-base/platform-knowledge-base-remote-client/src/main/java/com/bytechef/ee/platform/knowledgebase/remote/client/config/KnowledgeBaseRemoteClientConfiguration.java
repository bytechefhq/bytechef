/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.knowledgebase.remote.client.config;

import com.bytechef.ee.platform.knowledgebase.remote.client.RemoteKnowledgeBaseVectorStore;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.ai.vectorstore.VectorStore;
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
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class KnowledgeBaseRemoteClientConfiguration {

    @Bean
    VectorStore knowledgeBasePgVectorStore(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        return new RemoteKnowledgeBaseVectorStore(grpcChannels, objectMapper);
    }
}
