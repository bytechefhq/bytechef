/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.component.remote.grpc.ActionDefinitionFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.component.remote.grpc.ClusterElementDefinitionFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.component.remote.grpc.ComponentDefinitionFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.component.remote.grpc.ComponentProtoMapper;
import com.bytechef.ee.platform.component.remote.grpc.TriggerDefinitionFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.interceptor.DeadlineClientInterceptor;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
class ComponentFacadeRemoteServiceGrpcWireTest {

    private static final Map<String, Object> INPUT_PARAMETERS = Map.of(
        "count", 5, "nested", Map.of("flag", true), "rows", List.of(Map.of("id", 1)));
    private static final List<String> LOOKUP_DEPENDS_ON_PATHS = List.of("count");

    private final ActionDefinitionFacade actionDefinitionFacade = mock(ActionDefinitionFacade.class);
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade =
        mock(ClusterElementDefinitionFacade.class);
    private final ComponentDefinitionFacade componentDefinitionFacade = mock(ComponentDefinitionFacade.class);
    private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
    private final JsonMapper jsonMapper = JsonMapper.builder()
        .build();
    private final TriggerDefinitionFacade triggerDefinitionFacade = mock(TriggerDefinitionFacade.class);

    private ManagedChannel managedChannel;
    private RemoteActionDefinitionFacadeClient remoteActionDefinitionFacadeClient;
    private RemoteClusterElementDefinitionFacadeClient remoteClusterElementDefinitionFacadeClient;
    private RemoteComponentDefinitionFacadeClient remoteComponentDefinitionFacadeClient;
    private RemoteTriggerDefinitionFacadeClient remoteTriggerDefinitionFacadeClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();
        ComponentProtoMapper componentProtoMapper = new ComponentProtoMapper(jsonMapper);

        server = InProcessServerBuilder.forName(name)
            .addService(new ActionDefinitionFacadeRemoteServiceGrpcServer(actionDefinitionFacade, componentProtoMapper))
            .addService(
                new ClusterElementDefinitionFacadeRemoteServiceGrpcServer(
                    clusterElementDefinitionFacade, componentProtoMapper))
            .addService(
                new ComponentDefinitionFacadeRemoteServiceGrpcServer(componentDefinitionFacade, componentProtoMapper))
            .addService(
                new TriggerDefinitionFacadeRemoteServiceGrpcServer(triggerDefinitionFacade, componentProtoMapper))
            .build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        when(discoveryClient.getInstances("worker-app")).thenReturn(
            List.of(
                new DefaultServiceInstance(
                    "worker-app:1", "worker-app", "worker-1", 8080, false,
                    Map.of("components", "[{\"name\":\"keep\"}]", "grpc-port", "9090"))));

        FixedGrpcChannels fixedGrpcChannels = new FixedGrpcChannels(managedChannel);

