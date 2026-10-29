/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.repository.jdbc.JdbcWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.platform.configuration.remote.client.config.PlatformConfigurationRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.configuration.remote.grpc.PlatformConfigurationProtoMapper;
import com.bytechef.ee.platform.configuration.remote.grpc.WorkflowRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = PlatformConfigurationRemoteClientIntTestConfiguration.class,
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true", "spring.liquibase.contexts=configuration"
    })
class RemoteWorkflowServiceClientIntTest {

    private static final String DEFINITION = """
        {"label":"LABEL","tasks":[
            {"name":"randomHelper_1","type":"randomHelper/v1/randomFloat","parameters":{"from":1,"to":100}},
            {"name":"logger_1","type":"logger/v1/info","parameters":{"text":"${randomHelper_1}"}}]}
        """;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JdbcWorkflowRepository jdbcWorkflowRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkflowService workflowService;

    private ManagedChannel managedChannel;
    private RemoteWorkflowServiceClient remoteWorkflowServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new WorkflowRemoteServiceGrpcServer(
                        workflowService, new PlatformConfigurationProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteWorkflowServiceClient = new RemoteWorkflowServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        jdbcWorkflowRepository.deleteAll();

        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = Objects.requireNonNull(cacheManager.getCache(cacheName));

            cache.clear();
        }
    }

    @Test
    void testCreateWorkflowStoresTheDefinition() {
        String definition = definition("Live Workflow");

        Workflow createdWorkflow = remoteWorkflowServiceClient.create(
            definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        assertThat(createdWorkflow.getId()).isNotBlank();
        assertThat(createdWorkflow.getDefinition()).isEqualTo(definition);
        assertThat(createdWorkflow.getFormat()).isEqualTo(Workflow.Format.JSON);
        assertThat(createdWorkflow.getSourceType()).isEqualTo(Workflow.SourceType.JDBC);
        assertThat(createdWorkflow.getLabel()).isEqualTo("Live Workflow");

        WorkflowRepository workflowRepository = jdbcWorkflowRepository;

        Workflow storedWorkflow = workflowRepository.findById(createdWorkflow.getId())
            .orElseThrow();

        assertThat(storedWorkflow.getDefinition()).isEqualTo(definition);
        assertThat(createdWorkflow.getVersion()).isEqualTo(storedWorkflow.getVersion());
        assertThat(createdWorkflow.getLastModifiedDate()).isEqualTo(storedWorkflow.getLastModifiedDate());
    }

    @Test
    void testCreateWorkflowWithoutASourceTypeFailsWithItsMessage() {
        String definition = definition("Live Workflow");

        assertThatThrownBy(() -> remoteWorkflowServiceClient.create(definition, Workflow.Format.JSON, null))
            .hasMessageContaining("'sourceType' must not be null");

        assertThat(workflowService.getWorkflows()).isEmpty();
    }

    @Test
    void testFetchWorkflowReturnsTheStoredWorkflow() {
        Workflow workflow = createWorkflow("Live Workflow");

        assertThat(remoteWorkflowServiceClient.fetchWorkflow(workflow.getId()))
            .hasValueSatisfying(fetchedWorkflow -> {
                assertThat(fetchedWorkflow.getId()).isEqualTo(workflow.getId());
                assertThat(fetchedWorkflow.getDefinition()).isEqualTo(workflow.getDefinition());
                assertThat(fetchedWorkflow.getSourceType()).isEqualTo(Workflow.SourceType.JDBC);
            });
        assertThat(remoteWorkflowServiceClient.fetchWorkflow("ac7de6e1-08c3-4f6a-a8a4-5fd8f8e1a0b6")).isEmpty();
    }

    @Test
    void testGetWorkflowKeepsDefinitionAndTasks() {
        Workflow workflow = createWorkflow("Live Workflow");

        Workflow readWorkflow = remoteWorkflowServiceClient.getWorkflow(workflow.getId());

        assertThat(readWorkflow.getId()).isEqualTo(workflow.getId());
        assertThat(readWorkflow.getDefinition()).isEqualTo(workflow.getDefinition());
        assertThat(readWorkflow.getLabel()).isEqualTo("Live Workflow");
        assertThat(readWorkflow.getLastModifiedDate()).isEqualTo(workflow.getLastModifiedDate());
        assertThat(readWorkflow.getVersion()).isEqualTo(workflow.getVersion());
        assertThat(readWorkflow.getTasks()).hasSize(2)
            .extracting(WorkflowTask::toMap)
            .containsExactlyElementsOf(
                workflow.getTasks()
                    .stream()
                    .map(WorkflowTask::toMap)
                    .toList());
    }

    @Test
    void testGetUnknownWorkflowFailsWithItsMessage() {
        String workflowId = "ac7de6e1-08c3-4f6a-a8a4-5fd8f8e1a0b6";

        assertThatThrownBy(() -> remoteWorkflowServiceClient.getWorkflow(workflowId))
            .hasMessageContaining("Workflow with id: %s does not exist".formatted(workflowId));
    }

    @Test
    void testGetWorkflowsReturnsOnlyTheRequestedWorkflowsSortedByLabel() {
        Workflow betaWorkflow = createWorkflow("Beta");
        Workflow alphaWorkflow = createWorkflow("Alpha");

        createWorkflow("Gamma");

        List<String> workflowIds = List.of(betaWorkflow.getId(), alphaWorkflow.getId());

        assertThat(remoteWorkflowServiceClient.getWorkflows(workflowIds))
            .extracting(Workflow::getId, Workflow::getLabel)
            .containsExactly(
                tuple(alphaWorkflow.getId(), "Alpha"),
                tuple(betaWorkflow.getId(), "Beta"));
        assertThat(remoteWorkflowServiceClient.getWorkflows(List.of())).isEmpty();
    }

    private static String definition(String label) {
        return DEFINITION.replace("LABEL", label);
    }

    private Workflow createWorkflow(String label) {
        return workflowService.create(definition(label), Workflow.Format.JSON, Workflow.SourceType.JDBC);
    }
}
