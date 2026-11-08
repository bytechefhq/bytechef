/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.facade.EnvironmentPromotionFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * @version ee
 * @author Ivica Cardic
 */
public class PromoteToEnvironmentToolCallback implements ToolCallback {

    private static final Map<PromotionResourceType, String> TOOL_NAMES = Map.of(
        PromotionResourceType.API_COLLECTION, "promoteApiCollection",
        PromotionResourceType.MCP_SERVER, "promoteMcpServer",
        PromotionResourceType.A2A_SERVER, "promoteA2aServer",
        PromotionResourceType.PROJECT_DEPLOYMENT, "promoteProjectDeployment");

    private static final Map<PromotionResourceType, String> RESOURCE_LABELS = Map.of(
        PromotionResourceType.API_COLLECTION, "API collection",
        PromotionResourceType.MCP_SERVER, "MCP server",
        PromotionResourceType.A2A_SERVER, "A2A server",
        PromotionResourceType.PROJECT_DEPLOYMENT, "project deployment");

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "sourceId": {
                    "type": "number",
                    "description": "Id of the resource to promote, in its CURRENT environment."
                },
                "targetEnvironment": {
                    "type": "string",
                    "enum": ["DEVELOPMENT", "STAGING", "PRODUCTION"],
                    "description": "Environment to promote into. Must differ from the source's own."
                },
                "connectionMappings": {
                    "type": "object",
                    "description": "Optional. Source connection id (as a string key) to the target-environment \
        connection id to bind in its place. Anything not mapped is auto-matched by component and name where possible; \
        what still cannot be resolved comes back in unresolvedConnectionIds.",
                    "additionalProperties": {
                        "type": "number"
                    }
                }
            },
            "required": ["sourceId", "targetEnvironment"]
        }""";

    private final EnvironmentPromotionFacade environmentPromotionFacade;
    private final EnvironmentService environmentService;
    private final PromotionResourceType resourceType;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public PromoteToEnvironmentToolCallback(
        PromotionResourceType resourceType, EnvironmentPromotionFacade environmentPromotionFacade,
        EnvironmentService environmentService) {

        this.resourceType = resourceType;
        this.environmentPromotionFacade = environmentPromotionFacade;
        this.environmentService = environmentService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAMES.get(resourceType))
            .description(description())
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
            PromoteInput input = JsonUtils.read(toolInput, PromoteInput.class);

            Long sourceId = input.sourceId();

            if (sourceId == null) {
                return ToolErrors.toolError("sourceId is required.");
            }

            Environment targetEnvironment = environmentService.getEnvironment(input.targetEnvironment());

            long targetEnvironmentId = targetEnvironment.ordinal();

            Map<Long, Long> connectionMappings = connectionMappings(input.connectionMappings());

            EnvironmentPromotionPreview preview =
                environmentPromotionFacade.preview(resourceType, sourceId, targetEnvironmentId);

            EnvironmentPromotionResult result = environmentPromotionFacade.promote(
                resourceType, sourceId, targetEnvironmentId, connectionMappings);

            return JsonUtils.write(toSummary(targetEnvironment, preview, result));
        } catch (JacksonException exception) {
            return ToolErrors.toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return ToolErrors.toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                PromoteToEnvironmentToolCallback.class, TOOL_NAMES.get(resourceType), exception);
        }
    }

    private String description() {
        return DESCRIPTION_TEMPLATE.replace("{resource}", RESOURCE_LABELS.get(resourceType));
    }

    private static final String DESCRIPTION_TEMPLATE =
        """
            Promote a {resource} to the same {resource} in another environment, matched by cross-environment lineage rather than by \
            name, so a rename on either side does not break the link. Creates the counterpart if it does not exist \
            yet, otherwise updates the existing one IN PLACE. Use for "push the orders {resource} to production", "promote \
            this to staging", "copy my {resource} from dev to prod".

            A re-promotion syncs only the exposed surface: the pinned project version and the set of exposed \
            workflows with their mapping metadata. Everything environment-local (name, description, tags, every \
            enabled flag, authentication settings, secret keys and connection bindings already present in the \
            target) is left untouched.

            A NEWLY CREATED counterpart is always created DISABLED, so tell the user it needs enabling after they \
            review it. Any connection that could not be matched in the target environment comes back in \
            unresolvedConnectionIds; resolve those with connectionMappings and call again, or tell the user which \
            ones need wiring.""";

    private static Map<Long, Long> connectionMappings(@Nullable Map<String, Long> connectionMappings) {
        if (connectionMappings == null || connectionMappings.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> parsedConnectionMappings = new LinkedHashMap<>();

        for (Map.Entry<String, Long> entry : connectionMappings.entrySet()) {
            parsedConnectionMappings.put(Long.valueOf(entry.getKey()), entry.getValue());
        }

        return parsedConnectionMappings;
    }

    private PromotionSummary toSummary(
        Environment targetEnvironment, EnvironmentPromotionPreview preview, EnvironmentPromotionResult result) {

        List<String> notes = new ArrayList<>(preview.warnings());

        if (result.created()) {
            notes.add(
                "The promoted %s was created DISABLED; enable it after reviewing its connections."
                    .formatted(RESOURCE_LABELS.get(resourceType)));
        }

        if (!result.unresolvedConnectionIds()
            .isEmpty()) {

            notes.add(
                "%s source connection(s) could not be matched in %s; bind them before enabling."
                    .formatted(
                        result.unresolvedConnectionIds()
                            .size(),
                        targetEnvironment.name()));
        }

        return new PromotionSummary(
            RESOURCE_LABELS.get(resourceType), preview.sourceId(),
            preview.sourceEnvironment()
                .name(),
            targetEnvironment.name(), result.targetId(), result.created(), result.targetUrl(),
            result.unresolvedConnectionIds(), notes);
    }

    public record PromoteInput(
        @Nullable Long sourceId, @Nullable String targetEnvironment, @Nullable Map<String, Long> connectionMappings) {

        public PromoteInput {
            connectionMappings = connectionMappings == null ? null : Map.copyOf(connectionMappings);
        }
    }

    public record PromotionSummary(
        String resource, long sourceId, String sourceEnvironment, String targetEnvironment, long targetId,
        boolean created, @Nullable String targetUrl, List<Long> unresolvedConnectionIds, List<String> notes) {

        public PromotionSummary {
            unresolvedConnectionIds = List.copyOf(unresolvedConnectionIds);
            notes = List.copyOf(notes);
        }
    }
}
