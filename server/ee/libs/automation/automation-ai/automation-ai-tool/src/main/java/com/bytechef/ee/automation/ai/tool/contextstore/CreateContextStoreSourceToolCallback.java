/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.automation.contextstore.dto.CreateContextStoreSourceInput;
import com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreSourceFacade;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that provisions a new Context Store source. Creates the source row (which now carries
 * the record-shape fields directly — Phase 2 collapsed the Entity layer), an auto-generated sync workflow
 * ({@code [schedule.cron] -> [data-stream.stream(SOURCE=..., DESTINATION=contextStore.writeToReplica)]}), and triggers
 * the initial sync run. Workspace admin role required at the facade level; chat-level user confirmation expected before
 * execution per CC mutation-callback precedent.
 *
 * <p>
 * The tool resolves {@code workspaceId} from the {@link AgentToolInvocationContext} on the chat's {@link ToolContext}.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
public class CreateContextStoreSourceToolCallback implements ToolCallback {

    static final String TOOL_NAME = "createContextStoreSource";

    private static final String DESCRIPTION = """
        Provision a new Context Store source. Creates the source row, an auto-generated sync workflow, and triggers
        the initial sync run. Workspace admin role required.

        Always confirm with the user before calling — this creates persistent infrastructure and starts a sync job.

        Use listAvailableSourceComponents and describeSourceComponentEntities first to discover valid component
        names, ItemReader cluster element names, and properties. The indexedFields map is fieldName -> type with
        allowed types TEXT, NUMERIC, TIMESTAMP. Cadence accepts @hourly, @daily, @manual, or a quartz cron
        expression. sourceClusterElementName is optional — when omitted, the server picks the first ItemReader
        cluster element on the source component.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "contextStoreId": {"type": "integer", "description": "Parent Context Store ID; required."},
                    "name": {"type": "string", "description": "Display name for the source"},
                    "entityName": {"type": "string", "description": "Stable wire identifier for the records."},
                    "description": {"type": "string"},
                    "idField": {"type": "string"},
                    "indexedFields": {
                        "type": "object",
                        "description": "Flat map of fieldName -> type (TEXT, NUMERIC, TIMESTAMP)"
                    },
                    "storedFields": {"type": "object"},
                    "parameters": {"type": "object"},
                    "sourceComponentName": {"type": "string"},
                    "sourceComponentVersion": {"type": "integer"},
                    "sourceClusterElementName": {
                        "type": "string",
                        "description": "Optional. Omit to auto-pick the first ItemReader cluster element on the source component."
                    },
                    "connectionId": {"type": "integer"},
                    "cadence": {"type": "string", "description": "@hourly, @daily, @manual, or a cron expression"}
                },
                "required": ["contextStoreId", "name", "entityName", "idField", "indexedFields",
                    "sourceComponentName", "sourceComponentVersion", "cadence"]
            }""";

    private final WorkspaceContextStoreSourceFacade workspaceContextStoreSourceFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateContextStoreSourceToolCallback(WorkspaceContextStoreSourceFacade workspaceContextStoreSourceFacade) {
        this.workspaceContextStoreSourceFacade = workspaceContextStoreSourceFacade;
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
            CreateContextStoreSourceToolInput input =
                jsonMapper.readValue(toolInput, CreateContextStoreSourceToolInput.class);

            if (input.name() == null || input.name()
                .isBlank()) {
                return toolError("name is required");
            }

            if (input.entityName() == null || input.entityName()
                .isBlank()) {
                return toolError("entityName is required");
            }

            if (input.idField() == null || input.idField()
                .isBlank()) {
                return toolError("idField is required");
            }

            if (input.indexedFields() == null || input.indexedFields()
                .isEmpty()) {
                return toolError("indexedFields must contain at least one entry");
            }

            if (input.sourceComponentName() == null || input.sourceComponentName()
                .isBlank()) {
                return toolError("sourceComponentName is required");
            }

            if (input.cadence() == null || input.cadence()
                .isBlank()) {
                return toolError("cadence is required");
            }

            AgentToolInvocationContext invocationContext =
                AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the AI Hub of a workspace.");
            }

            if (input.contextStoreId() == null) {
                return toolError("contextStoreId is required");
            }

            CreateContextStoreSourceInput facadeInput = new CreateContextStoreSourceInput(
                input.contextStoreId(),
                input.name(),
                input.entityName(),
                input.description(),
                input.sourceComponentName(),
                input.sourceComponentVersion() == null ? 1 : input.sourceComponentVersion(),
                input.sourceClusterElementName(),
                input.connectionId(),
                input.cadence(),
                null,
                null,
                input.idField(),
                input.storedFields(),
                input.indexedFields(),
                null,
                input.parameters());

            ContextStoreSource created = workspaceContextStoreSourceFacade.create(workspaceId, facadeInput);

            Map<String, Object> response = new LinkedHashMap<>();

            response.put("id", created.getId());
            response.put("workflowId", created.getWorkflowId());
            response.put("status", created.getStatus()
                .name());
            response.put("enabled", created.isEnabled());

            return jsonMapper.writeValueAsString(response);
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, CreateContextStoreSourceToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record CreateContextStoreSourceToolInput(
        @Nullable Long contextStoreId,
        String name,
        String entityName,
        @Nullable String description,
        String idField,
        Map<String, Object> indexedFields,
        @Nullable Map<String, Object> storedFields,
        @Nullable Map<String, Object> parameters,
        String sourceComponentName,
        @Nullable Integer sourceComponentVersion,
        @Nullable String sourceClusterElementName,
        @Nullable Long connectionId,
        String cadence) {
    }
}
