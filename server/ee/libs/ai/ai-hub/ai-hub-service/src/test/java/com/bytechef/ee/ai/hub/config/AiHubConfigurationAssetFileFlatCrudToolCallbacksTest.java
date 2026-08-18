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

import com.bytechef.automation.ai.tool.AssetFileToolCallbacksFactory;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the seven asset-file CRUD tool names {@link AiHubConfiguration#assetFileFlatCrudToolCallbacks} restores flat on
 * the hub surfaces (ticket 732, Task 4 of the CRUD-delegate unwind), replacing the dissolved {@code asset_file_agent}
 * delegate.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubConfigurationAssetFileFlatCrudToolCallbacksTest {

    private static final Set<String> EXPECTED_READ_NAMES = Set.of("listAssetFiles", "getAssetFileContent");

    private static final Set<String> EXPECTED_WRITE_NAMES = Set.of(
        "listAssetFiles", "getAssetFileContent", "createAssetFile", "createBinaryAssetFile",
        "updateAssetFileContent", "cloneAssetFile", "createAssetFileFromUrl");

    @Test
    void testReadModeReturnsExactlyTheTwoReadNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.assetFileFlatCrudToolCallbacks(present(), false);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testWriteModeReturnsExactlyTheSevenNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.assetFileFlatCrudToolCallbacks(present(), true);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_WRITE_NAMES);
    }

    @Test
    void testAbsentFactoryReturnsEmptyListForBothModes() {
        assertThat(AiHubConfiguration.assetFileFlatCrudToolCallbacks(absent(), false)).isEmpty();
        assertThat(AiHubConfiguration.assetFileFlatCrudToolCallbacks(absent(), true)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<AssetFileToolCallbacksFactory> present() {
        AssetFileToolCallbacksFactory factory = new AssetFileToolCallbacksFactory(mock(AssetFileFacade.class), null);

        ObjectProvider<AssetFileToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(factory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<AssetFileToolCallbacksFactory> absent() {
        return mock(ObjectProvider.class);
    }
}
