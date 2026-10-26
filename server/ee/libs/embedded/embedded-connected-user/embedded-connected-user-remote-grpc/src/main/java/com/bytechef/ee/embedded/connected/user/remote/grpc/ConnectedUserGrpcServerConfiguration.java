/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.remote.grpc;

import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Serves connected users over gRPC from the microservice that owns them.
 *
 * @version ee
 */
@Configuration
@ConditionalOnClass(name = "com.bytechef.ee.embedded.connected.user.service.ConnectedUserServiceImpl")
public class ConnectedUserGrpcServerConfiguration {

    @Bean
    ConnectedUserRemoteServiceGrpcServer connectedUserRemoteServiceGrpcServer(
        ConnectedUserService connectedUserService) {

        return new ConnectedUserRemoteServiceGrpcServer(connectedUserService);
    }
}
