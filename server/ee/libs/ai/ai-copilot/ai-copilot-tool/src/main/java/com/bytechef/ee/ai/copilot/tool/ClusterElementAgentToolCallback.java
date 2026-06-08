/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import com.bytechef.ee.ai.agent.tool.Agent;
import com.bytechef.ee.ai.agent.tool.CurrentAgentContext;
import com.bytechef.ee.ai.agent.tool.CurrentAgentContext.AgentBinding;
import com.bytechef.ee.ai.agent.tool.ToolErrors;
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
import tools.jackson.databind.json.JsonMapper;

/**
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the Cluster Element Copilot subagent to the parent ai_hub
 * agent.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ClusterElementAgentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(ClusterElementAgentToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate a user request about cluster elements to a specialised Cluster Element subagent.
            Cluster elements are the slotted child operations inside cluster-root components (AI Agent,
            Knowledge Base, etc.) — model, chat memory, RAG source, guardrails, tools. The subagent owns
            the canonical behaviour for designing, editing, and explaining cluster element configuration;
            prefer calling it over reasoning about cluster element shape directly.""";

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

    private final ChatClient clusterElementChatClient;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ClusterElementAgentToolCallback(ChatClient clusterElementChatClient) {
        this.clusterElementChatClient = clusterElementChatClient;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("cluster_element_agent")
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
            ClusterElementAgentInput input = jsonMapper.readValue(toolInput, ClusterElementAgentInput.class);

            String request = input.request();

            if (request == null || request.isBlank()) {
                return toolError("request is required and must not be blank");
            }

            AgentBinding parent = CurrentAgentContext.current();
            Agent parentAgent = parent != null ? parent.agentName() : null;

            Map<String, Object> forwardedContext = toolContext == null ? Map.of() : toolContext.getContext();

            String result = CurrentAgentContext.callWith(Agent.CLUSTER_ELEMENT_AGENT, parentAgent,
                () -> clusterElementChatClient.prompt(request)
                    .toolContext(forwardedContext)
                    .call()
                    .content());

            if (result == null) {
                log.warn("cluster_element subagent returned null for request='{}'", request);

                return ToolErrors.toolError(jsonMapper, "cluster_element subagent returned null");
            }

            return result;
        } catch (JacksonException exception) {
            log.warn(
                "cluster_element_agent rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, ClusterElementAgentToolCallback.class, "cluster_element_agent", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record ClusterElementAgentInput(String request) {
    }
}
