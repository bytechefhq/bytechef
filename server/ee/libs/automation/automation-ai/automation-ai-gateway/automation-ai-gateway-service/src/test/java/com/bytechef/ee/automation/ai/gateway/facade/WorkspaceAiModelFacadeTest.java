/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProviderService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.ee.platform.ai.model.catalog.service.AiModelService;
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
class WorkspaceAiModelFacadeTest {

    @Mock
    private AiModelService aiModelService;

    @Mock
    private WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService;

    private WorkspaceAiModelFacadeImpl workspaceModelFacade;

    @BeforeEach
    void setUp() {
        workspaceModelFacade = new WorkspaceAiModelFacadeImpl(
            aiModelService, workspaceAiGatewayProviderService);
    }

    @Test
    void testCreateWorkspaceModelSucceedsWhenProviderBelongsToWorkspace() {
        AiGatewayProvider workspaceProvider = workspaceProvider();
        AiModel createdModel = new AiModel(5L, "gpt-4");

        ReflectionTestUtils.setField(createdModel, "id", 100L);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(workspaceProvider));
        when(aiModelService.create(any())).thenReturn(createdModel);

        AiModel result = workspaceModelFacade.createWorkspaceModel(
            1L, 5L, "gpt-4", null, 128000, null, null, null, null);

        assertNotNull(result);
        assertEquals("gpt-4", result.getName());

