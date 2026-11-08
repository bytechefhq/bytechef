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

import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalDefaults;
import com.bytechef.ee.automation.ai.tool.contextstore.ContextStoreToolCallbacksFactory;
import com.bytechef.ee.automation.contextstore.facade.ContextStoreFacade;
import com.bytechef.ee.automation.contextstore.facade.ContextStoreSourceFacade;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSemanticSearchService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
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
    void testReadModeReturnsExactlyTheReadNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.contextStoreFlatCrudToolCallbacks(present(), false);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testWriteModeReturnsTheReadAndMutationNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.contextStoreFlatCrudToolCallbacks(present(), true);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(union(EXPECTED_READ_NAMES, EXPECTED_MUTATION_NAMES));
    }

    @Test
    void testOnlyTheDeleteToolsAreGatedByDefault() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.contextStoreFlatCrudToolCallbacks(present(), true);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .filteredOn(AiHubToolApprovalDefaults::isGatedByDefault)
            .containsExactlyInAnyOrder("deleteContextStoreSource", "deleteContextStore");
    }

    @Test
    void testAbsentFactoryReturnsEmptyListForBothModes() {
        assertThat(AiHubConfiguration.contextStoreFlatCrudToolCallbacks(absent(), false)).isEmpty();
        assertThat(AiHubConfiguration.contextStoreFlatCrudToolCallbacks(absent(), true)).isEmpty();
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        Set<String> union = new HashSet<>(first);

        union.addAll(second);

        return union;
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
