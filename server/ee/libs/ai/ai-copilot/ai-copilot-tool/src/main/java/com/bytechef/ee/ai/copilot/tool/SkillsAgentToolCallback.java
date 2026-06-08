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
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the Skills Copilot subagent to the parent ai_hub agent.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SkillsAgentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(SkillsAgentToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate a user request about workflow Skills to a specialised Skills subagent.
            Skills are reusable parameterised workflow templates the user can compose into projects.
            The subagent owns the canonical behaviour for listing, explaining, creating, updating, and
            composing Skills; prefer calling it over reasoning about skills directly. The result is a
            synthesised markdown report or, in build mode, a summary of the mutations performed.""";

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

    private final ChatClient skillsChatClient;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SkillsAgentToolCallback(ChatClient skillsChatClient) {
        this.skillsChatClient = skillsChatClient;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("skills_agent")
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
            SkillsAgentInput input = jsonMapper.readValue(toolInput, SkillsAgentInput.class);

            String request = input.request();

            if (request == null || request.isBlank()) {
                return toolError("request is required and must not be blank");
            }

            AgentBinding parent = CurrentAgentContext.current();
            Agent parentAgent = parent != null ? parent.agentName() : null;

            Map<String, Object> forwardedContext = toolContext == null ? Map.of() : toolContext.getContext();

            String result = CurrentAgentContext.callWith(Agent.SKILLS, parentAgent,
                () -> skillsChatClient.prompt(request)
                    .toolContext(forwardedContext)
                    .call()
                    .content());

            if (result == null) {
                log.warn("skills subagent returned null for request='{}'", request);

                return ToolErrors.toolError(jsonMapper, "skills subagent returned null");
            }

            return result;
        } catch (JacksonException exception) {
            log.warn(
                "skills_agent rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, SkillsAgentToolCallback.class, "skills_agent", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record SkillsAgentInput(String request) {
    }
}
