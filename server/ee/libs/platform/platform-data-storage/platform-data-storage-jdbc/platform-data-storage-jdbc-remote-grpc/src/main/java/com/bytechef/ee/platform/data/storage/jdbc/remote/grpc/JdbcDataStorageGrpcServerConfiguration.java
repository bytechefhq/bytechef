/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.storage.jdbc.remote.grpc;

import com.bytechef.platform.data.storage.annotation.ConditionalOnDataStorageProviderJdbc;
import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnClass(name = "com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageServiceImpl")
@ConditionalOnDataStorageProviderJdbc
public class JdbcDataStorageGrpcServerConfiguration {

    @Bean
    JdbcDataStorageRemoteServiceGrpcServer jdbcDataStorageRemoteServiceGrpcServer(
        JdbcDataStorageService jdbcDataStorageService) {

        return new JdbcDataStorageRemoteServiceGrpcServer(jdbcDataStorageService);
    }
}
