/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.commons.util.MemoizationUtils;
import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.util.JsonSchemaGeneratorUtils;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that wraps a single tool-typed
 * {@link com.bytechef.platform.component.domain.ClusterElementDefinition} and dispatches LLM-issued invocations through
 * {@link ClusterElementDefinitionService#executeTool(String, int, String, Map, ComponentConnection, boolean)} — the
 * canonical entry point used by every other agent-component path. Reusing it gets us parameter coercion, output
 * marshalling, and the same observation hooks workflows already use, for free.
 *
 * <p>
 * Connection resolution is workspace-scoped: given the {@code AiHubToolInvocationContext} threaded through the
 * {@link ToolContext}, look up active connections for {@code (componentName, environmentId)} via
 * {@link ConnectionService#getConnections(String, Integer, Long, Long, PlatformType)}. Three outcomes:
 * </p>
 * <ul>
 * <li><b>Exactly one match</b> — use it.</li>
 * <li><b>Zero matches</b> when the cluster element requires a connection — return a structured
 * {@code connectionRequired} envelope so the chat surface renders the existing
 * {@link com.bytechef.ee.ai.hub.tool.CreateConnectionToolCallback} flow as a button.</li>
 * <li><b>Multiple matches</b> — return {@code connectionAmbiguous} envelope listing the candidate connection ids; the
 * LLM asks the user which to use.</li>
 * </ul>
 *
 * <p>
 * The cluster element's connection-required flag is approximated as "is there at least one auth-typed connection
 * registered for this component". That heuristic avoids reaching into per-action connection metadata which isn't
 * uniformly populated; the price is one extra DB query per invocation. Acceptable for v1; revisit if hot-path latency
 * shows up.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ClusterElementToolCallback implements ToolCallback {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final Logger log = LoggerFactory.getLogger(ClusterElementToolCallback.class);

    private final String toolName;
    private final String description;
    private final Supplier<String> inputSchemaSupplier;
    private final String componentName;
    private final int componentVersion;
    private final String clusterElementName;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ConnectionService connectionService;
    /**
     * Pre-bound connection id (v2 per-chat tools). When set, the callback uses {@code connectionService
     * .getConnection(pinnedConnectionId)} directly and skips the workspace-scoped lookup-and-disambiguate path. When
     * null (v1 catalog discovery path), the workspace-scoped lookup runs and returns the standard zero/one/many
     * envelopes documented in the class javadoc.
     */
    private final @Nullable Long pinnedConnectionId;
    /**
     * Pre-set parameter defaults from a per-chat tool binding. Merged INTO the LLM-supplied JSON args (LLM args take
     * precedence on conflict), so the chat agent can override per-call without losing the user's persisted defaults.
     */
    private final Map<String, ?> pinnedParameters;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ClusterElementToolCallback(
        String toolName, String description, String inputSchema, String componentName, int componentVersion,
        String clusterElementName, ClusterElementDefinitionService clusterElementDefinitionService,
        ConnectionService connectionService) {

        this(toolName, description, inputSchema, componentName, componentVersion, clusterElementName,
            clusterElementDefinitionService, connectionService, null, Map.of());
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ClusterElementToolCallback(
        String toolName, String description, String inputSchema, String componentName, int componentVersion,
        String clusterElementName, ClusterElementDefinitionService clusterElementDefinitionService,
        ConnectionService connectionService, @Nullable Long pinnedConnectionId,
        Map<String, ?> pinnedParameters) {

        this.toolName = toolName;
        this.description = description;
        this.inputSchemaSupplier = () -> inputSchema;
        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.clusterElementName = clusterElementName;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.connectionService = connectionService;
        this.pinnedConnectionId = pinnedConnectionId;
        this.pinnedParameters = pinnedParameters == null ? Map.of() : Map.copyOf(pinnedParameters);
    }

    /**
     * Catalog-discovery constructor: the input schema is generated lazily on first {@link #getToolDefinition()} access
     * by loading only this one component, and memoized. Keeps building the tool-search callback map free of component
     * loads — the schema materializes only when the tool is actually surfaced to the model.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ClusterElementToolCallback(
        String toolName, String description, String componentName, int componentVersion, String clusterElementName,
        ClusterElementDefinitionService clusterElementDefinitionService, ConnectionService connectionService) {

        this.toolName = toolName;
        this.description = description;
        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.clusterElementName = clusterElementName;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.connectionService = connectionService;
        this.pinnedConnectionId = null;
        this.pinnedParameters = Map.of();
        this.inputSchemaSupplier = MemoizationUtils.memoize(
            () -> generateInputSchema(
                clusterElementDefinitionService, componentName, componentVersion, clusterElementName));
    }

    /**
     * Generates the tool's input JSON schema by loading only this one component. Relocates the former build-time
     * malformed-tool guard (which used to skip a tool whose schema failed to generate) to the lazy point: a single tool
     * with an unbuildable property tree degrades to an empty-object schema and is logged, rather than throwing and
     * failing the chat turn when the tool is surfaced.
     */
    private static String generateInputSchema(
        ClusterElementDefinitionService clusterElementDefinitionService, String componentName, int componentVersion,
        String clusterElementName) {

        try {
            return JsonSchemaGeneratorUtils.generateInputSchema(
                clusterElementDefinitionService
                    .getClusterElementDefinition(componentName, componentVersion, clusterElementName)
                    .getProperties());
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to generate input schema for tool '{}' (component {}@{}); surfacing with an empty schema",
                clusterElementName, componentName, componentVersion, exception);

            return "{\"type\":\"object\",\"properties\":{}}";
        }
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(toolName)
            .description(description)
            .inputSchema(inputSchemaSupplier.get())
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            Map<String, Object> llmParameters = parseInput(toolInput);

            // Pinned defaults from a v2 chat binding go in FIRST so LLM-supplied keys overwrite them
            // on conflict — chat-driven per-call override of a persisted default is the natural mental model
            // for users who say "this time send to #urgent instead of #engineering".
            Map<String, Object> mergedParameters = new HashMap<>(pinnedParameters);
            mergedParameters.putAll(llmParameters);

            AiHubToolInvocationContext invocationContext =
                AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.workspaceId() == null) {
                return toolError(
                    "Workspace context unavailable — open this chat from the AI Hub of a workspace.");
            }

            long environmentId = AiHubToolInvocationContext.resolveEnvironmentOrDefault(invocationContext);

            ConnectionResolution resolution = resolveConnection(environmentId);

            if (resolution.error() != null) {
                return resolution.error();
            }

            Object result = clusterElementDefinitionService.executeTool(
                componentName, componentVersion, clusterElementName, mergedParameters,
                resolution.connection(), false);

            return jsonMapper.writeValueAsString(result);
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, ClusterElementToolCallback.class, toolName, exception);
        }
    }

    private Map<String, Object> parseInput(String toolInput) throws JacksonException {
        if (toolInput == null || toolInput.isBlank()) {
            return Map.of();
        }

        return jsonMapper.readValue(toolInput, MAP_TYPE);
    }

    /**
     * Resolves the workspace's active connection for this component+environment. The "connection required" verdict is
     * derived from whether the component publishes any connections at all in this environment — see class javadoc for
     * the rationale.
     */
    private ConnectionResolution resolveConnection(long environmentId) throws JacksonException {
        // v2 path: per-chat binding pinned a specific connection at attach time. Use it directly so
        // a workspace that subsequently grew a second connection of the same component doesn't trigger the
        // "ambiguous" envelope mid-chat. The user already disambiguated when they attached.
        if (pinnedConnectionId != null) {
            Connection pinned = connectionService.getConnection(pinnedConnectionId);

            return new ConnectionResolution(toComponentConnection(pinned), null);
        }

        // Pass null for connectionVersion to match any version; the cluster element executor coerces parameters per
        // its own version. Tools that have multiple connection versions (rare) match the first active one.
        List<Connection> connections = connectionService.getConnections(
            componentName, null, null, environmentId, PlatformType.AUTOMATION);

        if (connections.isEmpty()) {
            // We can't tell from outside the cluster element whether a connection is strictly required. Tools that
            // don't need one (logic/dateTime/math) will succeed without; tools that do will fail at executeTool with
            // an authorization error. In practice a missing connection on a tool that needs one almost always means
            // the user hasn't connected the service yet — return the connectionRequired envelope so chat surfaces
            // the connect-button. If the tool truly didn't need a connection, the LLM can retry with no expected
            // change in behavior.
            String envelope = jsonMapper.writeValueAsString(Map.of(
                "connectionRequired", true,
                "componentName", componentName,
                "componentVersion", componentVersion,
                "message",
                "No active connection for component '" + componentName + "' in environment " + environmentId
                    + ". Ask the user to connect this component before invoking this tool."));

            return new ConnectionResolution(null, envelope);
        }

        if (connections.size() > 1) {
            // Multiple matching connections (e.g. two Slack workspaces). Return the structured envelope so the LLM
            // can ask the user which to use; ambiguity at this layer must NOT be silently resolved by picking the
            // first match — a wrong-target side effect is worse than an extra clarifying question.
            List<Map<String, Object>> candidates = connections.stream()
                .map(connection -> Map.<String, Object>of(
                    "id", connection.getId(),
                    "name", connection.getName()))
                .toList();

            String envelope = jsonMapper.writeValueAsString(Map.of(
                "connectionAmbiguous", true,
                "componentName", componentName,
                "candidates", candidates,
                "message", "Multiple connections found for '" + componentName
                    + "'. Ask the user which connection to use, then retry with the picked id."));

            return new ConnectionResolution(null, envelope);
        }

        Connection connection = connections.getFirst();

        ComponentConnection componentConnection = toComponentConnection(connection);

        return new ConnectionResolution(componentConnection, null);
    }

    private ComponentConnection toComponentConnection(Connection connection) {
        Map<String, Object> parameters = new HashMap<>(connection.getParameters());

        return new ComponentConnection(
            connection.getComponentName(), connection.getConnectionVersion(), connection.getId(), parameters,
            connection.getAuthorizationType());
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    /**
     * Box for the resolve outcome: either a connection (success) OR a serialized error envelope to return to the LLM.
     * Mutually exclusive — exactly one field is non-null.
     */
    private record ConnectionResolution(@Nullable ComponentConnection connection, @Nullable String error) {
    }
}
