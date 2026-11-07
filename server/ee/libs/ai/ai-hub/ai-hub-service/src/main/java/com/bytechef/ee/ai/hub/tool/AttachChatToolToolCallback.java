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
import com.bytechef.ee.ai.hub.chat.AiHubChatToolConnectionValidator;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolFacade;
import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 * @author Ivica Cardic
 */
public class AttachChatToolToolCallback implements ToolCallback {

    static final String TOOL_NAME = "attachChatTool";

    private static final String DESCRIPTION = """
        Attach a configured tool to the current chat so the assistant can invoke it directly in
        future turns without going through search. Use after the user expresses intent like 'set up slack'
        or 'configure github for this chat'. Supply componentName (e.g. 'slack'), actionName (e.g.
        'sendMessage'), parameters (the pre-set values that should default; the assistant can still
        override per call), and connectionId (workspace connection to bind; obtain from listConnections or
        a prior createConnection step). componentVersion defaults to 1 unless specified. Returns
        {chatToolId, chatComponentId, attachedToolName, replacedExistingTool}.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "componentName": {"type": "string", "description": "ByteChef component name (e.g. 'slack')"},
                    "actionName": {"type": "string", "description": "Cluster element / action name (e.g. 'sendMessage')"},
                    "parameters": {"type": "object", "description": "Pre-set parameter values for the action"},
                    "connectionId": {"type": "string", "description": "Optional connection id (omit for connection-less tools)"},
                    "componentVersion": {"type": "integer", "description": "Optional component version, defaults to 1"}
                },
                "required": ["componentName", "actionName", "parameters"]
            }""";

    private final AiHubChatService chatService;
    private final AiHubChatToolFacade chatToolFacade;
    private final AiHubChatToolConnectionValidator connectionValidator;
    private final AiHubToolAttachMetrics metrics;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AttachChatToolToolCallback(
        AiHubChatService chatService,
        AiHubChatToolFacade chatToolFacade, AiHubChatToolConnectionValidator connectionValidator,
        AiHubToolAttachMetrics metrics) {

        this.chatService = chatService;
        this.chatToolFacade = chatToolFacade;
        this.connectionValidator = connectionValidator;
        this.metrics = metrics;
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
            AttachChatToolInput input = jsonMapper.readValue(toolInput, AttachChatToolInput.class);

            if (input.componentName() == null || input.componentName()
                .isBlank()) {
                return toolError("componentName is required");
            }

            if (input.actionName() == null || input.actionName()
                .isBlank()) {
                return toolError("actionName is required");
            }

            AiHubToolInvocationContext invocationContext =
                AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.threadId() == null) {
                return toolError(
                    "AiHubChat context unavailable — open this chat from the AI Hub.");
            }

            Optional<AiHubChat> chat = chatService.findNotDeletedByThreadId(invocationContext.threadId());

            if (chat.isEmpty()) {
                return toolError("AiHubChat not found for thread " + invocationContext.threadId());
            }

            Long userId = invocationContext.userId();

            if (userId == null || userId != chat.get()
                .getUserId()) {

                metrics.recordAttach("not_owner");

                return toolError("Only the owner of this chat can attach tools to it.");
            }

            Long connectionId = parseConnectionId(input.connectionId());
            Integer inputComponentVersion = input.componentVersion();
            int componentVersion = inputComponentVersion == null ? 1 : inputComponentVersion;
            int environment = (int) chat.get()
                .getEnvironmentId();

            if (connectionId != null) {
                try {
                    connectionValidator.validate(
                        chatService.getWorkspaceId(chat.get()
                            .getId()),
                        input.componentName(), connectionId, environment);
                } catch (IllegalArgumentException exception) {
                    metrics.recordAttach("connection_unavailable");

                    return toolError(
                        exception.getMessage() +
                            ". Use listConnectionsForComponent to find a connection in the current environment.");
                }
            }

            Optional<Long> existingComponentBindingId = chatToolFacade.findChatComponentIdIgnoringConnection(
                chat.get()
                    .getId(),
                input.componentName(), componentVersion, environment);

            long chatComponentId;

            if (existingComponentBindingId.isPresent()) {
                chatComponentId = existingComponentBindingId.get();

                chatToolFacade.setComponentConnection(chatComponentId, connectionId);

                metrics.recordAttach("rebound_connection");
            } else {
                chatComponentId = chatToolFacade.attachComponent(
                    chat.get()
                        .getId(),
                    input.componentName(), componentVersion, connectionId, environment);

                metrics.recordAttach("new_component");
            }

            Map<String, ?> parameters = input.parameters() == null ? Map.of() : input.parameters();

            long chatToolId = chatToolFacade.addTool(
                chatComponentId, input.actionName(), parameters);

            return jsonMapper.writeValueAsString(new AttachChatToolOutput(
                chatToolId, chatComponentId,
                input.componentName() + "_" + input.actionName(), false));
        } catch (JacksonException exception) {
            metrics.recordAttach("error");

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            metrics.recordAttach("error");

            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            metrics.recordAttach("error");

            return ToolErrors.runtimeFailure(
                jsonMapper, AttachChatToolToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static @Nullable Long parseConnectionId(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("connectionId must be a numeric id, was: " + raw);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record AttachChatToolInput(
        String componentName, String actionName, Map<String, Object> parameters, @Nullable String connectionId,
        @Nullable Integer componentVersion) {

        public AttachChatToolInput {
            parameters = parameters == null ? null : Map.copyOf(parameters);
        }
    }

    public record AttachChatToolOutput(
        long chatToolId, long chatComponentId, String attachedToolName,
        boolean replacedExistingTool) {
    }
}
