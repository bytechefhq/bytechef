/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * @version ee
 * @author Ivica Cardic
 */
public class ListApiCollectionsToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "listApiCollections";

    private static final String DESCRIPTION = """
        List the API collections belonging to the current workspace. Use this before createApiCollection
        when the user references a collection by name ("the staging customers API",
        "my main collection") so you can resolve the name to a numeric id without inventing one. Returns a
        JSON array of {id, name, description, contextPath, projectId, projectDeploymentId, environment,
        enabled} entries. Optional projectId filters to a single project's collections.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "projectId": {
                    "type": "number",
                    "description": "Optional. Project id to filter collections to a single project."
                }
            }
        }""";

    private final ApiCollectionFacade apiCollectionFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListApiCollectionsToolCallback(ApiCollectionFacade apiCollectionFacade) {
        this.apiCollectionFacade = apiCollectionFacade;
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
            AutomationToolInvocationContext context =
                AutomationToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = context == null ? null : context.workspaceId();

            if (workspaceId == null) {
                return ToolErrors.toolError("Workspace context unavailable - open this chat from a workspace.");
            }

            ListApiCollectionsInput input = JsonUtils.read(toolInput, ListApiCollectionsInput.class);

            List<ApiCollectionDTO> collections = apiCollectionFacade.getApiCollections(
                workspaceId, context.environmentId(), input.projectId(), null);

            return JsonUtils.write(collections.stream()
                .map(ListApiCollectionsToolCallback::toSummary)
                .toList());
        } catch (JacksonException exception) {
            return ToolErrors.toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(ListApiCollectionsToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static ApiCollectionSummary toSummary(ApiCollectionDTO collection) {
        Environment environment = collection.environment();

        return new ApiCollectionSummary(
            collection.id(), collection.name(), collection.description(), collection.contextPath(),
            collection.projectId(), collection.projectDeploymentId(),
            environment == null ? null : environment.name(), collection.enabled());
    }

    public record ListApiCollectionsInput(@Nullable Long projectId) {
    }

    public record ApiCollectionSummary(
        @Nullable Long id, String name, @Nullable String description, String contextPath, long projectId,
        long projectDeploymentId, @Nullable String environment, boolean enabled) {
    }
}
