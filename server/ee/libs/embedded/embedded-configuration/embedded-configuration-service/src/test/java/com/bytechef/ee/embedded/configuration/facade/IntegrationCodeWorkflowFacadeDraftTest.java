/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationCodeWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationCodeWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade.CodeWorkflowReconciliation;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.workflow.definition.CompositeTaskDefinition;
import com.bytechef.workflow.definition.ConnectionRequirement;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import com.bytechef.workflow.definition.WorkflowTaskDefinition;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationCodeWorkflowFacadeDraftTest {

    @Test
    void testEditorSaveUpdatesDraftContainerInPlace() {
        Integration integration = new Integration();

        integration.setId(1L);
        integration.setComponentName("my-integration");
        integration.setName("my-integration");
        integration.setIntegrationVersions(List.of(new IntegrationVersion(3)));

        IntegrationCodeWorkflow latestIntegrationCodeWorkflow = mock(IntegrationCodeWorkflow.class);

        when(latestIntegrationCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);
        when(latestIntegrationCodeWorkflow.getIntegrationVersion()).thenReturn(3);

        CodeWorkflowContainer draftCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(draftCodeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);

        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(1L))
            .thenReturn(Optional.of(latestIntegrationCodeWorkflow));

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(draftCodeWorkflowContainer);

        IntegrationService integrationService = mock(IntegrationService.class);

        when(integrationService.getIntegration(1L)).thenReturn(integration);

        CodeWorkflowContainer updatedCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        CodeWorkflowReconciliation reconciliation = new CodeWorkflowReconciliation(
            updatedCodeWorkflowContainer, Map.of(), Map.of());

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        when(codeWorkflowContainerFacade.update(
            eq(draftCodeWorkflowContainer), any(), any(), any(), eq(PlatformType.EMBEDDED)))
                .thenReturn(reconciliation);

        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);
        WorkflowService workflowService = mock(WorkflowService.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            integrationService, integrationWorkflowService, codeWorkflowContainerFacade,
            integrationCodeWorkflowService, codeWorkflowContainerService, mock(CodeWorkflowFileStorage.class),
            workflowService);

        integrationCodeWorkflowFacade.updateCodeWorkflowSource(1L, integrationScript("my-integration"));

        verify(codeWorkflowContainerFacade).update(
            eq(draftCodeWorkflowContainer), any(), any(), any(), eq(PlatformType.EMBEDDED));
        verify(codeWorkflowContainerFacade, never()).create(any(), any(), any(), any(), any(), any());
        verify(codeWorkflowContainerFacade, never()).create(any(), any(), any(), any(), any(), any(), any());
        verify(integrationService, never()).publishIntegration(anyLong(), any());
        verify(integrationCodeWorkflowService, never()).create(any(), any());
    }

    @Test
    void testEditorSaveAfterPublishMintsNewContainerReusingDraftWorkflowsByUuid() {
        Integration integration = new Integration();

        integration.setId(1L);
        integration.setComponentName("my-integration");
        integration.setName("my-integration");
        integration.setIntegrationVersions(List.of(new IntegrationVersion(2)));

        IntegrationCodeWorkflow publishedIntegrationCodeWorkflow = mock(IntegrationCodeWorkflow.class);

        when(publishedIntegrationCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);
        when(publishedIntegrationCodeWorkflow.getIntegrationVersion()).thenReturn(1);

        CodeWorkflowContainer publishedCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(publishedCodeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);
        when(publishedCodeWorkflowContainer.getWorkflowNameIds()).thenReturn(Map.of("wf-a", "pub-id"));

        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(1L))
            .thenReturn(Optional.of(publishedIntegrationCodeWorkflow));

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L))
            .thenReturn(publishedCodeWorkflowContainer);

        IntegrationService integrationService = mock(IntegrationService.class);

        when(integrationService.getIntegration(1L)).thenReturn(integration);

        UUID workflowUuid = UUID.randomUUID();

        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);

        when(integrationWorkflowService.getIntegrationWorkflows(1L, 1))
            .thenReturn(List.of(new IntegrationWorkflow(1L, 1, "pub-id", workflowUuid)));
        when(integrationWorkflowService.getIntegrationWorkflows(1L, 2))
            .thenReturn(List.of(new IntegrationWorkflow(1L, 2, "draft-id", workflowUuid)));

        CodeWorkflowContainer newCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        CodeWorkflowReconciliation reconciliation = new CodeWorkflowReconciliation(
            newCodeWorkflowContainer, Map.of(), Map.of());

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        when(codeWorkflowContainerFacade.create(
            any(), any(), any(), eq(Language.JAVASCRIPT), any(), eq(PlatformType.EMBEDDED), any()))
                .thenReturn(reconciliation);

        WorkflowService workflowService = mock(WorkflowService.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            integrationService, integrationWorkflowService, codeWorkflowContainerFacade,
            integrationCodeWorkflowService, codeWorkflowContainerService, mock(CodeWorkflowFileStorage.class),
            workflowService);

        integrationCodeWorkflowFacade.updateCodeWorkflowSource(1L, integrationScript("my-integration"));

        ArgumentCaptor<Map<String, String>> reusableWorkflowNameIdsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(codeWorkflowContainerFacade).create(
            eq("my-integration"), any(), any(), eq(Language.JAVASCRIPT), any(), eq(PlatformType.EMBEDDED),
            reusableWorkflowNameIdsCaptor.capture());

        assertThat(reusableWorkflowNameIdsCaptor.getValue())
            .isEqualTo(Map.of("wf-a", "draft-id"));

        verify(integrationCodeWorkflowService).create(newCodeWorkflowContainer, integration);
        verify(integrationService, never()).publishIntegration(anyLong(), any());
    }

    @Test
    void testEditorSaveSyncsAddedAndRemovedIntegrationWorkflows() {
        Integration integration = new Integration();

        integration.setId(1L);
        integration.setComponentName("my-integration");
        integration.setName("my-integration");
        integration.setIntegrationVersions(List.of(new IntegrationVersion(3)));

        IntegrationCodeWorkflow latestIntegrationCodeWorkflow = mock(IntegrationCodeWorkflow.class);

        when(latestIntegrationCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);
        when(latestIntegrationCodeWorkflow.getIntegrationVersion()).thenReturn(3);

        CodeWorkflowContainer draftCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(draftCodeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);

        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(1L))
            .thenReturn(Optional.of(latestIntegrationCodeWorkflow));

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(draftCodeWorkflowContainer);

        IntegrationService integrationService = mock(IntegrationService.class);

        when(integrationService.getIntegration(1L)).thenReturn(integration);

        CodeWorkflowContainer updatedCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        CodeWorkflowReconciliation reconciliation = new CodeWorkflowReconciliation(
            updatedCodeWorkflowContainer, Map.of("wf-new", "id-n"), Map.of("wf-old", "id-o"));

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        when(codeWorkflowContainerFacade.update(
            eq(draftCodeWorkflowContainer), any(), any(), any(), eq(PlatformType.EMBEDDED)))
                .thenReturn(reconciliation);

        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);
        WorkflowService workflowService = mock(WorkflowService.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            integrationService, integrationWorkflowService, codeWorkflowContainerFacade,
            integrationCodeWorkflowService, codeWorkflowContainerService, mock(CodeWorkflowFileStorage.class),
            workflowService);

        integrationCodeWorkflowFacade.updateCodeWorkflowSource(1L, integrationScript("my-integration"));

        verify(integrationWorkflowService).addWorkflow(1L, 3, "id-n");
        verify(integrationWorkflowService).delete(1L, 3, "id-o");
        verify(workflowService).delete("id-o");
    }

    @Test
    void testUploadOfANewIntegrationAddsEveryWorkflowToTheDraftAndPublishesIt() {
        IntegrationService integrationService = mock(IntegrationService.class);

        Integration createdIntegration = new Integration();

        createdIntegration.setId(9L);
        createdIntegration.setComponentName("my-integration");
        createdIntegration.setName("my-integration");

        when(integrationService.create(any())).thenReturn(createdIntegration);

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = reconcilingCodeWorkflowContainerFacade();
        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);
        IntegrationFacade integrationFacade = mock(IntegrationFacade.class);
        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            integrationService, integrationWorkflowService, codeWorkflowContainerFacade,
            integrationCodeWorkflowService, mock(IntegrationInstanceConfigurationFacade.class), integrationFacade,
            mock(CodeWorkflowContainerService.class), mock(CodeWorkflowFileStorage.class),
            mock(WorkflowService.class));

        integrationCodeWorkflowFacade.save(
            integrationScript("my-integration", "my-workflow").getBytes(StandardCharsets.UTF_8),
            Language.JAVASCRIPT);

        verify(codeWorkflowContainerFacade).create(
            eq("my-integration"), any(), any(), eq(Language.JAVASCRIPT), any(), eq(PlatformType.EMBEDDED),
            eq(Map.of()));
        verify(integrationCodeWorkflowService).create(any(), eq(createdIntegration));

        InOrder inOrder = inOrder(integrationWorkflowService, integrationFacade);

        inOrder.verify(integrationWorkflowService)
            .addWorkflow(9L, 1, "new-my-workflow");
        inOrder.verify(integrationFacade)
            .publishIntegration(9L, null);

        verify(integrationService, never()).publishIntegration(anyLong(), any());
    }

    @Test
    void testReUploadKeepsWorkflowIdsByNameDropsRemovedWorkflowsAndAddsNewOnes() {
        Integration integration = new Integration();

        integration.setId(9L);
        integration.setComponentName("my-integration");
        integration.setName("my-integration");
        integration.setIntegrationVersions(List.of(new IntegrationVersion(1), new IntegrationVersion(2)));

        IntegrationService integrationService = mock(IntegrationService.class);

        when(integrationService.getIntegrations("my-integration")).thenReturn(List.of(integration));
        when(integrationService.update(any(Integration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IntegrationCodeWorkflow publishedIntegrationCodeWorkflow = mock(IntegrationCodeWorkflow.class);

        when(publishedIntegrationCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);
        when(publishedIntegrationCodeWorkflow.getIntegrationVersion()).thenReturn(1);

        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(9L))
            .thenReturn(Optional.of(publishedIntegrationCodeWorkflow));

        CodeWorkflowContainer publishedCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(publishedCodeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);
        when(publishedCodeWorkflowContainer.getWorkflowNameIds())
            .thenReturn(Map.of("kept-workflow", "published-kept", "removed-workflow", "published-removed"));

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(publishedCodeWorkflowContainer);

        UUID keptWorkflowUuid = UUID.randomUUID();
        UUID removedWorkflowUuid = UUID.randomUUID();

        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);

        when(integrationWorkflowService.getIntegrationWorkflows(9L, 1))
            .thenReturn(
                List.of(
                    new IntegrationWorkflow(9L, 1, "published-kept", keptWorkflowUuid),
                    new IntegrationWorkflow(9L, 1, "published-removed", removedWorkflowUuid)));
        when(integrationWorkflowService.getIntegrationWorkflows(9L, 2))
            .thenReturn(
                List.of(
                    new IntegrationWorkflow(9L, 2, "draft-kept", keptWorkflowUuid),
                    new IntegrationWorkflow(9L, 2, "draft-removed", removedWorkflowUuid)));

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = reconcilingCodeWorkflowContainerFacade();
        IntegrationFacade integrationFacade = mock(IntegrationFacade.class);
        WorkflowService workflowService = mock(WorkflowService.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            integrationService, integrationWorkflowService, codeWorkflowContainerFacade,
            integrationCodeWorkflowService, mock(IntegrationInstanceConfigurationFacade.class), integrationFacade,
            codeWorkflowContainerService, mock(CodeWorkflowFileStorage.class), workflowService);

        integrationCodeWorkflowFacade.save(
            integrationScript("my-integration", "kept-workflow", "added-workflow").getBytes(StandardCharsets.UTF_8),
            Language.JAVASCRIPT);

        verify(codeWorkflowContainerFacade).create(
            eq("my-integration"), any(), any(), eq(Language.JAVASCRIPT), any(), eq(PlatformType.EMBEDDED),
            eq(Map.of("kept-workflow", "draft-kept", "removed-workflow", "draft-removed")));

        verify(integrationWorkflowService, times(1)).addWorkflow(anyLong(), anyInt(), anyString());
        verify(integrationWorkflowService).addWorkflow(9L, 2, "new-added-workflow");

        verify(integrationWorkflowService, times(1)).delete(anyLong(), anyInt(), anyString());
        verify(integrationWorkflowService).delete(9L, 2, "draft-removed");
        verify(workflowService).delete("draft-removed");
        verify(workflowService, never()).delete("draft-kept");

        verify(integrationFacade).publishIntegration(9L, null);
        verify(integrationService, never()).publishIntegration(anyLong(), any());
    }

    @Test
    void testUploadOverAnUnpublishedDraftInAnotherLanguageSwitchesTheDraftLanguage() {
        Integration integration = new Integration();

        integration.setId(1L);
        integration.setComponentName("my-integration");
        integration.setName("my-integration");
        integration.setIntegrationVersions(List.of(new IntegrationVersion(1)));

        IntegrationService integrationService = mock(IntegrationService.class);

        when(integrationService.getIntegrations("my-integration")).thenReturn(List.of(integration));
        when(integrationService.update(any(Integration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IntegrationCodeWorkflow draftIntegrationCodeWorkflow = mock(IntegrationCodeWorkflow.class);

        when(draftIntegrationCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);
        when(draftIntegrationCodeWorkflow.getIntegrationVersion()).thenReturn(1);

        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(1L))
            .thenReturn(Optional.of(draftIntegrationCodeWorkflow));

        CodeWorkflowContainer draftCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(draftCodeWorkflowContainer.getLanguage()).thenReturn(Language.PYTHON);

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(draftCodeWorkflowContainer);

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        when(codeWorkflowContainerFacade.update(
            eq(draftCodeWorkflowContainer), any(), any(), any(), eq(PlatformType.EMBEDDED)))
                .thenReturn(new CodeWorkflowReconciliation(draftCodeWorkflowContainer, Map.of(), Map.of()));

        IntegrationFacade integrationFacade = mock(IntegrationFacade.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            integrationService, mock(IntegrationWorkflowService.class), codeWorkflowContainerFacade,
            integrationCodeWorkflowService, mock(IntegrationInstanceConfigurationFacade.class), integrationFacade,
            codeWorkflowContainerService, mock(CodeWorkflowFileStorage.class), mock(WorkflowService.class));

        integrationCodeWorkflowFacade.save(
            integrationScript("my-integration").getBytes(StandardCharsets.UTF_8), Language.JAVASCRIPT);

        InOrder inOrder = inOrder(draftCodeWorkflowContainer, codeWorkflowContainerFacade, integrationFacade);

        inOrder.verify(draftCodeWorkflowContainer)
            .setLanguage(Language.JAVASCRIPT);
        inOrder.verify(codeWorkflowContainerFacade)
            .update(eq(draftCodeWorkflowContainer), any(), any(), any(), eq(PlatformType.EMBEDDED));
        inOrder.verify(integrationFacade)
            .publishIntegration(1L, null);
    }

    @Test
    void testUploadSaveWithoutEnableLeavesInstanceConfigurationsAlone() {
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade =
            mock(IntegrationInstanceConfigurationFacade.class);

        IntegrationFacade integrationFacade = mock(IntegrationFacade.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newUploadFacade(
            integrationFacade, integrationInstanceConfigurationFacade);

        integrationCodeWorkflowFacade.save(
            integrationScript("my-integration").getBytes(StandardCharsets.UTF_8), Language.JAVASCRIPT,
            Environment.STAGING, false);

        verify(integrationFacade).publishIntegration(9L, null);
        verifyNoInteractions(integrationInstanceConfigurationFacade);
    }

    @Test
    void testUploadSaveWithEnableEnablesThePublishedVersionInTheEnvironment() {
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade =
            mock(IntegrationInstanceConfigurationFacade.class);

        IntegrationFacade integrationFacade = mock(IntegrationFacade.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newUploadFacade(
            integrationFacade, integrationInstanceConfigurationFacade);

        integrationCodeWorkflowFacade.save(
            integrationScript("my-integration").getBytes(StandardCharsets.UTF_8), Language.JAVASCRIPT,
            Environment.STAGING, true);

        InOrder inOrder = inOrder(integrationFacade, integrationInstanceConfigurationFacade);

        inOrder.verify(integrationFacade)
            .publishIntegration(9L, null);
        inOrder.verify(integrationInstanceConfigurationFacade)
            .enableIntegrationVersion(9L, 1, Environment.STAGING);
    }

    @Test
    void testCreateEmptyCodeWorkflowCreatesDraftWithoutPublishing() {
        IntegrationService integrationService = mock(IntegrationService.class);

        when(integrationService.fetchIntegration("my-empty-integration")).thenReturn(Optional.empty());

        Integration createdIntegration = new Integration();

        createdIntegration.setId(11L);
        createdIntegration.setComponentName("my-empty-integration");
        createdIntegration.setName("my-empty-integration");

        when(integrationService.create(any())).thenReturn(createdIntegration);

        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(11L)).thenReturn(Optional.empty());

        CodeWorkflowContainer newCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        CodeWorkflowReconciliation reconciliation = new CodeWorkflowReconciliation(
            newCodeWorkflowContainer, Map.of(), Map.of());

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        when(codeWorkflowContainerFacade.create(
            eq("my-empty-integration"), any(), any(), eq(Language.JAVASCRIPT), any(), eq(PlatformType.EMBEDDED),
            eq(Map.of()))).thenReturn(reconciliation);

        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);
        WorkflowService workflowService = mock(WorkflowService.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            integrationService, integrationWorkflowService, codeWorkflowContainerFacade,
            integrationCodeWorkflowService, mock(CodeWorkflowContainerService.class),
            mock(CodeWorkflowFileStorage.class), workflowService);

        Integration result = integrationCodeWorkflowFacade.createEmptyCodeWorkflow(
            "my-empty-integration", Language.JAVASCRIPT);

        assertThat(result).isEqualTo(createdIntegration);

        verify(integrationCodeWorkflowService).create(newCodeWorkflowContainer, createdIntegration);
        verify(integrationService, never()).publishIntegration(anyLong(), any());
    }

    @Test
    void testEditorSaveAddsTheIntegrationOwnConnectionToATaskThatDeclaresNone() {
        Map<String, List<ConnectionRequirement>> taskConnections = editorSaveTaskConnections(
            "{name: \"undeclared\", perform: function() { return 1; }}");

        List<ConnectionRequirement> connectionRequirements = taskConnections.get("undeclared");

        assertThat(connectionRequirements).hasSize(1);

        ConnectionRequirement connectionRequirement = connectionRequirements.getFirst();

        assertThat(connectionRequirement.getName()).isEqualTo("googleMail");
        assertThat(connectionRequirement.getComponentName()).isEqualTo("googleMail");
        assertThat(connectionRequirement.getComponentVersion()).hasValue(1);
    }

    @Test
    void testEditorSaveAddsNoSecondOwnConnectionToATaskThatDeclaresOneUnderAnyName() {
        Map<String, List<ConnectionRequirement>> taskConnections = editorSaveTaskConnections(
            "{name: \"declaresOwn\", connections: [{componentName: \"googleMail\", name: \"gmail\"}], "
                + "perform: function() { return 1; }}");

        assertThat(connectionNameComponentNames(taskConnections.get("declaresOwn")))
            .isEqualTo(Map.of("gmail", "googleMail"));
    }

    @Test
    void testEditorSaveKeepsAnotherComponentConnectionAndAddsTheOwnOne() {
        Map<String, List<ConnectionRequirement>> taskConnections = editorSaveTaskConnections(
            "{name: \"declaresOther\", connections: [{componentName: \"slack\", name: \"slack-prod\"}], "
                + "perform: function() { return 1; }}");

        assertThat(connectionNameComponentNames(taskConnections.get("declaresOther")))
            .isEqualTo(Map.of("slack-prod", "slack", "googleMail", "googleMail"));
    }

    @Test
    void testEditorSaveLeavesAConnectionNamedAfterTheComponentButPointingElsewhereAlone() {
        Map<String, List<ConnectionRequirement>> taskConnections = editorSaveTaskConnections(
            "{name: \"nameTaken\", connections: [{componentName: \"slack\", name: \"googleMail\"}], "
                + "perform: function() { return 1; }}");

        assertThat(connectionNameComponentNames(taskConnections.get("nameTaken")))
            .isEqualTo(Map.of("googleMail", "slack"));
    }

    @Test
    void testEditorSaveAddsTheOwnConnectionToTasksInsideAGroup() {
        Map<String, List<ConnectionRequirement>> taskConnections = editorSaveTaskConnections(
            "{name: \"fan-out\", type: \"parallel\", tasks: [{name: \"inParallel\", perform: function() "
                + "{ return 1; }}]}, {name: \"fork\", type: \"forkJoin\", branches: [[{name: \"inBranch\", "
                + "perform: function() { return 1; }}]]}");

        assertThat(connectionNameComponentNames(taskConnections.get("inParallel")))
            .isEqualTo(Map.of("googleMail", "googleMail"));
        assertThat(connectionNameComponentNames(taskConnections.get("inBranch")))
            .isEqualTo(Map.of("googleMail", "googleMail"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUploadAddsTheIntegrationOwnConnectionToATaskThatDeclaresNone() {
        CodeWorkflowContainerFacade codeWorkflowContainerFacade = reconcilingCodeWorkflowContainerFacade();

        IntegrationService integrationService = mock(IntegrationService.class);

        Integration createdIntegration = new Integration();

        createdIntegration.setId(9L);
        createdIntegration.setComponentName("my-integration");
        createdIntegration.setName("my-integration");

        when(integrationService.create(any())).thenReturn(createdIntegration);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            integrationService, mock(IntegrationWorkflowService.class), codeWorkflowContainerFacade,
            mock(IntegrationCodeWorkflowService.class), mock(CodeWorkflowContainerService.class),
            mock(CodeWorkflowFileStorage.class), mock(WorkflowService.class));

        integrationCodeWorkflowFacade.save(
            integrationScript("my-integration").getBytes(StandardCharsets.UTF_8), Language.JAVASCRIPT);

        ArgumentCaptor<List<WorkflowDefinition>> workflowDefinitionsCaptor = ArgumentCaptor.forClass(List.class);

        verify(codeWorkflowContainerFacade).create(
            eq("my-integration"), any(), workflowDefinitionsCaptor.capture(), eq(Language.JAVASCRIPT), any(),
            eq(PlatformType.EMBEDDED), any());

        Map<String, List<ConnectionRequirement>> taskConnections = taskConnections(
            workflowDefinitionsCaptor.getValue());

        assertThat(connectionNameComponentNames(taskConnections.get("my-task")))
            .isEqualTo(Map.of("my-integration", "my-integration"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<ConnectionRequirement>> editorSaveTaskConnections(String tasks) {
        Integration integration = new Integration();

        integration.setId(1L);
        integration.setComponentName("googleMail");
        integration.setName("googleMail");
        integration.setIntegrationVersions(List.of(new IntegrationVersion(3)));

        IntegrationCodeWorkflow latestIntegrationCodeWorkflow = mock(IntegrationCodeWorkflow.class);

        when(latestIntegrationCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);
        when(latestIntegrationCodeWorkflow.getIntegrationVersion()).thenReturn(3);

        CodeWorkflowContainer draftCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(draftCodeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);

        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(1L))
            .thenReturn(Optional.of(latestIntegrationCodeWorkflow));

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(draftCodeWorkflowContainer);

        IntegrationService integrationService = mock(IntegrationService.class);

        when(integrationService.getIntegration(1L)).thenReturn(integration);

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        when(codeWorkflowContainerFacade.update(any(), any(), any(), any(), any()))
            .thenReturn(new CodeWorkflowReconciliation(mock(CodeWorkflowContainer.class), Map.of(), Map.of()));

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            integrationService, mock(IntegrationWorkflowService.class), codeWorkflowContainerFacade,
            integrationCodeWorkflowService, codeWorkflowContainerService, mock(CodeWorkflowFileStorage.class),
            mock(WorkflowService.class));

        integrationCodeWorkflowFacade.updateCodeWorkflowSource(
            1L,
            "({componentName: \"googleMail\", componentVersion: 1, workflows: [{name: \"my-workflow\", label: "
                + "\"My Workflow\", tasks: [" + tasks + "]}]})");

        ArgumentCaptor<List<WorkflowDefinition>> workflowDefinitionsCaptor = ArgumentCaptor.forClass(List.class);

        verify(codeWorkflowContainerFacade).update(
            eq(draftCodeWorkflowContainer), any(), workflowDefinitionsCaptor.capture(), any(),
            eq(PlatformType.EMBEDDED));

        return taskConnections(workflowDefinitionsCaptor.getValue());
    }

    private static Map<String, List<ConnectionRequirement>> taskConnections(
        List<WorkflowDefinition> workflowDefinitions) {

        Map<String, List<ConnectionRequirement>> taskConnections = new HashMap<>();

        for (WorkflowDefinition workflowDefinition : workflowDefinitions) {
            List<? extends WorkflowTaskDefinition> workflowTaskDefinitions = workflowDefinition.getTasks()
                .orElseGet(List::of);

            for (WorkflowTaskDefinition workflowTaskDefinition : workflowTaskDefinitions) {
                if (workflowTaskDefinition instanceof CompositeTaskDefinition compositeTaskDefinition) {
                    for (TaskDefinition taskDefinition : compositeTaskDefinition.getTasks()) {
                        putTaskConnections(taskConnections, taskDefinition);
                    }

                    for (List<? extends TaskDefinition> branch : compositeTaskDefinition.getBranches()) {
                        for (TaskDefinition taskDefinition : branch) {
                            putTaskConnections(taskConnections, taskDefinition);
                        }
                    }
                } else {
                    putTaskConnections(taskConnections, (TaskDefinition) workflowTaskDefinition);
                }
            }
        }

        return taskConnections;
    }

    private static void putTaskConnections(
        Map<String, List<ConnectionRequirement>> taskConnections, TaskDefinition taskDefinition) {

        List<? extends ConnectionRequirement> connectionRequirements = taskDefinition.getConnections()
            .orElseGet(List::of);

        taskConnections.put(taskDefinition.getName(), List.copyOf(connectionRequirements));
    }

    private static Map<String, String> connectionNameComponentNames(
        List<ConnectionRequirement> connectionRequirements) {

        return connectionRequirements.stream()
            .collect(Collectors.toMap(ConnectionRequirement::getName, ConnectionRequirement::getComponentName));
    }

    private static IntegrationCodeWorkflowFacadeImpl newUploadFacade(
        IntegrationFacade integrationFacade,
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade) {

        IntegrationService integrationService = mock(IntegrationService.class);

        Integration createdIntegration = new Integration();

        createdIntegration.setId(9L);
        createdIntegration.setComponentName("my-integration");
        createdIntegration.setName("my-integration");

        when(integrationService.create(any())).thenReturn(createdIntegration);

        return newFacade(
            integrationService, mock(IntegrationWorkflowService.class), reconcilingCodeWorkflowContainerFacade(),
            mock(IntegrationCodeWorkflowService.class), integrationInstanceConfigurationFacade, integrationFacade,
            mock(CodeWorkflowContainerService.class), mock(CodeWorkflowFileStorage.class),
            mock(WorkflowService.class));
    }

    private static CodeWorkflowContainerFacade reconcilingCodeWorkflowContainerFacade() {
        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        when(codeWorkflowContainerFacade.create(
            eq("my-integration"), any(), any(), eq(Language.JAVASCRIPT), any(), eq(PlatformType.EMBEDDED), any()))
                .thenAnswer(invocation -> reconcile(invocation.getArgument(2), invocation.getArgument(6)));
        when(codeWorkflowContainerFacade.create(
            eq("my-integration"), any(), any(), eq(Language.JAVASCRIPT), any(), eq(PlatformType.EMBEDDED)))
                .thenAnswer(invocation -> {
                    CodeWorkflowReconciliation reconciliation = reconcile(invocation.getArgument(2), Map.of());

                    return reconciliation.codeWorkflowContainer();
                });

        return codeWorkflowContainerFacade;
    }

    private static CodeWorkflowReconciliation reconcile(
        List<? extends WorkflowDefinition> workflowDefinitions, Map<String, String> reusableWorkflowNameIds) {

        Set<String> workflowNames = workflowDefinitions.stream()
            .map(WorkflowDefinition::getName)
            .collect(Collectors.toSet());

        Map<String, String> addedWorkflowNameIds = workflowNames.stream()
            .filter(workflowName -> !reusableWorkflowNameIds.containsKey(workflowName))
            .collect(Collectors.toMap(Function.identity(), workflowName -> "new-" + workflowName));

        Map<String, String> removedWorkflowNameIds = reusableWorkflowNameIds.entrySet()
            .stream()
            .filter(entry -> !workflowNames.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, String> workflowNameIds = workflowNames.stream()
            .collect(Collectors.toMap(Function.identity(),
                workflowName -> reusableWorkflowNameIds.getOrDefault(workflowName, "new-" + workflowName)));

        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(codeWorkflowContainer.getWorkflowNameIds()).thenReturn(workflowNameIds);

        return new CodeWorkflowReconciliation(codeWorkflowContainer, addedWorkflowNameIds, removedWorkflowNameIds);
    }

    private static String integrationScript(String componentName) {
        return integrationScript(componentName, "my-workflow");
    }

    private static String integrationScript(String componentName, String... workflowNames) {
        String workflows = Arrays.stream(workflowNames)
            .map(workflowName -> "{name: \"" + workflowName + "\", label: \"" + workflowName + "\", tasks: [{name: "
                + "\"my-task\", label: \"My Task\", perform: function() { return \"hello\"; }}]}")
            .collect(Collectors.joining(", "));

        return "({componentName: \"" + componentName + "\", componentVersion: 1, workflows: [" + workflows + "]})";
    }

    private static IntegrationCodeWorkflowFacadeImpl newFacade(
        IntegrationService integrationService, IntegrationWorkflowService integrationWorkflowService,
        CodeWorkflowContainerFacade codeWorkflowContainerFacade,
        IntegrationCodeWorkflowService integrationCodeWorkflowService,
        CodeWorkflowContainerService codeWorkflowContainerService, CodeWorkflowFileStorage codeWorkflowFileStorage,
        WorkflowService workflowService) {

        return newFacade(
            integrationService, integrationWorkflowService, codeWorkflowContainerFacade,
            integrationCodeWorkflowService, mock(IntegrationInstanceConfigurationFacade.class),
            mock(IntegrationFacade.class), codeWorkflowContainerService, codeWorkflowFileStorage, workflowService);
    }

    private static IntegrationCodeWorkflowFacadeImpl newFacade(
        IntegrationService integrationService, IntegrationWorkflowService integrationWorkflowService,
        CodeWorkflowContainerFacade codeWorkflowContainerFacade,
        IntegrationCodeWorkflowService integrationCodeWorkflowService,
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade,
        IntegrationFacade integrationFacade, CodeWorkflowContainerService codeWorkflowContainerService,
        CodeWorkflowFileStorage codeWorkflowFileStorage, WorkflowService workflowService) {

        return new IntegrationCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), codeWorkflowContainerFacade,
            integrationCodeWorkflowService, integrationInstanceConfigurationFacade, integrationFacade,
            integrationService,
            integrationWorkflowService, codeWorkflowContainerService, codeWorkflowFileStorage, mock(TagService.class),
            workflowService, List.of());
    }

    private static ApplicationProperties applicationProperties(boolean javaEnabled) {
        ApplicationProperties.Workflow workflow = new ApplicationProperties.Workflow();

        workflow.getCodeWorkflow()
            .setJavaEnabled(javaEnabled);

        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getWorkflow()).thenReturn(workflow);

        return applicationProperties;
    }
}
