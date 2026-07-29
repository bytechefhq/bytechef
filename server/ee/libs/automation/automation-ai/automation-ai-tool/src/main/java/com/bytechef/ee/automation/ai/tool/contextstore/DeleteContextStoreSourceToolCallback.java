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
 * Spring AI {@link ToolCallback} that deletes a Context Store source. Cascade deletes its entities, records, index
 * rows, the auto-generated workflow, and the corresponding {@code ProjectDeploymentWorkflow}. Irreversible — confirm
 * with the user before calling.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class DeleteContextStoreSourceToolCallback implements ToolCallback {

    static final String TOOL_NAME = "deleteContextStoreSource";

    private static final String DESCRIPTION = """
        Delete a Context Store source. Cascade deletes its entities, records, index rows, the auto-generated sync
        workflow, and its ProjectDeploymentWorkflow. Irreversible. Always confirm with the user before calling.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "id": {"type": "integer", "description": "Context Store source id to delete"}
                },
                "required": ["id"]
            }""";

    private final WorkspaceContextStoreSourceFacade workspaceContextStoreSourceFacade;
    private final WorkspaceContextStoreSourceService workspaceContextStoreSourceService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DeleteContextStoreSourceToolCallback(
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
            DeleteContextStoreSourceToolInput input =
                jsonMapper.readValue(toolInput, DeleteContextStoreSourceToolInput.class);

            if (input.id() == null) {
                return toolError("id is required");
            }

            Long workspaceId =
                workspaceContextStoreSourceService.fetchWorkspaceIdByContextStoreSourceId(input.id())
                    .orElse(null);

            if (workspaceId == null) {
                return toolError("ContextStoreSource " + input.id() + " has no owning workspace");
            }

            workspaceContextStoreSourceFacade.delete(workspaceId, input.id());

            return jsonMapper.writeValueAsString(Map.of("deleted", true, "id", input.id()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, DeleteContextStoreSourceToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record DeleteContextStoreSourceToolInput(Long id) {
    }
}
