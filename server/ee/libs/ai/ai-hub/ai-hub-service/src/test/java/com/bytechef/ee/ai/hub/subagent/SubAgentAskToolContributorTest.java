/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SubAgentAskToolContributorTest {

    @Test
    void testContributeRegistersTheAskToolOnTheRequest() {
        ChatClientRequestSpec chatClientRequestSpec = mock(ChatClientRequestSpec.class);

        when(chatClientRequestSpec.toolCallbacks(any(ToolCallback[].class))).thenReturn(chatClientRequestSpec);

        SubAgentAskToolContributor contributor = new SubAgentAskToolContributor();

        contributor.contribute(chatClientRequestSpec, Map.of());

        ArgumentCaptor<ToolCallback[]> toolCallbacksCaptor = ArgumentCaptor.forClass(ToolCallback[].class);

        verify(chatClientRequestSpec).toolCallbacks(toolCallbacksCaptor.capture());

        assertThat(toolCallbacksCaptor.getValue()).hasOnlyElementsOfType(SubagentAskUserQuestionToolCallback.class);
    }

    /**
     * Registration is request-level so the tool ADDS to the specialist's own {@code defaultTools} rather than replacing
     * them — that is what lets a CE-configured specialist gain an EE tool without the CE configuration importing it.
     */
    @Test
    void testContributeDoesNotReplaceTheSpecialistsOwnTools() {
        ChatClientRequestSpec chatClientRequestSpec = mock(ChatClientRequestSpec.class);

        when(chatClientRequestSpec.toolCallbacks(any(ToolCallback[].class))).thenReturn(chatClientRequestSpec);

        SubAgentAskToolContributor contributor = new SubAgentAskToolContributor();

        ChatClientRequestSpec result = contributor.contribute(chatClientRequestSpec, Map.of());

        assertThat(result).isSameAs(chatClientRequestSpec);

        verify(chatClientRequestSpec).toolCallbacks(any(ToolCallback[].class));
    }
}
