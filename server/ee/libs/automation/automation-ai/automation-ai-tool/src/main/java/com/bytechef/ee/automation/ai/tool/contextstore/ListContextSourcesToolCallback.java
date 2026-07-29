/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that enumerates Context Store sources visible in the current workspace, including the
 * record-shape (entity name, idField, indexed-field schema) each source exposes. The agent uses this as the discovery
 * entry point — without an inventory of sources and their indexed fields, calls to
 * {@link SearchContextStoreToolCallback} would have to be guessed.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class ListContextSourcesToolCallback implements ToolCallback {

    static final String TOOL_NAME = "listContextSources";

    private static final String DESCRIPTION = """
        List all Context Store sources in the current workspace. For each source returns the source id, name, the
        entity name and indexed-field schema, the source's status (BUILDING_PREVIEW, PREVIEW, READY, FAILED,
        DISABLED), and enabled flag. Call this first when the user asks about Context Store data — the returned
        sourceId is the input to searchContextStore and getContextStoreRecord.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {}
        }""";

    private final WorkspaceContextStoreSourceService workspaceContextStoreSourceService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListContextSourcesToolCallback(WorkspaceContextStoreSourceService workspaceContextStoreSourceService) {
        this.workspaceContextStoreSourceService = workspaceContextStoreSourceService;
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
            AgentToolInvocationContext invocationContext =
                AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the AI Hub of a workspace.");
            }

            List<ContextStoreSource> sources =
                workspaceContextStoreSourceService.getAllSourcesByWorkspaceId(workspaceId);
            List<Map<String, Object>> response = new ArrayList<>(sources.size());

            for (ContextStoreSource source : sources) {
                Map<String, Object> sourceSummary = new LinkedHashMap<>();

                sourceSummary.put("sourceId", source.getId());
                sourceSummary.put("name", source.getName());
                sourceSummary.put("entityName", source.getEntityName());
                sourceSummary.put("idField", source.getIdField());
                sourceSummary.put("indexedFields", source.getIndexedFields());

                if (source.getDescription() != null) {
                    sourceSummary.put("description", source.getDescription());
                }

                sourceSummary.put("status", source.getStatus()
                    .name());
                sourceSummary.put("enabled", source.isEnabled());
                sourceSummary.put("sourceComponentName", source.getSourceComponentName());
                sourceSummary.put("sourceComponentVersion", source.getSourceComponentVersion());
                sourceSummary.put("cadence", source.getCadence());

                response.add(sourceSummary);
            }

            return jsonMapper.writeValueAsString(response);
        } catch (JacksonException exception) {
            return toolError("Serialization error: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, ListContextSourcesToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }
}
