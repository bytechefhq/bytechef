/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.tool.contextstore.ContextStoreToolCallbacksFactory;
import com.bytechef.ee.automation.contextstore.facade.ContextStoreFacade;
import com.bytechef.ee.automation.contextstore.facade.ContextStoreSourceFacade;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSemanticSearchService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the six read names {@link AiHubConfiguration#contextStoreFlatCrudToolCallbacks} restores flat on both hub
 * agents, and the six mutation names {@link AiHubConfiguration#contextStoreCatalogToolCallbacks} registers on the BUILD
 * agent's searchable tool catalog (ticket 732, Task 7 of the CRUD-delegate unwind), replacing the dissolved
 * {@code context_store_agent} delegate.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubConfigurationContextStoreFlatCrudToolCallbacksTest {

    private static final Set<String> EXPECTED_READ_NAMES = Set.of(
        "listContextSources", "searchContextStore", "getContextStoreRecord", "listAvailableSourceComponents",
        "describeSourceComponentEntities", "semanticSearchContextStore");

    private static final Set<String> EXPECTED_MUTATION_NAMES = Set.of(
        "createContextStoreSource", "updateContextStoreSource", "deleteContextStoreSource",
        "refreshContextStoreSource", "setContextStoreSourceEnabled", "deleteContextStore");

    @Test
    void testFlatCrudReturnsExactlyTheSixReadNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.contextStoreFlatCrudToolCallbacks(present());

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testFlatCrudReturnsEmptyListWhenFactoryAbsent() {
        assertThat(AiHubConfiguration.contextStoreFlatCrudToolCallbacks(absent())).isEmpty();
    }

    @Test
    void testCatalogReturnsExactlyTheSixMutationNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.contextStoreCatalogToolCallbacks(present());

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_MUTATION_NAMES);
    }

    @Test
    void testCatalogNeverIncludesAReadName() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.contextStoreCatalogToolCallbacks(present());

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .doesNotContainAnyElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testCatalogReturnsEmptyListWhenFactoryAbsent() {
        assertThat(AiHubConfiguration.contextStoreCatalogToolCallbacks(absent())).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ContextStoreToolCallbacksFactory> present() {
        ContextStoreToolCallbacksFactory factory = new ContextStoreToolCallbacksFactory(
            mock(WorkspaceContextStoreSourceService.class), mock(ContextStoreQueryService.class),
            mock(ContextStoreSourceFacade.class), mock(ContextStoreFacade.class),
            mock(ContextStoreSemanticSearchService.class), mock(ClusterElementDefinitionService.class));

        ObjectProvider<ContextStoreToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(factory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ContextStoreToolCallbacksFactory> absent() {
        return mock(ObjectProvider.class);
    }
}
