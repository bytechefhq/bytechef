/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SlideBuilderToolCallbackTest {

    private final ChatClient chatClient = mock(ChatClient.class);
    private final JsonMapper jsonMapper = new JsonMapper();
    private final ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
    private final CallResponseSpec responseSpec = mock(CallResponseSpec.class);
    private final List<ToolCallback> toolCallbacks = List.of(mock(ToolCallback.class));

    @Test
    void testCallReturnsTheSubagentSummary() {
        stubSubagent("Saved q3-roadmap.pptx with 6 slides.");

        JsonNode node = jsonMapper.readTree(newCallback().call("{\"topic\":\"Q3 roadmap\"}"));

        assertThat(node.get("summary")
            .asString()).isEqualTo("Saved q3-roadmap.pptx with 6 slides.");
    }

    @Test
    void testCallHandsTheSubagentTheHintsTheToolsAndTheParentContext() {
        stubSubagent("Saved.");

        Map<String, Object> parentContext = Map.of(AiHubToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 3L);

        newCallback().call(
            "{\"topic\":\"Q3 roadmap\",\"outlineOrSourceFileId\":\"5\",\"filename\":\"q3.pptx\",\"slideCount\":6}",
            new ToolContext(parentContext));

        verify(chatClient).prompt(
            "Q3 roadmap\n\nSource file id for outline/material: 5\nSave as filename: q3.pptx\nRequested slide count: 6");
        verify(requestSpec).tools(toolCallbacks);
        verify(requestSpec).toolContext(parentContext);
    }

    @Test
    void testCallReturnsErrorWhenTopicIsBlank() {
        assertThat(newCallback().call("{\"topic\":\" \"}")).contains("topic is required");

        verifyNoInteractions(chatClient);
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentReturnsNull() {
        stubSubagent(null);

        assertThat(newCallback().call("{\"topic\":\"Q3 roadmap\"}")).contains("slide_builder subagent returned null");
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() {
        assertThat(newCallback().call("not-json")).containsIgnoringCase("invalid input");
    }

    @Test
    void testToolDefinitionIsNamedSlideBuilder() {
        assertThat(newCallback().getToolDefinition()
            .name()).isEqualTo("slide_builder");
    }

    private SlideBuilderToolCallback newCallback() {
        return new SlideBuilderToolCallback(candidateChatModel -> chatClient, null, toolCallbacks);
    }

    private void stubSubagent(String content) {
        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(content);
    }
}
