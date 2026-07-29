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
 * Spring AI {@link ToolCallback} that toggles a Context Store source's enabled flag. Flipping enabled also toggles the
 * underlying {@code ProjectDeploymentWorkflow}, which in turn enables/disables the cron trigger in the scheduler. A
 * disabled source's records remain queryable; only the periodic sync is paused.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class SetContextStoreSourceEnabledToolCallback implements ToolCallback {

    static final String TOOL_NAME = "setContextStoreSourceEnabled";

    private static final String DESCRIPTION = """
        Enable or disable periodic sync for a Context Store source. Disabling pauses the cron trigger in the
        scheduler — already-synced records remain queryable. Enabling resumes the schedule on the source's existing
        cadence.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "id": {"type": "integer", "description": "Context Store source id"},
                    "enabled": {"type": "boolean", "description": "true to resume periodic sync, false to pause it"}
                },
                "required": ["id", "enabled"]
            }""";

    private final WorkspaceContextStoreSourceFacade workspaceContextStoreSourceFacade;
    private final WorkspaceContextStoreSourceService workspaceContextStoreSourceService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SetContextStoreSourceEnabledToolCallback(
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
            SetContextStoreSourceEnabledToolInput input =
                jsonMapper.readValue(toolInput, SetContextStoreSourceEnabledToolInput.class);

            if (input.id() == null) {
                return toolError("id is required");
            }

            if (input.enabled() == null) {
                return toolError("enabled is required");
            }

            Long workspaceId =
                workspaceContextStoreSourceService.fetchWorkspaceIdByContextStoreSourceId(input.id())
                    .orElse(null);

            if (workspaceId == null) {
                return toolError("ContextStoreSource " + input.id() + " has no owning workspace");
            }

            workspaceContextStoreSourceFacade.setEnabled(workspaceId, input.id(), input.enabled());

            return jsonMapper.writeValueAsString(Map.of("id", input.id(), "enabled", input.enabled()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, SetContextStoreSourceEnabledToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record SetContextStoreSourceEnabledToolInput(Long id, Boolean enabled) {
    }
}
