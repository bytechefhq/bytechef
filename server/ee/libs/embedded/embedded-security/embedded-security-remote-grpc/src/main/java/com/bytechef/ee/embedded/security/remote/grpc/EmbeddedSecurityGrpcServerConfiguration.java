/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.remote.grpc;

import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 */
@Configuration
@ConditionalOnClass(name = "com.bytechef.ee.embedded.security.service.SigningKeyServiceImpl")
public class EmbeddedSecurityGrpcServerConfiguration {

    @Bean
    JwtTokenRemoteServiceGrpcServer jwtTokenRemoteServiceGrpcServer(JwtTokenService jwtTokenService) {
        return new JwtTokenRemoteServiceGrpcServer(jwtTokenService);
    }

    @Bean
    SigningKeyRemoteServiceGrpcServer signingKeyRemoteServiceGrpcServer(SigningKeyService signingKeyService) {
        return new SigningKeyRemoteServiceGrpcServer(signingKeyService);
    }
}
