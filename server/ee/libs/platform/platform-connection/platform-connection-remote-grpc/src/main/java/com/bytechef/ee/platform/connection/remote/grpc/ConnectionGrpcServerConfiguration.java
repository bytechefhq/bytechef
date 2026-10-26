/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc;

import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnClass(name = "com.bytechef.platform.connection.service.ConnectionServiceImpl")
public class ConnectionGrpcServerConfiguration {

    @Bean
    ConnectionFacadeRemoteServiceGrpcServer connectionFacadeRemoteServiceGrpcServer(
        ConnectionFacade connectionFacade, ObjectMapper objectMapper) {

        return new ConnectionFacadeRemoteServiceGrpcServer(connectionFacade, new ConnectionProtoMapper(objectMapper));
    }

    @Bean
    ConnectionRemoteServiceGrpcServer connectionRemoteServiceGrpcServer(
        ConnectionService connectionService, ObjectMapper objectMapper) {

        return new ConnectionRemoteServiceGrpcServer(connectionService, new ConnectionProtoMapper(objectMapper));
    }
}
