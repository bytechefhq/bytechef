/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.facade.AiModelFacade;
import com.bytechef.ee.automation.ai.gateway.facade.WorkspaceAiModelFacade;
import com.bytechef.ee.automation.ai.gateway.web.graphql.config.AiGatewayGraphQlConfigurationSharedMocks;
import com.bytechef.ee.automation.ai.gateway.web.graphql.config.AiGatewayGraphQlTestConfiguration;
import com.bytechef.ee.platform.ai.gateway.catalog.AiModelCatalogReconciler;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AiGatewayGraphQlTestConfiguration.class,
    AiModelGraphQlController.class,
    WorkspaceAiModelGraphQlController.class
})
@GraphQlTest(
    controllers = {
        AiModelGraphQlController.class,
        WorkspaceAiModelGraphQlController.class
    },
    properties = {
        "bytechef.ai.gateway.enabled=true",
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@AiGatewayGraphQlConfigurationSharedMocks
class AiModelGraphQlControllerIntTest {

    @Autowired
    private AiModelCatalogReconciler aiModelCatalogReconciler;

    @Autowired
    private AiModelFacade aiModelFacade;

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private WorkspaceAiModelFacade workspaceAiModelFacade;

    @Test
    void testWorkspaceAiModelsResolvesCatalogManagedAcrossControllers() {
        AiModel catalogManagedModel = createMockModel(1L, 100L, "gpt-4o", false);
        AiModel unmanagedModel = createMockModel(2L, 100L, "custom-deployment", false);
        AiModel pinnedModel = createMockModel(3L, 100L, "gpt-4o-pinned", true);

        when(workspaceAiModelFacade.getWorkspaceModels(1L))
            .thenReturn(List.of(catalogManagedModel, unmanagedModel, pinnedModel));
        when(aiModelCatalogReconciler.catalogManagedModelIds(any())).thenReturn(Set.of(1L));

        this.graphQlTester
            .document("""
                query {
                    workspaceAiModels(workspaceId: "1") {
                        id
                        catalogPinned
                        catalogManaged
                    }
                }
                """)
            .execute()
            .path("workspaceAiModels[0].id")
            .entity(String.class)
            .isEqualTo("1")
            .path("workspaceAiModels[0].catalogPinned")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("workspaceAiModels[0].catalogManaged")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("workspaceAiModels[1].id")
            .entity(String.class)
            .isEqualTo("2")
            .path("workspaceAiModels[1].catalogPinned")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("workspaceAiModels[1].catalogManaged")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("workspaceAiModels[2].id")
            .entity(String.class)
            .isEqualTo("3")
            .path("workspaceAiModels[2].catalogPinned")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("workspaceAiModels[2].catalogManaged")
            .entity(Boolean.class)
            .isEqualTo(false);
    }

    @Test
    void testCatalogManagedBatchResolverInvokesReconcilerOnceForMultipleRows() {
        AiModel firstModel = createMockModel(1L, 100L, "gpt-4o", false);
        AiModel secondModel = createMockModel(2L, 100L, "gpt-4o-mini", false);
        AiModel thirdModel = createMockModel(3L, 100L, "custom-deployment", false);

        when(workspaceAiModelFacade.getWorkspaceModels(1L))
            .thenReturn(List.of(firstModel, secondModel, thirdModel));
        when(aiModelCatalogReconciler.catalogManagedModelIds(any())).thenReturn(Set.of(1L, 2L));

        this.graphQlTester
            .document("""
                query {
                    workspaceAiModels(workspaceId: "1") {
                        id
                        catalogManaged
                    }
                }
                """)
            .execute()
            .path("workspaceAiModels")
            .entityList(Object.class)
            .hasSize(3);

        verify(aiModelCatalogReconciler, times(1)).catalogManagedModelIds(any());
    }

    @Test
    void testUnpinAiModel() {
        AiModel unpinnedModel = createMockModel(5L, 100L, "gpt-4o", false);

        when(aiModelFacade.unpin(5L)).thenReturn(unpinnedModel);

        this.graphQlTester
            .document("""
                mutation {
                    unpinAiModel(id: "5") {
                        id
                        catalogPinned
                    }
                }
                """)
            .execute()
            .path("unpinAiModel.id")
            .entity(String.class)
            .isEqualTo("5")
            .path("unpinAiModel.catalogPinned")
            .entity(Boolean.class)
            .isEqualTo(false);

        verify(aiModelFacade).unpin(5L);
    }

    @Test
    void testUnpinWorkspaceAiModel() {
        AiModel unpinnedModel = createMockModel(5L, 100L, "gpt-4o", false);

        when(workspaceAiModelFacade.unpinWorkspaceModel(10L, 5L)).thenReturn(unpinnedModel);

        this.graphQlTester
            .document("""
                mutation {
                    unpinWorkspaceAiModel(workspaceId: "10", modelId: "5") {
                        id
                        catalogPinned
                    }
                }
                """)
            .execute()
            .path("unpinWorkspaceAiModel.id")
            .entity(String.class)
            .isEqualTo("5")
            .path("unpinWorkspaceAiModel.catalogPinned")
            .entity(Boolean.class)
            .isEqualTo(false);

        verify(workspaceAiModelFacade).unpinWorkspaceModel(10L, 5L);
    }

    @Test
    void testReconcileAiModelCatalog() {
        this.graphQlTester
            .document("""
                mutation {
                    reconcileAiModelCatalog
                }
                """)
            .execute()
            .path("reconcileAiModelCatalog")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(aiModelFacade).reconcileCatalog();
    }

    private AiModel createMockModel(Long id, Long providerId, String name, boolean catalogPinned) {
        AiModel model = new AiModel(providerId, name);

        model.setCatalogPinned(catalogPinned);
        setId(model, id);

        return model;
    }

    private void setId(AiModel model, Long id) {
        try {
            Field field = AiModel.class.getDeclaredField("id");

            field.setAccessible(true);
            field.set(model, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
