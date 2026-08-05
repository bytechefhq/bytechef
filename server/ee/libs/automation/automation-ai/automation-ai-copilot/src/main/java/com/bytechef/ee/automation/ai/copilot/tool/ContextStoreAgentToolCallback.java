/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.tool;

import com.bytechef.ai.agent.tool.AgentType;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.agent.tool.CurrentAgentContext;
import com.bytechef.ai.agent.tool.CurrentAgentContext.AgentBinding;
import com.bytechef.ai.agent.tool.ToolErrors;
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
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the Context Store Copilot subagent to the parent ai_hub
 * agent.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ContextStoreAgentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(ContextStoreAgentToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate a user request about Context Stores to a specialised Context Store subagent.
            A Context Store ingests data from a source component on a cadence and exposes searchable,
            optionally-embedded records. The subagent owns listing, explaining, searching, and (in build
            mode) creating/updating/refreshing/enabling/deleting sources and stores. Prefer calling it over
            reasoning about context stores directly. Returns a synthesised markdown report or a summary of
            the mutations performed.""";

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

    private final ChatClient contextStoreChatClient;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ContextStoreAgentToolCallback(ChatClient contextStoreChatClient) {
        this.contextStoreChatClient = contextStoreChatClient;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("context_store_agent")
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
            ContextStoreAgentInput input = JsonUtils.read(toolInput, ContextStoreAgentInput.class);

            String request = input.request();

            if (request == null || request.isBlank()) {
                return toolError("request is required and must not be blank");
            }

            AgentBinding parent = CurrentAgentContext.current();
            AgentType parentAgent = parent != null ? parent.agentName() : null;

            Map<String, Object> forwardedContext = toolContext == null ? Map.of() : toolContext.getContext();

            String result = CurrentAgentContext.callWith(
                CopilotAgentType.CONTEXT_STORE_AGENT, parentAgent,
                () -> contextStoreChatClient.prompt(request)
                    .toolContext(forwardedContext)
                    .call()
                    .content());

            if (result == null) {
                log.warn("context_store subagent returned null for request='{}'", request);

                return ToolErrors.toolError("context_store subagent returned null");
            }

            return result;
        } catch (JacksonException exception) {
            log.warn(
                "context_store_agent rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                ContextStoreAgentToolCallback.class, "context_store_agent", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record ContextStoreAgentInput(String request) {
    }
}
