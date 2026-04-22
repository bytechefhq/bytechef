/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayModelService;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProviderService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceAiGatewayModelFacadeTest {

    @Mock
    private AiGatewayModelService aiGatewayModelService;

    @Mock
    private WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService;

    private WorkspaceAiGatewayModelFacadeImpl workspaceModelFacade;

    @BeforeEach
    void setUp() {
        workspaceModelFacade = new WorkspaceAiGatewayModelFacadeImpl(
            aiGatewayModelService, workspaceAiGatewayProviderService);
    }

    @Test
    void testCreateWorkspaceModelSucceedsWhenProviderBelongsToWorkspace() {
        WorkspaceAiGatewayProvider workspaceProvider = new WorkspaceAiGatewayProvider(5L, 1L);
        AiGatewayModel createdModel = new AiGatewayModel(5L, "gpt-4");

        ReflectionTestUtils.setField(createdModel, "id", 100L);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(workspaceProvider));
        when(aiGatewayModelService.create(any())).thenReturn(createdModel);

        AiGatewayModel result = workspaceModelFacade.createWorkspaceModel(
            1L, 5L, "gpt-4", null, 128000, null, null, null);

        assertNotNull(result);
        assertEquals("gpt-4", result.getName());

        verify(aiGatewayModelService).create(any());
    }

    @Test
    void testCreateWorkspaceModelRejectsProviderFromDifferentWorkspace() {
        WorkspaceAiGatewayProvider workspaceProvider = new WorkspaceAiGatewayProvider(5L, 1L);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(workspaceProvider));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> workspaceModelFacade.createWorkspaceModel(
                1L, 999L, "gpt-4", null, null, null, null, null));

        assertTrue(exception.getMessage()
            .contains("does not belong to workspace"));

        verify(aiGatewayModelService, never()).create(any());
    }

    @Test
    void testDeleteWorkspaceModelSucceedsWhenModelBelongsToWorkspace() {
        WorkspaceAiGatewayProvider workspaceProvider = new WorkspaceAiGatewayProvider(5L, 1L);
        AiGatewayModel model = new AiGatewayModel(5L, "gpt-4");

        ReflectionTestUtils.setField(model, "id", 100L);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(workspaceProvider));
        when(aiGatewayModelService.getModel(100L)).thenReturn(model);

        workspaceModelFacade.deleteWorkspaceModel(1L, 100L);

        verify(aiGatewayModelService).delete(100L);
    }

    @Test
    void testDeleteWorkspaceModelRejectsModelFromDifferentWorkspace() {
        WorkspaceAiGatewayProvider workspaceProvider = new WorkspaceAiGatewayProvider(5L, 1L);
        AiGatewayModel model = new AiGatewayModel(999L, "gpt-4");

        ReflectionTestUtils.setField(model, "id", 100L);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(workspaceProvider));
        when(aiGatewayModelService.getModel(100L)).thenReturn(model);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> workspaceModelFacade.deleteWorkspaceModel(1L, 100L));

        assertTrue(exception.getMessage()
            .contains("does not belong to workspace"));

        verify(aiGatewayModelService, never()).delete(any(Long.class));
    }

    @Test
    void testGetWorkspaceModelsReturnsOnlyModelsFromWorkspaceProviders() {
        WorkspaceAiGatewayProvider workspaceProvider = new WorkspaceAiGatewayProvider(5L, 1L);
        AiGatewayModel model1 = new AiGatewayModel(5L, "gpt-4");
        AiGatewayModel model2 = new AiGatewayModel(5L, "gpt-3.5");

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(workspaceProvider));
        when(aiGatewayModelService.getModelsByProviderId(5L))
            .thenReturn(List.of(model1, model2));

        List<AiGatewayModel> models = workspaceModelFacade.getWorkspaceModels(1L);

        assertEquals(2, models.size());
    }

    @Test
    void testGetWorkspaceModelsReturnsEmptyWhenNoProviders() {
        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of());

        List<AiGatewayModel> models = workspaceModelFacade.getWorkspaceModels(1L);

        assertTrue(models.isEmpty());
    }
}
