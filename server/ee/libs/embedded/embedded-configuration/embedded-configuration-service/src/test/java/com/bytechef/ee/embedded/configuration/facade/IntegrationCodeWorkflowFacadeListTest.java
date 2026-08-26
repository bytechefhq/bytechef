/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationCodeWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationCodeWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

/**
 * Verifies {@link IntegrationCodeWorkflowFacadeImpl#getCodeWorkflowIntegrations} and
 * {@link IntegrationCodeWorkflowFacadeImpl#getCodeWorkflowLanguage}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationCodeWorkflowFacadeListTest {

    @Test
    void testGetCodeWorkflowIntegrationsReturnsIntegrationsForDistinctIds() {
        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        when(integrationCodeWorkflowService.getCodeWorkflowIntegrationIds()).thenReturn(List.of(1L, 2L));

        Integration firstIntegration = new Integration();

        firstIntegration.setId(1L);
        firstIntegration.setComponentName("first-integration");

        Integration secondIntegration = new Integration();

        secondIntegration.setId(2L);
        secondIntegration.setComponentName("second-integration");

        IntegrationService integrationService = mock(IntegrationService.class);

        when(integrationService.getIntegrations(List.of(1L, 2L)))
            .thenReturn(List.of(firstIntegration, secondIntegration));

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            integrationService, integrationCodeWorkflowService, mock(CodeWorkflowContainerService.class));

        List<Integration> integrations = integrationCodeWorkflowFacade.getCodeWorkflowIntegrations();

        assertThat(integrations).containsExactly(firstIntegration, secondIntegration);
    }

    @Test
    void testGetCodeWorkflowIntegrationsReturnsEmptyListWhenNoCodeWorkflowsExist() {
        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        when(integrationCodeWorkflowService.getCodeWorkflowIntegrationIds()).thenReturn(List.of());

        IntegrationService integrationService = mock(IntegrationService.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            integrationService, integrationCodeWorkflowService, mock(CodeWorkflowContainerService.class));

        List<Integration> integrations = integrationCodeWorkflowFacade.getCodeWorkflowIntegrations();

        assertThat(integrations).isEmpty();

        verify(integrationService, never()).getIntegrations(anyList());
    }

    @Test
    void testGetCodeWorkflowLanguageReturnsLanguageWhenCodeWorkflowExists() {
        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        CodeWorkflowContainer storedCodeWorkflowContainer = new CodeWorkflowContainer(UUID.randomUUID());

        storedCodeWorkflowContainer.setId(5L);

        IntegrationCodeWorkflow integrationCodeWorkflow = new IntegrationCodeWorkflow();

        integrationCodeWorkflow.setCodeWorkflowContainer(storedCodeWorkflowContainer);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(1L))
            .thenReturn(Optional.of(integrationCodeWorkflow));

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(codeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);
        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(codeWorkflowContainer);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            mock(IntegrationService.class), integrationCodeWorkflowService, codeWorkflowContainerService);

        Optional<String> language = integrationCodeWorkflowFacade.getCodeWorkflowLanguage(1L);

        assertThat(language).contains("JAVASCRIPT");
    }

    @Test
    void testGetCodeWorkflowLanguageReturnsEmptyWhenCodeWorkflowContainerIsOrphaned() {
        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        CodeWorkflowContainer storedCodeWorkflowContainer = new CodeWorkflowContainer(UUID.randomUUID());

        storedCodeWorkflowContainer.setId(5L);

        IntegrationCodeWorkflow integrationCodeWorkflow = new IntegrationCodeWorkflow();

        integrationCodeWorkflow.setCodeWorkflowContainer(storedCodeWorkflowContainer);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(1L))
            .thenReturn(Optional.of(integrationCodeWorkflow));

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L))
            .thenThrow(new IllegalArgumentException("Code workflow container not found"));

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            mock(IntegrationService.class), integrationCodeWorkflowService, codeWorkflowContainerService);

        Optional<String> language = integrationCodeWorkflowFacade.getCodeWorkflowLanguage(1L);

        assertThat(language).isEmpty();
    }

    @Test
    void testGetCodeWorkflowLanguageReturnsEmptyWhenNoCodeWorkflowExists() {
        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(1L)).thenReturn(Optional.empty());

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = newFacade(
            mock(IntegrationService.class), integrationCodeWorkflowService, mock(CodeWorkflowContainerService.class));

        Optional<String> language = integrationCodeWorkflowFacade.getCodeWorkflowLanguage(1L);

        assertThat(language).isEmpty();
    }

    private static IntegrationCodeWorkflowFacadeImpl newFacade(
        IntegrationService integrationService, IntegrationCodeWorkflowService integrationCodeWorkflowService,
        CodeWorkflowContainerService codeWorkflowContainerService) {

        return new IntegrationCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), mock(CodeWorkflowContainerFacade.class),
            integrationCodeWorkflowService, integrationService, mock(IntegrationWorkflowService.class),
            codeWorkflowContainerService, mock(CodeWorkflowFileStorage.class), mock(TagService.class),
            mock(WorkflowService.class), List.of());
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
