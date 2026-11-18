/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.auto.memory.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.embedded.ai.auto.memory.domain.EmbeddedAiAutoMemory;
import com.bytechef.ee.embedded.ai.auto.memory.domain.EmbeddedAiAutoMemoryConnectedUser;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserReferenceDeployment;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserReferenceDeploymentService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryNotFoundException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedAiAutoMemoryFacadeTest {

    private static final long AUTOMATION_PROJECT_ID = 500L;
    private static final long CONFIGURATION_ID = 300L;
    private static final long CONNECTED_USER_ID = 200L;
    private static final long CUSTOM_WORKFLOWS_DEPLOYMENT_ID = 9L;
    private static final long INSTANCE_ID = 100L;
    private static final long INTEGRATION_ID = 400L;
    private static final long OTHER_CONNECTED_USER_ID = 201L;
    private static final long OTHER_INSTANCE_ID = 101L;
    private static final long REFERENCE_DEPLOYMENT_ID = 8L;
    private static final long REGULAR_DEPLOYMENT_ID = 6L;

    private static final AiAutoMemoryOwner INSTANCE_OWNER = new AiAutoMemoryOwner(
        Workspace.DEFAULT_WORKSPACE_ID, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, INSTANCE_ID,
        Environment.DEVELOPMENT);
    private static final AiAutoMemoryOwner REFERENCE_DEPLOYMENT_OWNER = new AiAutoMemoryOwner(
        Workspace.DEFAULT_WORKSPACE_ID, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, REFERENCE_DEPLOYMENT_ID,
        Environment.DEVELOPMENT);

    private final AiAutoMemoryService aiAutoMemoryService = mock(AiAutoMemoryService.class);
    private final ConnectedUserProjectService connectedUserProjectService = mock(ConnectedUserProjectService.class);
    private final ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService =
        mock(ConnectedUserReferenceDeploymentService.class);
    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService =
        mock(IntegrationInstanceConfigurationService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final IntegrationService integrationService = mock(IntegrationService.class);
    private final ProjectOwnershipService projectOwnershipService = mock(ProjectOwnershipService.class);
    private final ProjectService projectService = mock(ProjectService.class);

    private EmbeddedAiAutoMemoryFacade embeddedAiAutoMemoryFacade;

    @BeforeEach
    void beforeEach() {
        embeddedAiAutoMemoryFacade = new EmbeddedAiAutoMemoryFacadeImpl(
            aiAutoMemoryService, connectedUserProjectService, connectedUserReferenceDeploymentService,
            connectedUserService, integrationInstanceConfigurationService, integrationInstanceService,
            integrationService, projectOwnershipService, projectService);

        when(integrationInstanceService.getIntegrationInstances(any())).thenReturn(
            List.of(
                integrationInstance(INSTANCE_ID, CONNECTED_USER_ID),
                integrationInstance(OTHER_INSTANCE_ID, OTHER_CONNECTED_USER_ID)));
        when(connectedUserService.getConnectedUsers(any())).thenReturn(
            List.of(
                connectedUser(CONNECTED_USER_ID, "Ana", "ana@acme.com", "ana-1"),
                connectedUser(OTHER_CONNECTED_USER_ID, "Ben", "ben@acme.com", "ben-1")));
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(any()))
            .thenReturn(List.of(integrationInstanceConfiguration()));
        when(integrationService.getIntegrations(any())).thenReturn(List.of(integration()));
        when(projectOwnershipService.getOwnedProjectDeploymentIds(any(), eq(ProjectDeploymentOwner.EMBEDDED)))
            .thenAnswer(invocation -> {
                Collection<Long> projectDeploymentIds = invocation.getArgument(0);

                return projectDeploymentIds.stream()
                    .filter(projectDeploymentId -> projectDeploymentId != REGULAR_DEPLOYMENT_ID)
                    .collect(Collectors.toSet());
            });
        when(connectedUserReferenceDeploymentService.fetchByProjectDeploymentId(anyLong()))
            .thenReturn(Optional.empty());
        when(connectedUserReferenceDeploymentService.fetchByProjectDeploymentId(REFERENCE_DEPLOYMENT_ID)).thenReturn(
            Optional.of(
                new ConnectedUserReferenceDeployment(
                    OTHER_CONNECTED_USER_ID, AUTOMATION_PROJECT_ID, REFERENCE_DEPLOYMENT_ID)));
        when(connectedUserProjectService.fetchConnectedUserId(anyLong())).thenReturn(Optional.empty());
        when(connectedUserProjectService.fetchConnectedUserId(CUSTOM_WORKFLOWS_DEPLOYMENT_ID))
            .thenReturn(Optional.of(CONNECTED_USER_ID));
        when(projectService.getProjects(any())).thenReturn(List.of(project()));
    }

    @Test
    void testGetMemoriesKeepsIntegrationInstanceRows() {
        when(aiAutoMemoryService.listAllOwners(Workspace.DEFAULT_WORKSPACE_ID, Environment.DEVELOPMENT, null))
            .thenReturn(
                List.of(
                    memory(1L, AiAutoMemoryPrincipalType.USER, 5L, Environment.DEVELOPMENT),
                    memory(
                        2L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, REGULAR_DEPLOYMENT_ID,
                        Environment.DEVELOPMENT),
                    memory(3L, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, INSTANCE_ID, Environment.DEVELOPMENT)));

        List<EmbeddedAiAutoMemory> embeddedAiAutoMemories = embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemories(
            Environment.DEVELOPMENT, null, null);

        assertThat(embeddedAiAutoMemories).hasSize(1);

        EmbeddedAiAutoMemory embeddedAiAutoMemory = embeddedAiAutoMemories.getFirst();

        AiAutoMemory aiAutoMemory = embeddedAiAutoMemory.memory();

        assertThat(aiAutoMemory.getId()).isEqualTo(3L);
        assertThat(embeddedAiAutoMemory.connectedUserId()).isEqualTo(CONNECTED_USER_ID);
        assertThat(embeddedAiAutoMemory.connectedUserName()).isEqualTo("Ana");
        assertThat(embeddedAiAutoMemory.connectedUserEmail()).isEqualTo("ana@acme.com");
        assertThat(embeddedAiAutoMemory.connectedUserExternalId()).isEqualTo("ana-1");
        assertThat(embeddedAiAutoMemory.integrationId()).isEqualTo(INTEGRATION_ID);
        assertThat(embeddedAiAutoMemory.integrationName()).isEqualTo("Memory Demo");
        assertThat(embeddedAiAutoMemory.integrationComponentName()).isEqualTo("httpClient");
        assertThat(embeddedAiAutoMemory.automationId()).isNull();
    }

    @Test
    void testGetMemoriesListsEmbeddedAutomationRowsWithTheirConnectedUser() {
        when(aiAutoMemoryService.listAllOwners(Workspace.DEFAULT_WORKSPACE_ID, Environment.DEVELOPMENT, null))
            .thenReturn(
                List.of(
                    memory(
                        2L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, REGULAR_DEPLOYMENT_ID,
                        Environment.DEVELOPMENT),
                    memory(
                        8L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, REFERENCE_DEPLOYMENT_ID,
                        Environment.DEVELOPMENT),
                    memory(
                        9L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, CUSTOM_WORKFLOWS_DEPLOYMENT_ID,
                        Environment.DEVELOPMENT)));

        List<EmbeddedAiAutoMemory> embeddedAiAutoMemories = embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemories(
            Environment.DEVELOPMENT, null, null);

        assertThat(embeddedAiAutoMemories).hasSize(2);

        EmbeddedAiAutoMemory referenceDeploymentMemory = embeddedAiAutoMemories.getFirst();

        assertThat(referenceDeploymentMemory.connectedUserId()).isEqualTo(OTHER_CONNECTED_USER_ID);
        assertThat(referenceDeploymentMemory.connectedUserName()).isEqualTo("Ben");
        assertThat(referenceDeploymentMemory.automationId()).isEqualTo(AUTOMATION_PROJECT_ID);
        assertThat(referenceDeploymentMemory.automationName()).isEqualTo("Lead Notes");
        assertThat(referenceDeploymentMemory.integrationId()).isNull();

        EmbeddedAiAutoMemory customWorkflowsMemory = embeddedAiAutoMemories.get(1);

        assertThat(customWorkflowsMemory.connectedUserId()).isEqualTo(CONNECTED_USER_ID);
        assertThat(customWorkflowsMemory.automationId()).isNull();
        assertThat(customWorkflowsMemory.automationName()).isNull();
        assertThat(customWorkflowsMemory.integrationId()).isNull();
    }

    @Test
    void testGetMemoriesFiltersByConnectedUser() {
        when(aiAutoMemoryService.listAllOwners(
            Workspace.DEFAULT_WORKSPACE_ID, Environment.DEVELOPMENT, AiAutoMemoryType.USER))
                .thenReturn(
                    List.of(
                        memory(
                            3L, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, INSTANCE_ID, Environment.DEVELOPMENT),
                        memory(
                            4L, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, OTHER_INSTANCE_ID,
                            Environment.DEVELOPMENT)));

        List<EmbeddedAiAutoMemory> embeddedAiAutoMemories = embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemories(
            Environment.DEVELOPMENT, OTHER_CONNECTED_USER_ID, AiAutoMemoryType.USER);

        assertThat(embeddedAiAutoMemories)
            .extracting(EmbeddedAiAutoMemory::connectedUserId)
            .containsExactly(OTHER_CONNECTED_USER_ID);
    }

    @Test
    void testGetMemoriesListsAnInstanceWhoseConnectedUserIsGone() {
        when(integrationInstanceService.getIntegrationInstances(any())).thenReturn(List.of());
        when(aiAutoMemoryService.listAllOwners(Workspace.DEFAULT_WORKSPACE_ID, Environment.DEVELOPMENT, null))
            .thenReturn(
                List.of(
                    memory(3L, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, INSTANCE_ID, Environment.DEVELOPMENT)));

        List<EmbeddedAiAutoMemory> embeddedAiAutoMemories = embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemories(
            Environment.DEVELOPMENT, null, null);

        assertThat(embeddedAiAutoMemories).hasSize(1);

        EmbeddedAiAutoMemory embeddedAiAutoMemory = embeddedAiAutoMemories.getFirst();

        assertThat(embeddedAiAutoMemory.connectedUserId()).isNull();
        assertThat(embeddedAiAutoMemory.integrationName()).isNull();
    }

    @Test
    void testGetConnectedUsersCountsMemoriesPerUser() {
        when(aiAutoMemoryService.listAllOwners(Workspace.DEFAULT_WORKSPACE_ID, Environment.DEVELOPMENT, null))
            .thenReturn(
                List.of(
                    memory(3L, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, INSTANCE_ID, Environment.DEVELOPMENT),
                    memory(4L, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, INSTANCE_ID, Environment.DEVELOPMENT),
                    memory(5L, AiAutoMemoryPrincipalType.USER, OTHER_CONNECTED_USER_ID, Environment.DEVELOPMENT),
                    memory(
                        6L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, REFERENCE_DEPLOYMENT_ID,
                        Environment.DEVELOPMENT),
                    memory(
                        7L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, CUSTOM_WORKFLOWS_DEPLOYMENT_ID,
                        Environment.DEVELOPMENT)));

        List<EmbeddedAiAutoMemoryConnectedUser> connectedUsers =
            embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemoryConnectedUsers(Environment.DEVELOPMENT);

        assertThat(connectedUsers).containsExactly(
            new EmbeddedAiAutoMemoryConnectedUser(CONNECTED_USER_ID, "Ana", "ana@acme.com", "ana-1", 3),
            new EmbeddedAiAutoMemoryConnectedUser(OTHER_CONNECTED_USER_ID, "Ben", "ben@acme.com", "ben-1", 1));
    }

    @Test
    void testUpdateAddressesTheIntegrationInstanceOwnerWithoutListingTheWorkspace() {
        AiAutoMemory aiAutoMemory = memory(
            3L, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, INSTANCE_ID, Environment.DEVELOPMENT);
        AiAutoMemoryPatch aiAutoMemoryPatch = new AiAutoMemoryPatch("New title", null, null, null);

        when(aiAutoMemoryService.updateById(INSTANCE_OWNER, 3L, 7L, aiAutoMemoryPatch))
            .thenReturn(aiAutoMemory);

        EmbeddedAiAutoMemory embeddedAiAutoMemory = embeddedAiAutoMemoryFacade.updateEmbeddedAiAutoMemory(
            Environment.DEVELOPMENT, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, INSTANCE_ID, 3L, 7L,
            aiAutoMemoryPatch);

        assertThat(embeddedAiAutoMemory.connectedUserId()).isEqualTo(CONNECTED_USER_ID);

        verify(aiAutoMemoryService).updateById(INSTANCE_OWNER, 3L, 7L, aiAutoMemoryPatch);
        verify(aiAutoMemoryService, never()).listAllOwners(anyLong(), any(), any());
    }

    @Test
    void testUpdatePropagatesAMemoryTheOwnerDoesNotHold() {
        AiAutoMemoryPatch aiAutoMemoryPatch = new AiAutoMemoryPatch("New title", null, null, null);

        when(aiAutoMemoryService.updateById(INSTANCE_OWNER, 3L, 7L, aiAutoMemoryPatch))
            .thenThrow(new AiAutoMemoryNotFoundException("Memory not found"));

        assertThatExceptionOfType(AiAutoMemoryNotFoundException.class)
            .isThrownBy(() -> embeddedAiAutoMemoryFacade.updateEmbeddedAiAutoMemory(
                Environment.DEVELOPMENT, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, INSTANCE_ID, 3L, 7L,
                aiAutoMemoryPatch));
    }

    @Test
    void testDeleteAddressesTheIntegrationInstanceOwnerWithoutListingTheWorkspace() {
        embeddedAiAutoMemoryFacade.deleteEmbeddedAiAutoMemory(Environment.DEVELOPMENT,
            AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, INSTANCE_ID, 3L);

        verify(aiAutoMemoryService).deleteById(INSTANCE_OWNER, 3L);
        verify(aiAutoMemoryService, never()).listAllOwners(anyLong(), any(), any());
    }

    @Test
    void testDeletePropagatesAMemoryTheOwnerDoesNotHold() {
        when(aiAutoMemoryService.deleteById(INSTANCE_OWNER, 3L))
            .thenThrow(new AiAutoMemoryNotFoundException("Memory not found"));

        assertThatExceptionOfType(AiAutoMemoryNotFoundException.class)
            .isThrownBy(
                () -> embeddedAiAutoMemoryFacade.deleteEmbeddedAiAutoMemory(Environment.DEVELOPMENT,
                    AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, INSTANCE_ID, 3L));
    }

    @Test
    void testUpdateAddressesAnEmbeddedDeploymentOwner() {
        AiAutoMemory aiAutoMemory = memory(
            8L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, REFERENCE_DEPLOYMENT_ID, Environment.DEVELOPMENT);
        AiAutoMemoryPatch aiAutoMemoryPatch = new AiAutoMemoryPatch("New title", null, null, null);

        when(aiAutoMemoryService.updateById(REFERENCE_DEPLOYMENT_OWNER, 8L, 7L, aiAutoMemoryPatch))
            .thenReturn(aiAutoMemory);

        EmbeddedAiAutoMemory embeddedAiAutoMemory = embeddedAiAutoMemoryFacade.updateEmbeddedAiAutoMemory(
            Environment.DEVELOPMENT, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, REFERENCE_DEPLOYMENT_ID, 8L, 7L,
            aiAutoMemoryPatch);

        assertThat(embeddedAiAutoMemory.automationName()).isEqualTo("Lead Notes");
    }

    @Test
    void testUpdateRejectsADeploymentEmbeddedDoesNotOwn() {
        AiAutoMemoryPatch aiAutoMemoryPatch = new AiAutoMemoryPatch("New title", null, null, null);

        assertThatExceptionOfType(AiAutoMemoryNotFoundException.class)
            .isThrownBy(() -> embeddedAiAutoMemoryFacade.updateEmbeddedAiAutoMemory(
                Environment.DEVELOPMENT, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, REGULAR_DEPLOYMENT_ID, 2L, 7L,
                aiAutoMemoryPatch));

        verify(aiAutoMemoryService, never()).updateById(any(), anyLong(), anyLong(), any());
    }

    @Test
    void testDeleteAddressesAnEmbeddedDeploymentOwner() {
        embeddedAiAutoMemoryFacade.deleteEmbeddedAiAutoMemory(
            Environment.DEVELOPMENT, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, REFERENCE_DEPLOYMENT_ID, 8L);

        verify(aiAutoMemoryService).deleteById(REFERENCE_DEPLOYMENT_OWNER, 8L);
    }

    @Test
    void testDeleteRejectsADeploymentEmbeddedDoesNotOwn() {
        assertThatExceptionOfType(AiAutoMemoryNotFoundException.class)
            .isThrownBy(() -> embeddedAiAutoMemoryFacade.deleteEmbeddedAiAutoMemory(
                Environment.DEVELOPMENT, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, REGULAR_DEPLOYMENT_ID, 2L));

        verify(aiAutoMemoryService, never()).deleteById(any(), anyLong());
    }

    @Test
    void testDeleteRejectsAUserOwner() {
        assertThatExceptionOfType(AiAutoMemoryNotFoundException.class)
            .isThrownBy(() -> embeddedAiAutoMemoryFacade.deleteEmbeddedAiAutoMemory(
                Environment.DEVELOPMENT, AiAutoMemoryPrincipalType.USER, 5L, 1L));

        verify(aiAutoMemoryService, never()).deleteById(any(), anyLong());
    }

    private static ConnectedUser connectedUser(long id, String name, String email, String externalId) {
        return new ConnectedUser(Map.of(), email, true, externalId, id, name, 0);
    }

    private static Integration integration() {
        Integration integration = new Integration();

        integration.setComponentName("httpClient");
        integration.setId(INTEGRATION_ID);
        integration.setName("Memory Demo");

        return integration;
    }

    private static Project project() {
        Project project = new Project();

        project.setId(AUTOMATION_PROJECT_ID);
        project.setName("Lead Notes");

        return project;
    }

    private static IntegrationInstance integrationInstance(long id, long connectedUserId) {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectedUserId(connectedUserId);
        integrationInstance.setId(id);
        integrationInstance.setIntegrationInstanceConfigurationId(CONFIGURATION_ID);

        return integrationInstance;
    }

    private static IntegrationInstanceConfiguration integrationInstanceConfiguration() {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setId(CONFIGURATION_ID);
        integrationInstanceConfiguration.setIntegrationId(INTEGRATION_ID);

        return integrationInstanceConfiguration;
    }

    private static AiAutoMemory memory(
        long id, AiAutoMemoryPrincipalType principalType, long principalId, Environment environment) {

        AiAutoMemoryOwner aiAutoMemoryOwner = new AiAutoMemoryOwner(
            Workspace.DEFAULT_WORKSPACE_ID, principalType, principalId, environment);

        return AiAutoMemory.restore(
            aiAutoMemoryOwner, id, "note_" + id, "Note " + id, null, AiAutoMemoryType.USER, "content", null, null, 7L);
    }
}
