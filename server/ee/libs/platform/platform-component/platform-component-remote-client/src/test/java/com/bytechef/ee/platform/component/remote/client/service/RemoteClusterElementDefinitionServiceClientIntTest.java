/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.COMPONENT_NAME;
import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.LOOKUP_TOOL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.component.remote.client.config.ComponentRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorker;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorkerFactory;
import com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ComponentRemoteClientIntTestConfiguration.class, properties = "spring.quartz.auto-startup=false")
class RemoteClusterElementDefinitionServiceClientIntTest {

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private ComponentWorkerFactory componentWorkerFactory;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<ComponentWorker> componentWorkers = new ArrayList<>();
    private ClusterElementDefinitionService clusterElementDefinitionService;
    private ComponentWorker componentWorker;
    private RemoteClusterElementDefinitionServiceClient remoteClusterElementDefinitionServiceClient;

    @BeforeEach
    void beforeEach() throws IOException {
        componentWorker = componentWorkerFactory.start("worker-1");

        componentWorkers.add(componentWorker);

        clusterElementDefinitionService = componentWorker.getComponentWorkerServices()
            .clusterElementDefinitionService();
        remoteClusterElementDefinitionServiceClient = new RemoteClusterElementDefinitionServiceClient(
            ComponentWorker.grpcChannels(componentWorker), ComponentWorker.discoveryClient(componentWorker),
            objectMapper);
    }

    @AfterEach
    void afterEach() {
        componentWorkers.forEach(ComponentWorker::close);
    }

    @Test
    void testGetClusterElementDefinitionReturnsWhatTheWorkerServes() {
        ClusterElementDefinition expectedClusterElementDefinition =
            clusterElementDefinitionService.getClusterElementDefinition(COMPONENT_NAME, 1, LOOKUP_TOOL);

        assertThat(remoteClusterElementDefinitionServiceClient.getClusterElementDefinition(COMPONENT_NAME, LOOKUP_TOOL))
            .usingRecursiveComparison()
            .isEqualTo(expectedClusterElementDefinition);
        assertThat(
            remoteClusterElementDefinitionServiceClient.getClusterElementDefinition(COMPONENT_NAME, 1, LOOKUP_TOOL))
                .usingRecursiveComparison()
                .isEqualTo(expectedClusterElementDefinition);
        assertThat(
            remoteClusterElementDefinitionServiceClient.getClusterElementDefinition(
                COMPONENT_NAME, 1, LOOKUP_TOOL, "TOOLS"))
                    .usingRecursiveComparison()
                    .isEqualTo(expectedClusterElementDefinition);
    }

    @Test
    void testGetUnknownClusterElementDefinitionFailsWithItsMessage() {
        assertThatThrownBy(
            () -> remoteClusterElementDefinitionServiceClient.getClusterElementDefinition(
                COMPONENT_NAME, 1, LOOKUP_TOOL, "MODEL"))
                    .isInstanceOf(StatusRuntimeException.class)
                    .hasMessageContaining(
                        "Cluster element definition lookup with type MODEL not found in component grpcTest");
    }

    @Test
    void testGetComponentClusterElementDefinitionsOfAType() {
        assertThat(
            remoteClusterElementDefinitionServiceClient.getClusterElementDefinitions(
                COMPONENT_NAME, 1, BaseToolFunction.TOOLS))
                    .extracting(ClusterElementDefinition::getName)
                    .containsExactly(LOOKUP_TOOL);
    }

    @Test
    void testGetClusterElementDefinitionsOfATypeAreMergedAcrossWorkers() throws IOException {
        ComponentWorker secondComponentWorker = componentWorkerFactory.start(
            "worker-2",
            ComponentRemoteClientIntTestConfiguration.componentDefinitionRegistry(
                applicationProperties,
                List.of(
                    new GrpcTestComponentHandler(1, "gRPC Test"), new GrpcTestComponentHandler(2, "gRPC Test v2"))));

        componentWorkers.add(secondComponentWorker);

        RemoteClusterElementDefinitionServiceClient mergingRemoteClusterElementDefinitionServiceClient =
            new RemoteClusterElementDefinitionServiceClient(
                ComponentWorker.grpcChannels(componentWorker, secondComponentWorker),
                ComponentWorker.discoveryClient(componentWorker, secondComponentWorker), objectMapper);

        List<Tuple> expectedTuples = new ArrayList<>();

        for (ClusterElementDefinition clusterElementDefinition : clusterElementDefinitionService
            .getClusterElementDefinitions(BaseToolFunction.TOOLS)) {

            expectedTuples.add(
                tuple(
                    clusterElementDefinition.getComponentName(), clusterElementDefinition.getComponentVersion(),
                    clusterElementDefinition.getName()));
        }

        expectedTuples.add(tuple(COMPONENT_NAME, 2, LOOKUP_TOOL));

        assertThat(
            mergingRemoteClusterElementDefinitionServiceClient.getClusterElementDefinitions(BaseToolFunction.TOOLS))
                .extracting(
                    ClusterElementDefinition::getComponentName, ClusterElementDefinition::getComponentVersion,
                    ClusterElementDefinition::getName)
                .contains(tuple(COMPONENT_NAME, 1, LOOKUP_TOOL), tuple(COMPONENT_NAME, 2, LOOKUP_TOOL))
                .containsExactlyInAnyOrderElementsOf(expectedTuples);
    }

    @Test
    void testGetClusterElementTypeAndRootClusterElementDefinitions() {
        ClusterElementType clusterElementType = remoteClusterElementDefinitionServiceClient.getClusterElementType(
            COMPONENT_NAME, 1, "TOOLS");

        assertThat(clusterElementType)
            .usingRecursiveComparison()
            .isEqualTo(BaseToolFunction.TOOLS);
        assertThat(
            remoteClusterElementDefinitionServiceClient.getRootClusterElementDefinitions(COMPONENT_NAME, 1, "TOOLS"))
                .extracting(ClusterElementDefinition::getComponentName, ClusterElementDefinition::getName)
                .contains(tuple(COMPONENT_NAME, LOOKUP_TOOL))
                .containsExactlyInAnyOrderElementsOf(
                    clusterElementDefinitionService.getRootClusterElementDefinitions(COMPONENT_NAME, 1, "TOOLS")
                        .stream()
                        .map(clusterElementDefinition -> tuple(
                            clusterElementDefinition.getComponentName(), clusterElementDefinition.getName()))
                        .toList());
    }

    @Test
    void testExecuteWorkflowNodeDescriptionRunsTheComponentsFunction() {
        assertThat(
            remoteClusterElementDefinitionServiceClient.executeWorkflowNodeDescription(
                COMPONENT_NAME, 1, LOOKUP_TOOL, Map.of("query", "customers")))
                    .isEqualTo("Look up customers");
    }
}
