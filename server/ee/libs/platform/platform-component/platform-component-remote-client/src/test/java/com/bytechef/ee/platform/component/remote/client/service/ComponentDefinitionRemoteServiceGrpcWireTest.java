/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.ee.platform.component.remote.grpc.ActionDefinitionRemoteServiceGrpcServer;
import com.bytechef.ee.platform.component.remote.grpc.ComponentDefinitionRemoteServiceGrpcServer;
import com.bytechef.ee.platform.component.remote.grpc.ComponentProtoMapper;
import com.bytechef.ee.platform.component.remote.grpc.ConnectionDefinitionRemoteServiceGrpcServer;
import com.bytechef.ee.platform.component.remote.grpc.TriggerDefinitionRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.filter.ComponentDefinitionFilter;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.constant.PlatformType;
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
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ComponentDefinitionRemoteServiceGrpcWireTest {

    private static final String COMPONENTS_METADATA = "[{\"name\":\"keep\"},{\"name\":\"drop\"}]";

    private final ActionDefinitionService actionDefinitionService = mock(ActionDefinitionService.class);
    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final ConnectionDefinitionService connectionDefinitionService = mock(ConnectionDefinitionService.class);
    private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
    private final JsonMapper jsonMapper = JsonMapper.builder()
        .build();
    private final TriggerDefinitionService triggerDefinitionService = mock(TriggerDefinitionService.class);

    private ManagedChannel managedChannel;
    private RemoteActionDefinitionServiceClient remoteActionDefinitionServiceClient;
    private RemoteComponentDefinitionServiceClient remoteComponentDefinitionServiceClient;
    private RemoteConnectionDefinitionServiceClient remoteConnectionDefinitionServiceClient;
    private RemoteTriggerDefinitionServiceClient remoteTriggerDefinitionServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();
        ComponentProtoMapper componentProtoMapper = new ComponentProtoMapper(jsonMapper);

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new ActionDefinitionRemoteServiceGrpcServer(actionDefinitionService, componentProtoMapper))
            .addService(
                new ComponentDefinitionRemoteServiceGrpcServer(componentDefinitionService, componentProtoMapper))
            .addService(
                new ConnectionDefinitionRemoteServiceGrpcServer(connectionDefinitionService, componentProtoMapper))
            .addService(new TriggerDefinitionRemoteServiceGrpcServer(triggerDefinitionService, componentProtoMapper))
            .build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        when(discoveryClient.getInstances("worker-app")).thenReturn(
            List.of(
                new DefaultServiceInstance(
                    "worker-app:1", "worker-app", "worker-1", 8080, false,
                    Map.of("components", COMPONENTS_METADATA, "grpc-port", "9090")),
                new DefaultServiceInstance(
                    "worker-app:2", "worker-app", "worker-2", 8080, false,
                    Map.of("components", COMPONENTS_METADATA, "grpc-port", "9090"))));

        FixedGrpcChannels fixedGrpcChannels = new FixedGrpcChannels(managedChannel);

        remoteActionDefinitionServiceClient = new RemoteActionDefinitionServiceClient(
            fixedGrpcChannels, discoveryClient, jsonMapper);
        remoteComponentDefinitionServiceClient = new RemoteComponentDefinitionServiceClient(
            fixedGrpcChannels, discoveryClient, jsonMapper, List.of(new DropForAutomationComponentDefinitionFilter()));
        remoteConnectionDefinitionServiceClient = new RemoteConnectionDefinitionServiceClient(
            fixedGrpcChannels, discoveryClient, jsonMapper);
        remoteTriggerDefinitionServiceClient = new RemoteTriggerDefinitionServiceClient(
            fixedGrpcChannels, discoveryClient, jsonMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testGetComponentDefinitionsAppliesCallerPlatformFilter() {
        when(componentDefinitionService.getComponentDefinitions(true, null, null, true, null, null))
            .thenReturn(List.of(componentDefinition("keep", 1), componentDefinition("drop", 1)));

        List<ComponentDefinition> componentDefinitions = remoteComponentDefinitionServiceClient.getComponentDefinitions(
            true, null, null, true, null, PlatformType.AUTOMATION);

        assertThat(componentDefinitions).extracting(ComponentDefinition::getName)
            .containsExactly("keep");
    }

    @Test
    void testGetComponentDefinitionsWithIncludeKeepsIncludeOrder() {
        when(componentDefinitionService.getComponentDefinitions(
            isNull(), isNull(), isNull(), isNull(), org.mockito.ArgumentMatchers.eq(List.of("drop", "keep")),
            isNull()))
                .thenReturn(List.of(componentDefinition("drop", 1), componentDefinition("keep", 1)));

        List<ComponentDefinition> componentDefinitions = remoteComponentDefinitionServiceClient.getComponentDefinitions(
            null, null, null, null, List.of("drop", "keep"), null);

        assertThat(componentDefinitions).extracting(ComponentDefinition::getName)
            .containsExactly("drop", "keep");
    }

    @Test
    void testDefinitionsFromTwoWorkersAreMergedOncePerNameAndVersion() {
        when(componentDefinitionService.getComponentDefinitions())
            .thenReturn(List.of(componentDefinition("keep", 1), componentDefinition("keep", 2)));

        assertThat(remoteComponentDefinitionServiceClient.getComponentDefinitions())
            .extracting(ComponentDefinition::getName, ComponentDefinition::getVersion)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("keep", 1), org.assertj.core.groups.Tuple.tuple("keep", 2));
    }

    @Test
    void testComponentOnNoWorkerFailsNamingTheComponent() {
        assertThatThrownBy(() -> remoteComponentDefinitionServiceClient.getComponentDefinition("missing", 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("missing");
    }

    @Test
    void testGetComponentDefinitionWithoutVersionAsksForLatest() {
        ComponentDefinition componentDefinition = componentDefinition("keep", 3);

        when(componentDefinitionService.getComponentDefinition("keep", null)).thenReturn(componentDefinition);

        assertThat(remoteComponentDefinitionServiceClient.getComponentDefinition("keep", null))
            .usingRecursiveComparison()
            .isEqualTo(componentDefinition);
    }

    @Test
    void testSearchComponentDefinitionsAppliesCallerPlatformFilter() {
        when(componentDefinitionService.getComponentDefinitions("ke", null))
            .thenReturn(List.of(componentDefinition("keep", 1), componentDefinition("drop", 1)));

        assertThat(remoteComponentDefinitionServiceClient.getComponentDefinitions("ke", PlatformType.AUTOMATION))
            .extracting(ComponentDefinition::getName)
            .containsExactly("keep");
    }

    @Test
    void testFetchComponentDefinition() {
        ComponentDefinition componentDefinition = componentDefinition("keep", 2);

        when(componentDefinitionService.fetchComponentDefinition("keep", 2))
            .thenReturn(Optional.of(componentDefinition));
        when(componentDefinitionService.fetchComponentDefinition("drop", null)).thenReturn(Optional.empty());

        assertThat(remoteComponentDefinitionServiceClient.fetchComponentDefinition("keep", 2))
            .get()
            .usingRecursiveComparison()
            .isEqualTo(componentDefinition);
        assertThat(remoteComponentDefinitionServiceClient.fetchComponentDefinition("drop", null)).isEmpty();
        assertThat(remoteComponentDefinitionServiceClient.fetchComponentDefinition("missing", null)).isEmpty();
    }

    @Test
    void testGetConnectionComponentDefinition() {
        ComponentDefinition componentDefinition = componentDefinition("keep", 1);

        when(componentDefinitionService.getConnectionComponentDefinition("keep", 1)).thenReturn(componentDefinition);

        assertThat(remoteComponentDefinitionServiceClient.getConnectionComponentDefinition("keep", 1))
            .usingRecursiveComparison()
            .isEqualTo(componentDefinition);
    }

    @Test
    void testGetComponentDefinitionVersions() {
        when(componentDefinitionService.getComponentDefinitionVersions("keep"))
            .thenReturn(List.of(componentDefinition("keep", 1), componentDefinition("keep", 2)));

        assertThat(remoteComponentDefinitionServiceClient.getComponentDefinitionVersions("keep"))
            .extracting(ComponentDefinition::getVersion)
            .containsExactly(1, 2);
    }

    @Test
    void testGetActionDefinitions() {
        ComponentDefinition componentDefinition = componentDefinition("keep", 1);
        ActionDefinition actionDefinition = componentDefinition.getActions()
            .getFirst();

        when(actionDefinitionService.getActionDefinition("keep", 1, "get")).thenReturn(actionDefinition);
        when(actionDefinitionService.getActionDefinitions("keep", 1)).thenReturn(componentDefinition.getActions());

        assertThat(remoteActionDefinitionServiceClient.getActionDefinition("keep", 1, "get"))
            .usingRecursiveComparison()
            .isEqualTo(actionDefinition);
        assertThat(remoteActionDefinitionServiceClient.getActionDefinitions("keep", 1))
            .usingRecursiveComparison()
            .isEqualTo(componentDefinition.getActions());
    }

    @Test
    void testActionDefinitionQueries() {
        Map<String, Object> inputParameters = Map.of("count", 5);

        when(actionDefinitionService.isDynamicOutputDefined("keep", 1, "get")).thenReturn(true);
        when(actionDefinitionService.executeWorkflowNodeDescription("keep", 1, "get", inputParameters))
            .thenReturn("Get 5");

        assertThat(remoteActionDefinitionServiceClient.isDynamicOutputDefined("keep", 1, "get")).isTrue();
        assertThat(
            remoteActionDefinitionServiceClient.executeWorkflowNodeDescription("keep", 1, "get", inputParameters))
                .isEqualTo("Get 5");
    }

    @Test
    void testTriggerIsDynamicOutputDefined() {
        when(triggerDefinitionService.isDynamicOutputDefined("keep", 1, "new")).thenReturn(true);

        assertThat(remoteTriggerDefinitionServiceClient.isDynamicOutputDefined("keep", 1, "new")).isTrue();
    }

    @Test
    void testTriggerDefinitionCalls() {
        ComponentDefinition componentDefinition = componentDefinition("keep", 1);
        TriggerDefinition triggerDefinition = componentDefinition.getTriggers()
            .getFirst();
        WebhookTriggerFlags webhookTriggerFlags = new WebhookTriggerFlags(true, false, true, false);
        Map<String, Object> inputParameters = Map.of("count", 5, "name", "x");

        when(triggerDefinitionService.getTriggerDefinition("keep", 1, "new")).thenReturn(triggerDefinition);
        when(triggerDefinitionService.getTriggerDefinitions("keep", 1)).thenReturn(componentDefinition.getTriggers());
        when(triggerDefinitionService.getWebhookTriggerFlags("keep", 1, "new")).thenReturn(webhookTriggerFlags);
        when(triggerDefinitionService.executeWorkflowNodeDescription("keep", 1, "new", inputParameters))
            .thenReturn("New record");

        assertThat(remoteTriggerDefinitionServiceClient.getTriggerDefinition("keep", 1, "new"))
            .usingRecursiveComparison()
            .isEqualTo(triggerDefinition);
        assertThat(remoteTriggerDefinitionServiceClient.getTriggerDefinitions("keep", 1))
            .usingRecursiveComparison()
            .isEqualTo(componentDefinition.getTriggers());
        assertThat(remoteTriggerDefinitionServiceClient.getWebhookTriggerFlags("keep", 1, "new"))
            .isEqualTo(webhookTriggerFlags);
        assertThat(
            remoteTriggerDefinitionServiceClient.executeWorkflowNodeDescription("keep", 1, "new", inputParameters))
                .isEqualTo("New record");
    }

    @Test
    void testConnectionDefinitionCalls() {
        ConnectionDefinition connectionDefinition = componentDefinition("keep", 1).getConnection();

        when(connectionDefinitionService.getAuthorizationType("keep", 1, AuthorizationType.API_KEY))
            .thenReturn(AuthorizationType.API_KEY);
        when(connectionDefinitionService.getConnectionDefinition("keep", null)).thenReturn(connectionDefinition);
        when(connectionDefinitionService.getConnectionDefinitions("keep", 1)).thenReturn(List.of(connectionDefinition));
        when(connectionDefinitionService.getConnectionDefinitions()).thenReturn(List.of(connectionDefinition));

        assertThat(remoteConnectionDefinitionServiceClient.getAuthorizationType("keep", 1, AuthorizationType.API_KEY))
            .isEqualTo(AuthorizationType.API_KEY);
        assertThat(remoteConnectionDefinitionServiceClient.getConnectionDefinition("keep", null))
            .usingRecursiveComparison()
            .isEqualTo(connectionDefinition);
        assertThat(remoteConnectionDefinitionServiceClient.getConnectionDefinitions("keep", 1))
            .usingRecursiveComparison()
            .isEqualTo(List.of(connectionDefinition));
        assertThat(remoteConnectionDefinitionServiceClient.getConnectionDefinitions())
            .usingRecursiveComparison()
            .isEqualTo(List.of(connectionDefinition));

        verify(connectionDefinitionService).getConnectionDefinition("keep", null);
        verify(componentDefinitionService, org.mockito.Mockito.never())
            .getComponentDefinitions(anyBoolean(), isNull(), isNull(), isNull(), isNull(), isNull());
    }

    private static ComponentDefinition componentDefinition(String name, int version) {
        return new ComponentDefinition(
            component(name)
                .title(name)
                .description(name + " component")
                .version(version)
                .connection(connection().authorizations(authorization(AuthorizationType.API_KEY)))
                .actions(
                    action("get")
                        .title("Get")
                        .description("Gets a record"))
                .triggers(
                    trigger("new")
                        .title("New")
                        .description("New record")
                        .type(TriggerType.POLLING)));
    }

    private static class DropForAutomationComponentDefinitionFilter implements ComponentDefinitionFilter {

        @Override
        public boolean filter(ComponentDefinition componentDefinition) {
            return !"drop".equals(componentDefinition.getName());
        }

        @Override
        public boolean supports(PlatformType type) {
            return PlatformType.AUTOMATION.equals(type);
        }
    }
}
