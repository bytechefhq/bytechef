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

package com.bytechef.component.ai.llm.voice;

import com.bytechef.platform.component.definition.voice.VoiceAgentToolset;
import com.bytechef.platform.component.definition.voice.VoiceToolDefinition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
public class ToolCallbackVoiceAgentToolset implements VoiceAgentToolset {

    private static final Logger log = LoggerFactory.getLogger(ToolCallbackVoiceAgentToolset.class);

    private final Map<String, ToolCallback> toolCallbacks = new LinkedHashMap<>();

    public ToolCallbackVoiceAgentToolset(List<ToolCallback> toolCallbacks) {
        for (ToolCallback toolCallback : toolCallbacks) {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();
            String name = toolDefinition.name();

            if (this.toolCallbacks.containsKey(name)) {
                log.warn("Duplicate voice tool name, keeping the first tool: tool={}", name);

                continue;
            }

            this.toolCallbacks.put(name, toolCallback);
        }
    }

    @Override
    public List<VoiceToolDefinition> definitions() {
        return toolCallbacks.values()
            .stream()
            .map(ToolCallback::getToolDefinition)
            .map(toolDefinition -> new VoiceToolDefinition(
                toolDefinition.name(), toolDefinition.description(), toolDefinition.inputSchema()))
            .toList();
    }

    @Override
    public String call(String name, String argumentsJson) {
        ToolCallback toolCallback = toolCallbacks.get(name);

        if (toolCallback == null) {
            return VoiceAgentToolset.unknownTool(name);
        }

        try {
            return toolCallback.call(argumentsJson);
        } catch (Throwable throwable) {
            log.warn("Voice tool call failed: tool={}", name, throwable);

            return "Tool failed: " + describe(throwable);
        }
    }

    private static String describe(Throwable throwable) {
        String message = throwable.getMessage();

        if (message == null || message.isBlank()) {
            return throwable.getClass()
                .getSimpleName();
        }

        return message;
    }
}
