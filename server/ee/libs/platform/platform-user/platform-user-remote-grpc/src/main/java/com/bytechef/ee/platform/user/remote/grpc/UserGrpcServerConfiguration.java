/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.remote.grpc;

import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Serves users and authorities over gRPC from the microservice that owns them. Other microservices have only the remote
 * clients, so the user service implementation is on the classpath only where users live.
 *
 * @version ee
 */
@Configuration
@ConditionalOnClass(name = "com.bytechef.platform.user.service.UserServiceImpl")
public class UserGrpcServerConfiguration {

    @Bean
    AuthorityRemoteServiceGrpcServer authorityRemoteServiceGrpcServer(AuthorityService authorityService) {
        return new AuthorityRemoteServiceGrpcServer(authorityService);
    }

    @Bean
    UserRemoteServiceGrpcServer userRemoteServiceGrpcServer(UserService userService) {
        return new UserRemoteServiceGrpcServer(userService);
    }
}
