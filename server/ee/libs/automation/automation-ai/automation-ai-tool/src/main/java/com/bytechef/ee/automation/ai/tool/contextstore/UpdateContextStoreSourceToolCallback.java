/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ee.automation.contextstore.dto.UpdateContextStoreSourceInput;
import com.bytechef.ee.automation.contextstore.facade.ContextStoreSourceFacade;
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
 * Spring AI {@link ToolCallback} that updates a Context Store source's name, cadence, or enabled flag. A cadence change
 * rewrites the auto-generated workflow's cron-trigger parameter only; an enabled change toggles the underlying
 * {@code ProjectDeploymentWorkflow}. All fields are optional — {@code null} means "leave unchanged".
 *
 * <p>
 * Calls the authorization-enforcing {@link ContextStoreSourceFacade}, which requires the admin role and resolves the
 * source's owning workspace itself — the tool needs no workspace context of its own.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class UpdateContextStoreSourceToolCallback implements ToolCallback {

    static final String TOOL_NAME = "updateContextStoreSource";

    private static final String DESCRIPTION = """
        Update a Context Store source's name, cadence, or enabled flag. Pass only the fields to change; null/missing
        fields are left untouched. A cadence change targets the workflow's cron-trigger parameter only; the rest of
        the workflow definition is preserved. Requires the admin role - a non-admin caller is rejected. Confirm with
        the user before calling.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "id": {"type": "integer", "description": "Context Store source id to update"},
                    "name": {"type": "string"},
                    "cadence": {"type": "string", "description": "@hourly, @daily, @manual, or a cron expression"},
                    "enabled": {"type": "boolean"}
                },
                "required": ["id"]
            }""";

    private final ContextStoreSourceFacade contextStoreSourceFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public UpdateContextStoreSourceToolCallback(ContextStoreSourceFacade contextStoreSourceFacade) {
        this.contextStoreSourceFacade = contextStoreSourceFacade;
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
            UpdateContextStoreSourceToolInput input =
                jsonMapper.readValue(toolInput, UpdateContextStoreSourceToolInput.class);

            if (input.id() == null) {
                return toolError("id is required");
            }

            UpdateContextStoreSourceInput facadeInput = new UpdateContextStoreSourceInput(
                input.name(), input.cadence(), input.enabled(), null, null);

            ContextStoreSource updated = contextStoreSourceFacade.updateContextStoreSource(input.id(), facadeInput);

            Map<String, Object> response = new LinkedHashMap<>();

            response.put("id", updated.getId());
            response.put("name", updated.getName());
            response.put("cadence", updated.getCadence());
            response.put("enabled", updated.isEnabled());
            response.put("status", updated.getStatus()
                .name());

            return jsonMapper.writeValueAsString(response);
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, UpdateContextStoreSourceToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record UpdateContextStoreSourceToolInput(
        Long id, @Nullable String name, @Nullable String cadence, @Nullable Boolean enabled) {
    }
}
