/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.tool;

import com.bytechef.ai.agent.tool.AgentType;
import com.bytechef.ai.agent.tool.CurrentAgentContext;
import com.bytechef.ai.agent.tool.CurrentAgentContext.AgentBinding;
import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolution;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the Code Workflow Copilot subagent to the parent ai_hub
 * agent.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CodeWorkflowAgentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(CodeWorkflowAgentToolCallback.class);

    // RUBY-DISABLED: "or Ruby" removed from the subagent description below — org.graalvm.polyglot:ruby is published
    // only up to 25.0.0 and crashes on the Truffle 25.2.4 this repo pins, and the create-empty facade now rejects
    // RUBY, so a copilot that offered Ruby would produce a workflow that cannot be created or run. Restore it once a
    // polyglot ruby jar built on Truffle 25.2+ is published (or GraalVM is downgraded). Grep RUBY-DISABLED.
    private static final String DESCRIPTION =
        """
            Delegate a user request about code workflows to a specialised Code Workflow subagent. Code
            workflows are whole automation projects authored as a single JavaScript or Python script.
            The subagent owns the canonical behaviour for listing, explaining, creating, and updating code
            workflows, including authoring and iterating the source until it compiles. Prefer calling it over
            reasoning about code workflows directly. The result is a synthesised markdown report or, in build
            mode, a summary of the mutations performed including the affected project id, name, and language.
            Creates a new code workflow project when the request describes one that doesn't exist, or updates
            an existing one named or identified by id in the request.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "request": {
                        "type": "string",
                        "description": "The user request in natural language. Pass through verbatim — the subagent does its own task decomposition."
                    }
                },
                "required": ["request"]
            }""";

    private final IntelligentToolChatClientFactory chatClientFactory;
    private final @Nullable SubAgentChatModelResolver chatModelResolver;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CodeWorkflowAgentToolCallback(
        IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver) {

        this.chatClientFactory = chatClientFactory;
        this.chatModelResolver = chatModelResolver;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("buildCodeWorkflow")
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            CodeWorkflowAgentInput input = JsonUtils.read(toolInput, CodeWorkflowAgentInput.class);

            String request = input.request();

            if (request == null || request.isBlank()) {
                return toolError("request is required and must not be blank");
            }

            AgentBinding parent = CurrentAgentContext.current();
            AgentType parentAgent = parent != null ? parent.agentName() : null;

            Map<String, Object> parentContext = toolContext == null ? Map.of() : toolContext.getContext();

            ChatClient codeWorkflowChatClient =
                chatClientFactory.get(SubAgentChatModelResolution.resolve(chatModelResolver, parentContext));

            String result = CurrentAgentContext.callWith(
                CopilotAgentType.BUILD_CODE_WORKFLOW, parentAgent,
                () -> codeWorkflowChatClient.prompt(request)
                    .toolContext(parentContext)
                    .call()
                    .content());

            if (result == null) {
                log.warn("code_workflow subagent returned null for request='{}'", request);

                return ToolErrors.toolError("code_workflow subagent returned null");
            }

            return result;
        } catch (JacksonException exception) {
            log.warn(
                "buildCodeWorkflow rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                CodeWorkflowAgentToolCallback.class, "buildCodeWorkflow", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record CodeWorkflowAgentInput(String request) {
    }
}
