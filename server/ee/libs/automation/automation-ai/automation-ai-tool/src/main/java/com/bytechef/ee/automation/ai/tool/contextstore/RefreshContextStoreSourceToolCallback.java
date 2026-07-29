/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreSourceFacade;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that triggers an immediate sync run on a Context Store source. Returns the created
 * Atlas job id (not a Spring Batch execution id) so the agent can report back the in-flight sync. Honors the source's
 * existing cadence and workflow definition — does not mutate either.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class RefreshContextStoreSourceToolCallback implements ToolCallback {

    static final String TOOL_NAME = "refreshContextStoreSource";

    private static final String DESCRIPTION = """
        Trigger an immediate sync run on a Context Store source. Honors the source's existing workflow definition;
        does not mutate cadence or any other field. Returns the created job id so the user can be told a sync is
        in-flight.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "id": {"type": "integer", "description": "Context Store source id to sync now"}
                },
                "required": ["id"]
            }""";

    private final WorkspaceContextStoreSourceFacade workspaceContextStoreSourceFacade;
    private final WorkspaceContextStoreSourceService workspaceContextStoreSourceService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public RefreshContextStoreSourceToolCallback(
        WorkspaceContextStoreSourceFacade workspaceContextStoreSourceFacade,
        WorkspaceContextStoreSourceService workspaceContextStoreSourceService) {

        this.workspaceContextStoreSourceFacade = workspaceContextStoreSourceFacade;
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
            RefreshContextStoreSourceToolInput input =
                jsonMapper.readValue(toolInput, RefreshContextStoreSourceToolInput.class);

            if (input.id() == null) {
                return toolError("id is required");
            }

            Long workspaceId =
                workspaceContextStoreSourceService.fetchWorkspaceIdByContextStoreSourceId(input.id())
                    .orElse(null);

            if (workspaceId == null) {
                return toolError("ContextStoreSource " + input.id() + " has no owning workspace");
            }

            long jobId = workspaceContextStoreSourceFacade.refreshNow(workspaceId, input.id());

            return jsonMapper.writeValueAsString(Map.of("id", input.id(), "jobId", jobId));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, RefreshContextStoreSourceToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record RefreshContextStoreSourceToolInput(Long id) {
    }
}
