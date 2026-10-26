/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationConfigurationProtoMapperTest {

    static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();
    static final Instant PUBLISHED_DATE = Instant.parse("2026-10-26T10:30:00Z");

    private final AutomationConfigurationProtoMapper automationConfigurationProtoMapper =
        new AutomationConfigurationProtoMapper(JSON_MAPPER);

    @Test
    void testProjectRoundTrip() {
        Project project = project(7L);

        assertThat(project.getCreatedDate()).isNotNull();
        assertThat(project.getProjectVersions()).hasSize(2);
        assertThat(project.getTagIds()).containsExactlyInAnyOrder(3L, 4L);

        Project readProject = automationConfigurationProtoMapper.toProject(
            automationConfigurationProtoMapper.toProto(project));

        assertThat(readProject).usingRecursiveComparison()
            .isEqualTo(project);
        assertThat(readProject.getLastPublishedProjectVersion()).isEqualTo(project.getLastPublishedProjectVersion());
        assertThat(readProject.getLastStatus()).isEqualTo(ProjectVersion.Status.DRAFT);
    }

    @Test
    void testProjectsRoundTrip() {
        List<Project> projects = List.of(project(7L), project(8L));

        assertThat(automationConfigurationProtoMapper.toProjects(
            automationConfigurationProtoMapper.toRemoteProjects(projects)))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyElementsOf(projects);
    }

    @Test
    void testProjectDeploymentRoundTrip() {
        ProjectDeployment projectDeployment = projectDeployment();

        assertThat(projectDeployment.getCreatedBy()).isEqualTo("admin@localhost.com");
        assertThat(projectDeployment.getProjectId()).isEqualTo(7L);

        assertThat(automationConfigurationProtoMapper.toProjectDeployment(
            automationConfigurationProtoMapper.toProto(projectDeployment)))
                .usingRecursiveComparison()
                .isEqualTo(projectDeployment);
    }

    @Test
    void testProjectDeploymentWorkflowRoundTrip() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflow();

        ProjectDeploymentWorkflow readProjectDeploymentWorkflow =
            automationConfigurationProtoMapper.toProjectDeploymentWorkflow(
                automationConfigurationProtoMapper.toProto(projectDeploymentWorkflow));

        assertThat(readProjectDeploymentWorkflow).usingRecursiveComparison()
            .isEqualTo(projectDeploymentWorkflow);
        assertThat(readProjectDeploymentWorkflow.getInputs()
            .get("id")).isInstanceOf(Long.class);
    }

    @Test
    void testProjectDeploymentWorkflowConnectionRoundTrip() {
        List<ProjectDeploymentWorkflowConnection> connections = List.of(connection(4L), connection(5L));

        assertThat(automationConfigurationProtoMapper.toProjectDeploymentWorkflowConnections(
            automationConfigurationProtoMapper.toRemoteProjectDeploymentWorkflowConnections(connections)))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyElementsOf(connections);
    }

    static ProjectDeploymentWorkflowConnection connection(long connectionId) {
        return new ProjectDeploymentWorkflowConnection(connectionId, "airtable", "airtable_1");
    }

    static Project project(long id) {
        Project project = new Project();

        project.setCategoryId(2L);
        project.setDescription("Live");
        project.setId(id);
        project.setName("Live Project");
        project.setProjectVersions(
            List.of(
                new ProjectVersion(1, ProjectVersion.Status.PUBLISHED.ordinal(), PUBLISHED_DATE, "First"),
                new ProjectVersion(2)));
        project.setTagIds(List.of(3L, 4L));
        project.setUuid("3e4078b4-87bb-43c0-9cc6-1270f73b131d");
        project.setVersion(5);
        project.setWorkspaceId(1049L);

        setAuditFields(project);

        return project;
    }

    static ProjectDeployment projectDeployment() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setDescription("Deployed");
        projectDeployment.setEnabled(true);
        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setId(9L);
        projectDeployment.setName("Live Deployment");
        projectDeployment.setProjectId(7L);
        projectDeployment.setProjectVersion(1);
        projectDeployment.setTagIds(List.of(3L));
        projectDeployment.setVersion(2);

        setAuditFields(projectDeployment);

        return projectDeployment;
    }

    static ProjectDeploymentWorkflow projectDeploymentWorkflow() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setConnections(List.of(connection(4L)));
        projectDeploymentWorkflow.setEnabled(true);
        projectDeploymentWorkflow.setId(11L);
        projectDeploymentWorkflow.setInputs(Map.of("id", 9_007_199_254_740_993L, "ratio", 5.0));
        projectDeploymentWorkflow.setProjectDeploymentId(9L);
        projectDeploymentWorkflow.setVersion(2);
        projectDeploymentWorkflow.setWorkflowId("3e4078b4-87bb-43c0-9cc6-1270f73b131d");

        setAuditFields(projectDeploymentWorkflow);

        return projectDeploymentWorkflow;
    }

    private static void setAuditFields(Object object) {
        ReflectionTestUtils.setField(object, "createdBy", "admin@localhost.com");
        ReflectionTestUtils.setField(object, "createdDate", Instant.parse("2026-10-26T10:00:00Z"));
        ReflectionTestUtils.setField(object, "lastModifiedBy", "user@localhost.com");
        ReflectionTestUtils.setField(object, "lastModifiedDate", Instant.parse("2026-10-26T11:00:00Z"));
    }
}
