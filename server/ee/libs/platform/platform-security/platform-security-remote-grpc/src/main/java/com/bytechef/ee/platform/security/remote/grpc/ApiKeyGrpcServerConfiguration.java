/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.remote.grpc;

import com.bytechef.platform.security.service.ApiKeyService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Serves API key lookups over gRPC from the microservice that owns API keys.
 *
 * @version ee
 */
@Configuration
@ConditionalOnClass(name = "com.bytechef.platform.security.service.ApiKeyServiceImpl")
public class ApiKeyGrpcServerConfiguration {

    @Bean
    ApiKeyRemoteServiceGrpcServer apiKeyRemoteServiceGrpcServer(ApiKeyService apiKeyService) {
        return new ApiKeyRemoteServiceGrpcServer(apiKeyService);
    }
}
