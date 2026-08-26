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

import com.bytechef.ee.automation.ai.tool.ApiCollectionToolCallbacksFactory;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the three API-collection CRUD tool names {@link AiHubConfiguration#apiCollectionFlatCrudToolCallbacks} restores
 * flat on the hub surfaces (ticket 732, Task 2 of the CRUD-delegate unwind), replacing the dissolved
 * {@code api_collection_agent} delegate.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubConfigurationApiCollectionFlatCrudToolCallbacksTest {

    private static final Set<String> EXPECTED_READ_NAMES = Set.of("listApiCollections");

    private static final Set<String> EXPECTED_WRITE_NAMES = Set.of(
        "listApiCollections", "createApiCollection");

    @Test
    void testReadModeReturnsExactlyTheOneReadName() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.apiCollectionFlatCrudToolCallbacks(present(), false);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testWriteModeReturnsExactlyTheThreeNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.apiCollectionFlatCrudToolCallbacks(present(), true);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_WRITE_NAMES);
    }

    @Test
    void testAbsentFactoryReturnsEmptyListForBothModes() {
        assertThat(AiHubConfiguration.apiCollectionFlatCrudToolCallbacks(absent(), false)).isEmpty();
        assertThat(AiHubConfiguration.apiCollectionFlatCrudToolCallbacks(absent(), true)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ApiCollectionToolCallbacksFactory> present() {
        ApiCollectionToolCallbacksFactory factory = new ApiCollectionToolCallbacksFactory(
            mock(ApiCollectionFacade.class));

        ObjectProvider<ApiCollectionToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(factory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ApiCollectionToolCallbacksFactory> absent() {
        return mock(ObjectProvider.class);
    }
}
