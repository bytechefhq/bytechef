/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.ConnectedUserProjectDeploymentResolver;
import com.bytechef.ee.embedded.configuration.domain.AppEvent;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.remote.client.facade.RemoteAutomationWorkflowProjectFacadeClient;
import com.bytechef.ee.embedded.configuration.remote.client.facade.RemoteConnectedUserProjectFacadeClient;
import com.bytechef.ee.embedded.configuration.remote.client.service.RemoteAppEventServiceClient;
import com.bytechef.ee.embedded.configuration.remote.client.service.RemoteConnectedUserProjectDeploymentResolverClient;
import com.bytechef.ee.embedded.configuration.remote.client.service.RemoteConnectedUserProjectServiceClient;
import com.bytechef.ee.embedded.configuration.remote.client.service.RemoteIntegrationInstanceConfigurationConfigurationWorkflowServiceClient;
import com.bytechef.ee.embedded.configuration.remote.client.service.RemoteIntegrationInstanceConfigurationServiceClient;
import com.bytechef.ee.embedded.configuration.remote.client.service.RemoteIntegrationInstanceServiceClient;
import com.bytechef.ee.embedded.configuration.remote.client.service.RemoteIntegrationInstanceWorkflowServiceClient;
import com.bytechef.ee.embedded.configuration.remote.client.service.RemoteIntegrationServiceClient;
import com.bytechef.ee.embedded.configuration.remote.grpc.AppEventRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.AutomationWorkflowProjectFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.ConnectedUserProjectDeploymentResolverRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.ConnectedUserProjectFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.ConnectedUserProjectRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.IntegrationInstanceConfigurationRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.IntegrationInstanceConfigurationWorkflowRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.IntegrationInstanceRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.IntegrationInstanceWorkflowRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.IntegrationRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConnectedUserIntegrationInstancesRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.service.AppEventService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.configuration.domain.Environment;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedConfigurationRemoteServiceGrpcWireTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();

    private final AppEventService appEventService = mock(AppEventService.class);
    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade =
        mock(AutomationWorkflowProjectFacade.class);
    private final ConnectedUserProjectDeploymentResolver connectedUserProjectDeploymentResolver =
        mock(ConnectedUserProjectDeploymentResolver.class);
    private final ConnectedUserProjectFacade connectedUserProjectFacade = mock(ConnectedUserProjectFacade.class);
    private final ConnectedUserProjectService connectedUserProjectService = mock(ConnectedUserProjectService.class);
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService =
        mock(IntegrationInstanceConfigurationService.class);
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService =
        mock(IntegrationInstanceConfigurationWorkflowService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService =
        mock(IntegrationInstanceWorkflowService.class);
    private final IntegrationService integrationService = mock(IntegrationService.class);

    private FixedGrpcChannels fixedGrpcChannels;
    private ManagedChannel managedChannel;
    private Server server;

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();
        EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper =
            new EmbeddedConfigurationProtoMapper(JSON_MAPPER);
        ServerInterceptor serverInterceptor = new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler());

        InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(name)
            .directExecutor();

        for (BindableService bindableService : List.<BindableService>of(
            new AppEventRemoteServiceGrpcServer(appEventService, embeddedConfigurationProtoMapper),
            new AutomationWorkflowProjectFacadeRemoteServiceGrpcServer(
                automationWorkflowProjectFacade, embeddedConfigurationProtoMapper),
            new ConnectedUserProjectDeploymentResolverRemoteServiceGrpcServer(connectedUserProjectDeploymentResolver),
            new ConnectedUserProjectFacadeRemoteServiceGrpcServer(connectedUserProjectFacade),
            new ConnectedUserProjectRemoteServiceGrpcServer(connectedUserProjectService),
            new IntegrationInstanceConfigurationRemoteServiceGrpcServer(
                integrationInstanceConfigurationService, embeddedConfigurationProtoMapper),
            new IntegrationInstanceConfigurationWorkflowRemoteServiceGrpcServer(
                integrationInstanceConfigurationWorkflowService, embeddedConfigurationProtoMapper),
            new IntegrationInstanceRemoteServiceGrpcServer(integrationInstanceService,
                embeddedConfigurationProtoMapper),
            new IntegrationInstanceWorkflowRemoteServiceGrpcServer(
                integrationInstanceWorkflowService, embeddedConfigurationProtoMapper),
            new IntegrationRemoteServiceGrpcServer(integrationService, embeddedConfigurationProtoMapper))) {

            inProcessServerBuilder.addService(ServerInterceptors.intercept(bindableService, serverInterceptor));
        }

        server = inProcessServerBuilder.build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();
        fixedGrpcChannels = new FixedGrpcChannels(managedChannel);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testIntegrationCalls() {
        Integration integration = integration(7L);

        when(integrationService.getIntegration(7L)).thenReturn(integration);
        when(integrationService.getIntegrationInstanceIntegration(12L)).thenReturn(integration);
        when(integrationService.getIntegrations()).thenReturn(List.of(integration, integration(8L)));
        when(integrationService.getWorkflowIntegration("workflow-1")).thenReturn(integration);

        RemoteIntegrationServiceClient remoteIntegrationServiceClient = new RemoteIntegrationServiceClient(
            fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteIntegrationServiceClient.getIntegration(7L)).usingRecursiveComparison()
            .isEqualTo(integration);
        assertThat(remoteIntegrationServiceClient.getIntegrationInstanceIntegration(12L)).usingRecursiveComparison()
            .isEqualTo(integration);
        assertThat(remoteIntegrationServiceClient.getIntegrations()).extracting(Integration::getId)
            .containsExactly(7L, 8L);
        assertThat(remoteIntegrationServiceClient.getWorkflowIntegration("workflow-1")).usingRecursiveComparison()
            .isEqualTo(integration);
    }

    @Test
    void testGetIntegrationsByIds() {
        when(integrationService.getIntegrations(List.of(7L, 8L))).thenReturn(List.of(integration(7L), integration(8L)));
        when(integrationService.getIntegrations(List.of())).thenReturn(List.of());

        RemoteIntegrationServiceClient remoteIntegrationServiceClient = new RemoteIntegrationServiceClient(
            fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteIntegrationServiceClient.getIntegrations(List.of(7L, 8L))).extracting(Integration::getId)
            .containsExactly(7L, 8L);
        assertThat(remoteIntegrationServiceClient.getIntegrations(List.of())).isEmpty();
    }

    @Test
    void testGetUnknownIntegrationFailsWithItsMessage() {
        when(integrationService.getIntegration(7L)).thenThrow(new IllegalArgumentException("Integration 7 not found"));

        RemoteIntegrationServiceClient remoteIntegrationServiceClient = new RemoteIntegrationServiceClient(
            fixedGrpcChannels, JSON_MAPPER);

        assertThatThrownBy(() -> remoteIntegrationServiceClient.getIntegration(7L))
            .hasMessageContaining("Integration 7 not found");
    }

    @Test
    void testGetIntegrationInstanceConfiguration() {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of("key", "secret"));
        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setId(5L);
        integrationInstanceConfiguration.setIntegrationId(7L);

        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(5L))
            .thenReturn(integrationInstanceConfiguration);

        RemoteIntegrationInstanceConfigurationServiceClient remoteIntegrationInstanceConfigurationServiceClient =
            new RemoteIntegrationInstanceConfigurationServiceClient(fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteIntegrationInstanceConfigurationServiceClient.getIntegrationInstanceConfiguration(5L))
            .usingRecursiveComparison()
            .isEqualTo(integrationInstanceConfiguration);
    }

    @Test
    void testGetIntegrationInstanceConfigurationsByIds() {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setId(5L);
        integrationInstanceConfiguration.setIntegrationId(7L);

        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(List.of(5L)))
            .thenReturn(List.of(integrationInstanceConfiguration));
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(List.of()))
            .thenReturn(List.of());

        RemoteIntegrationInstanceConfigurationServiceClient remoteIntegrationInstanceConfigurationServiceClient =
            new RemoteIntegrationInstanceConfigurationServiceClient(fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteIntegrationInstanceConfigurationServiceClient.getIntegrationInstanceConfigurations(
            List.of(5L)))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(integrationInstanceConfiguration);
        assertThat(remoteIntegrationInstanceConfigurationServiceClient.getIntegrationInstanceConfigurations(List.of()))
            .isEmpty();
    }

    @Test
    void testIntegrationInstanceConfigurationWorkflowCalls() {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setConnections(List.of(connection(4L)));
        integrationInstanceConfigurationWorkflow.setId(11L);
        integrationInstanceConfigurationWorkflow.setInputs(Map.of("id", 9_007_199_254_740_993L));
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(5L);
        integrationInstanceConfigurationWorkflow.setWorkflowId("workflow-1");

        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
            5L, "workflow-1"))
                .thenReturn(integrationInstanceConfigurationWorkflow);
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflowConnection(
            5L, "workflow-1", "airtable_1", "airtable"))
                .thenReturn(connection(4L));
        when(integrationInstanceConfigurationWorkflowService.fetchIntegrationInstanceConfigurationWorkflowConnection(
            5L, "workflow-1", "airtable_1", "airtable"))
                .thenReturn(Optional.of(connection(4L)));
        when(integrationInstanceConfigurationWorkflowService.fetchIntegrationInstanceConfigurationWorkflowConnection(
            5L, "workflow-1", "airtable_2", "airtable"))
                .thenReturn(Optional.empty());
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflowConnections(
            5L, "workflow-1", "airtable_1"))
                .thenReturn(List.of(connection(4L), connection(5L)));

        RemoteIntegrationInstanceConfigurationConfigurationWorkflowServiceClient client =
            new RemoteIntegrationInstanceConfigurationConfigurationWorkflowServiceClient(
                fixedGrpcChannels, JSON_MAPPER);

        assertThat(client.getIntegrationInstanceConfigurationWorkflow(5L, "workflow-1")).usingRecursiveComparison()
            .isEqualTo(integrationInstanceConfigurationWorkflow);
        assertThat(client.getIntegrationInstanceConfigurationWorkflowConnection(
            5L, "workflow-1", "airtable_1", "airtable"))
                .usingRecursiveComparison()
                .isEqualTo(connection(4L));
        assertThat(client.fetchIntegrationInstanceConfigurationWorkflowConnection(
            5L, "workflow-1", "airtable_1", "airtable"))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(connection(4L));
        assertThat(client.fetchIntegrationInstanceConfigurationWorkflowConnection(
            5L, "workflow-1", "airtable_2", "airtable")).isEmpty();
        assertThat(client.getIntegrationInstanceConfigurationWorkflowConnections(5L, "workflow-1", "airtable_1"))
            .extracting(IntegrationInstanceConfigurationWorkflowConnection::getConnectionId)
            .containsExactly(4L, 5L);
    }

    @Test
    void testIntegrationInstanceConfigurationWorkflowLookups() {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setConnections(List.of(connection(4L)));
        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setId(11L);
        integrationInstanceConfigurationWorkflow.setInputs(Map.of("count", 5));
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(5L);
        integrationInstanceConfigurationWorkflow.setWorkflowId("workflow-1");

        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(11L))
            .thenReturn(integrationInstanceConfigurationWorkflow);
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(99L))
            .thenThrow(new IllegalArgumentException("Integration instance configuration workflow not found"));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(5L))
            .thenReturn(List.of(integrationInstanceConfigurationWorkflow));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(6L))
            .thenReturn(List.of());
        when(integrationInstanceConfigurationWorkflowService.isIntegrationInstanceWorkflowEnabled(5L, "workflow-1"))
            .thenReturn(true);
        when(integrationInstanceConfigurationWorkflowService.isIntegrationInstanceWorkflowEnabled(5L, "workflow-2"))
            .thenReturn(false);

        RemoteIntegrationInstanceConfigurationConfigurationWorkflowServiceClient client =
            new RemoteIntegrationInstanceConfigurationConfigurationWorkflowServiceClient(
                fixedGrpcChannels, JSON_MAPPER);

        assertThat(client.getIntegrationInstanceConfigurationWorkflow(11L)).usingRecursiveComparison()
            .isEqualTo(integrationInstanceConfigurationWorkflow);
        assertThatThrownBy(() -> client.getIntegrationInstanceConfigurationWorkflow(99L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Integration instance configuration workflow not found");
        assertThat(client.getIntegrationInstanceConfigurationWorkflows(5L))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(integrationInstanceConfigurationWorkflow);
        assertThat(client.getIntegrationInstanceConfigurationWorkflows(6L)).isEmpty();
        assertThat(client.isIntegrationInstanceWorkflowEnabled(5L, "workflow-1")).isTrue();
        assertThat(client.isIntegrationInstanceWorkflowEnabled(5L, "workflow-2")).isFalse();
    }

    @Test
    void testIsConnectionUsed() {
        when(integrationInstanceConfigurationWorkflowService.isConnectionUsed(7L)).thenReturn(true);
        when(integrationInstanceConfigurationWorkflowService.isConnectionUsed(8L)).thenReturn(false);

        RemoteIntegrationInstanceConfigurationConfigurationWorkflowServiceClient client =
            new RemoteIntegrationInstanceConfigurationConfigurationWorkflowServiceClient(
                fixedGrpcChannels, JSON_MAPPER);

        assertThat(client.isConnectionUsed(7L)).isTrue();
        assertThat(client.isConnectionUsed(8L)).isFalse();
    }

    @Test
    void testGetConnectedUserIntegrationInstances() {
        IntegrationInstance integrationInstance = integrationInstance();

        when(integrationInstanceService.getConnectedUserIntegrationInstances(7L, true))
            .thenReturn(List.of(integrationInstance));
        when(integrationInstanceService.getConnectedUserIntegrationInstances(7L, Environment.PRODUCTION))
            .thenReturn(List.of(integrationInstance));
        when(integrationInstanceService.getConnectedUserIntegrationInstances(7L))
            .thenReturn(List.of());

        RemoteIntegrationInstanceServiceClient remoteIntegrationInstanceServiceClient =
            new RemoteIntegrationInstanceServiceClient(fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteIntegrationInstanceServiceClient.getConnectedUserIntegrationInstances(7L, true))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(integrationInstance);
        assertThat(remoteIntegrationInstanceServiceClient.getConnectedUserIntegrationInstances(
            7L, Environment.PRODUCTION))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(integrationInstance);
        assertThat(remoteIntegrationInstanceServiceClient.getConnectedUserIntegrationInstances(7L)).isEmpty();
    }

    @Test
    void testGetConnectedUserIntegrationInstancesWithUnknownEnvironmentIsAnInvalidArgument() {
        IntegrationInstanceRemoteServiceGrpc.IntegrationInstanceRemoteServiceBlockingStub stub =
            IntegrationInstanceRemoteServiceGrpc.newBlockingStub(managedChannel);
        ConnectedUserIntegrationInstancesRequest request = ConnectedUserIntegrationInstancesRequest.newBuilder()
            .setConnectedUserId(7L)
            .setEnvironment(Environment.values().length)
            .build();

        assertThatThrownBy(() -> stub.getConnectedUserIntegrationInstances(request))
            .isInstanceOfSatisfying(
                StatusRuntimeException.class,
                exception -> assertThat(exception.getStatus()
                    .getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
    }

    @Test
    void testIntegrationInstanceCalls() {
        IntegrationInstance integrationInstance = integrationInstance();

        when(integrationInstanceService.getIntegrationInstance(17L)).thenReturn(integrationInstance);
        when(integrationInstanceService.fetchIntegrationInstance(7L, "airtable", Environment.PRODUCTION))
            .thenReturn(Optional.of(integrationInstance));
        when(integrationInstanceService.fetchIntegrationInstance(7L, "slack", Environment.PRODUCTION))
            .thenReturn(Optional.empty());
        when(integrationInstanceService.getIntegrationInstance(7L, "workflow-1", Environment.PRODUCTION))
            .thenReturn(integrationInstance);
        when(integrationInstanceService.getIntegrationInstances(List.of(17L))).thenReturn(List.of(integrationInstance));
        when(integrationInstanceService.getIntegrationInstanceConfigurationIntegrationInstances(List.of(11L)))
            .thenReturn(List.of(integrationInstance));
        when(integrationInstanceService.getIntegrationInstanceConfigurationIntegrationInstances(List.of()))
            .thenReturn(List.of());

        RemoteIntegrationInstanceServiceClient remoteIntegrationInstanceServiceClient =
            new RemoteIntegrationInstanceServiceClient(fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteIntegrationInstanceServiceClient.getIntegrationInstance(17L))
            .usingRecursiveComparison()
            .isEqualTo(integrationInstance);
        assertThat(remoteIntegrationInstanceServiceClient.fetchIntegrationInstance(
            7L, "airtable", Environment.PRODUCTION))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(integrationInstance);
        assertThat(remoteIntegrationInstanceServiceClient.fetchIntegrationInstance(
            7L, "slack", Environment.PRODUCTION)).isEmpty();
        assertThat(remoteIntegrationInstanceServiceClient.getIntegrationInstance(
            7L, "workflow-1", Environment.PRODUCTION))
                .usingRecursiveComparison()
                .isEqualTo(integrationInstance);
        assertThat(remoteIntegrationInstanceServiceClient.getIntegrationInstances(List.of(17L)))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(integrationInstance);
        assertThat(remoteIntegrationInstanceServiceClient.getIntegrationInstanceConfigurationIntegrationInstances(
            List.of(11L)))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(integrationInstance);
        assertThat(remoteIntegrationInstanceServiceClient.getIntegrationInstanceConfigurationIntegrationInstances(
            List.of())).isEmpty();
    }

    @Test
    void testGetUnknownIntegrationInstanceIsAnIllegalArgument() {
        when(integrationInstanceService.getIntegrationInstance(99L))
            .thenThrow(new IllegalArgumentException("Integration instance not found"));
        when(integrationInstanceService.getIntegrationInstance(7L, "workflow-2", Environment.PRODUCTION))
            .thenThrow(new IllegalArgumentException("Integration instance not found"));

        RemoteIntegrationInstanceServiceClient remoteIntegrationInstanceServiceClient =
            new RemoteIntegrationInstanceServiceClient(fixedGrpcChannels, JSON_MAPPER);

        assertThatThrownBy(() -> remoteIntegrationInstanceServiceClient.getIntegrationInstance(99L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Integration instance not found");
        assertThatThrownBy(() -> remoteIntegrationInstanceServiceClient.getIntegrationInstance(
            7L, "workflow-2", Environment.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Integration instance not found");
    }

    @Test
    void testGetIntegrationInstanceWorkflow() {
        IntegrationInstanceWorkflow integrationInstanceWorkflow = new IntegrationInstanceWorkflow();

        integrationInstanceWorkflow.setId(13L);
        integrationInstanceWorkflow.setInputs(Map.of("count", 5));
        integrationInstanceWorkflow.setIntegrationInstanceConfigurationWorkflowId(11L);
        integrationInstanceWorkflow.setIntegrationInstanceId(12L);

        when(integrationInstanceWorkflowService.getIntegrationInstanceWorkflow(12L, "workflow-1"))
            .thenReturn(integrationInstanceWorkflow);

        RemoteIntegrationInstanceWorkflowServiceClient remoteIntegrationInstanceWorkflowServiceClient =
            new RemoteIntegrationInstanceWorkflowServiceClient(fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteIntegrationInstanceWorkflowServiceClient.getIntegrationInstanceWorkflow(12L, "workflow-1"))
            .usingRecursiveComparison()
            .isEqualTo(integrationInstanceWorkflow);
    }

    @Test
    void testIntegrationInstanceWorkflowLookups() {
        IntegrationInstanceWorkflow integrationInstanceWorkflow = new IntegrationInstanceWorkflow();

        integrationInstanceWorkflow.setEnabled(true);
        integrationInstanceWorkflow.setId(13L);
        integrationInstanceWorkflow.setInputs(Map.of("count", 5));
        integrationInstanceWorkflow.setIntegrationInstanceConfigurationWorkflowId(11L);
        integrationInstanceWorkflow.setIntegrationInstanceId(12L);

        when(integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(12L))
            .thenReturn(List.of(integrationInstanceWorkflow));
        when(integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(14L)).thenReturn(List.of());
        when(integrationInstanceWorkflowService.fetchIntegrationInstanceWorkflow(12L, "workflow-1"))
            .thenReturn(Optional.of(integrationInstanceWorkflow));
        when(integrationInstanceWorkflowService.fetchIntegrationInstanceWorkflow(12L, "workflow-2"))
            .thenReturn(Optional.empty());

        RemoteIntegrationInstanceWorkflowServiceClient remoteIntegrationInstanceWorkflowServiceClient =
            new RemoteIntegrationInstanceWorkflowServiceClient(fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteIntegrationInstanceWorkflowServiceClient.getIntegrationInstanceWorkflows(12L))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(integrationInstanceWorkflow);
        assertThat(remoteIntegrationInstanceWorkflowServiceClient.getIntegrationInstanceWorkflows(14L)).isEmpty();
        assertThat(remoteIntegrationInstanceWorkflowServiceClient.fetchIntegrationInstanceWorkflow(12L, "workflow-1"))
            .get()
            .usingRecursiveComparison()
            .isEqualTo(integrationInstanceWorkflow);
        assertThat(remoteIntegrationInstanceWorkflowServiceClient.fetchIntegrationInstanceWorkflow(12L, "workflow-2"))
            .isEmpty();
    }

    @Test
    void testGetAppEvent() {
        AppEvent appEvent = new AppEvent();

        appEvent.setId(3L);
        appEvent.setName("order.created");
        appEvent.setSchema("{}");

        when(appEventService.getAppEvent(3L)).thenReturn(appEvent);

        RemoteAppEventServiceClient remoteAppEventServiceClient = new RemoteAppEventServiceClient(
            fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteAppEventServiceClient.getAppEvent(3L)).usingRecursiveComparison()
            .isEqualTo(appEvent);
    }

    @Test
    void testGetProjectDeploymentIds() {
        when(connectedUserProjectDeploymentResolver.getProjectDeploymentIds(7L)).thenReturn(List.of(3L, 5L, 11L));
        when(connectedUserProjectDeploymentResolver.getProjectDeploymentIds(8L)).thenReturn(List.of());

        RemoteConnectedUserProjectDeploymentResolverClient client =
            new RemoteConnectedUserProjectDeploymentResolverClient(fixedGrpcChannels);

        assertThat(client.getProjectDeploymentIds(7L)).containsExactly(3L, 5L, 11L);
        assertThat(client.getProjectDeploymentIds(8L)).isEmpty();
    }

    @Test
    void testGetPublishedProjects() {
        List<AutomationWorkflowProjectDTO> automationWorkflowProjectDTOs = List.of(
            new AutomationWorkflowProjectDTO(
                7L, "Live Project", "Live", null, List.of(3L), true, 2, 1, List.of(), null, false));

        when(automationWorkflowProjectFacade.getPublishedProjects()).thenReturn(automationWorkflowProjectDTOs);

        RemoteAutomationWorkflowProjectFacadeClient remoteAutomationWorkflowProjectFacadeClient =
            new RemoteAutomationWorkflowProjectFacadeClient(fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteAutomationWorkflowProjectFacadeClient.getPublishedProjects())
            .isEqualTo(automationWorkflowProjectDTOs);
    }

    @Test
    void testCopyWorkflowTemplateReturnsTheNewWorkflowId() {
        when(connectedUserProjectFacade.copyWorkflowTemplate("user-1", "uuid-1", Environment.PRODUCTION))
            .thenReturn("workflow-2");

        RemoteConnectedUserProjectFacadeClient remoteConnectedUserProjectFacadeClient =
            new RemoteConnectedUserProjectFacadeClient(fixedGrpcChannels);

        assertThat(remoteConnectedUserProjectFacadeClient.copyWorkflowTemplate(
            "user-1", "uuid-1", Environment.PRODUCTION)).isEqualTo("workflow-2");
    }

    @Test
    void testContainsProjectDeployment() {
        when(connectedUserProjectService.containsProjectDeployment(1051L)).thenReturn(false);
        when(connectedUserProjectService.containsProjectDeployment(1052L)).thenReturn(true);

        RemoteConnectedUserProjectServiceClient remoteConnectedUserProjectServiceClient =
            new RemoteConnectedUserProjectServiceClient(fixedGrpcChannels);

        assertThat(remoteConnectedUserProjectServiceClient.containsProjectDeployment(1051L)).isFalse();
        assertThat(remoteConnectedUserProjectServiceClient.containsProjectDeployment(1052L)).isTrue();
    }

    private static IntegrationInstance integrationInstance() {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectedUserId(7L);
        integrationInstance.setConnectionId(21L);
        integrationInstance.setEnabled(true);
        integrationInstance.setId(17L);
        integrationInstance.setIntegrationInstanceConfigurationId(11L);
        integrationInstance.setVersion(2);

        ReflectionTestUtils.setField(integrationInstance, "createdBy", "admin@localhost.com");
        ReflectionTestUtils.setField(integrationInstance, "createdDate", Instant.parse("2026-10-26T10:00:00Z"));
        ReflectionTestUtils.setField(integrationInstance, "lastModifiedBy", "user@localhost.com");
        ReflectionTestUtils.setField(integrationInstance, "lastModifiedDate", Instant.parse("2026-10-27T11:30:00Z"));

        return integrationInstance;
    }

    private static IntegrationInstanceConfigurationWorkflowConnection connection(long connectionId) {
        return new IntegrationInstanceConfigurationWorkflowConnection(connectionId, "airtable", "airtable_1");
    }

    private static Integration integration(long id) {
        Integration integration = new Integration();

        integration.setComponentName("airtable");
        integration.setId(id);
        integration.setName("Airtable");

        return integration;
    }
}