        remoteActionDefinitionFacadeClient = new RemoteActionDefinitionFacadeClient(
            applicationProperties(), fixedGrpcChannels, discoveryClient, jsonMapper);
        remoteClusterElementDefinitionFacadeClient = new RemoteClusterElementDefinitionFacadeClient(
            applicationProperties(), fixedGrpcChannels, discoveryClient, jsonMapper);
        remoteComponentDefinitionFacadeClient = new RemoteComponentDefinitionFacadeClient(
            applicationProperties(), fixedGrpcChannels, discoveryClient, jsonMapper);
        remoteTriggerDefinitionFacadeClient = new RemoteTriggerDefinitionFacadeClient(
            applicationProperties(), fixedGrpcChannels, discoveryClient, jsonMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testActionExecuteDynamicProperties() {
        List<Property> properties = properties();

        when(actionDefinitionFacade.executeDynamicProperties(
            "keep", 1, "get", "fields", INPUT_PARAMETERS, LOOKUP_DEPENDS_ON_PATHS, "workflow-1", null))
                .thenReturn(properties);

        assertThat(remoteActionDefinitionFacadeClient.executeDynamicProperties(
            "keep", 1, "get", "fields", INPUT_PARAMETERS, LOOKUP_DEPENDS_ON_PATHS, "workflow-1", null))
                .usingRecursiveComparison()
                .isEqualTo(properties);
    }

    @Test
    void testActionExecuteOptionsWithOneConnection() {
        List<Option> options = options();

        when(actionDefinitionFacade.executeOptions(
            "keep", 1, "get", "status", INPUT_PARAMETERS, LOOKUP_DEPENDS_ON_PATHS, "op", 7L))
                .thenReturn(options);

        assertThat(remoteActionDefinitionFacadeClient.executeOptions(
            "keep", 1, "get", "status", INPUT_PARAMETERS, LOOKUP_DEPENDS_ON_PATHS, "op", 7L))
                .usingRecursiveComparison()
                .isEqualTo(options);
    }

    @Test
    void testActionExecuteOptionsWithConnections() {
        List<Option> options = options();
        Map<String, Long> connectionIds = Map.of("keep", 7L);
        Map<String, Object> extensions = Map.of("tool", "x");

        when(actionDefinitionFacade.executeOptions(
            "keep", 1, "get", "status", INPUT_PARAMETERS, LOOKUP_DEPENDS_ON_PATHS, null, connectionIds, extensions))
                .thenReturn(options);

        assertThat(remoteActionDefinitionFacadeClient.executeOptions(
            "keep", 1, "get", "status", INPUT_PARAMETERS, LOOKUP_DEPENDS_ON_PATHS, null, connectionIds, extensions))
                .usingRecursiveComparison()
                .isEqualTo(options);
    }

    @Test
    void testActionExecuteOutput() {
        OutputResponse outputResponse = outputResponse();
        Map<String, Long> connectionIds = Map.of("keep", 7L);

        when(actionDefinitionFacade.executeOutput("keep", 1, "get", INPUT_PARAMETERS, connectionIds))
            .thenReturn(outputResponse);

        assertThat(remoteActionDefinitionFacadeClient.executeOutput("keep", 1, "get", INPUT_PARAMETERS, connectionIds))
            .usingRecursiveComparison()
            .isEqualTo(outputResponse);
    }

    @Test
    void testExecutePerformReturnsNestedResult() {
        Instant suspendExpiresAt = Instant.parse("2026-10-26T10:15:30Z");
        Map<String, Object> result = Map.of("rows", List.of(Map.of("id", 1)));

        when(actionDefinitionFacade.executePerform(
            "keep", 1, "get", 11L, 12L, 13L, 14L, "workflow-1", INPUT_PARAMETERS, Map.of("keep", 7L),
            Map.of("tool", "x"), 2L, PlatformType.AUTOMATION, true, Map.of("page", 2), null, suspendExpiresAt))
                .thenReturn(result);

        assertThat(remoteActionDefinitionFacadeClient.executePerform(
            "keep", 1, "get", 11L, 12L, 13L, 14L, "workflow-1", INPUT_PARAMETERS, Map.of("keep", 7L),
            Map.of("tool", "x"), 2L, PlatformType.AUTOMATION, true, Map.of("page", 2), null, suspendExpiresAt))
                .isEqualTo(result);
    }

    @Test
    void testExecutePerformIsNotCutOffByTheLookupDeadline() {
        RemoteActionDefinitionFacadeClient deadlineRemoteActionDefinitionFacadeClient =
            new RemoteActionDefinitionFacadeClient(
                applicationProperties(),
                new FixedGrpcChannels(
                    ClientInterceptors.intercept(
                        managedChannel, new DeadlineClientInterceptor(Duration.ofMillis(100)))),
                discoveryClient, jsonMapper);

        when(actionDefinitionFacade.executePerform(
            "keep", 1, "slow", null, null, null, null, null, Map.of(), Map.of(), Map.of(), null, null, false, null,
            null, null))
                .thenAnswer(invocation -> {
                    Thread.sleep(500);

                    return "done";
                });

        assertThat(deadlineRemoteActionDefinitionFacadeClient.executePerform(
            "keep", 1, "slow", null, null, null, null, null, Map.of(), Map.of(), Map.of(), null, null, false, null,
            null, null))
                .isEqualTo("done");
    }

    @Test
    void testExecutePerformWithNullOptionalArguments() {
        when(actionDefinitionFacade.executePerform(
            "keep", 1, "get", null, null, null, null, null, INPUT_PARAMETERS, Map.of(), Map.of(), null, null, false,
            null, null, null))
                .thenReturn("done");

        assertThat(remoteActionDefinitionFacadeClient.executePerform(
            "keep", 1, "get", null, null, null, null, null, INPUT_PARAMETERS, Map.of(), Map.of(), null, null, false,
            null, null, null))
                .isEqualTo("done");
    }

    @Test
    void testClusterElementExecuteDynamicProperties() {
        List<Property> properties = properties();

        when(clusterElementDefinitionFacade.executeDynamicProperties(
            "keep", 1, "search", "fields", INPUT_PARAMETERS, LOOKUP_DEPENDS_ON_PATHS, 7L))
                .thenReturn(properties);

        assertThat(remoteClusterElementDefinitionFacadeClient.executeDynamicProperties(
            "keep", 1, "search", "fields", INPUT_PARAMETERS, LOOKUP_DEPENDS_ON_PATHS, 7L))
                .usingRecursiveComparison()
                .isEqualTo(properties);
    }

    @Test
    void testClusterElementExecuteDynamicPropertiesWithClusterElements() {
        List<Property> properties = properties();
        Map<String, Object> extensions = Map.of("parent", "agent");
        Map<String, Long> clusterElementConnectionIds = Map.of("model", 8L);
        Map<String, Map<String, ?>> clusterElementInputParameters = Map.of("model", Map.of("temperature", 1));

        when(clusterElementDefinitionFacade.executeDynamicProperties(
            "keep", 1, "search", "fields", INPUT_PARAMETERS, extensions, LOOKUP_DEPENDS_ON_PATHS, null,
            clusterElementConnectionIds, clusterElementInputParameters))
                .thenReturn(properties);

        assertThat(remoteClusterElementDefinitionFacadeClient.executeDynamicProperties(
            "keep", 1, "search", "fields", INPUT_PARAMETERS, extensions, LOOKUP_DEPENDS_ON_PATHS, null,
            clusterElementConnectionIds, clusterElementInputParameters))
                .usingRecursiveComparison()
                .isEqualTo(properties);
    }

    @Test
    void testClusterElementExecuteOptions() {
        List<Option> options = options();
        Map<String, Object> extensions = Map.of("parent", "agent");
        Map<String, Long> clusterElementConnectionIds = Map.of("model", 8L);
        Map<String, Map<String, ?>> clusterElementInputParameters = Map.of("model", Map.of("temperature", 1));

        when(clusterElementDefinitionFacade.executeOptions(
            "keep", 1, "search", "status", INPUT_PARAMETERS, extensions, LOOKUP_DEPENDS_ON_PATHS, "op", 7L,
            clusterElementConnectionIds, clusterElementInputParameters))
                .thenReturn(options);

        assertThat(remoteClusterElementDefinitionFacadeClient.executeOptions(
            "keep", 1, "search", "status", INPUT_PARAMETERS, extensions, LOOKUP_DEPENDS_ON_PATHS, "op", 7L,
            clusterElementConnectionIds, clusterElementInputParameters))
                .usingRecursiveComparison()
                .isEqualTo(options);
    }

    @Test
    void testClusterElementExecuteOutput() {
        OutputResponse outputResponse = outputResponse();

        when(clusterElementDefinitionFacade.executeOutput("keep", 1, "search", INPUT_PARAMETERS, 7L))
            .thenReturn(outputResponse);

        assertThat(remoteClusterElementDefinitionFacadeClient.executeOutput("keep", 1, "search", INPUT_PARAMETERS, 7L))
            .usingRecursiveComparison()
            .isEqualTo(outputResponse);
    }

    @Test
    void testClusterElementExecuteTool() {
        Map<String, Object> result = Map.of("rows", List.of(Map.of("id", 1)));

        when(clusterElementDefinitionFacade.executeTool("keep", 1, "search", INPUT_PARAMETERS, 7L, false))
            .thenReturn(result);
        when(clusterElementDefinitionFacade.executeTool("keep", 1, "search", INPUT_PARAMETERS, 7L, true))
            .thenReturn("editor");
        when(clusterElementDefinitionFacade.executeTool("keep", "search", INPUT_PARAMETERS, null))
            .thenReturn("latest");

        assertThat(remoteClusterElementDefinitionFacadeClient.executeTool("keep", 1, "search", INPUT_PARAMETERS, 7L))
            .isEqualTo(result);
        assertThat(remoteClusterElementDefinitionFacadeClient.executeTool("keep", "search", INPUT_PARAMETERS, null))
            .isEqualTo("latest");
        assertThat(
            remoteClusterElementDefinitionFacadeClient.executeTool("keep", 1, "search", INPUT_PARAMETERS, 7L, true))
                .isEqualTo("editor");
    }

    @Test
    void testTriggerExecuteDynamicProperties() {
        List<Property> properties = properties();

        when(triggerDefinitionFacade.executeDynamicProperties(
            "keep", 1, "new", "fields", INPUT_PARAMETERS, LOOKUP_DEPENDS_ON_PATHS, 7L))
                .thenReturn(properties);

        assertThat(remoteTriggerDefinitionFacadeClient.executeDynamicProperties(
            "keep", 1, "new", "fields", INPUT_PARAMETERS, LOOKUP_DEPENDS_ON_PATHS, 7L))
                .usingRecursiveComparison()
                .isEqualTo(properties);
    }

    @Test
    void testTriggerExecuteDynamicWebhookRefresh() {
        WebhookEnableOutput webhookEnableOutput = webhookEnableOutput();

        when(triggerDefinitionFacade.executeDynamicWebhookRefresh("keep", 1, "new", Map.of("id", "hook-1"), null))
            .thenReturn(webhookEnableOutput);

        assertThat(
            remoteTriggerDefinitionFacadeClient.executeDynamicWebhookRefresh(
                "keep", 1, "new", Map.of("id", "hook-1"), null))
                    .isEqualTo(webhookEnableOutput);
    }

    @Test
    void testTriggerExecuteListenerDisableAndEnable() {
        remoteTriggerDefinitionFacadeClient.executeListenerDisable(
            "keep", 1, "new", INPUT_PARAMETERS, "execution-1", 7L);
        remoteTriggerDefinitionFacadeClient.executeListenerEnable(
            "keep", 1, "new", INPUT_PARAMETERS, "execution-1", null);

        verify(triggerDefinitionFacade).executeListenerDisable(
            "keep", 1, "new", INPUT_PARAMETERS, "execution-1", 7L);
        verify(triggerDefinitionFacade).executeListenerEnable(
            "keep", 1, "new", INPUT_PARAMETERS, "execution-1", null);
    }

    @Test
    void testTriggerExecuteOptions() {
        List<Option> options = options();

        when(triggerDefinitionFacade.executeOptions(
            "keep", 1, "new", "status", INPUT_PARAMETERS, LOOKUP_DEPENDS_ON_PATHS, "op", null))
                .thenReturn(options);

        assertThat(remoteTriggerDefinitionFacadeClient.executeOptions(
            "keep", 1, "new", "status", INPUT_PARAMETERS, LOOKUP_DEPENDS_ON_PATHS, "op", null))
                .usingRecursiveComparison()
                .isEqualTo(options);
    }

    @Test
    void testTriggerExecuteOutput() {
        OutputResponse outputResponse = outputResponse();

        when(triggerDefinitionFacade.executeOutput("keep", 1, "new", INPUT_PARAMETERS, 7L))
            .thenReturn(outputResponse);

        assertThat(remoteTriggerDefinitionFacadeClient.executeOutput("keep", 1, "new", INPUT_PARAMETERS, 7L))
            .usingRecursiveComparison()
            .isEqualTo(outputResponse);
    }

    @Test
    void testTriggerExecuteTrigger() {
        TriggerOutput triggerOutput = new TriggerOutput(List.of(Map.of("id", 1)), Map.of("cursor", "b"), true);
        WebhookRequest webhookRequest = webhookRequest();

        when(triggerDefinitionFacade.executeTrigger(
            eq("keep"), eq(1), eq("new"), eq(11L), eq("workflow-uuid"), eq(15L), eq(INPUT_PARAMETERS),
            eq(Map.of("cursor", "a")),
            sameJson(webhookRequest), eq(7L), eq(2L), eq(PlatformType.EMBEDDED), eq(false)))
                .thenReturn(triggerOutput);

        TriggerOutput remoteTriggerOutput = remoteTriggerDefinitionFacadeClient.executeTrigger(
            "keep", 1, "new", 11L, "workflow-uuid", 15L, INPUT_PARAMETERS, Map.of("cursor", "a"), webhookRequest, 7L,
            2L, PlatformType.EMBEDDED, false);

        assertThat(remoteTriggerOutput).isEqualTo(triggerOutput);
    }

    @Test
    void testTriggerExecuteWebhookDisable() {
        remoteTriggerDefinitionFacadeClient.executeWebhookDisable(
            "keep", 1, "new", INPUT_PARAMETERS, "execution-1", Map.of("id", "hook-1"), 7L);

        verify(triggerDefinitionFacade).executeWebhookDisable(
            "keep", 1, "new", INPUT_PARAMETERS, "execution-1", Map.of("id", "hook-1"), 7L);
    }

    @Test
    void testTriggerExecuteWebhookEnable() {
        WebhookEnableOutput webhookEnableOutput = webhookEnableOutput();

        when(triggerDefinitionFacade.executeWebhookEnable(
            "keep", 1, "new", INPUT_PARAMETERS, "execution-1", 7L, "https://example.com/webhooks/1", 2L))
                .thenReturn(webhookEnableOutput);

        assertThat(remoteTriggerDefinitionFacadeClient.executeWebhookEnable(
            "keep", 1, "new", INPUT_PARAMETERS, "execution-1", 7L, "https://example.com/webhooks/1", 2L))
                .isEqualTo(webhookEnableOutput);
    }

    @Test
    void testTriggerExecuteWebhookValidate() {
        WebhookRequest webhookRequest = webhookRequest();
        WebhookValidateResponse webhookValidateResponse = new WebhookValidateResponse(
            Map.of("ok", true), Map.of("X-Hook", List.of("a")), 202);

        when(triggerDefinitionFacade.executeWebhookValidate(
            eq("keep"), eq(1), eq("new"), eq(INPUT_PARAMETERS), sameJson(webhookRequest), eq(7L)))
                .thenReturn(webhookValidateResponse);

        assertThat(remoteTriggerDefinitionFacadeClient.executeWebhookValidate(
            "keep", 1, "new", INPUT_PARAMETERS, webhookRequest, 7L))
                .isEqualTo(webhookValidateResponse);
    }

    @Test
    void testTriggerExecuteWebhookValidateOnEnable() {
        WebhookRequest webhookRequest = webhookRequest();
        WebhookValidateResponse webhookValidateResponse = new WebhookValidateResponse(
            Map.of("challenge", "abc"), Map.of("X-Hook", List.of("b")), 200);

        when(triggerDefinitionFacade.executeWebhookValidateOnEnable(
            eq("keep"), eq(1), eq("new"), eq(INPUT_PARAMETERS), sameJson(webhookRequest), eq(7L)))
                .thenReturn(webhookValidateResponse);

        assertThat(remoteTriggerDefinitionFacadeClient.executeWebhookValidateOnEnable(
            "keep", 1, "new", INPUT_PARAMETERS, webhookRequest, 7L))
                .isEqualTo(webhookValidateResponse);
    }

    @Test
    void testTriggerExecuteWebhookValidateOnEnableWithoutConnection() {
        WebhookValidateResponse webhookValidateResponse = new WebhookValidateResponse(null, null, 200);

        when(triggerDefinitionFacade.executeWebhookValidateOnEnable(
            "keep", 1, "new", INPUT_PARAMETERS, null, null))
                .thenReturn(webhookValidateResponse);

        assertThat(remoteTriggerDefinitionFacadeClient.executeWebhookValidateOnEnable(
            "keep", 1, "new", INPUT_PARAMETERS, null, null))
                .isEqualTo(webhookValidateResponse);
    }

    @Test
    void testExecuteWorkflowInputOptions() {
        List<Option> options = options();

        when(componentDefinitionFacade.executeWorkflowInputOptions(
            "keep", 1, "inputs", "status", INPUT_PARAMETERS, LOOKUP_DEPENDS_ON_PATHS, "op", 7L))
                .thenReturn(options);

        assertThat(remoteComponentDefinitionFacadeClient.executeWorkflowInputOptions(
            "keep", 1, "inputs", "status", INPUT_PARAMETERS, LOOKUP_DEPENDS_ON_PATHS, "op", 7L))
                .usingRecursiveComparison()
                .isEqualTo(options);
    }

    @Test
    void testExecuteWorkflowInputOptionsWithoutConnectionOrSearchText() {
        List<Option> options = options();

        when(componentDefinitionFacade.executeWorkflowInputOptions(
            "keep", 1, "inputs", "status", INPUT_PARAMETERS, List.of(), null, null))
                .thenReturn(options);

        assertThat(remoteComponentDefinitionFacadeClient.executeWorkflowInputOptions(
            "keep", 1, "inputs", "status", INPUT_PARAMETERS, List.of(), null, null))
                .usingRecursiveComparison()
                .isEqualTo(options);
    }

    private WebhookRequest sameJson(WebhookRequest expected) {
        String expectedJson = jsonMapper.writeValueAsString(expected);

        return argThat(actual -> expectedJson.equals(jsonMapper.writeValueAsString(actual)));
    }

    private static ApplicationProperties applicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getRemote()
            .getGrpc()
            .setExecutionDeadline(Duration.ofSeconds(5));

        return applicationProperties;
    }

    private static List<Option> options() {
        return List.of(new Option(option("Open", "open")), new Option(option("Closed", "closed")));
    }

    private static OutputResponse outputResponse() {
        return new OutputResponse(properties().getFirst(), Map.of("name", "x", "count", 3), null);
    }

    private static List<Property> properties() {
        ComponentDefinition componentDefinition = new ComponentDefinition(
            component("keep")
                .title("Keep")
                .description("Keep component")
                .actions(
                    action("get")
                        .title("Get")
                        .description("Gets a record")
                        .properties(
                            string("name")
                                .label("Name")
                                .required(true))));

        return List.copyOf(componentDefinition.getActions()
            .getFirst()
            .getProperties());
    }

    private static WebhookEnableOutput webhookEnableOutput() {
        return new WebhookEnableOutput(Map.of("id", "hook-1"), Instant.parse("2026-10-26T10:15:30Z"));
    }

    private static WebhookRequest webhookRequest() {
        return new WebhookRequest(
            Map.of("Content-Type", List.of("application/json")), Map.of("q", List.of("1")),
            new WebhookRequest.WebhookBodyImpl(
                Map.of("name", "x"), ContentType.JSON, "application/json", "{\"name\":\"x\"}"),
            WebhookMethod.POST, true);
    }
}
