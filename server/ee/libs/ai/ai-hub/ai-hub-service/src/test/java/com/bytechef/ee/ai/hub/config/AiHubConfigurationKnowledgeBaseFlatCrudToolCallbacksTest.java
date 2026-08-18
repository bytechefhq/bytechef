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
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the two read names {@link AiHubConfiguration#knowledgeBaseFlatCrudToolCallbacks} restores flat on both hub
 * agents, and the five mutation names {@link AiHubConfiguration#knowledgeBaseCatalogToolCallbacks} registers on the
 * BUILD agent's searchable tool catalog (ticket 732, Task 6 of the CRUD-delegate unwind), replacing the dissolved
 * {@code knowledge_base_agent} delegate.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubConfigurationKnowledgeBaseFlatCrudToolCallbacksTest {

    private static final Set<String> EXPECTED_READ_NAMES = Set.of("listKnowledgeBases", "queryKnowledgeBase");

    private static final Set<String> EXPECTED_MUTATION_NAMES = Set.of(
        "createKnowledgeBase", "addKnowledgeBaseDocument", "deleteKnowledgeBaseDocument", "cloneKnowledgeBase",
        "deleteKnowledgeBase");

    @Test
    void testFlatCrudReturnsExactlyTheTwoReadNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.knowledgeBaseFlatCrudToolCallbacks(present());

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testFlatCrudReturnsEmptyListWhenFactoryAbsent() {
        assertThat(AiHubConfiguration.knowledgeBaseFlatCrudToolCallbacks(absent())).isEmpty();
    }

    @Test
    void testCatalogReturnsExactlyTheFiveMutationNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.knowledgeBaseCatalogToolCallbacks(present());

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_MUTATION_NAMES);
    }

    @Test
    void testCatalogNeverIncludesAReadName() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.knowledgeBaseCatalogToolCallbacks(present());

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .doesNotContainAnyElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testCatalogReturnsEmptyListWhenFactoryAbsent() {
        assertThat(AiHubConfiguration.knowledgeBaseCatalogToolCallbacks(absent())).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<KnowledgeBaseToolCallbacksFactory> present() {
        KnowledgeBaseToolCallbacksFactory factory = new KnowledgeBaseToolCallbacksFactory(
            mock(WorkspaceKnowledgeBaseFacade.class), mock(KnowledgeBaseFacade.class), mock(KnowledgeBaseService.class),
            mock(KnowledgeBaseDocumentFacade.class), mock(KnowledgeBaseDocumentService.class), null);

        ObjectProvider<KnowledgeBaseToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(factory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<KnowledgeBaseToolCallbacksFactory> absent() {
        return mock(ObjectProvider.class);
    }
}
