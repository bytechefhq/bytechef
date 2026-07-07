/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.copilot.tool;

import com.bytechef.ai.agent.tool.Agent;
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
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the Converter Copilot subagent to the parent ai_hub BUILD
 * agent. BUILD-only — there is no ASK variant of the Converter Copilot specialist.
 *
 * @author Ivica Cardic
 */
public class ConverterAgentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(ConverterAgentToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate a request to convert an external workflow definition (n8n, Make, Zapier, Workato,
            etc.) into a ByteChef workflow. The Converter subagent owns the canonical behaviour for this
            domain — translating constructs, mapping integrations, and producing valid ByteChef workflow
            JSON plus a rationale.""";

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

    private final ChatClient converterChatClient;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ConverterAgentToolCallback(ChatClient converterChatClient) {
        this.converterChatClient = converterChatClient;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("converter_agent")
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
            ConverterAgentInput input = JsonUtils.read(toolInput, ConverterAgentInput.class);

            String request = input.request();

            if (request == null || request.isBlank()) {
                return toolError("request is required and must not be blank");
            }

            AgentBinding parent = CurrentAgentContext.current();
            Agent parentAgent = parent != null ? parent.agentName() : null;

            Map<String, Object> forwardedContext = toolContext == null ? Map.of() : toolContext.getContext();

            String result = CurrentAgentContext.callWith(
                Agent.CONVERTER_AGENT, parentAgent,
                () -> converterChatClient.prompt(request)
                    .toolContext(forwardedContext)
                    .call()
                    .content());

            if (result == null) {
                log.warn("converter subagent returned null for request='{}'", request);

                return ToolErrors.toolError("converter subagent returned null");
            }

            return result;
        } catch (JacksonException exception) {
            log.warn(
                "converter_agent rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                ConverterAgentToolCallback.class, "converter_agent", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record ConverterAgentInput(String request) {
    }
}
