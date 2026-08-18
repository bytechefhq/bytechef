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

import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.automation.ai.tool.aiagent.AiAgentToolCallbacksFactory;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the two read names {@link AiHubConfiguration#aiAgentFlatCrudToolCallbacks} restores flat on both hub agents, and
 * the nine mutation names {@link AiHubConfiguration#aiAgentCatalogToolCallbacks} registers on the BUILD agent's
 * searchable tool catalog (ticket 732, Task 8 of the CRUD-delegate unwind — the LAST delegate in the plan), replacing
 * the dissolved {@code ai_agent_agent} delegate.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubConfigurationAiAgentFlatCrudToolCallbacksTest {

    private static final Set<String> EXPECTED_READ_NAMES = Set.of("listAiAgents", "getAiAgent");

    private static final Set<String> EXPECTED_MUTATION_NAMES = Set.of(
        "createAiAgent", "updateAiAgent", "addAiAgentChannel", "deleteAiAgentChannel", "addAiAgentElement",
        "updateAiAgentElement", "deleteAiAgentElement", "updateAiAgentSettings", "publishAiAgent");

    @Test
    void testFlatCrudReturnsExactlyTheTwoReadNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.aiAgentFlatCrudToolCallbacks(present());

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testFlatCrudReturnsEmptyListWhenFactoryAbsent() {
        assertThat(AiHubConfiguration.aiAgentFlatCrudToolCallbacks(absent())).isEmpty();
    }

    @Test
    void testCatalogReturnsExactlyTheNineMutationNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.aiAgentCatalogToolCallbacks(present());

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_MUTATION_NAMES);
    }

    @Test
    void testCatalogNeverIncludesAReadName() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.aiAgentCatalogToolCallbacks(present());

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .doesNotContainAnyElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testCatalogReturnsEmptyListWhenFactoryAbsent() {
        assertThat(AiHubConfiguration.aiAgentCatalogToolCallbacks(absent())).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<AiAgentToolCallbacksFactory> present() {
        AiAgentToolCallbacksFactory factory = new AiAgentToolCallbacksFactory(mock(AiAgentFacade.class));

        ObjectProvider<AiAgentToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(factory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<AiAgentToolCallbacksFactory> absent() {
        return mock(ObjectProvider.class);
    }
}
