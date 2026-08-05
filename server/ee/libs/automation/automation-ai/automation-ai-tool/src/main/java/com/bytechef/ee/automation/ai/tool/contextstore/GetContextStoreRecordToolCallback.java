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
import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that fetches a single Context Store record by its (sourceId, sourceRecordId) tuple.
 * Read-only; thin wrapper around {@link ContextStoreQueryService#get}. Workspace scoping is enforced by resolving the
 * {@code sourceId} against the chat's workspace before issuing the lookup.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class GetContextStoreRecordToolCallback implements ToolCallback {

    static final String TOOL_NAME = "getContextStoreRecord";

    private static final String DESCRIPTION = """
        Fetch a single Context Store record by sourceId and the record's source-system id. Returns the record's
        payload, lastSeenAt, and metadata. Returns {error: "Record not found"} when the tuple does not resolve to a
        row.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "sourceId": {"type": "integer", "description": "Context Store source id"},
                    "sourceRecordId": {"type": "string", "description": "The source system's id for the record"}
                },
                "required": ["sourceId", "sourceRecordId"]
            }""";

    private final ContextStoreQueryService contextStoreQueryService;
    private final WorkspaceContextStoreSourceService workspaceContextStoreSourceService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public GetContextStoreRecordToolCallback(
        ContextStoreQueryService contextStoreQueryService,
        WorkspaceContextStoreSourceService workspaceContextStoreSourceService) {

        this.contextStoreQueryService = contextStoreQueryService;
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
            GetContextStoreRecordInput input = jsonMapper.readValue(toolInput, GetContextStoreRecordInput.class);

            if (input.sourceId() == null) {
                return toolError("sourceId is required");
            }

            if (input.sourceRecordId() == null || input.sourceRecordId()
                .isBlank()) {
                return toolError("sourceRecordId is required");
            }

            AgentToolInvocationContext invocationContext =
                AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the AI Hub of a workspace.");
            }

            Long sourceWorkspaceId = workspaceContextStoreSourceService
                .fetchWorkspaceIdByContextStoreSourceId(input.sourceId())
                .orElse(null);

            if (sourceWorkspaceId == null || !workspaceId.equals(sourceWorkspaceId)) {
                return toolError("Context Store source " + input.sourceId() + " not found in the current workspace.");
            }

            Optional<ContextStoreRecord> recordOptional = contextStoreQueryService.get(
                input.sourceId(), input.sourceRecordId());

            if (recordOptional.isEmpty()) {
                return toolError("Record not found");
            }

            ContextStoreRecord record = recordOptional.get();

            Map<String, Object> response = new LinkedHashMap<>();

            response.put("id", record.getId());
            response.put("sourceRecordId", record.getSourceRecordId());
            response.put("payload", record.getPayload());

            Instant lastSeenAt = record.getLastSeenAt();

            response.put("lastSeenAt", lastSeenAt == null ? null : lastSeenAt.toString());

            Instant deletedAt = record.getDeletedAt();

            response.put("deletedAt", deletedAt == null ? null : deletedAt.toString());

            return jsonMapper.writeValueAsString(response);
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, GetContextStoreRecordToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record GetContextStoreRecordInput(Long sourceId, String sourceRecordId) {
    }
}
