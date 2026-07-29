/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationCodeWorkflow;
import com.bytechef.ee.embedded.configuration.exception.CodeWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.service.IntegrationCodeWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;

/**
 * Verifies {@link IntegrationCodeWorkflowFacadeImpl#getCodeWorkflowSource} and
 * {@link IntegrationCodeWorkflowFacadeImpl#updateCodeWorkflowSource}.
 *
 * <p>
 * {@code getCodeWorkflowSource} returns the stored script for a polyglot-backed container and rejects Java containers
 * since compiled jars have no editable source. {@code updateCodeWorkflowSource} compile-gates the incoming content
 * through the real JavaScript loader (an unparseable script fails the deploy before anything is persisted) and
 * name-locks it against the owning integration's component name (editing source cannot rename the integration).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationCodeWorkflowFacadeSourceTest {

    @Test
    void testGetCodeWorkflowSourceReturnsSourceForNonJavaLanguage() {
        FileEntry workflowsFile = new FileEntry("my-integration.js", "file:///my-integration.js");

        IntegrationCodeWorkflow integrationCodeWorkflow = mock(IntegrationCodeWorkflow.class);

        when(integrationCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);

        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(codeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);
        when(codeWorkflowContainer.getWorkflows()).thenReturn(workflowsFile);

        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(42L))
            .thenReturn(Optional.of(integrationCodeWorkflow));

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(codeWorkflowContainer);

        CodeWorkflowFileStorage codeWorkflowFileStorage = mock(CodeWorkflowFileStorage.class);

        when(codeWorkflowFileStorage.readCodeWorkflowFileContent(workflowsFile))
            .thenReturn("({componentName:'my-integration'})");

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            mock(IntegrationService.class), mock(IntegrationWorkflowService.class),
            mock(CodeWorkflowContainerFacade.class), integrationCodeWorkflowService, codeWorkflowContainerService,
            codeWorkflowFileStorage);

        String source = integrationCodeWorkflowFacade.getCodeWorkflowSource(42L);

        assertThat(source).isEqualTo("({componentName:'my-integration'})");
    }

    @Test
    void testGetCodeWorkflowSourceThrowsForJavaLanguage() {
        IntegrationCodeWorkflow integrationCodeWorkflow = mock(IntegrationCodeWorkflow.class);

        when(integrationCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);

        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(codeWorkflowContainer.getLanguage()).thenReturn(Language.JAVA);

        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(42L))
            .thenReturn(Optional.of(integrationCodeWorkflow));

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(codeWorkflowContainer);

        CodeWorkflowFileStorage codeWorkflowFileStorage = mock(CodeWorkflowFileStorage.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            mock(IntegrationService.class), mock(IntegrationWorkflowService.class),
            mock(CodeWorkflowContainerFacade.class), integrationCodeWorkflowService, codeWorkflowContainerService,
            codeWorkflowFileStorage);

        assertThatThrownBy(() -> integrationCodeWorkflowFacade.getCodeWorkflowSource(42L))
            .isInstanceOf(ConfigurationException.class)
            .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
            .isEqualTo(CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED.getErrorKey());

        verifyNoInteractions(codeWorkflowFileStorage);
    }

    @Test
    void testGetCodeWorkflowSourceThrowsWhenNoCodeWorkflowExists() {
        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(42L)).thenReturn(Optional.empty());

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);
        CodeWorkflowFileStorage codeWorkflowFileStorage = mock(CodeWorkflowFileStorage.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            mock(IntegrationService.class), mock(IntegrationWorkflowService.class),
            mock(CodeWorkflowContainerFacade.class), integrationCodeWorkflowService, codeWorkflowContainerService,
            codeWorkflowFileStorage);

        assertThatThrownBy(() -> integrationCodeWorkflowFacade.getCodeWorkflowSource(42L))
            .isInstanceOf(ConfigurationException.class)
            .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
            .isEqualTo(CodeWorkflowErrorType.SOURCE_LOAD_FAILED.getErrorKey());

        verifyNoInteractions(codeWorkflowContainerService, codeWorkflowFileStorage);
    }

    @Test
    void testUpdateCodeWorkflowSourceWithSameComponentNameRedeploys() {
        IntegrationCodeWorkflow integrationCodeWorkflow = mock(IntegrationCodeWorkflow.class);

        when(integrationCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);

        CodeWorkflowContainer existingCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(existingCodeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);

        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(1L))
            .thenReturn(Optional.of(integrationCodeWorkflow));

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(existingCodeWorkflowContainer);

        Integration integration = new Integration();

        integration.setId(1L);
        integration.setComponentName("my-integration");
        integration.setName("my-integration");

        IntegrationService integrationService = mock(IntegrationService.class);

        when(integrationService.getIntegration(1L)).thenReturn(integration);

        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);

        CodeWorkflowContainer redeployedCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(redeployedCodeWorkflowContainer.getWorkflowNameIds())
            .thenReturn(Map.of("my-workflow", UUID.randomUUID()
                .toString()));

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        when(codeWorkflowContainerFacade.create(any(), any(), any(), eq(Language.JAVASCRIPT), any(), any()))
            .thenReturn(redeployedCodeWorkflowContainer);

        CodeWorkflowFileStorage codeWorkflowFileStorage = mock(CodeWorkflowFileStorage.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            integrationService, integrationWorkflowService, codeWorkflowContainerFacade,
            integrationCodeWorkflowService, codeWorkflowContainerService, codeWorkflowFileStorage);

        integrationCodeWorkflowFacade.updateCodeWorkflowSource(1L, integrationScript("my-integration"));

        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);

        verify(codeWorkflowContainerFacade).create(
            eq("my-integration"), any(), any(), eq(Language.JAVASCRIPT), bytesCaptor.capture(), any());

        assertThat(new String(bytesCaptor.getValue(), StandardCharsets.UTF_8))
            .isEqualTo(integrationScript("my-integration"));

        verify(integrationCodeWorkflowService).create(redeployedCodeWorkflowContainer, integration);
        verify(integrationService).publishIntegration(1L, null);
    }

    @Test
    void testUpdateCodeWorkflowSourceWithUnparseableScriptThrowsSourceLoadFailed() {
        IntegrationCodeWorkflow integrationCodeWorkflow = mock(IntegrationCodeWorkflow.class);

        when(integrationCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);

        CodeWorkflowContainer existingCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(existingCodeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);

        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(1L))
            .thenReturn(Optional.of(integrationCodeWorkflow));

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(existingCodeWorkflowContainer);

        Integration integration = new Integration();

        integration.setId(1L);
        integration.setComponentName("my-integration");
        integration.setName("my-integration");

        IntegrationService integrationService = mock(IntegrationService.class);

        when(integrationService.getIntegration(1L)).thenReturn(integration);

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            integrationService, mock(IntegrationWorkflowService.class), codeWorkflowContainerFacade,
            integrationCodeWorkflowService, codeWorkflowContainerService, mock(CodeWorkflowFileStorage.class));

        assertThatThrownBy(
            () -> integrationCodeWorkflowFacade.updateCodeWorkflowSource(1L, "this is not { valid javascript ("))
                .isInstanceOf(ConfigurationException.class)
                .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                .isEqualTo(CodeWorkflowErrorType.SOURCE_LOAD_FAILED.getErrorKey());

        verifyNoInteractions(codeWorkflowContainerFacade);
    }

    @Test
    void testUpdateCodeWorkflowSourceWithDifferentComponentNameThrowsCodeWorkflowNameMismatch() {
        IntegrationCodeWorkflow integrationCodeWorkflow = mock(IntegrationCodeWorkflow.class);

        when(integrationCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);

        CodeWorkflowContainer existingCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(existingCodeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);

        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(1L))
            .thenReturn(Optional.of(integrationCodeWorkflow));

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(existingCodeWorkflowContainer);

        Integration integration = new Integration();

        integration.setId(1L);
        integration.setComponentName("my-integration");
        integration.setName("my-integration");

        IntegrationService integrationService = mock(IntegrationService.class);

        when(integrationService.getIntegration(1L)).thenReturn(integration);

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            integrationService, mock(IntegrationWorkflowService.class), codeWorkflowContainerFacade,
            integrationCodeWorkflowService, codeWorkflowContainerService, mock(CodeWorkflowFileStorage.class));

        assertThatThrownBy(() -> integrationCodeWorkflowFacade.updateCodeWorkflowSource(
            1L, integrationScript("a-completely-different-name")))
                .isInstanceOf(ConfigurationException.class)
                .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                .isEqualTo(CodeWorkflowErrorType.CODE_WORKFLOW_NAME_MISMATCH.getErrorKey());

        verifyNoInteractions(codeWorkflowContainerFacade);
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
        CodeWorkflowContainerService codeWorkflowContainerService, CodeWorkflowFileStorage codeWorkflowFileStorage) {

        return new IntegrationCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), codeWorkflowContainerFacade,
            integrationCodeWorkflowService, integrationService, integrationWorkflowService,
            codeWorkflowContainerService, codeWorkflowFileStorage);
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
