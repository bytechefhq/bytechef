/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.task.AiHubTaskToolBinding;
import com.bytechef.ee.ai.hub.task.AiHubTaskToolFacade;
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
 * Spring AI {@link ToolCallback} that lists tools already attached to the current AI Hub task. Used by the LLM to avoid
 * duplicate attaches ("is Slack already wired here?") and to answer "what's set up on this chat?". The output mirrors
 * {@link AiHubTaskToolBinding} so subsequent {@code removeTaskTool} or {@code attachTaskTool} calls have everything
 * they need to address an existing binding.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ListTaskToolsToolCallback implements ToolCallback {

    static final String TOOL_NAME = "listTaskTools";

    private static final String DESCRIPTION = """
        List the tools currently attached to this chat task. Use before attachTaskTool to avoid duplicates and
        when the user asks what's configured here. Returns a tools array of
        {taskToolId, taskComponentId, componentName, componentVersion, actionName, connectionId, parameters}.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {}
        }""";

    private final AiHubTaskService taskService;
    private final AiHubTaskToolFacade taskToolFacade;
    private final AiHubToolAttachMetrics metrics;
    private final JsonMapper jsonMapper;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListTaskToolsToolCallback(
        AiHubTaskService taskService, AiHubTaskToolFacade taskToolFacade, AiHubToolAttachMetrics metrics,
        JsonMapper jsonMapper) {

        this.taskService = taskService;
        this.taskToolFacade = taskToolFacade;
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
                return toolError("AiHubTask context unavailable — open this chat from the AI Hub.");
            }

            Optional<AiHubTask> task = taskService.findByThreadId(invocationContext.threadId());

            if (task.isEmpty()) {
                return toolError("AiHubTask not found for thread " + invocationContext.threadId());
            }

            List<AiHubTaskToolBinding> bindings = taskToolFacade.listTaskTools(task.get()
                .getId());

            List<Map<String, Object>> tools = new ArrayList<>(bindings.size());

            for (AiHubTaskToolBinding binding : bindings) {
                Map<String, Object> row = new LinkedHashMap<>();

                row.put("taskToolId", binding.taskToolId());
                row.put("taskComponentId", binding.taskComponentId());
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

            return ToolErrors.runtimeFailure(jsonMapper, ListTaskToolsToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }
}
