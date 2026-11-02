/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.tenant.remote.grpc.TenantRemoteServiceGrpcServer;
import com.bytechef.ee.tenant.repository.TenantRepository;
import com.bytechef.ee.tenant.service.MultiTenantService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = RemoteTenantServiceClientIntTest.RemoteTenantServiceClientIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "bytechef.tenant.mode=multi"
    })
@Import(PostgreSQLContainerConfiguration.class)
class RemoteTenantServiceClientIntTest {

    @Autowired
    private MultiTenantService multiTenantService;

    @Autowired
    private TenantRepository tenantRepository;

    private ManagedChannel managedChannel;
    private RemoteTenantServiceClient remoteTenantServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new TenantRemoteServiceGrpcServer(multiTenantService))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteTenantServiceClient = new RemoteTenantServiceClient(new FixedGrpcChannels(managedChannel));

        tenantRepository.createTenant("000001");
        tenantRepository.createTenant("000002");
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        tenantRepository.deleteTenant("000001");
        tenantRepository.deleteTenant("000002");
    }

    @Test
    void testGetTenantIdsReturnsTheTenantSchemasOfConfigurationApp() {
        assertThat(remoteTenantServiceClient.getTenantIds()).containsExactlyInAnyOrder("000001", "000002");
    }

    @Configuration
    @EnableConfigurationProperties(LiquibaseProperties.class)
    @ImportAutoConfiguration({
        DataSourceAutoConfiguration.class, ServiceConnectionAutoConfiguration.class
    })
    @Import({
        MultiTenantService.class, TenantRepository.class
    })
    static class RemoteTenantServiceClientIntTestConfiguration {
    }
}
