/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.ai.agent.tool.ToolErrors;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * Signaling-only Spring AI {@link ToolCallback} that instructs the chat client to render a dropdown of EXISTING
 * workspace connections for a given component so the user can pick one. Companion to
 * {@link CreateConnectionToolCallback}, which is reserved for the "create a new connection" intent — this one is
 * reserved for the "pick from existing" intent. Keeping the two intents under distinct tools means the LLM communicates
 * its intent cleanly and the client renders an unambiguous UI for each case (a single Connect button vs a Select
 * dropdown).
 *
 * <p>
 * Server returns a marker payload with {@code kind: "select-connection"} that the client subscriber recognises and
 * renders as a dropdown. The client fetches the list of existing connections itself (via the standard
 * {@code useGetWorkspaceConnectionsQuery}) — no list is embedded in the payload, both to keep the tool result compact
 * and to ensure freshness if a connection was added/removed between the LLM's last {@code listConnectionsForComponent}
 * call and the user clicking the dropdown.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SelectConnectionToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(SelectConnectionToolCallback.class);

    private static final String DESCRIPTION = """
        Ask the user to PICK an existing connection for a specific component — for example to choose
        between multiple already-configured Slack workspaces. Call this when the user has multiple
        existing connections for the component AND the next workflow step needs exactly one. The
        client renders a dropdown listing the workspace's existing connections for this component; the
        user picks one and their choice is captured as their next chat message. Do NOT use this tool
        to create a new connection — use createConnection for that. Do NOT use this tool when the
        workspace has zero or one matching connection — call createConnection (zero existing) or
        just proceed with the single existing connection (one existing).""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "componentName": {
                        "type": "string",
                        "description": "Lowercase slug of the component whose existing connections the user should pick from — e.g. \\"slack\\", \\"gmail\\""
                    }
                },
                "required": ["componentName"]
            }""";

    private final ComponentDefinitionService componentDefinitionService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SelectConnectionToolCallback(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("selectConnection")
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
            SelectConnectionInput input = JsonUtils.read(toolInput, SelectConnectionInput.class);

            String componentName = input.componentName();

            if (componentName == null || componentName.isBlank()) {
                return toolError("componentName is required and must not be blank");
            }

            Optional<ComponentDefinition> componentDefinition =
                componentDefinitionService.fetchComponentDefinition(componentName, null);

            String resolvedFromComponentName = null;

            if (componentDefinition.isEmpty()) {
                // The slug doesn't exist verbatim. If a colloquial name (e.g. "gmail"/"google-mail") unambiguously
                // resolves to a single catalog slug, render the picker for it directly — this saves the agent a wasted
                // round-trip guessing the exact slug. An ambiguous or unknown name still fails loud with candidate
                // slugs: emitting a "select-connection" marker with a bogus or wrong slug would otherwise hard-fail the
                // client's strict component-definition lookup with an opaque "Bad Request". Mirrors the
                // resolve/validate
                // path in CreateConnectionToolCallback.
                String resolvedComponentName =
                    ComponentSlugSuggestions.resolveSingleMatch(componentName, componentDefinitionService);

                if (resolvedComponentName == null) {
                    return toolError(
                        ComponentSlugSuggestions.unknownComponentMessage(componentName, componentDefinitionService));
                }

                resolvedFromComponentName = componentName;
                componentName = resolvedComponentName;
                componentDefinition = componentDefinitionService.fetchComponentDefinition(componentName, null);

                if (componentDefinition.isEmpty()) {
                    // The slug came straight from the catalog scan, so a missing definition here means a concurrent
                    // catalog change between the two lookups — fail loud rather than emit a marker for a vanished slug.
                    return toolError(
                        ComponentSlugSuggestions.unknownComponentMessage(componentName, componentDefinitionService));
                }
            }

            String componentLabel = resolveComponentLabel(componentDefinition.get(), componentName);

            return JsonUtils.write(
                new SelectConnectionOutput(
                    "select-connection", componentName, componentLabel, resolvedFromComponentName));
        } catch (JacksonException exception) {
            // Malformed tool input is a recoverable LLM error — return a typed tool error so the agent can retry.
            log.warn(
                "selectConnection rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            // Outer guard mirrors every other subagent callback in this directory.
            return ToolErrors.runtimeFailure(
                SelectConnectionToolCallback.class, "selectConnection", exception);
        }
    }

    private static String resolveComponentLabel(ComponentDefinition componentDefinition, String componentName) {
        String title = componentDefinition.getTitle();

        return (title == null || title.isBlank()) ? componentName : title;
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record SelectConnectionInput(String componentName) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SelectConnectionOutput(String kind, String componentName, String componentLabel,
        @Nullable String resolvedFromComponentName) {
    }
}
