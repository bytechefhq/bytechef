/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.agui.core.agent.RunAgentInput;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.bytechef.ee.ai.copilot.util.Source;
import com.bytechef.ee.automation.workspacefile.ai.tool.AgUiToolContextWorkspaceContextProvider;
import com.bytechef.ee.automation.workspacefile.ai.tool.WorkspaceInvocationContext;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class FilesSpringAIAgentTest {

    @Test
    void testBuildInvocationContextExtractsWorkspaceIdFromState() throws AGUIException {
        FilesSpringAIAgent agent = newAgent();

        State state = new State();

        state.set("workspaceId", 42L);

        UserMessage userMessage = new UserMessage();

        userMessage.setContent("Write a runbook");

        RunAgentInput input = new RunAgentInput(
            "thread", "run", state, List.of((BaseMessage) userMessage), List.of(), List.of(), null);

        WorkspaceInvocationContext context = agent.buildInvocationContext(input);

        assertThat(context.workspaceId()).isEqualTo(42L);
        assertThat(context.sourceOrdinal()).isEqualTo((short) Source.FILES.ordinal());
        assertThat(context.lastUserPrompt()).isEqualTo("Write a runbook");
    }

    @Test
    void testBuildInvocationContextCoercesWorkspaceIdFromString() throws AGUIException {
        FilesSpringAIAgent agent = newAgent();

        State state = new State();

        state.set("workspaceId", "1049");

        RunAgentInput input = new RunAgentInput(
            "thread", "run", state, List.of(), List.of(), List.of(), null);

        WorkspaceInvocationContext context = agent.buildInvocationContext(input);

        assertThat(context.workspaceId()).isEqualTo(1049L);
        assertThat(context.lastUserPrompt()).isNull();
    }

    @Test
    void testBuildInvocationContextReturnsNullWorkspaceIdWhenMissing() throws AGUIException {
        FilesSpringAIAgent agent = newAgent();

        State state = new State();

        RunAgentInput input = new RunAgentInput(
            "thread", "run", state, List.of(), List.of(), List.of(), null);

        WorkspaceInvocationContext context = agent.buildInvocationContext(input);

        assertThat(context.workspaceId()).isNull();
        assertThat(context.sourceOrdinal()).isEqualTo((short) Source.FILES.ordinal());
    }

    @Test
    void testRunBindsContextVisibleToProviderWhileSuperRunExecutes() throws AGUIException {
        AtomicReference<WorkspaceInvocationContext> observed = new AtomicReference<>();

        AgUiToolContextWorkspaceContextProvider provider = new AgUiToolContextWorkspaceContextProvider();

        FilesSpringAIAgent agent = newAgent();

        State state = new State();

        state.set("workspaceId", 9L);

        UserMessage userMessage = new UserMessage();

        userMessage.setContent("hello");

        RunAgentInput input = new RunAgentInput(
            "thread", "run", state, List.of((BaseMessage) userMessage), List.of(), List.of(), null);

        AgUiToolContextWorkspaceContextProvider.runWithContext(
            agent.buildInvocationContext(input),
            () -> observed.set(
                new WorkspaceInvocationContext(
                    provider.currentWorkspaceId(),
                    provider.currentSourceOrdinal(),
                    provider.lastUserPrompt())));

        assertThat(observed.get()
            .workspaceId()).isEqualTo(9L);
        assertThat(observed.get()
            .sourceOrdinal()).isEqualTo((short) Source.FILES.ordinal());
        assertThat(observed.get()
            .lastUserPrompt()).isEqualTo("hello");
        assertThat(provider.currentWorkspaceId()).isNull();
    }

    private FilesSpringAIAgent newAgent() throws AGUIException {
        return FilesSpringAIAgent.builder()
            .agentId("files")
            .chatModel((ChatModel) (prompt -> null))
            .systemMessage("test")
            .state(new State())
            .build();
    }
}
