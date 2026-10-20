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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationCodeWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationCodeWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
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
import org.springframework.beans.factory.ObjectProvider;

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
    private IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade;

    @Mock
    private ObjectProvider<IntegrationCodeWorkflowFacade> integrationCodeWorkflowFacadeProvider;

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
            integrationCodeWorkflowFacadeProvider, integrationCodeWorkflowService, integrationService,
            mock(IntegrationVersionWorkflowChangeFacade.class), integrationWorkflowService,
            integrationInstanceConfigurationFacade, integrationInstanceConfigurationService,
            mock(IntegrationInstanceService.class), mock(PermissionService.class), tagService, workflowService,
            workflowTestConfigurationService, workflowNodeTestOutputService, List.of());

        integration = Integration.builder()
            .id(INTEGRATION_ID)
            .componentName("slack")
            .componentVersion(1)
            .name("Slack Integration")
            .build();

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        lenient().when(integrationService.getIntegration(INTEGRATION_ID))
            .thenReturn(integration);
        lenient().when(componentDefinitionService.fetchComponentDefinition("slack", null))
            .thenReturn(Optional.of(componentDefinition));
        lenient().when(integrationWorkflowService.getIntegrationWorkflowIds(anyLong(), anyInt()))
            .thenReturn(List.of());
        lenient().when(tagService.getTags(any()))
            .thenReturn(List.of());
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

    @Test
    void testUpdateIntegrationWithANewNameRewritesTheCodeWorkflowSource() {
        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(INTEGRATION_ID))
            .thenReturn(Optional.of(mock(IntegrationCodeWorkflow.class)));
        when(integrationCodeWorkflowFacadeProvider.getObject()).thenReturn(integrationCodeWorkflowFacade);

        integrationFacade.updateIntegration(renamedIntegrationDTO("Slack Sync"));

        verify(integrationService).update(any(Integration.class));
        verify(integrationCodeWorkflowFacade).renameCodeWorkflowSource(INTEGRATION_ID);
    }

    @Test
    void testUpdateIntegrationWithTheSameNameLeavesTheCodeWorkflowSource() {
        integrationFacade.updateIntegration(renamedIntegrationDTO("Slack Integration"));

        verify(integrationService).update(any(Integration.class));
        verifyNoInteractions(integrationCodeWorkflowFacadeProvider);
    }

    @Test
    void testUpdateIntegrationWithANewNameLeavesANonCodeIntegrationAlone() {
        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(INTEGRATION_ID))
            .thenReturn(Optional.empty());

        integrationFacade.updateIntegration(renamedIntegrationDTO("Slack Sync"));

        verifyNoInteractions(integrationCodeWorkflowFacadeProvider);
    }

    @Test
    void testRestoreIntegrationVersionRestoresTheCodeWorkflowSource() {
        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(INTEGRATION_ID))
            .thenReturn(Optional.of(mock(IntegrationCodeWorkflow.class)));
        when(integrationCodeWorkflowFacadeProvider.getObject()).thenReturn(integrationCodeWorkflowFacade);

        integrationFacade.restoreIntegrationVersion(INTEGRATION_ID, 2);

        verify(integrationService).getPublishedIntegrationVersion(INTEGRATION_ID, 2);
        verify(integrationCodeWorkflowFacade).restoreCodeWorkflowSource(INTEGRATION_ID, 2);
        verifyNoInteractions(integrationWorkflowService, workflowService);
    }

    @Test
    void testRestoreIntegrationVersionLeavesANonCodeIntegrationToTheWorkflowCopy() {
        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(INTEGRATION_ID))
            .thenReturn(Optional.empty());

        integration.setIntegrationVersions(List.of(new IntegrationVersion(3)));

        integrationFacade.restoreIntegrationVersion(INTEGRATION_ID, 2);

        verify(integrationWorkflowService).getIntegrationWorkflows(INTEGRATION_ID, 2);
        verifyNoInteractions(integrationCodeWorkflowFacadeProvider);
    }

    private static IntegrationDTO renamedIntegrationDTO(String name) {
        return IntegrationDTO.builder()
            .componentName("slack")
            .id(INTEGRATION_ID)
            .name(name)
            .build();
    }
}
