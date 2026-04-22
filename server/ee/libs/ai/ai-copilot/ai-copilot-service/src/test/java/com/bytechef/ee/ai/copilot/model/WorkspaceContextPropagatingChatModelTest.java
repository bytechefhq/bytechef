/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.workspacefile.ai.tool.AgUiToolContextWorkspaceContextProvider;
import com.bytechef.ee.automation.workspacefile.ai.tool.WorkspaceInvocationContext;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceContextPropagatingChatModelTest {

    @Mock
    private ChatModel delegate;

    @Mock
    private ChatResponse response;

    @Test
    void testCallWithoutThreadBoundContextPassesPromptAsIs() {
        Prompt prompt = new Prompt("hi");

        when(delegate.call(prompt)).thenReturn(response);

        WorkspaceContextPropagatingChatModel chatModel = new WorkspaceContextPropagatingChatModel(delegate);

        assertThat(chatModel.call(prompt)).isSameAs(response);

        verify(delegate).call(prompt);
    }

    @Test
    void testCallMergesThreadBoundContextIntoPromptOptions() {
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);

        when(delegate.call(any(Prompt.class))).thenReturn(response);

        WorkspaceContextPropagatingChatModel chatModel = new WorkspaceContextPropagatingChatModel(delegate);

        WorkspaceInvocationContext invocationContext = new WorkspaceInvocationContext(123L, (short) 2, "draft");

        AgUiToolContextWorkspaceContextProvider.runWithContext(
            invocationContext, () -> chatModel.call(new Prompt("hi")));

        verify(delegate).call(promptCaptor.capture());

        Prompt captured = promptCaptor.getValue();

        assertThat(captured.getOptions()).isInstanceOf(ToolCallingChatOptions.class);

        Map<String, Object> toolContext = ((ToolCallingChatOptions) captured.getOptions()).getToolContext();

        assertThat(toolContext).containsEntry(
            AgUiToolContextWorkspaceContextProvider.TOOL_CONTEXT_WORKSPACE_ID_KEY, 123L);
        assertThat(toolContext).containsEntry(
            AgUiToolContextWorkspaceContextProvider.TOOL_CONTEXT_SOURCE_ORDINAL_KEY, (short) 2);
        assertThat(toolContext).containsEntry(
            AgUiToolContextWorkspaceContextProvider.TOOL_CONTEXT_LAST_USER_PROMPT_KEY, "draft");
    }
}
