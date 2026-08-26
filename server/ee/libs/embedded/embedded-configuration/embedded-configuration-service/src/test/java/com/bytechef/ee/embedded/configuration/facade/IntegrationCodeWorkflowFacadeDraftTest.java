/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.service.TagService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;

/**
 * Verifies that the editor save paths on {@link IntegrationCodeWorkflowFacadeImpl}
 * ({@link IntegrationCodeWorkflowFacadeImpl#updateCodeWorkflowSource} and
 * {@link IntegrationCodeWorkflowFacadeImpl#createEmptyCodeWorkflow}) always persist a draft and never publish, while
 * the upload path ({@link IntegrationCodeWorkflowFacadeImpl#save}) keeps its deploy-and-publish semantics.
 *
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
    void testUploadSaveStillPublishes() {
        IntegrationService integrationService = mock(IntegrationService.class);

        when(integrationService.fetchIntegration("my-integration")).thenReturn(Optional.empty());

        Integration createdIntegration = new Integration();

        createdIntegration.setId(9L);
        createdIntegration.setComponentName("my-integration");
        createdIntegration.setName("my-integration");

        when(integrationService.create(any())).thenReturn(createdIntegration);

        CodeWorkflowContainer uploadedCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(uploadedCodeWorkflowContainer.getWorkflowNameIds()).thenReturn(Map.of("my-workflow", "wf-id-1"));

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        when(codeWorkflowContainerFacade.create(
            eq("my-integration"), any(), any(), eq(Language.JAVASCRIPT), any(), eq(PlatformType.EMBEDDED)))
                .thenReturn(uploadedCodeWorkflowContainer);

        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);
        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);
        WorkflowService workflowService = mock(WorkflowService.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            integrationService, integrationWorkflowService, codeWorkflowContainerFacade,
            integrationCodeWorkflowService, mock(CodeWorkflowContainerService.class),
            mock(CodeWorkflowFileStorage.class), workflowService);

        integrationCodeWorkflowFacade.save(
            integrationScript("my-integration").getBytes(StandardCharsets.UTF_8), Language.JAVASCRIPT);

        verify(codeWorkflowContainerFacade).create(
            eq("my-integration"), any(), any(), eq(Language.JAVASCRIPT), any(), eq(PlatformType.EMBEDDED));
        verify(integrationCodeWorkflowService).create(uploadedCodeWorkflowContainer, createdIntegration);
        verify(integrationWorkflowService).addWorkflow(9L, 1, "wf-id-1");
        verify(integrationService).publishIntegration(9L, null);
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

    private static String integrationScript(String componentName) {
        return "({componentName: \"" + componentName + "\", componentVersion: 1, workflows: [{name: "
            + "\"my-workflow\", label: \"My Workflow\", tasks: [{name: \"my-task\", label: \"My Task\", "
            + "perform: function() { return \"hello\"; }}]}]})";
    }

    private static IntegrationCodeWorkflowFacadeImpl newFacade(
        IntegrationService integrationService, IntegrationWorkflowService integrationWorkflowService,
        CodeWorkflowContainerFacade codeWorkflowContainerFacade,
        IntegrationCodeWorkflowService integrationCodeWorkflowService,
        CodeWorkflowContainerService codeWorkflowContainerService, CodeWorkflowFileStorage codeWorkflowFileStorage,
        WorkflowService workflowService) {

        return new IntegrationCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), codeWorkflowContainerFacade,
            integrationCodeWorkflowService, integrationService, integrationWorkflowService,
            codeWorkflowContainerService, codeWorkflowFileStorage, mock(TagService.class), workflowService, List.of());
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