        verify(aiModelService).create(any());
    }

    @Test
    void testCreateWorkspaceModelRejectsProviderFromDifferentWorkspace() {
        AiGatewayProvider workspaceProvider = workspaceProvider();

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(workspaceProvider));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> workspaceModelFacade.createWorkspaceModel(
                1L, 999L, "gpt-4", null, null, null, null, null, null));

        assertTrue(exception.getMessage()
            .contains("does not belong to workspace"));

        verify(aiModelService, never()).create(any());
    }

    @Test
    void testCreateWorkspaceModelRetainsRoutingPolicyWhenProvided() {
        AiGatewayProvider workspaceProvider = workspaceProvider();

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(workspaceProvider));
        when(aiModelService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiModel result = workspaceModelFacade.createWorkspaceModel(
            1L, 5L, "gpt-4", null, 128000, null, null, null, 42L);

        assertNotNull(result);
        assertEquals(42L, result.getDefaultRoutingPolicyId());

        verify(aiModelService).create(any());
    }

    @Test
    void testCreateWorkspaceModelAcceptsNullRoutingPolicy() {
        AiGatewayProvider workspaceProvider = workspaceProvider();

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(workspaceProvider));
        when(aiModelService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiModel result = workspaceModelFacade.createWorkspaceModel(
            1L, 5L, "gpt-4", null, 128000, null, null, null, null);

        assertNotNull(result);
        assertNull(result.getDefaultRoutingPolicyId());

        verify(aiModelService).create(any());
    }

    @Test
    void testDeleteWorkspaceModelSucceedsWhenModelBelongsToWorkspace() {
        AiGatewayProvider workspaceProvider = workspaceProvider();
        AiModel model = new AiModel(5L, "gpt-4");

        ReflectionTestUtils.setField(model, "id", 100L);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(workspaceProvider));
        when(aiModelService.getModel(100L)).thenReturn(model);

        workspaceModelFacade.deleteWorkspaceModel(1L, 100L);

        verify(aiModelService).delete(100L);
    }

    @Test
    void testDeleteWorkspaceModelRejectsModelFromDifferentWorkspace() {
        AiGatewayProvider workspaceProvider = workspaceProvider();
        AiModel model = new AiModel(999L, "gpt-4");

        ReflectionTestUtils.setField(model, "id", 100L);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(workspaceProvider));
        when(aiModelService.getModel(100L)).thenReturn(model);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> workspaceModelFacade.deleteWorkspaceModel(1L, 100L));

        assertTrue(exception.getMessage()
            .contains("does not belong to workspace"));

        verify(aiModelService, never()).delete(any(Long.class));
    }

    @Test
    void testGetWorkspaceModelsReturnsOnlyModelsFromWorkspaceProviders() {
        AiGatewayProvider workspaceProvider = workspaceProvider();
        AiModel model1 = new AiModel(5L, "gpt-4");
        AiModel model2 = new AiModel(5L, "gpt-3.5");

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(workspaceProvider));
        when(aiModelService.getModelsByProviderId(5L))
            .thenReturn(List.of(model1, model2));

        List<AiModel> models = workspaceModelFacade.getWorkspaceModels(1L);

        assertEquals(2, models.size());
    }

    @Test
    void testGetWorkspaceModelsReturnsEmptyWhenNoProviders() {
        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of());

        List<AiModel> models = workspaceModelFacade.getWorkspaceModels(1L);

        assertTrue(models.isEmpty());
    }

    @Test
    void testUnpinWorkspaceModelSucceedsWhenModelBelongsToWorkspace() {
        AiGatewayProvider workspaceProvider = workspaceProvider();
        AiModel model = new AiModel(5L, "gpt-4");

        ReflectionTestUtils.setField(model, "id", 100L);

        AiModel unpinned = new AiModel(5L, "gpt-4");

        ReflectionTestUtils.setField(unpinned, "id", 100L);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(workspaceProvider));
        when(aiModelService.getModel(100L)).thenReturn(model);
        when(aiModelService.unpin(100L)).thenReturn(unpinned);

        AiModel result = workspaceModelFacade.unpinWorkspaceModel(1L, 100L);

        assertEquals(unpinned, result);

        verify(aiModelService).unpin(100L);
    }

    /**
     * The ownership guard is what stops a caller from unpinning a model that belongs to a workspace they don't have
     * access to — an unguarded unpin would let anyone strip an administrator's negotiated rate off any row in the
     * system, not just their own workspace's.
     */
    @Test
    void testUnpinWorkspaceModelRejectsModelFromDifferentWorkspace() {
        AiGatewayProvider workspaceProvider = workspaceProvider();
        AiModel model = new AiModel(999L, "gpt-4");

        ReflectionTestUtils.setField(model, "id", 100L);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(workspaceProvider));
        when(aiModelService.getModel(100L)).thenReturn(model);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> workspaceModelFacade.unpinWorkspaceModel(1L, 100L));

        assertTrue(exception.getMessage()
            .contains("does not belong to workspace"));

        verify(aiModelService, never()).unpin(anyLong());
    }

    @Test
    void testUpdateWorkspaceModelSucceedsWhenModelBelongsToWorkspace() {
        AiGatewayProvider workspaceProvider = workspaceProvider();
        AiModel model = new AiModel(5L, "gpt-4");

        ReflectionTestUtils.setField(model, "id", 100L);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(workspaceProvider));
        when(aiModelService.getModel(100L)).thenReturn(model);
        when(aiModelService.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiModel result = workspaceModelFacade.updateWorkspaceModel(
            1L, 100L, "gpt-4-turbo", null, null, null, null, null, null, null);

        assertNotNull(result);
        assertEquals("gpt-4-turbo", result.getName());

        verify(aiModelService).update(any());
    }

    /**
     * The ownership guard is what stops a caller from updating a model that belongs to a workspace they don't have
     * access to — an unguarded update would let anyone rewrite another workspace's billing-relevant model configuration
     * (cost rates, routing policy, enabled state) as well as any admin-negotiated rate.
     */
    @Test
    void testUpdateWorkspaceModelRejectsModelFromDifferentWorkspace() {
        AiGatewayProvider workspaceProvider = workspaceProvider();
        AiModel model = new AiModel(999L, "gpt-4");

        ReflectionTestUtils.setField(model, "id", 100L);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(workspaceProvider));
        when(aiModelService.getModel(100L)).thenReturn(model);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> workspaceModelFacade.updateWorkspaceModel(
                1L, 100L, "gpt-4-turbo", null, null, null, null, null, null, null));

        assertTrue(exception.getMessage()
            .contains("does not belong to workspace"));

        verify(aiModelService, never()).update(any());
    }

    /**
     * A provider owned by workspace 1, id 5. Ownership is now carried by the provider's own {@code workspace_id}
     * column, so the workspace service hands back the providers themselves rather than membership rows.
     */
    private static AiGatewayProvider workspaceProvider() {
        AiGatewayProvider provider = new AiGatewayProvider("openai", AiGatewayProviderType.OPENAI, "sk-123");

        ReflectionTestUtils.setField(provider, "id", 5L);

        provider.setWorkspaceId(1L);

        return provider;
    }
}
