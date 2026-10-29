/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.ee.automation.configuration.remote.client.config.AutomationConfigurationIntTestData;
import com.bytechef.ee.automation.configuration.remote.client.config.AutomationConfigurationRemoteClientIntTestConfiguration;
import com.bytechef.ee.automation.configuration.remote.grpc.AutomationConfigurationProtoMapper;
import com.bytechef.ee.automation.configuration.remote.grpc.ProjectDeploymentRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AutomationConfigurationRemoteClientIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "bytechef.workflow.repository.jdbc.enabled=true",
        "spring.liquibase.contexts=configuration,connection,execution,user"
    })
class RemoteProjectDeploymentServiceClientIntTest {

    @Autowired
    private AutomationConfigurationIntTestData automationConfigurationIntTestData;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    private ManagedChannel managedChannel;
    private RemoteProjectDeploymentServiceClient remoteProjectDeploymentServiceClient;
    private Server server;
    private Workspace workspace;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new ProjectDeploymentRemoteServiceGrpcServer(
                        projectDeploymentService, new AutomationConfigurationProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteProjectDeploymentServiceClient = new RemoteProjectDeploymentServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);

        workspace = automationConfigurationIntTestData.createWorkspace("workspace");
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        automationConfigurationIntTestData.deleteAll();
    }

    @Test
    void testGetProjectDeploymentReturnsTheStoredDeployment() {
        Project project = automationConfigurationIntTestData.createProject("Live Project", workspace.getId());
        Tag tag = automationConfigurationIntTestData.createTag("live");

        ProjectDeployment projectDeployment = automationConfigurationIntTestData.createProjectDeployment(
            "Live Deployment", project.getId(), 1, Environment.PRODUCTION, List.of(tag.getId()));

        ProjectDeployment fetchedProjectDeployment = remoteProjectDeploymentServiceClient.getProjectDeployment(
            projectDeployment.getId());

        assertThat(fetchedProjectDeployment.getId()).isEqualTo(projectDeployment.getId());
        assertThat(fetchedProjectDeployment.getName()).isEqualTo("Live Deployment");
        assertThat(fetchedProjectDeployment.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(fetchedProjectDeployment.getProjectId()).isEqualTo(project.getId());
        assertThat(fetchedProjectDeployment.getProjectVersion()).isEqualTo(1);
        assertThat(fetchedProjectDeployment.getTagIds()).containsExactly(tag.getId());
        assertThat(fetchedProjectDeployment.isEnabled()).isFalse();
        assertThat(fetchedProjectDeployment.getVersion()).isEqualTo(projectDeployment.getVersion());
    }

    @Test
    void testGetUnknownProjectDeploymentFailsWithTheServerError() {
        assertThatThrownBy(() -> remoteProjectDeploymentServiceClient.getProjectDeployment(Long.MAX_VALUE))
            .isInstanceOf(StatusRuntimeException.class)
            .hasMessage("INTERNAL: NoSuchElementException: No value present");
    }

    @Test
    void testGetProjectDeploymentsByIdsReturnsOnlyThoseDeployments() {
        Project project = automationConfigurationIntTestData.createProject("Live Project", workspace.getId());

        ProjectDeployment productionProjectDeployment = automationConfigurationIntTestData.createProjectDeployment(
            "Production", project.getId(), 1, Environment.PRODUCTION, List.of());
        ProjectDeployment stagingProjectDeployment = automationConfigurationIntTestData.createProjectDeployment(
            "Staging", project.getId(), 1, Environment.STAGING, List.of());

        automationConfigurationIntTestData.createProjectDeployment(
            "Development", project.getId(), 1, Environment.DEVELOPMENT, List.of());

        assertThat(
            remoteProjectDeploymentServiceClient.getProjectDeployments(
                List.of(stagingProjectDeployment.getId(), productionProjectDeployment.getId())))
                    .extracting(ProjectDeployment::getId)
                    .containsExactlyInAnyOrder(stagingProjectDeployment.getId(), productionProjectDeployment.getId());
        assertThat(remoteProjectDeploymentServiceClient.getProjectDeployments(List.of())).isEmpty();
    }

    @Test
    void testGetProjectDeploymentsByFilterAppliesEveryFilter() {
        Project project = automationConfigurationIntTestData.createProject("Live Project", workspace.getId());
        Project otherProject = automationConfigurationIntTestData.createProject("Other Project", workspace.getId());
        Tag tag = automationConfigurationIntTestData.createTag("live");
        Workspace otherWorkspace = automationConfigurationIntTestData.createWorkspace("other workspace");

        Project otherWorkspaceProject = automationConfigurationIntTestData.createProject(
            "Other Workspace Project", otherWorkspace.getId());

        ProjectDeployment taggedProjectDeployment = automationConfigurationIntTestData.createProjectDeployment(
            "Alpha", project.getId(), 1, Environment.PRODUCTION, List.of(tag.getId()));
        ProjectDeployment stagingProjectDeployment = automationConfigurationIntTestData.createProjectDeployment(
            "Beta", project.getId(), 1, Environment.STAGING, List.of());
        ProjectDeployment untaggedProjectDeployment = automationConfigurationIntTestData.createProjectDeployment(
            "Zeta", project.getId(), 2, Environment.PRODUCTION, List.of());
        ProjectDeployment otherProjectDeployment = automationConfigurationIntTestData.createProjectDeployment(
            "Gamma", otherProject.getId(), 1, Environment.PRODUCTION, List.of());
        ProjectDeployment embeddedProjectDeployment = automationConfigurationIntTestData.createProjectDeployment(
            "__EMBEDDED__Delta", project.getId(), 1, Environment.PRODUCTION, List.of());

        automationConfigurationIntTestData.createProjectDeployment(
            "Epsilon", otherWorkspaceProject.getId(), 1, Environment.PRODUCTION, List.of());

        assertThat(
            remoteProjectDeploymentServiceClient.getProjectDeployments(null, null, null, null, workspace.getId()))
                .extracting(ProjectDeployment::getId)
                .containsExactlyInAnyOrder(
                    embeddedProjectDeployment.getId(), taggedProjectDeployment.getId(),
                    stagingProjectDeployment.getId(), otherProjectDeployment.getId(),
                    untaggedProjectDeployment.getId());
        assertThat(
            remoteProjectDeploymentServiceClient.getProjectDeployments(
                false, Environment.PRODUCTION, project.getId(), tag.getId(), workspace.getId()))
                    .singleElement()
                    .satisfies(projectDeployment -> {
                        assertThat(projectDeployment.getId()).isEqualTo(taggedProjectDeployment.getId());
                        assertThat(projectDeployment.getTagIds()).containsExactly(tag.getId());
                    });
        assertThat(
            remoteProjectDeploymentServiceClient.getProjectDeployments(
                true, Environment.PRODUCTION, null, null, workspace.getId()))
                    .extracting(ProjectDeployment::getId)
                    .containsExactly(embeddedProjectDeployment.getId());
        assertThat(
            remoteProjectDeploymentServiceClient.getProjectDeployments(
                false, Environment.PRODUCTION, project.getId(), null, workspace.getId()))
                    .extracting(ProjectDeployment::getId)
                    .containsExactly(taggedProjectDeployment.getId(), untaggedProjectDeployment.getId());
        assertThat(
            remoteProjectDeploymentServiceClient.getProjectDeployments(
                false, Environment.STAGING, project.getId(), null, null))
                    .extracting(ProjectDeployment::getId)
                    .containsExactly(stagingProjectDeployment.getId());
    }

    @Test
    void testIsProjectDeploymentEnabledReflectsTheStoredFlag() {
        Project project = automationConfigurationIntTestData.createProject("Live Project", workspace.getId());

        ProjectDeployment enabledProjectDeployment = automationConfigurationIntTestData.createProjectDeployment(
            "Enabled", project.getId(), 1, Environment.PRODUCTION, List.of());
        ProjectDeployment disabledProjectDeployment = automationConfigurationIntTestData.createProjectDeployment(
            "Disabled", project.getId(), 1, Environment.STAGING, List.of());

        projectDeploymentService.updateEnabled(enabledProjectDeployment.getId(), true);

        assertThat(remoteProjectDeploymentServiceClient.isProjectDeploymentEnabled(enabledProjectDeployment.getId()))
            .isTrue();
        assertThat(
            remoteProjectDeploymentServiceClient.isProjectDeploymentEnabled(disabledProjectDeployment.getId()))
                .isFalse();
        assertThat(remoteProjectDeploymentServiceClient.isProjectDeploymentEnabled(Long.MAX_VALUE)).isFalse();
    }
}
