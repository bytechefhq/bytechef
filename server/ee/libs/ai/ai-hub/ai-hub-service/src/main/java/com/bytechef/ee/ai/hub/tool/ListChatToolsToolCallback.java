/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolBinding;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolFacade;
import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that lists tools already attached to the current AI Hub chat. Used by the LLM to avoid
 * duplicate attaches ("is Slack already wired here?") and to answer "what's set up on this chat?". The output mirrors
 * {@link AiHubChatToolBinding} so subsequent {@code removeChatTool} or {@code attachChatTool} calls have everything
 * they need to address an existing binding.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ListChatToolsToolCallback implements ToolCallback {

    static final String TOOL_NAME = "listChatTools";

    private static final String DESCRIPTION = """
        List the tools currently attached to this chat. Use before attachChatTool to avoid duplicates and
        when the user asks what's configured here. Returns a tools array of
        {chatToolId, chatComponentId, componentName, componentVersion, actionName, connectionId, parameters}.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {}
        }""";

    private final AiHubChatService chatService;
    private final AiHubChatToolFacade chatToolFacade;
    private final AiHubToolAttachMetrics metrics;
    private final JsonMapper jsonMapper;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListChatToolsToolCallback(
        AiHubChatService chatService, AiHubChatToolFacade chatToolFacade, AiHubToolAttachMetrics metrics,
        JsonMapper jsonMapper) {

        this.chatService = chatService;
        this.chatToolFacade = chatToolFacade;
        this.metrics = metrics;
        this.jsonMapper = jsonMapper;
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
            AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.threadId() == null) {
                return toolError("AiHubChat context unavailable — open this chat from the AI Hub.");
            }

            Optional<AiHubChat> chat = chatService.findByThreadId(invocationContext.threadId());

            if (chat.isEmpty()) {
                return toolError("AiHubChat not found for thread " + invocationContext.threadId());
            }

            List<AiHubChatToolBinding> bindings = chatToolFacade.listChatTools(chat.get()
                .getId());

            List<Map<String, Object>> tools = new ArrayList<>(bindings.size());

            for (AiHubChatToolBinding binding : bindings) {
                Map<String, Object> row = new LinkedHashMap<>();

                row.put("chatToolId", binding.chatToolId());
                row.put("chatComponentId", binding.chatComponentId());
                row.put("componentName", binding.componentName());
                row.put("componentVersion", binding.componentVersion());
                row.put("actionName", binding.clusterElementName());
                row.put("connectionId", binding.connectionId());
                row.put("parameters", binding.parameters());

                tools.add(row);
            }

            Map<String, Object> envelope = new LinkedHashMap<>();

            envelope.put("tools", tools);

            metrics.recordStateVisibility(TOOL_NAME, tools.isEmpty() ? "empty" : "success");

            return jsonMapper.writeValueAsString(envelope);
        } catch (JacksonException exception) {
            metrics.recordStateVisibility(TOOL_NAME, "error");

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            metrics.recordStateVisibility(TOOL_NAME, "error");

            return ToolErrors.runtimeFailure(jsonMapper, ListChatToolsToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }
}
