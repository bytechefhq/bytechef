/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.task.dispatcher.registry.remote.client.service;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.integer;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.object;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.definition.BaseProperty;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.TaskDispatcherDefinitionRemoteServiceGrpcServer;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.TaskDispatcherProtoMapper;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionRegistry;
import com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty;
import com.bytechef.platform.workflow.task.dispatcher.domain.Option;
import com.bytechef.platform.workflow.task.dispatcher.domain.Property;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionServiceImpl;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.domain.SubflowEntry;
import com.bytechef.task.dispatcher.condition.ConditionTaskDispatcherDefinitionFactory;
import com.bytechef.task.dispatcher.loop.LoopTaskDispatcherDefinitionFactory;
import com.bytechef.task.dispatcher.subflow.SubflowTaskDispatcherDefinitionFactory;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = RemoteTaskDispatcherDefinitionServiceClientIntTest.Config.class)
class RemoteTaskDispatcherDefinitionServiceClientIntTest {

    private static final String ORDERS_WORKFLOW_UUID = "4c1d8e2a-7f1b-4a55-9a51-0b6a1b2c3d4e";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    private ManagedChannel managedChannel;
    private RemoteTaskDispatcherDefinitionServiceClient remoteTaskDispatcherDefinitionServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new TaskDispatcherDefinitionRemoteServiceGrpcServer(
                        taskDispatcherDefinitionService, new TaskDispatcherProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteTaskDispatcherDefinitionServiceClient = new RemoteTaskDispatcherDefinitionServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testGetTaskDispatcherDefinitionWithAndWithoutVersion() {
        TaskDispatcherDefinition latestTaskDispatcherDefinition =
            remoteTaskDispatcherDefinitionServiceClient.getTaskDispatcherDefinition("loop", null);
        TaskDispatcherDefinition versionedTaskDispatcherDefinition =
            remoteTaskDispatcherDefinitionServiceClient.getTaskDispatcherDefinition("loop", 1);

        assertThat(latestTaskDispatcherDefinition.getName()).isEqualTo("loop");
        assertThat(latestTaskDispatcherDefinition.getTitle()).isEqualTo("Loop");
        assertThat(latestTaskDispatcherDefinition.getVersion()).isEqualTo(1);
        assertThat(latestTaskDispatcherDefinition.isVariablePropertiesDefined()).isTrue();
        assertThat(latestTaskDispatcherDefinition.getProperties())
            .extracting(Property::getName)
            .containsExactly("items", "loopForever");
        assertThat(latestTaskDispatcherDefinition.getTaskProperties())
            .extracting(Property::getName)
            .containsExactly("iteratee");
        assertThat(latestTaskDispatcherDefinition)
            .usingRecursiveComparison()
            .isEqualTo(taskDispatcherDefinitionService.getTaskDispatcherDefinition("loop", null));
        assertThat(versionedTaskDispatcherDefinition)
            .usingRecursiveComparison()
            .isEqualTo(taskDispatcherDefinitionService.getTaskDispatcherDefinition("loop", 1));
    }

    @Test
    void testGetTaskDispatcherDefinitionForUnknownNameFails() {
        assertThatThrownBy(() -> remoteTaskDispatcherDefinitionServiceClient.getTaskDispatcherDefinition(
            "missing", null))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("Task dispatcher definition with name 'missing' and version 'null' not found")
                .extracting(exception -> ((StatusRuntimeException) exception).getStatus()
                    .getCode())
                .isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void testGetTaskDispatcherDefinitions() {
        List<TaskDispatcherDefinition> taskDispatcherDefinitions =
            remoteTaskDispatcherDefinitionServiceClient.getTaskDispatcherDefinitions();

        assertThat(taskDispatcherDefinitions)
            .extracting(TaskDispatcherDefinition::getName)
            .containsExactly("condition", "loop", "subflow");
        assertThat(taskDispatcherDefinitions)
            .usingRecursiveComparison()
            .isEqualTo(taskDispatcherDefinitionService.getTaskDispatcherDefinitions());
    }

    @Test
    void testGetTaskDispatcherDefinitionVersions() {
        List<TaskDispatcherDefinition> taskDispatcherDefinitions =
            remoteTaskDispatcherDefinitionServiceClient.getTaskDispatcherDefinitionVersions("condition");

        assertThat(taskDispatcherDefinitions)
            .extracting(TaskDispatcherDefinition::getName, TaskDispatcherDefinition::getVersion)
            .containsExactly(tuple("condition", 1));
        assertThat(remoteTaskDispatcherDefinitionServiceClient.getTaskDispatcherDefinitionVersions("missing"))
            .isEmpty();
    }

    @Test
    void testFetchTaskDispatcherDefinition() {
        Optional<TaskDispatcherDefinition> subflowTaskDispatcherDefinition =
            remoteTaskDispatcherDefinitionServiceClient.fetchTaskDispatcherDefinition("subflow", 1);

        assertThat(subflowTaskDispatcherDefinition)
            .get()
            .usingRecursiveComparison()
            .isEqualTo(taskDispatcherDefinitionService.getTaskDispatcherDefinition("subflow", 1));
        assertThat(subflowTaskDispatcherDefinition.get()
            .getProperties())
                .extracting(Property::getName)
                .containsExactly("workflowUuid", "inputs");
        assertThat(remoteTaskDispatcherDefinitionServiceClient.fetchTaskDispatcherDefinition("subflow", 2))
            .isEmpty();
        assertThat(remoteTaskDispatcherDefinitionServiceClient.fetchTaskDispatcherDefinition("missing", null))
            .isEmpty();
    }

    @Test
    void testExecuteVariableProperties() {
        Map<String, Object> inputParameters = Map.of("items", List.of(Map.of("sku", "A-1"), Map.of("sku", "B-2")));

        OutputResponse outputResponse = remoteTaskDispatcherDefinitionServiceClient.executeVariableProperties(
            "loop", 1, inputParameters);

        assertThat(outputResponse).isNotNull();
        assertThat(outputResponse.sampleOutput()).isEqualTo(Map.of("item", Map.of("sku", "A-1"), "index", 0));
        assertThat(outputResponse.outputSchema()).isInstanceOf(ObjectProperty.class);
        assertThat(((ObjectProperty) outputResponse.outputSchema()).getProperties())
            .extracting(Property::getName)
            .containsExactlyInAnyOrder("item", "index");
        assertThat(outputResponse)
            .usingRecursiveComparison()
            .isEqualTo(taskDispatcherDefinitionService.executeVariableProperties("loop", 1, inputParameters));
        assertThat(remoteTaskDispatcherDefinitionServiceClient.executeVariableProperties("condition", 1, Map.of()))
            .isNull();
    }

    @Test
    void testExecuteOutput() {
        OutputResponse outputResponse = remoteTaskDispatcherDefinitionServiceClient.executeOutput(
            "subflow", 1, Map.of("workflowUuid", ORDERS_WORKFLOW_UUID));

        assertThat(outputResponse).isNotNull();
        assertThat(outputResponse.outputSchema()).isInstanceOf(ObjectProperty.class);
        assertThat(((ObjectProperty) outputResponse.outputSchema()).getProperties())
            .extracting(Property::getName)
            .containsExactly("orderCount");
        assertThat(remoteTaskDispatcherDefinitionServiceClient.executeOutput("subflow", 1, Map.of()))
            .isNull();
        assertThat(remoteTaskDispatcherDefinitionServiceClient.executeOutput("condition", 1, Map.of()))
            .isNull();
    }

    @Test
    void testExecuteWorkflowNodeDescription() {
        Map<String, Object> inputParameters = Map.of("items", List.of(1, 2));

        String workflowNodeDescription = remoteTaskDispatcherDefinitionServiceClient.executeWorkflowNodeDescription(
            "loop", 1, inputParameters);

        assertThat(workflowNodeDescription)
            .contains("<div class=\"font-semibold\">Loop</div>")
            .isEqualTo(taskDispatcherDefinitionService.executeWorkflowNodeDescription("loop", 1, inputParameters));
    }

    @Test
    void testIsDynamicOutputDefined() {
        assertThat(remoteTaskDispatcherDefinitionServiceClient.isDynamicOutputDefined("loop", 1)).isTrue();
        assertThat(remoteTaskDispatcherDefinitionServiceClient.isDynamicOutputDefined("subflow", 1)).isTrue();
        assertThat(remoteTaskDispatcherDefinitionServiceClient.isDynamicOutputDefined("condition", 1)).isFalse();
    }

    @Test
    void testExecuteOptions() {
        List<Option> options = remoteTaskDispatcherDefinitionServiceClient.executeOptions(
            "subflow", 1, "workflowUuid", "ord");

        assertThat(options)
            .extracting(Option::getLabel, Option::getValue)
            .containsExactly(tuple("Process orders", ORDERS_WORKFLOW_UUID));
        assertThat(remoteTaskDispatcherDefinitionServiceClient.executeOptions("subflow", 1, "workflowUuid", null))
            .extracting(Option::getLabel)
            .containsExactly("Process orders", "Send invoices");
        assertThat(remoteTaskDispatcherDefinitionServiceClient.executeOptions("subflow", 1, "inputs", null))
            .isEmpty();
    }

    @Test
    void testExecuteDynamicProperties() {
        List<Property> properties = remoteTaskDispatcherDefinitionServiceClient.executeDynamicProperties(
            "subflow", 1, "inputs", Map.of("workflowUuid", ORDERS_WORKFLOW_UUID));

        assertThat(properties)
            .extracting(Property::getName)
            .containsExactly("customerId", "limit");
        assertThat(properties)
            .usingRecursiveComparison()
            .isEqualTo(
                taskDispatcherDefinitionService.executeDynamicProperties(
                    "subflow", 1, "inputs", Map.of("workflowUuid", ORDERS_WORKFLOW_UUID)));
        assertThat(remoteTaskDispatcherDefinitionServiceClient.executeDynamicProperties(
            "subflow", 1, "inputs", Map.of()))
                .isEmpty();
    }

    @EnableAutoConfiguration
    @Import({
        ConditionTaskDispatcherDefinitionFactory.class, JacksonConfiguration.class,
        LoopTaskDispatcherDefinitionFactory.class, SubflowTaskDispatcherDefinitionFactory.class,
        TaskDispatcherDefinitionRegistry.class, TaskDispatcherDefinitionServiceImpl.class
    })
    @SpringBootConfiguration
    static class Config {

        @Bean
        SubflowDataSource subflowDataSource() {
            return new InMemorySubflowDataSource();
        }
    }

    private static class InMemorySubflowDataSource implements SubflowDataSource {

        private static final List<SubflowEntry> SUBFLOW_ENTRIES = List.of(
            new SubflowEntry(ORDERS_WORKFLOW_UUID, "Process orders"),
            new SubflowEntry("9e8d7c6b-5a49-4382-a716-1f2e3d4c5b6a", "Send invoices"));

        @Override
        public BaseProperty.@Nullable BaseValueProperty<?> getSubWorkflowInputSchema(String workflowUuid) {
            if (!ORDERS_WORKFLOW_UUID.equals(workflowUuid)) {
                return null;
            }

            return object().properties(string("customerId"), integer("limit"));
        }

        @Override
        public BaseProperty.@Nullable BaseValueProperty<?> getSubWorkflowOutputSchema(String workflowUuid) {
            if (!ORDERS_WORKFLOW_UUID.equals(workflowUuid)) {
                return null;
            }

            return object().properties(integer("orderCount"));
        }

        @Override
        public List<SubflowEntry> getSubWorkflows(PlatformType platformType, String triggerName, String search) {
            return SUBFLOW_ENTRIES.stream()
                .filter(subflowEntry -> search == null || subflowEntry.name()
                    .toLowerCase()
                    .contains(search))
                .toList();
        }
    }
}
