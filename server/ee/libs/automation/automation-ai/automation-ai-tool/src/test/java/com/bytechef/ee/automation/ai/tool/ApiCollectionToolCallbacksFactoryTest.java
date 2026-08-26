/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ApiCollectionToolCallbacksFactoryTest {

    private final ApiCollectionToolCallbacksFactory factory = new ApiCollectionToolCallbacksFactory(
        mock(ApiCollectionFacade.class));

    @Test
    void readListContainsOnlyListApiCollections() {
        List<String> names = toolNames(factory.readToolCallbacks());

        assertThat(names).containsExactly("listApiCollections");
    }

    @Test
    void writeListIncludesReadsAndAllMutations() {
        List<String> names = toolNames(factory.writeToolCallbacks());

        assertThat(names).containsExactlyInAnyOrder(
            "listApiCollections", "createApiCollection");
    }

    private static List<String> toolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }
}
