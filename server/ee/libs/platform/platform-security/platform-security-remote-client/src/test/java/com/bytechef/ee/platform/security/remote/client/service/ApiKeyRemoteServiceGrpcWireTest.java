/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.security.remote.grpc.ApiKeyRemoteServiceGrpcServer;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class ApiKeyRemoteServiceGrpcWireTest {

    private final ApiKeyService apiKeyService = mock(ApiKeyService.class);
    private ManagedChannel managedChannel;
    private RemoteApiKeyServiceClient remoteApiKeyServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new ApiKeyRemoteServiceGrpcServer(apiKeyService))
            .build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteApiKeyServiceClient = new RemoteApiKeyServiceClient(serviceId -> managedChannel);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testExists() {
        when(apiKeyService.exists("secret-key", 2L)).thenReturn(true);

        assertThat(remoteApiKeyServiceClient.exists("secret-key", 2L)).isTrue();
        assertThat(remoteApiKeyServiceClient.exists("other-key", 2L)).isFalse();
    }

    @Test
    void testFetchApiKeyReturnsEmptyWhenMissing() {
        when(apiKeyService.fetchApiKey("missing")).thenReturn(Optional.empty());

        assertThat(remoteApiKeyServiceClient.fetchApiKey("missing")).isEmpty();
    }

    @Test
    void testGetApiKeyBySecretKeyAndEnvironment() {
        when(apiKeyService.getApiKey("secret-key", 2L)).thenReturn(apiKey());

        assertThat(remoteApiKeyServiceClient.getApiKey("secret-key", 2L)
            .getId()).isEqualTo(5L);
    }

    @Test
    void testGetApiKeyThrowsWhenMissing() {
        when(apiKeyService.getApiKey("missing", 2L)).thenThrow(new IllegalArgumentException("Api key not found."));
        when(apiKeyService.fetchApiKey("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> remoteApiKeyServiceClient.getApiKey("missing", 2L))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> remoteApiKeyServiceClient.getApiKey("missing"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static ApiKey apiKey() {
        ApiKey apiKey = new ApiKey();

        apiKey.setEnvironment(Environment.PRODUCTION);
        apiKey.setId(5L);
        apiKey.setSecretKey("secret-key");
        apiKey.setUserId(1L);

        return apiKey;
    }
}
