/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.storage.db.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.data.jdbc.converter.StringToTypedValueConverter;
import com.bytechef.commons.data.jdbc.converter.TypedValueToStringConverter;
import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.JdbcDataStorageRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.repository.DataStorageRepository;
import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageService;
import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageServiceImpl;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(
    classes = RemoteJdbcDataStorageServiceClientIntTest.Config.class,
    properties = "spring.liquibase.contexts=execution")
@Import(PostgreSQLContainerConfiguration.class)
class RemoteJdbcDataStorageServiceClientIntTest {

    private static final long ENVIRONMENT_ID = 2L;
    private static final Map<String, Object> VALUE = Map.of("cursor", "abc", "page", 2, "ids", List.of(1, 2));

    @Autowired
    private DataStorageRepository dataStorageRepository;

    @Autowired
    private JdbcDataStorageService jdbcDataStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    private ManagedChannel managedChannel;
    private RemoteJdbcDataStorageServiceClient remoteJdbcDataStorageServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new JdbcDataStorageRemoteServiceGrpcServer(jdbcDataStorageService, objectMapper))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteJdbcDataStorageServiceClient = new RemoteJdbcDataStorageServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        dataStorageRepository.deleteAll();
    }

    @Test
    void testPutStoresANestedValueThatFetchReturns() {
        remoteJdbcDataStorageServiceClient.put(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "state", ENVIRONMENT_ID, PlatformType.AUTOMATION,
            VALUE);

        assertThat(jdbcDataStorageService.<Object>fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "state", ENVIRONMENT_ID, PlatformType.AUTOMATION))
                .contains(VALUE);

        Optional<Object> value = remoteJdbcDataStorageServiceClient.fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "state", ENVIRONMENT_ID, PlatformType.AUTOMATION);

        assertThat(value).contains(VALUE);
    }

    @Test
    void testPutReplacesTheStoredValueOfAnExistingKey() {
        remoteJdbcDataStorageServiceClient.put(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "cursor", ENVIRONMENT_ID, PlatformType.AUTOMATION,
            "first");
        remoteJdbcDataStorageServiceClient.put(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "cursor", ENVIRONMENT_ID, PlatformType.AUTOMATION,
            "second");

        assertThat(dataStorageRepository.count()).isEqualTo(1);
        assertThat(jdbcDataStorageService.<Object>fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "cursor", ENVIRONMENT_ID, PlatformType.AUTOMATION))
                .contains("second");
    }

    @Test
    void testStoredNumbersKeepTheirTypes() {
        remoteJdbcDataStorageServiceClient.put(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "id", ENVIRONMENT_ID, PlatformType.AUTOMATION,
            9_007_199_254_740_993L);
        remoteJdbcDataStorageServiceClient.put(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "ratio", ENVIRONMENT_ID, PlatformType.AUTOMATION,
            5.0);

        assertThat(jdbcDataStorageService.<Object>fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "id", ENVIRONMENT_ID, PlatformType.AUTOMATION))
                .containsInstanceOf(Long.class)
                .contains(9_007_199_254_740_993L);
        assertThat(remoteJdbcDataStorageServiceClient.<Object>fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "id", ENVIRONMENT_ID, PlatformType.AUTOMATION))
                .containsInstanceOf(Long.class)
                .contains(9_007_199_254_740_993L);
        assertThat(remoteJdbcDataStorageServiceClient.<Object>fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "ratio", ENVIRONMENT_ID, PlatformType.AUTOMATION))
                .containsInstanceOf(Double.class)
                .contains(5.0);
    }

    @Test
    void testFetchIsScopedByComponentScopeIdEnvironmentAndType() {
        jdbcDataStorageService.put(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "state", ENVIRONMENT_ID, PlatformType.AUTOMATION,
            "automation-state");

        assertThat(remoteJdbcDataStorageServiceClient.<Object>fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "state", ENVIRONMENT_ID, PlatformType.AUTOMATION))
                .contains("automation-state");
        assertThat(remoteJdbcDataStorageServiceClient.fetch(
            "gitlab", DataStorageScope.WORKFLOW, "workflow-1", "state", ENVIRONMENT_ID, PlatformType.AUTOMATION))
                .isEmpty();
        assertThat(remoteJdbcDataStorageServiceClient.fetch(
            "github", DataStorageScope.ACCOUNT, "workflow-1", "state", ENVIRONMENT_ID, PlatformType.AUTOMATION))
                .isEmpty();
        assertThat(remoteJdbcDataStorageServiceClient.fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-2", "state", ENVIRONMENT_ID, PlatformType.AUTOMATION))
                .isEmpty();
        assertThat(remoteJdbcDataStorageServiceClient.fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "state", 1L, PlatformType.AUTOMATION))
                .isEmpty();
        assertThat(remoteJdbcDataStorageServiceClient.fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "state", ENVIRONMENT_ID, PlatformType.EMBEDDED))
                .isEmpty();
    }

    @Test
    void testFetchOfMissingKeyIsEmpty() {
        assertThat(remoteJdbcDataStorageServiceClient.fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "missing", ENVIRONMENT_ID, PlatformType.AUTOMATION))
                .isEmpty();
    }

    @Test
    void testGetReturnsTheStoredValue() {
        jdbcDataStorageService.put(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "state", ENVIRONMENT_ID, PlatformType.AUTOMATION,
            VALUE);

        Object value = remoteJdbcDataStorageServiceClient.get(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "state", ENVIRONMENT_ID, PlatformType.AUTOMATION);

        assertThat(value).isEqualTo(VALUE);
    }

    @Test
    void testGetOfMissingKeyFailsNamingTheKey() {
        assertThatThrownBy(() -> remoteJdbcDataStorageServiceClient.get(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "missing", ENVIRONMENT_ID, PlatformType.AUTOMATION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No data storage value for key missing");
    }

    @Test
    void testGetAllReturnsEveryStoredValueOfTheScope() {
        jdbcDataStorageService.put(
            "github", DataStorageScope.ACCOUNT, "account-1", "state", ENVIRONMENT_ID, PlatformType.EMBEDDED, VALUE);
        jdbcDataStorageService.put(
            "github", DataStorageScope.ACCOUNT, "account-1", "count", ENVIRONMENT_ID, PlatformType.EMBEDDED, 3);
        jdbcDataStorageService.put(
            "github", DataStorageScope.ACCOUNT, "account-2", "count", ENVIRONMENT_ID, PlatformType.EMBEDDED, 4);
        jdbcDataStorageService.put(
            "github", DataStorageScope.ACCOUNT, "account-1", "automation-count", ENVIRONMENT_ID,
            PlatformType.AUTOMATION,
            5);

        Map<String, Object> values = remoteJdbcDataStorageServiceClient.getAll(
            "github", DataStorageScope.ACCOUNT, "account-1", ENVIRONMENT_ID, PlatformType.EMBEDDED);

        assertThat(values).isEqualTo(Map.of("state", VALUE, "count", 3));
    }

    @Test
    void testGetAllOfAnEmptyScopeIsEmpty() {
        assertThat(remoteJdbcDataStorageServiceClient.getAll(
            "github", DataStorageScope.ACCOUNT, "account-1", ENVIRONMENT_ID, PlatformType.EMBEDDED))
                .isEmpty();
    }

    @Test
    void testDeleteRemovesOnlyTheGivenKey() {
        jdbcDataStorageService.put(
            "github", DataStorageScope.ACCOUNT, "account-1", "state", ENVIRONMENT_ID, PlatformType.EMBEDDED, VALUE);
        jdbcDataStorageService.put(
            "github", DataStorageScope.ACCOUNT, "account-1", "count", ENVIRONMENT_ID, PlatformType.EMBEDDED, 3);

        remoteJdbcDataStorageServiceClient.delete(
            "github", DataStorageScope.ACCOUNT, "account-1", "state", ENVIRONMENT_ID, PlatformType.EMBEDDED);

        assertThat(jdbcDataStorageService.fetch(
            "github", DataStorageScope.ACCOUNT, "account-1", "state", ENVIRONMENT_ID, PlatformType.EMBEDDED))
                .isEmpty();
        assertThat(jdbcDataStorageService.getAll(
            "github", DataStorageScope.ACCOUNT, "account-1", ENVIRONMENT_ID, PlatformType.EMBEDDED))
                .isEqualTo(Map.of("count", 3));
    }

    @Test
    void testDeleteScopeDataRemovesEveryComponentValueOfTheScopeId() {
        jdbcDataStorageService.put(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "state", ENVIRONMENT_ID, PlatformType.AUTOMATION,
            VALUE);
        jdbcDataStorageService.put(
            "gitlab", DataStorageScope.WORKFLOW, "workflow-1", "cursor", ENVIRONMENT_ID, PlatformType.AUTOMATION, 3);
        jdbcDataStorageService.put(
            "github", DataStorageScope.WORKFLOW, "workflow-2", "state", ENVIRONMENT_ID, PlatformType.AUTOMATION, 4);

        remoteJdbcDataStorageServiceClient.deleteScopeData(DataStorageScope.WORKFLOW, "workflow-1");

        assertThat(jdbcDataStorageService.fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "state", ENVIRONMENT_ID, PlatformType.AUTOMATION))
                .isEmpty();
        assertThat(jdbcDataStorageService.fetch(
            "gitlab", DataStorageScope.WORKFLOW, "workflow-1", "cursor", ENVIRONMENT_ID, PlatformType.AUTOMATION))
                .isEmpty();
        assertThat(jdbcDataStorageService.<Object>fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-2", "state", ENVIRONMENT_ID, PlatformType.AUTOMATION))
                .contains(4);
    }

    @EnableAutoConfiguration
    @Import({
        LiquibaseConfiguration.class,
        RemoteJdbcDataStorageServiceClientIntTest.Config.DataStorageJdbcConfiguration.class
    })
    @SpringBootConfiguration
    static class Config {

        @Bean
        JdbcDataStorageService jdbcDataStorageService(DataStorageRepository dataStorageRepository) {
            return new JdbcDataStorageServiceImpl(dataStorageRepository);
        }

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class DataStorageJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            @Autowired
            private ObjectMapper objectMapper;

            @Override
            protected List<?> userConverters() {
                return List.of(
                    new StringToTypedValueConverter(objectMapper), new TypedValueToStringConverter(objectMapper));
            }
        }
    }
}
