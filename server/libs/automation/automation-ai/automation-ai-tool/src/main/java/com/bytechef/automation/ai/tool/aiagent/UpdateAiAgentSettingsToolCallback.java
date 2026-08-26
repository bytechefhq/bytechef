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

package com.bytechef.automation.ai.tool.aiagent;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Replaces an AI Agent's built-in-tool settings map.
 *
 * @author Ivica Cardic
 */
public class UpdateAiAgentSettingsToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "updateAiAgentSettings";

    private static final String DESCRIPTION =
        """
            Replace an AI Agent's settings — currently just builtInTools, a set of on/off switches for
            the platform's built-in agent tools: askUserQuestion (default ON), autoMemory (default ON),
            skills (default ON — gates the SKILL elements aggregate), skillManagement (default ON — the
            five create/update/delete/append/removeFile skill-authoring tools), webSearch (default OFF).
            Web search picks its source with webSearchProvider: BRAVE (the default), FIRECRAWL — both of
            which need webSearchConnectionId set to publish — or NATIVE, the model provider's own search,
            which takes no connection and only works on a model provider that supports it (anthropic).
            This REPLACES the entire settings object, not a per-key merge — always include every key you
            want to keep, not just the ones you're changing. Example: {"builtInTools": {"webSearch": true,
            "webSearchProvider": "FIRECRAWL", "webSearchConnectionId": 42}}. Any key you omit resets to its
            documented default.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "id": {"type": "integer", "description": "AI Agent id"},
                "settings": {
                    "type": "object",
                    "description": "The full settings object to save, e.g. {\\"builtInTools\\": {...}}"
                }
            },
            "required": ["id", "settings"]
        }""";

    private final AiAgentFacade aiAgentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public UpdateAiAgentSettingsToolCallback(AiAgentFacade aiAgentFacade) {
        this.aiAgentFacade = aiAgentFacade;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
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
            UpdateAiAgentSettingsInput input = jsonMapper.readValue(toolInput, UpdateAiAgentSettingsInput.class);

            if (input.id() == null) {
                return toolError("id is required");
            }

            if (input.settings() == null) {
                return toolError("settings is required");
            }

            aiAgentFacade.updateAgentSettings(input.id(), input.settings());

            return jsonMapper.writeValueAsString(Map.of("updated", true, "id", input.id()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, UpdateAiAgentSettingsToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record UpdateAiAgentSettingsInput(Long id, @Nullable Map<String, Object> settings) {
    }
}
