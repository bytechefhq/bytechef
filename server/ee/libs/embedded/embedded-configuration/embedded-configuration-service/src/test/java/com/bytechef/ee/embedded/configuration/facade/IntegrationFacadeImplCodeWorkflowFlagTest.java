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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationCodeWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationCodeWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class IntegrationFacadeImplCodeWorkflowFlagTest {

    private static final long INTEGRATION_ID = 42L;

    @Mock
    private CategoryService categoryService;

    @Mock
    private CodeWorkflowContainerService codeWorkflowContainerService;

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private IntegrationCodeWorkflowService integrationCodeWorkflowService;

    @Mock
    private IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;

    @Mock
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Mock
    private IntegrationService integrationService;

    @Mock
    private IntegrationWorkflowService integrationWorkflowService;

    @Mock
    private TagService tagService;

    @Mock
    private WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private Integration integration;
    private IntegrationFacadeImpl integrationFacade;

    @BeforeEach
    void setUp() {
        integrationFacade = new IntegrationFacadeImpl(
            categoryService, codeWorkflowContainerService, componentDefinitionService,
            integrationCodeWorkflowService, integrationService, integrationWorkflowService,
            integrationInstanceConfigurationFacade, integrationInstanceConfigurationService, tagService,
            workflowService, workflowTestConfigurationService, workflowNodeTestOutputService, List.of());

        integration = Integration.builder()
            .id(INTEGRATION_ID)
            .componentName("slack")
            .componentVersion(1)
            .name("Slack Integration")
            .build();

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        // Shared setup: the list-path test exercises getIntegrations instead, so this stub is
        // lenient rather than duplicated into each by-id test
        lenient().when(integrationService.getIntegration(INTEGRATION_ID))
            .thenReturn(integration);
        when(componentDefinitionService.fetchComponentDefinition("slack", null))
            .thenReturn(Optional.of(componentDefinition));
        when(integrationWorkflowService.getIntegrationWorkflowIds(anyLong(), anyInt())).thenReturn(List.of());
        when(tagService.getTags(any())).thenReturn(List.of());
    }

    @Test
    void testGetIntegrationWithoutCodeWorkflowJoinReturnsFalse() {
        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(INTEGRATION_ID))
            .thenReturn(Optional.empty());

        IntegrationDTO integrationDTO = integrationFacade.getIntegration(INTEGRATION_ID);

        assertThat(integrationDTO.codeWorkflow()).isFalse();
        assertThat(integrationDTO.codeWorkflowLanguage()).isNull();
    }

    @Test
    void testGetIntegrationWithCodeWorkflowJoinReturnsLanguage() {
        IntegrationCodeWorkflow integrationCodeWorkflow = mock(IntegrationCodeWorkflow.class);
        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(INTEGRATION_ID))
            .thenReturn(Optional.of(integrationCodeWorkflow));
        when(integrationCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(7L);
        when(codeWorkflowContainerService.getCodeWorkflowContainer(7L)).thenReturn(codeWorkflowContainer);
        when(codeWorkflowContainer.getLanguage()).thenReturn(CodeWorkflowContainer.Language.PYTHON);

        IntegrationDTO integrationDTO = integrationFacade.getIntegration(INTEGRATION_ID);

        assertThat(integrationDTO.codeWorkflow()).isTrue();
        assertThat(integrationDTO.codeWorkflowLanguage()).isEqualTo("PYTHON");
    }

    @Test
    void testGetIntegrationsReturnsLanguageForCodeIntegrations() {
        IntegrationCodeWorkflow integrationCodeWorkflow = mock(IntegrationCodeWorkflow.class);
        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(integrationService.getIntegrations(null, List.of(), null, null)).thenReturn(List.of(integration));
        when(integrationCodeWorkflowService.getCodeWorkflowIntegrationIds()).thenReturn(List.of(INTEGRATION_ID));
        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(INTEGRATION_ID))
            .thenReturn(Optional.of(integrationCodeWorkflow));
        when(integrationCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(7L);
        when(codeWorkflowContainerService.getCodeWorkflowContainer(7L)).thenReturn(codeWorkflowContainer);
        when(codeWorkflowContainer.getLanguage()).thenReturn(CodeWorkflowContainer.Language.JAVASCRIPT);
        when(categoryService.getCategories(List.of())).thenReturn(List.of());

        List<IntegrationDTO> integrationDTOs = integrationFacade.getIntegrations(null, false, null, null, true);

        // The list path used to report a null language, so the client's badge fell back to a bare
        // "Code" rather than naming the language
        assertThat(integrationDTOs).hasSize(1);
        assertThat(integrationDTOs.getFirst()
            .codeWorkflowLanguage()).isEqualTo("JAVASCRIPT");
    }

    @Test
    void testGetIntegrationWithMissingCodeWorkflowContainerReturnsFalse() {
        IntegrationCodeWorkflow integrationCodeWorkflow = mock(IntegrationCodeWorkflow.class);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(INTEGRATION_ID))
            .thenReturn(Optional.of(integrationCodeWorkflow));
        when(integrationCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(7L);
        when(codeWorkflowContainerService.getCodeWorkflowContainer(7L))
            .thenThrow(new IllegalArgumentException("CodeWorkflowContainer not found"));

        IntegrationDTO integrationDTO = integrationFacade.getIntegration(INTEGRATION_ID);

        assertThat(integrationDTO.codeWorkflow()).isFalse();
        assertThat(integrationDTO.codeWorkflowLanguage()).isNull();
    }
}
