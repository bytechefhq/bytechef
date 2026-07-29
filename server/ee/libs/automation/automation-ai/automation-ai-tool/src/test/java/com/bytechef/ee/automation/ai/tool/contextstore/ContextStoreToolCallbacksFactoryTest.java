/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreFacade;
import com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreSourceFacade;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSemanticSearchService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ContextStoreToolCallbacksFactoryTest {

    private final ContextStoreToolCallbacksFactory factory = new ContextStoreToolCallbacksFactory(
        Mockito.mock(WorkspaceContextStoreSourceService.class),
        Mockito.mock(ContextStoreQueryService.class),
        Mockito.mock(WorkspaceContextStoreSourceFacade.class),
        Mockito.mock(WorkspaceContextStoreFacade.class),
        Mockito.mock(ContextStoreSemanticSearchService.class),
        Mockito.mock(ClusterElementDefinitionService.class));

    @Test
    void readListExcludesMutations() {
        List<String> names = toolNames(factory.readToolCallbacks());

        assertThat(names).contains("listContextSources", "searchContextStore");
        assertThat(names).doesNotContain("deleteContextStore", "createContextStoreSource");
    }

    @Test
    void writeListIncludesReadsAndMutationsAndTopLevelDelete() {
        List<String> names = toolNames(factory.writeToolCallbacks());

        assertThat(names).contains(
            "listContextSources", "createContextStoreSource", "deleteContextStoreSource", "deleteContextStore");
    }

    private static List<String> toolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .collect(Collectors.toList());
    }
}
