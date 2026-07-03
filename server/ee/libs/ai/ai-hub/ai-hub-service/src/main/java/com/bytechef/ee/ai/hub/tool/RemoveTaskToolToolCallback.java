/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.task.AiHubTaskToolBinding;
import com.bytechef.ee.ai.hub.task.AiHubTaskToolFacade;
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
 * Spring AI {@link ToolCallback} for "stop using component X.action in this chat". Looks up the task tool by
 * (componentName, actionName), deletes the row. Idempotent — removing a non-existent tool returns a structured "not
 * attached" envelope so the LLM can confirm the post-condition without surprising the user.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RemoveTaskToolToolCallback implements ToolCallback {

    static final String TOOL_NAME = "removeTaskTool";

    private static final String DESCRIPTION = """
        Detach a previously-attached tool from the current task. The assistant will no longer have
        the tool's pre-configured parameters available, though it can still discover the underlying tool
        again via searchTool. Supply componentName (e.g. 'slack') and actionName (e.g. 'sendMessage').
        Returns {removed: true, taskToolId} on success or {removed: false, message} when no
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

    private final AiHubTaskService taskService;
    private final AiHubTaskToolFacade taskToolFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public RemoveTaskToolToolCallback(
        AiHubTaskService taskService,
        AiHubTaskToolFacade taskToolFacade) {

        this.taskService = taskService;
        this.taskToolFacade = taskToolFacade;
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
            RemoveTaskToolInput input = jsonMapper.readValue(toolInput, RemoveTaskToolInput.class);

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
                    "AiHubTask context unavailable — open this chat from the AI Hub.");
            }

            Optional<AiHubTask> task =
                taskService.findByThreadId(invocationContext.threadId());

            if (task.isEmpty()) {
                return toolError("AiHubTask not found for thread " + invocationContext.threadId());
            }

            // List + filter is fine here: a task typically has < 20 attached tools and lookup is rare
            // (called only when the user asks to remove). Avoiding a custom join repo method keeps the
            // persistence layer simple.
            List<AiHubTaskToolBinding> bindings = taskToolFacade.listTaskTools(
                task.get()
                    .getId());

            Optional<AiHubTaskToolBinding> match = bindings.stream()
                .filter(binding -> binding.componentName()
                    .equals(input.componentName())
                    && binding.clusterElementName()
                        .equals(input.actionName()))
                .findFirst();

            if (match.isEmpty()) {
                return jsonMapper.writeValueAsString(new RemoveTaskToolOutput(
                    false, null,
                    "No attached tool found for " + input.componentName() + "/" + input.actionName()));
            }

            taskToolFacade.removeTool(match.get()
                .taskToolId());

            return jsonMapper.writeValueAsString(new RemoveTaskToolOutput(
                true, match.get()
                    .taskToolId(),
                "Removed " + input.componentName() + "/" + input.actionName()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, RemoveTaskToolToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record RemoveTaskToolInput(String componentName, String actionName) {
    }

    public record RemoveTaskToolOutput(boolean removed, @Nullable Long taskToolId, String message) {
    }
}
