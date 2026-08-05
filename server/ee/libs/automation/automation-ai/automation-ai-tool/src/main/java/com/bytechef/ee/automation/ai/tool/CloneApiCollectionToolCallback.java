/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that clones an existing
 * {@link com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection} into the target environment. The
 * clone reuses the source's projectId, projectVersion, collectionVersion, description, and (by default) name; the
 * caller MUST supply a {@code newContextPath} because the existing context path is unique within the target environment
 * and a clone with the same path would collide.
 *
 * <p>
 * Endpoints are <strong>not</strong> copied. The collection shell — projectId / projectVersion binding, context path,
 * collection version — is the slow-changing surface; endpoints follow the workflow set, which the user is expected to
 * pick deliberately after cloning. This mirrors {@link CreateApiCollectionToolCallback}'s contract: it stands up the
 * collection, the LLM follows up with {@code createApiCollectionEndpoint} for each workflow it should expose.
 * </p>
 *
 * <p>
 * Cross-environment scope: pass {@code targetEnvironment} to land the clone in DEVELOPMENT, STAGING, or PRODUCTION. The
 * underlying {@link com.bytechef.automation.configuration.domain.ProjectDeployment} is auto-created by the facade when
 * the collection is created — there's no need for the LLM to call {@code promoteWorkflow} first as a prerequisite.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
public class CloneApiCollectionToolCallback implements ToolCallback {

    static final String TOOL_NAME = "cloneApiCollection";

    private static final String SUPPORTED_ENVIRONMENTS = Arrays.stream(Environment.values())
        .map(Environment::name)
        .collect(Collectors.joining(", "));

    private static final String DESCRIPTION = """
        Clone an existing API collection into the target environment, reusing the source's projectId,
        projectVersion, collectionVersion, name, and description. Use when the user says "promote billing-v1
        to PROD" or "duplicate my customer-api into staging." Pass apiCollectionId (from listing collections),
        targetEnvironment (one of: %s), and newContextPath (REQUIRED — context paths are unique per
        environment, so the clone must use a different one). Optionally pass newName to override the
        collection name. Endpoints are NOT copied — the LLM should follow up with createApiCollectionEndpoint
        for each workflow to expose. Returns {apiCollectionId, projectDeploymentId, contextPath, environment,
        collectionVersion}.""".formatted(SUPPORTED_ENVIRONMENTS);

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "apiCollectionId": {"type": "integer", "description": "Source API collection id. Never invent ids."},
                    "targetEnvironment": {"type": "string", "description": "DEVELOPMENT, STAGING, or PRODUCTION"},
                    "newContextPath": {"type": "string", "description": "URL prefix for the clone — must be unique within the target environment"},
                    "newName": {"type": "string", "description": "Optional collection name override; falls back to the source name when omitted"}
                },
                "required": ["apiCollectionId", "targetEnvironment", "newContextPath"]
            }""";

    private final ApiCollectionFacade apiCollectionFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CloneApiCollectionToolCallback(ApiCollectionFacade apiCollectionFacade) {
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
            CloneApiCollectionInput input = jsonMapper.readValue(toolInput, CloneApiCollectionInput.class);

            Long apiCollectionId = input.apiCollectionId();

            if (apiCollectionId == null) {
                return toolError("apiCollectionId is required");
            }

            String targetEnvironmentName = input.targetEnvironment();

            if (targetEnvironmentName == null || targetEnvironmentName.isBlank()) {
                return toolError("targetEnvironment is required (one of: " + SUPPORTED_ENVIRONMENTS + ")");
            }

            String newContextPath = input.newContextPath();

            if (newContextPath == null || newContextPath.isBlank()) {
                return toolError("newContextPath is required — context paths must be unique per environment");
            }

            Environment targetEnvironment;

            try {
                targetEnvironment = Environment.valueOf(targetEnvironmentName.toUpperCase());
            } catch (IllegalArgumentException exception) {
                return toolError(
                    "Unknown targetEnvironment '" + targetEnvironmentName + "'. Supported: "
                        + SUPPORTED_ENVIRONMENTS);
            }

            ApiCollectionDTO source = apiCollectionFacade.getApiCollection(apiCollectionId);

            if (source == null) {
                return toolError("API collection " + apiCollectionId + " not found");
            }

            String newName = input.newName();

            String resolvedName = newName != null && !newName.isBlank() ? newName : source.name();

            // Reuse all slow-changing fields verbatim. Endpoints are intentionally List.of() — the LLM should add
            // them deliberately via createApiCollectionEndpoint after the clone is created. Tags are dropped on the
            // clone so the destination doesn't inherit env-specific labels (e.g. "needs-review-DEV") that would be
            // wrong in the target.
            ApiCollectionDTO cloneDto = new ApiCollectionDTO(
                source.collectionVersion(), newContextPath, null, null, source.description(), false,
                List.of(), targetEnvironment, null, null, null, resolvedName, null, source.projectId(), null, 0,
                source.projectVersion(), List.of(), 0);

            ApiCollectionDTO created = apiCollectionFacade.createApiCollection(cloneDto);

            return jsonMapper.writeValueAsString(
                new CloneApiCollectionOutput(
                    created.id(), created.projectDeploymentId(), created.contextPath(),
                    created.environment()
                        .name(),
                    created.collectionVersion()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException | com.bytechef.exception.ConfigurationException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, CloneApiCollectionToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record CloneApiCollectionInput(
        @Nullable Long apiCollectionId, @Nullable String targetEnvironment, @Nullable String newContextPath,
        @Nullable String newName) {
    }

    public record CloneApiCollectionOutput(
        long apiCollectionId, long projectDeploymentId, String contextPath, String environment,
        int collectionVersion) {
    }
}
