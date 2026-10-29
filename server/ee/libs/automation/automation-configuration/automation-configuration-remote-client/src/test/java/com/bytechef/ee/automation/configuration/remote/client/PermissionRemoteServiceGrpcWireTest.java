/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.remote.client.service.RemotePermissionServiceClient;
import com.bytechef.ee.automation.configuration.remote.grpc.PermissionRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.InProcessGrpc;
import com.bytechef.ee.remote.grpc.interceptor.UserContextClientInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.UserContextServerInterceptor;
import io.grpc.StatusRuntimeException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PermissionRemoteServiceGrpcWireTest {

    private final PermissionService permissionService = mock(PermissionService.class);

    private InProcessGrpc inProcessGrpc;
    private RemotePermissionServiceClient remotePermissionServiceClient;

    @BeforeEach
    void beforeEach() throws Exception {
        inProcessGrpc = new InProcessGrpc(
            new PermissionRemoteServiceGrpcServer(permissionService), List.of(new UserContextServerInterceptor()),
            List.of(new UserContextClientInterceptor()));

        remotePermissionServiceClient = new RemotePermissionServiceClient(
            new FixedGrpcChannels(inProcessGrpc.channel()));
    }

    @AfterEach
    void afterEach() {
        inProcessGrpc.close();

        SecurityContextHolder.clearContext();
    }

    @Test
    void testServerFailurePropagatesInsteadOfDenying() {
        when(permissionService.hasWorkspaceScope(3L, "PROJECT_VIEW")).thenThrow(new IllegalStateException("down"));

        assertThatThrownBy(() -> remotePermissionServiceClient.hasWorkspaceScope(3L, "PROJECT_VIEW"))
            .isInstanceOf(StatusRuntimeException.class);

        verify(permissionService).hasWorkspaceScope(3L, "PROJECT_VIEW");
    }

    @Test
    void testEvictionsStayLocal() {
        remotePermissionServiceClient.evictWorkspaceScopeCache(1L, 2L);
        remotePermissionServiceClient.evictAllWorkspaceScopeCache();

        verifyNoInteractions(permissionService);
    }
}
