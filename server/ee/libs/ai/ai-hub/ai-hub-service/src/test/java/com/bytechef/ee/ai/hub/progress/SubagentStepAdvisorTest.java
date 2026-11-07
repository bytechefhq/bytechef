/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SubagentStepAdvisorTest {

    @Test
    void testWrapsEveryToolOfTheRequestAndKeepsItsToolContext() {
        ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder()
            .toolCallbacks(List.of(toolCallback("searchComponents"), toolCallback("updateWorkflow")))
            .toolContext(Map.of("tenantId", "acme"))
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt("Build it", chatOptions))
            .build();

        ChatClientRequest advisedRequest = SubagentStepAdvisor.withStepProgress(request);

        ToolCallingChatOptions advisedChatOptions = (ToolCallingChatOptions) advisedRequest.prompt()
            .getOptions();

        assertThat(advisedChatOptions.getToolCallbacks()).hasSize(2)
            .allSatisfy(advisedToolCallback -> assertThat(advisedToolCallback)
                .isInstanceOf(SubagentStepToolCallback.class))
            .extracting(advisedToolCallback -> advisedToolCallback.getToolDefinition()
                .name())
            .containsExactly("searchComponents", "updateWorkflow");
        assertThat(advisedChatOptions.getToolContext()).containsEntry("tenantId", "acme");
    }

    @Test
    void testLeavesARequestWithoutToolsAsItIs() {
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt("Build it", ToolCallingChatOptions.builder()
                .build()))
            .build();

        assertThat(SubagentStepAdvisor.withStepProgress(request)).isSameAs(request);

        ChatClientRequest plainRequest = ChatClientRequest.builder()
            .prompt(new Prompt("Build it", ChatOptions.builder()
                .build()))
            .build();

        assertThat(SubagentStepAdvisor.withStepProgress(plainRequest)).isSameAs(plainRequest);
    }

    private static ToolCallback toolCallback(String name) {
        return FunctionToolCallback.builder(name, (String input) -> "ok")
            .description(name)
            .inputType(String.class)
            .build();
    }
}
