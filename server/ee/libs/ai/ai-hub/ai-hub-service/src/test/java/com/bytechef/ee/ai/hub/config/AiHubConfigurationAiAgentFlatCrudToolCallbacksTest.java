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
import com.bytechef.ee.ai.hub.tool.AiHubChatArtifactRecorder;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the AI Agent builder tool names {@link AiHubConfiguration#aiAgentFlatCrudToolCallbacks} puts flat on the hub
 * agents, and when {@link AiHubConfiguration#openAiAgentTabToolCallbacks} offers {@code openAiAgentTab}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubConfigurationAiAgentFlatCrudToolCallbacksTest {

    private static final Set<String> EXPECTED_READ_NAMES =
        Set.of("listAiAgents", "getAiAgent", "searchAiAgentTools", "searchAiAgentReferences");

    private static final Set<String> EXPECTED_WRITE_NAMES = Set.of(
        "listAiAgents", "getAiAgent", "searchAiAgentTools", "searchAiAgentReferences", "createAiAgent", "updateAiAgent",
        "addAiAgentChannel",
        "deleteAiAgentChannel", "addAiAgentElement", "updateAiAgentElement", "deleteAiAgentElement",
        "updateAiAgentSettings");

    @Test
    void testReadModeReturnsExactlyTheReadNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.aiAgentFlatCrudToolCallbacks(presentFactory(), false);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testWriteModeReturnsTheReadAndMutationNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.aiAgentFlatCrudToolCallbacks(presentFactory(), true);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_WRITE_NAMES);
    }

    @Test
    void testAbsentFactoryReturnsEmptyListForBothModes() {
        assertThat(AiHubConfiguration.aiAgentFlatCrudToolCallbacks(absent(), false)).isEmpty();
        assertThat(AiHubConfiguration.aiAgentFlatCrudToolCallbacks(absent(), true)).isEmpty();
    }

    @Test
    void testOpenAiAgentTabIsOfferedOnlyWhenTheAiAgentFacadeIsPresent() {
        ObjectProvider<AiAgentFacade> aiAgentFacadeProvider = provider(mock(AiAgentFacade.class));

        assertThat(
            AiHubConfiguration.openAiAgentTabToolCallbacks(
                aiAgentFacadeProvider, mock(AiHubChatArtifactRecorder.class)))
                    .extracting(toolCallback -> toolCallback.getToolDefinition()
                        .name())
                    .containsExactly("openAiAgentTab");
        assertThat(AiHubConfiguration.openAiAgentTabToolCallbacks(absent(), null)).isEmpty();
    }

    private static ObjectProvider<AiAgentToolCallbacksFactory> presentFactory() {
        return provider(
            new AiAgentToolCallbacksFactory(mock(AiAgentFacade.class), mock(ClusterElementDefinitionService.class),
                null, null));
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T bean) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(bean);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> absent() {
        return mock(ObjectProvider.class);
    }
}
