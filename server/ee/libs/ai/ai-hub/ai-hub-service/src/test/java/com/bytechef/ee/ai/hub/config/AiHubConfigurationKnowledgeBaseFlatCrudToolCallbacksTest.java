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

import com.bytechef.automation.ai.tool.knowledgebase.KnowledgeBaseToolCallbacksFactory;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
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
class AiHubConfigurationKnowledgeBaseFlatCrudToolCallbacksTest {

    private static final Set<String> EXPECTED_READ_NAMES = Set.of("listKnowledgeBases", "queryKnowledgeBase");

    private static final Set<String> EXPECTED_MUTATION_NAMES = Set.of(
        "createKnowledgeBase", "addKnowledgeBaseDocument", "deleteKnowledgeBaseDocument", "cloneKnowledgeBase",
        "deleteKnowledgeBase");

    @Test
    void testReadModeReturnsExactlyTheReadNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.knowledgeBaseFlatCrudToolCallbacks(present(), false);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testWriteModeReturnsTheReadAndMutationNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.knowledgeBaseFlatCrudToolCallbacks(present(), true);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(union(EXPECTED_READ_NAMES, EXPECTED_MUTATION_NAMES));
    }

    @Test
    void testAbsentFactoryReturnsEmptyListForBothModes() {
        assertThat(AiHubConfiguration.knowledgeBaseFlatCrudToolCallbacks(absent(), false)).isEmpty();
        assertThat(AiHubConfiguration.knowledgeBaseFlatCrudToolCallbacks(absent(), true)).isEmpty();
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        Set<String> union = new HashSet<>(first);

        union.addAll(second);

        return union;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<KnowledgeBaseToolCallbacksFactory> present() {
        KnowledgeBaseToolCallbacksFactory factory = new KnowledgeBaseToolCallbacksFactory(
            mock(WorkspaceKnowledgeBaseFacade.class), mock(KnowledgeBaseFacade.class),
            mock(KnowledgeBaseDocumentFacade.class));

        ObjectProvider<KnowledgeBaseToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(factory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<KnowledgeBaseToolCallbacksFactory> absent() {
        return mock(ObjectProvider.class);
    }
}
