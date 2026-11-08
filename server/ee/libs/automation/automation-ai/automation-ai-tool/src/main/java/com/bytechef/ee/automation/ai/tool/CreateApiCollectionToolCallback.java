/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.ai.tool.ToolValidationErrors;
import com.bytechef.commons.util.JsonUtils;
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

/**
 * @version ee
 * @author Ivica Cardic
 */
@SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
public class CreateApiCollectionToolCallback implements ToolCallback {

    static final String TOOL_NAME = "createApiCollection";

    private static final String SUPPORTED_ENVIRONMENTS = Arrays.stream(Environment.values())
        .map(Environment::name)
        .collect(Collectors.joining(", "));

    private static final String DESCRIPTION = """
        Create an API collection — the EE "Deploy as REST" surface — that bundles a project version under
        a context path. Supply projectId, projectVersion (PUBLISHED), environment (one of: %s), name,
        and contextPath (the URL prefix for all endpoints in this collection, e.g. "billing-v1").
        Optionally supply description and collectionVersion (defaults to 1 — bump when publishing a
        breaking-change revision of the API contract). Returns {apiCollectionId, projectDeploymentId,
        contextPath, collectionVersion} on success. Endpoints — the per-workflow REST routes — are added
        separately and are not created by this tool.""".formatted(SUPPORTED_ENVIRONMENTS);

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "projectId": {"type": "string", "description": "Numeric project id"},
                "projectVersion": {"type": "integer", "description": "PUBLISHED project version"},
                "environment": {"type": "string", "description": "DEVELOPMENT, STAGING, or PRODUCTION"},
                "name": {"type": "string", "description": "Human-readable collection name"},
                "contextPath": {"type": "string", "description": "URL prefix for endpoints in this collection"},
                "description": {"type": "string", "description": "Optional description"},
                "collectionVersion": {"type": "integer", "description": "API contract version, defaults to 1"}
            },
            "required": ["projectId", "projectVersion", "environment", "name", "contextPath"]
        }""";

    private final ApiCollectionFacade apiCollectionFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateApiCollectionToolCallback(ApiCollectionFacade apiCollectionFacade) {
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
            CreateApiCollectionInput input = JsonUtils.read(toolInput, CreateApiCollectionInput.class);

            if (input.projectId() == null || input.projectId()
                .isBlank()) {
                return toolError("projectId is required");
            }

            if (input.projectVersion() == null) {
                return toolError("projectVersion is required");
            }

            if (input.environment() == null || input.environment()
                .isBlank()) {
                return toolError("environment is required (one of: " + SUPPORTED_ENVIRONMENTS + ")");
            }

            if (input.name() == null || input.name()
                .isBlank()) {
                return toolError("name is required");
            }

            if (input.contextPath() == null || input.contextPath()
                .isBlank()) {
                return toolError("contextPath is required");
            }

            long projectId;

            try {
                projectId = Long.parseLong(input.projectId());
            } catch (NumberFormatException exception) {
                return toolError("Invalid projectId — must be a numeric id");
            }

            Environment environment;

            try {
                environment = Environment.valueOf(input.environment()
                    .toUpperCase());
            } catch (IllegalArgumentException exception) {
                return toolError(
                    "Unknown environment '" + input.environment() + "'. Supported: " + SUPPORTED_ENVIRONMENTS);
            }

            Integer inputCollectionVersion = input.collectionVersion();

            int collectionVersion = inputCollectionVersion == null ? 1 : inputCollectionVersion;

            ApiCollectionDTO dto = new ApiCollectionDTO(
                collectionVersion, input.contextPath(), null, null, input.description(), false, List.of(),
                environment, null, null, null, input.name(), null, projectId, null, 0, input.projectVersion(),
                List.of(), 0, null);

            ApiCollectionDTO created = apiCollectionFacade.createApiCollection(dto);

            return JsonUtils.write(
                new CreateApiCollectionOutput(
                    created.id(), created.projectDeploymentId(), created.contextPath(),
                    created.collectionVersion()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException | com.bytechef.exception.ConfigurationException exception) {
            return ToolValidationErrors.validationFailure(CreateApiCollectionToolCallback.class, TOOL_NAME, exception);
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(CreateApiCollectionToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record CreateApiCollectionInput(
        String projectId, Integer projectVersion, String environment, String name, String contextPath,
        @Nullable String description, @Nullable Integer collectionVersion) {
    }

    public record CreateApiCollectionOutput(
        long apiCollectionId, long projectDeploymentId, String contextPath, int collectionVersion) {
    }
}
