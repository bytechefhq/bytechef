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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} for "stop using component X.action in this chat". Looks up the chat tool by
 * (componentName, actionName), deletes the row. Idempotent — removing a non-existent tool returns a structured "not
 * attached" envelope so the LLM can confirm the post-condition without surprising the user.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RemoveChatToolToolCallback implements ToolCallback {

    static final String TOOL_NAME = "removeChatTool";

    private static final String DESCRIPTION = """
        Detach a previously-attached tool from the current chat. The assistant will no longer have
        the tool's pre-configured parameters available, though it can still discover the underlying tool
        again via searchTool. Supply componentName (e.g. 'slack') and actionName (e.g. 'sendMessage').
        Returns {removed: true, chatToolId} on success or {removed: false, message} when no
        matching attachment was found.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "componentName": {"type": "string"},
                "actionName": {"type": "string"}
            },
            "required": ["componentName", "actionName"]
        }""";

    private final AiHubChatService chatService;
    private final AiHubChatToolFacade chatToolFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public RemoveChatToolToolCallback(
        AiHubChatService chatService,
        AiHubChatToolFacade chatToolFacade) {

        this.chatService = chatService;
        this.chatToolFacade = chatToolFacade;
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
            RemoveChatToolInput input = jsonMapper.readValue(toolInput, RemoveChatToolInput.class);

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

            Optional<AiHubChat> chat =
                chatService.findByThreadId(invocationContext.threadId());

            if (chat.isEmpty()) {
                return toolError("AiHubChat not found for thread " + invocationContext.threadId());
            }

            // List + filter is fine here: a chat typically has < 20 attached tools and lookup is rare
            // (called only when the user asks to remove). Avoiding a custom join repo method keeps the
            // persistence layer simple.
            List<AiHubChatToolBinding> bindings = chatToolFacade.listChatTools(
                chat.get()
                    .getId());

            Optional<AiHubChatToolBinding> match = bindings.stream()
                .filter(binding -> binding.componentName()
                    .equals(input.componentName())
                    && binding.clusterElementName()
                        .equals(input.actionName()))
                .findFirst();

            if (match.isEmpty()) {
                return jsonMapper.writeValueAsString(new RemoveChatToolOutput(
                    false, null,
                    "No attached tool found for " + input.componentName() + "/" + input.actionName()));
            }

            chatToolFacade.removeTool(match.get()
                .chatToolId());

            return jsonMapper.writeValueAsString(new RemoveChatToolOutput(
                true, match.get()
                    .chatToolId(),
                "Removed " + input.componentName() + "/" + input.actionName()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, RemoveChatToolToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record RemoveChatToolInput(String componentName, String actionName) {
    }

    public record RemoveChatToolOutput(boolean removed, @Nullable Long chatToolId, String message) {
    }
}
