/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.storage.jdbc.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class JdbcDataStorageGrpcServerConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(JdbcDataStorageGrpcServerConfiguration.class);

    @Test
    void testServedWhenTheProviderIsJdbc() {
        applicationContextRunner.withPropertyValues("bytechef.data-storage.provider=jdbc")
            .withBean(JdbcDataStorageService.class, () -> mock(JdbcDataStorageService.class))
            .run(context -> assertThat(context).hasSingleBean(JdbcDataStorageRemoteServiceGrpcServer.class));
    }

    @Test
    void testNotServedWhenTheProviderIsNotJdbc() {
        applicationContextRunner.withPropertyValues("bytechef.data-storage.provider=filesystem")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(JdbcDataStorageRemoteServiceGrpcServer.class);
            });
    }
}
