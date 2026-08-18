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
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the Custom Component Copilot subagent to the parent ai_hub
 * agent.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CustomComponentAgentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(CustomComponentAgentToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate a user request about custom components to a specialised Custom Component subagent.
            Custom components are single-file, user-authored components (JavaScript, Python, Ruby) that add
            new actions to the platform. The subagent owns the canonical behaviour for listing, explaining,
            creating, updating, and deleting custom components, including authoring and iterating the source
            until it compiles. Prefer calling it over reasoning about custom components directly. The result
            is a synthesised markdown report or, in build mode, a summary of the mutations performed including
            the affected custom component id and name. Creates a new custom component when the request
            describes one that doesn't exist, or updates/deletes an existing one named or identified by id in
            the request.""";

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
    public CustomComponentAgentToolCallback(
        IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver) {

        this.chatClientFactory = chatClientFactory;
        this.chatModelResolver = chatModelResolver;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("buildCustomComponent")
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
            CustomComponentAgentInput input = JsonUtils.read(toolInput, CustomComponentAgentInput.class);

            String request = input.request();

            if (request == null || request.isBlank()) {
                return toolError("request is required and must not be blank");
            }

            AgentBinding parent = CurrentAgentContext.current();
            AgentType parentAgent = parent != null ? parent.agentName() : null;

            Map<String, Object> parentContext = toolContext == null ? Map.of() : toolContext.getContext();

            ChatClient customComponentChatClient =
                chatClientFactory.get(SubAgentChatModelResolution.resolve(chatModelResolver, parentContext));

            String result = CurrentAgentContext.callWith(
                CopilotAgentType.BUILD_CUSTOM_COMPONENT, parentAgent,
                () -> customComponentChatClient.prompt(request)
                    .toolContext(parentContext)
                    .call()
                    .content());

            if (result == null) {
                log.warn("custom_component subagent returned null for request='{}'", request);

                return ToolErrors.toolError("custom_component subagent returned null");
            }

            return result;
        } catch (JacksonException exception) {
            log.warn(
                "buildCustomComponent rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                CustomComponentAgentToolCallback.class, "buildCustomComponent", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record CustomComponentAgentInput(String request) {
    }
}
