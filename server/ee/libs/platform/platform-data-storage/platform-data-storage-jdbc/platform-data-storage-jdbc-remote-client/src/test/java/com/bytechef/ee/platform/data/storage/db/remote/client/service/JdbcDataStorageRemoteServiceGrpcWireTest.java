/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.storage.db.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.data.storage.jdbc.remote.grpc.JdbcDataStorageRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class JdbcDataStorageRemoteServiceGrpcWireTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();

    private static final Map<String, Object> VALUE = Map.of("cursor", "abc", "page", 2, "ids", List.of(1, 2));

    private final JdbcDataStorageService jdbcDataStorageService = mock(JdbcDataStorageService.class);

    private ManagedChannel managedChannel;
    private RemoteJdbcDataStorageServiceClient remoteJdbcDataStorageServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new JdbcDataStorageRemoteServiceGrpcServer(jdbcDataStorageService, JSON_MAPPER))
            .build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteJdbcDataStorageServiceClient = new RemoteJdbcDataStorageServiceClient(
            new FixedGrpcChannels(managedChannel), JSON_MAPPER);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testDataStorageKeepsNumberTypes() {
        remoteJdbcDataStorageServiceClient.put(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "id", 2L, PlatformType.AUTOMATION,
            9_007_199_254_740_993L);

        verify(jdbcDataStorageService).put(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "id", 2L, PlatformType.AUTOMATION,
            9_007_199_254_740_993L);

        when(jdbcDataStorageService.fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "id", 2L, PlatformType.AUTOMATION))
                .thenReturn(Optional.of(9_007_199_254_740_993L));
        when(jdbcDataStorageService.fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "ratio", 2L, PlatformType.AUTOMATION))
                .thenReturn(Optional.of(5.0));

        assertThat(remoteJdbcDataStorageServiceClient.<Object>fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "id", 2L, PlatformType.AUTOMATION))
                .containsInstanceOf(Long.class)
                .contains(9_007_199_254_740_993L);
        assertThat(remoteJdbcDataStorageServiceClient.<Object>fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "ratio", 2L, PlatformType.AUTOMATION))
                .containsInstanceOf(Double.class)
                .contains(5.0);
    }

    @Test
    void testPutAndFetchNestedValue() {
        remoteJdbcDataStorageServiceClient.put(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "state", 2L, PlatformType.AUTOMATION, VALUE);

        verify(jdbcDataStorageService).put(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "state", 2L, PlatformType.AUTOMATION, VALUE);

        when(jdbcDataStorageService.fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "state", 2L, PlatformType.AUTOMATION))
                .thenReturn(Optional.of(VALUE));

        Optional<Object> value = remoteJdbcDataStorageServiceClient.fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "state", 2L, PlatformType.AUTOMATION);

        assertThat(value).contains(VALUE);
    }

    @Test
    void testFetchOfMissingKeyIsEmpty() {
        when(jdbcDataStorageService.fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "missing", 2L, PlatformType.AUTOMATION))
                .thenReturn(Optional.empty());

        assertThat(remoteJdbcDataStorageServiceClient.fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "missing", 2L, PlatformType.AUTOMATION)).isEmpty();
    }

    @Test
    void testGetOfMissingKeyFailsNamingTheKey() {
        when(jdbcDataStorageService.fetch(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "missing", 2L, PlatformType.AUTOMATION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> remoteJdbcDataStorageServiceClient.get(
            "github", DataStorageScope.WORKFLOW, "workflow-1", "missing", 2L, PlatformType.AUTOMATION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void testGetAllAndDelete() {
        when(jdbcDataStorageService.getAll("github", DataStorageScope.ACCOUNT, "account-1", 2L, PlatformType.EMBEDDED))
            .thenReturn(Map.of("state", VALUE, "count", 3));

        Map<String, Object> values = remoteJdbcDataStorageServiceClient.getAll(
            "github", DataStorageScope.ACCOUNT, "account-1", 2L, PlatformType.EMBEDDED);

        assertThat(values).isEqualTo(Map.of("state", VALUE, "count", 3));

        remoteJdbcDataStorageServiceClient.delete(
            "github", DataStorageScope.ACCOUNT, "account-1", "state", 2L, PlatformType.EMBEDDED);

        verify(jdbcDataStorageService).delete(
            "github", DataStorageScope.ACCOUNT, "account-1", "state", 2L, PlatformType.EMBEDDED);
    }

    @Test
    void testDeleteScopeData() {
        remoteJdbcDataStorageServiceClient.deleteScopeData(DataStorageScope.WORKFLOW, "workflow-1");

        verify(jdbcDataStorageService).deleteScopeData(DataStorageScope.WORKFLOW, "workflow-1");
    }
}
