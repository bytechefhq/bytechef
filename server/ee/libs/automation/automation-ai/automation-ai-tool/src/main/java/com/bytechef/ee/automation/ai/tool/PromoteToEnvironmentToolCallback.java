/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
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
import tools.jackson.databind.json.JsonMapper;

/**
 * Promotes one resource to its counterpart in another environment.
 *
 * <p>
 * One implementation, registered once per {@link PromotionResourceType} under a concrete tool name
 * ({@code promoteApiCollection}, {@code promoteMcpServer}, {@code promoteA2aServer}, {@code promoteProjectDeployment}).
 * <b>Concrete verbs rather than one generic tool taking a type argument</b>, even though the GraphQL surface below is
 * deliberately generic: a generic tool would let the model pair a correct {@code sourceId} with the WRONG
 * {@code resourceType}, and since ids are per-table a plausible id usually exists in the other table too — the
 * promotion would succeed against something the user never named. Four names make that class of mistake
 * unrepresentable, and cost only four catalog entries.
 * </p>
 *
 * <p>
 * <b>One shot, previewed internally.</b> The tool runs {@code preview} before {@code promote} purely to enrich its own
 * result — the target's name, the connections that will not resolve, the handler's warnings — so the model can tell the
 * user what happened without a second round trip and without a separate dry-run tool to keep in sync. The promotion
 * itself is a single call.
 * </p>
 *
 * <p>
 * <b>The result must be read carefully by the model, hence {@code disabled} is stated explicitly.</b> A newly created
 * counterpart is always created disabled (spec §6.3), whether or not its connections resolved, so reporting only
 * "promoted" would leave the user believing traffic is flowing.
 * </p>
 *
 * @version ee
 *
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
    private final JsonMapper jsonMapper = new JsonMapper();
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
            PromoteInput input = jsonMapper.readValue(toolInput, PromoteInput.class);

            Long sourceId = input.sourceId();

            if (sourceId == null) {
                return ToolErrors.toolError(jsonMapper, "sourceId is required.");
            }

            Environment targetEnvironment = environmentService.getEnvironment(input.targetEnvironment());

            long targetEnvironmentId = targetEnvironment.ordinal();

            Map<Long, Long> connectionMappings = connectionMappings(input.connectionMappings());

            // Previewed only to enrich the result; the promotion below is what writes.
            EnvironmentPromotionPreview preview =
                environmentPromotionFacade.preview(resourceType, sourceId, targetEnvironmentId);

            EnvironmentPromotionResult result = environmentPromotionFacade.promote(
                resourceType, sourceId, targetEnvironmentId, connectionMappings);

            return jsonMapper.writeValueAsString(toSummary(targetEnvironment, preview, result));
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return ToolErrors.toolError(jsonMapper, exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, PromoteToEnvironmentToolCallback.class, TOOL_NAMES.get(resourceType), exception);
        }
    }

    private String description() {
        // replace() rather than formatted(): the description is a multi-line text block, and a format string
        // containing literal newlines trips SpotBugs' VA_FORMAT_STRING_USES_NEWLINE. A named placeholder also reads
        // better than four positional %s in one paragraph.
        return DESCRIPTION_TEMPLATE.replace("{resource}", RESOURCE_LABELS.get(resourceType));
    }

    private static final String DESCRIPTION_TEMPLATE =
        """
            Promote a {resource} to the same {resource} in another environment, matched by cross-environment lineage rather than by \
            name, so a rename on either side does not break the link. Creates the counterpart if it does not exist \
            yet, otherwise updates the existing one IN PLACE. Use for "push the orders {resource} to production", "promote \
            this to staging", "copy my {resource} from dev to prod".

            A re-promotion syncs only the exposed surface: the pinned project version and the set of exposed \
            workflows with their mapping metadata. Everything environment-local — name, description, tags, every \
            enabled flag, authentication settings, secret keys and connection bindings already present in the \
            target — is left untouched.

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
            // JSON object keys are strings; the facade keys on the source connection id.
            parsedConnectionMappings.put(Long.valueOf(entry.getKey()), entry.getValue());
        }

        return parsedConnectionMappings;
    }

    private PromotionSummary toSummary(
        Environment targetEnvironment, EnvironmentPromotionPreview preview, EnvironmentPromotionResult result) {

        List<String> notes = new ArrayList<>(preview.warnings());

        if (result.created()) {
            notes.add(
                "The promoted %s was created DISABLED — enable it after reviewing its connections."
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
