/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
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
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that lists API collections in the current workspace. Read leg paired with the existing
 * {@link CreateApiCollectionToolCallback}: that tool requires a collection or project id, and without this tool the LLM
 * has no discoverable way to resolve a name like "the staging customers API" to a numeric id — it would either invent
 * one or have to ask the user to read it from the UI.
 *
 * <p>
 * Returns a compact summary per collection: {@code id}, {@code name}, {@code description}, {@code contextPath},
 * {@code projectId}, {@code projectDeploymentId}, {@code environment}, and {@code enabled}. The {@code environment} is
 * stringified ({@code DEVELOPMENT}, {@code PRODUCTION}) so the JSON shape stays stable across enum reorderings.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ListApiCollectionsToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "listApiCollections";

    private static final String DESCRIPTION = """
        List the API collections belonging to the current workspace. Use this before createApiCollection or
        promoteApiCollection when the user references a collection by name ("the staging customers API",
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
    private final JsonMapper jsonMapper = new JsonMapper();

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
                return ToolErrors.toolError(
                    jsonMapper,
                    "Workspace context unavailable — open this chat from the AI Hub of a workspace.");
            }

            ListApiCollectionsInput input = jsonMapper.readValue(toolInput, ListApiCollectionsInput.class);

            // Pass {@code environmentId} from the invocation context so a chat opened against PROD only sees
            // PROD-bound collections rather than every collection across every environment. tagId left null
            // — surfaces aren't tag-scoping API collections via the LLM today; can be added later.
            List<ApiCollectionDTO> collections = apiCollectionFacade.getApiCollections(
                workspaceId, context.environmentId(), input.projectId(), null);

            return jsonMapper.writeValueAsString(collections.stream()
                .map(ListApiCollectionsToolCallback::toSummary)
                .toList());
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, ListApiCollectionsToolCallback.class, TOOL_NAME, exception);
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
