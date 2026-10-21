/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AppApiCollectionResolverTest {

    private static final long API_COLLECTION_ID = 21L;
    private static final long DEPLOYMENT_ID = 11L;
    private static final long PROJECT_ID = 3L;
    private static final long WORKSPACE_ID = 7L;

    @Mock
    private ApiCollectionService apiCollectionService;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectService projectService;

    private AppApiCollectionResolver resolver;

    @BeforeEach
    void beforeEach() {
        resolver = new AppApiCollectionResolver(apiCollectionService, projectDeploymentService, projectService);
    }

    @Test
    void testResolveReturnsEnabledCollectionInWorkspaceAndEnvironment() {
        ApiCollection apiCollection = apiCollection(API_COLLECTION_ID, DEPLOYMENT_ID);

        stubCollection(apiCollection, deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true), project(WORKSPACE_ID));

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, API_COLLECTION_ID)).contains(apiCollection);
    }

    @Test
    void testResolveEmptyForDisabledDeployment() {
        stubCollection(
            apiCollection(API_COLLECTION_ID, DEPLOYMENT_ID), deployment(DEPLOYMENT_ID, Environment.PRODUCTION, false),
            project(WORKSPACE_ID));

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, API_COLLECTION_ID)).isEmpty();
    }

    @Test
    void testResolveEmptyForOtherEnvironment() {
        stubCollection(
            apiCollection(API_COLLECTION_ID, DEPLOYMENT_ID), deployment(DEPLOYMENT_ID, Environment.STAGING, true),
            project(WORKSPACE_ID));

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, API_COLLECTION_ID)).isEmpty();
    }

    @Test
    void testResolveEmptyForOtherWorkspace() {
        stubCollection(
            apiCollection(API_COLLECTION_ID, DEPLOYMENT_ID), deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true),
            project(8L));

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, API_COLLECTION_ID)).isEmpty();
    }

    @Test
    void testResolveEmptyForMissingCollection() {
        when(apiCollectionService.fetchApiCollection(API_COLLECTION_ID)).thenReturn(Optional.empty());

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, API_COLLECTION_ID)).isEmpty();
    }

    @Test
    void testResolveEmptyForMissingDeployment() {
        when(apiCollectionService.fetchApiCollection(API_COLLECTION_ID))
            .thenReturn(Optional.of(apiCollection(API_COLLECTION_ID, DEPLOYMENT_ID)));
        when(projectDeploymentService.fetchProjectDeployment(DEPLOYMENT_ID)).thenReturn(Optional.empty());

        assertThat(resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, API_COLLECTION_ID)).isEmpty();
    }

    @Test
    void testBindableIncludesDisabledDeployment() {
        ApiCollection enabledApiCollection = apiCollection(API_COLLECTION_ID, DEPLOYMENT_ID);
        ApiCollection disabledApiCollection = apiCollection(22L, 12L);
        ApiCollection orphanApiCollection = apiCollection(23L, 13L);

        when(apiCollectionService.getApiCollections(WORKSPACE_ID, Environment.PRODUCTION, null, null))
            .thenReturn(List.of(enabledApiCollection, disabledApiCollection, orphanApiCollection));
        when(projectDeploymentService.fetchProjectDeployment(DEPLOYMENT_ID))
            .thenReturn(Optional.of(deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true)));
        when(projectDeploymentService.fetchProjectDeployment(12L))
            .thenReturn(Optional.of(deployment(12L, Environment.PRODUCTION, false)));
        when(projectDeploymentService.fetchProjectDeployment(13L)).thenReturn(Optional.empty());
        when(projectService.fetchProject(PROJECT_ID)).thenReturn(Optional.of(project(WORKSPACE_ID)));

        assertThat(resolver.getBindableApiCollections(WORKSPACE_ID, Environment.PRODUCTION))
            .containsExactly(enabledApiCollection, disabledApiCollection);
    }

    @Test
    void testBindableExcludesOtherEnvironmentAndWorkspace() {
        ApiCollection stagingApiCollection = apiCollection(API_COLLECTION_ID, DEPLOYMENT_ID);
        ApiCollection otherWorkspaceApiCollection = apiCollection(22L, 12L);
        Project otherWorkspaceProject = project(8L);

        otherWorkspaceProject.setId(4L);

        ProjectDeployment otherWorkspaceDeployment = deployment(12L, Environment.PRODUCTION, true);

        otherWorkspaceDeployment.setProjectId(4L);

        when(apiCollectionService.getApiCollections(WORKSPACE_ID, Environment.PRODUCTION, null, null))
            .thenReturn(List.of(stagingApiCollection, otherWorkspaceApiCollection));
        when(projectDeploymentService.fetchProjectDeployment(DEPLOYMENT_ID))
            .thenReturn(Optional.of(deployment(DEPLOYMENT_ID, Environment.STAGING, true)));
        when(projectDeploymentService.fetchProjectDeployment(12L)).thenReturn(Optional.of(otherWorkspaceDeployment));
        lenient().when(projectService.fetchProject(PROJECT_ID))
            .thenReturn(Optional.of(project(WORKSPACE_ID)));
        when(projectService.fetchProject(4L)).thenReturn(Optional.of(otherWorkspaceProject));

        assertThat(resolver.getBindableApiCollections(WORKSPACE_ID, Environment.PRODUCTION)).isEmpty();
    }

    private static ApiCollection apiCollection(long id, long projectDeploymentId) {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setId(id);
        apiCollection.setCollectionVersion(1);
        apiCollection.setContextPath("crm-" + id);
        apiCollection.setName("CRM " + id);
        apiCollection.setProjectDeploymentId(projectDeploymentId);

        return apiCollection;
    }

    private static ProjectDeployment deployment(long id, Environment environment, boolean enabled) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(id);
        projectDeployment.setEnabled(enabled);
        projectDeployment.setEnvironment(environment);
        projectDeployment.setProjectId(PROJECT_ID);

        return projectDeployment;
    }

    private static Project project(long workspaceId) {
        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setWorkspaceId(workspaceId);

        return project;
    }

    private void stubCollection(ApiCollection apiCollection, ProjectDeployment projectDeployment, Project project) {
        when(apiCollectionService.fetchApiCollection(apiCollection.getId())).thenReturn(Optional.of(apiCollection));
        when(projectDeploymentService.fetchProjectDeployment(projectDeployment.getId()))
            .thenReturn(Optional.of(projectDeployment));
        lenient().when(projectService.fetchProject(projectDeployment.getProjectId()))
            .thenReturn(Optional.of(project));
    }
}
