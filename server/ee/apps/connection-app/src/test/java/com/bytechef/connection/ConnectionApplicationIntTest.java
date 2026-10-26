/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.connection;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.BindableService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest
@Import(PostgreSQLContainerConfiguration.class)
public class ConnectionApplicationIntTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Test
    void testContextLoads() {
    }

    @Test
    void testSharedAndAppConfigurationLoad() {
        assertThat(environment.getProperty("spring.task.execution.thread-name-prefix"))
            .isEqualTo("connection-app-task-");
        assertThat(environment.getProperty("docs.documentation-url")).isEqualTo("https://docs.bytechef.io");
    }

    @Test
    void testConnectionGrpcServicesAreServed() {
        List<String> serviceNames = applicationContext.getBeansOfType(BindableService.class)
            .values()
            .stream()
            .map(bindableService -> bindableService.bindService()
                .getServiceDescriptor()
                .getName())
            .toList();

        assertThat(serviceNames).contains(
            "bytechef.platform.connection.remote.v1.ConnectionRemoteService",
            "bytechef.platform.connection.remote.v1.ConnectionFacadeRemoteService");
    }

    @Test
    void testJobPrincipalAccessorsResolveForEveryPlatformType() {
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry = applicationContext.getBean(
            JobPrincipalAccessorRegistry.class);

        assertThat(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION)).isNotNull();
        assertThat(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.EMBEDDED)).isNotNull();
    }
}
