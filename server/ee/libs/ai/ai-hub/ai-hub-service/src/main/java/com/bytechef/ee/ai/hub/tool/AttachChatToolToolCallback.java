/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolFacade;
import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} the LLM uses to attach a configured tool to the current chat. Combines the two facade
 * calls (attach component + add tool) in one shot because the LLM never knows the internal {@code chatComponentId} — it
 * only knows component + action names.
 *
 * <p>
 * Example chat sequence:
 * </p>
 * <ol>
 * <li>User: "I want to use Slack from this chat — channel #engineering"</li>
 * <li>LLM (after a connection-resolution step): {@code attachChatTool({componentName: "slack", actionName:
 * "sendMessage", parameters: {channel: "#engineering"}, connectionId: 42})}</li>
 * <li>This callback: resolves chat_id from thread, attaches component (idempotent), upserts tool with the supplied
 * parameters, returns the persisted ids so the LLM can echo them in its reply.</li>
 * </ol>
 *
 * <p>
 * Idempotent at every level: re-attaching the same component is a no-op; re-adding a tool with new params updates the
 * existing row. The LLM can therefore reissue this callback safely whenever the user adjusts a tool's configuration
 * mid-chat.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AttachChatToolToolCallback implements ToolCallback {

    static final String TOOL_NAME = "attachChatTool";

    private static final String DESCRIPTION = """
        Attach a configured tool to the current chat so the assistant can invoke it directly in
        future turns without going through search. Use after the user expresses intent like 'set up slack'
        or 'configure github for this chat'. Supply componentName (e.g. 'slack'), actionName (e.g.
        'sendMessage'), parameters (the pre-set values that should default; the assistant can still
        override per call), and connectionId (workspace connection to bind; obtain from listConnections or
        a prior createConnection step). componentVersion defaults to 1 unless specified. Returns
        {chatToolId, chatComponentId, attachedToolName, replacedExistingTool}.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "componentName": {"type": "string", "description": "ByteChef component name (e.g. 'slack')"},
                    "actionName": {"type": "string", "description": "Cluster element / action name (e.g. 'sendMessage')"},
                    "parameters": {"type": "object", "description": "Pre-set parameter values for the action"},
                    "connectionId": {"type": "string", "description": "Optional connection id (omit for connection-less tools)"},
                    "componentVersion": {"type": "integer", "description": "Optional component version, defaults to 1"}
                },
                "required": ["componentName", "actionName", "parameters"]
            }""";

    private final AiHubChatService chatService;
    private final AiHubChatToolFacade chatToolFacade;
    private final ConnectionService connectionService;
    private final AiHubToolAttachMetrics metrics;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AttachChatToolToolCallback(
        AiHubChatService chatService,
        AiHubChatToolFacade chatToolFacade, ConnectionService connectionService, AiHubToolAttachMetrics metrics) {

        this.chatService = chatService;
        this.chatToolFacade = chatToolFacade;
        this.connectionService = connectionService;
        this.metrics = metrics;
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
            AttachChatToolInput input = jsonMapper.readValue(toolInput, AttachChatToolInput.class);

            if (input.componentName() == null || input.componentName()
                .isBlank()) {
                return toolError("componentName is required");
            }

            if (input.actionName() == null || input.actionName()
                .isBlank()) {
                return toolError("actionName is required");
            }

            AiHubToolInvocationContext invocationContext =
                AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.threadId() == null) {
                return toolError(
                    "AiHubChat context unavailable — open this chat from the AI Hub.");
            }

            Optional<AiHubChat> chat =
                chatService.findByThreadId(invocationContext.threadId());

            if (chat.isEmpty()) {
                return toolError("AiHubChat not found for thread " + invocationContext.threadId());
            }

            Long connectionId = parseConnectionId(input.connectionId());
            Integer inputComponentVersion = input.componentVersion();
            int componentVersion = inputComponentVersion == null ? 1 : inputComponentVersion;
            int environment = AiHubToolInvocationContext.resolveEnvironmentOrDefault(invocationContext);

            // Defense-in-depth: reject a cross-environment connection. listConnectionsForComponent filters by env,
            // so the LLM-suggested connectionId is normally in-env. The hole this closes: the LLM hallucinates a
            // connection id from prior chat memory (it remembered an id from a previous env's transcript), or
            // pattern-matches on an id it never saw verified. Without this check the facade accepts whatever id
            // is passed and the binding ends up pointing across env boundaries silently — caught only when a
            // human reviews the workflow editor and notices the cross-env arrow.
            if (connectionId != null) {
                String envMismatchError = checkConnectionEnvironment(connectionId, environment);

                if (envMismatchError != null) {
                    metrics.recordAttach("connection_environment_mismatch");

                    return toolError(envMismatchError);
                }
            }

            // Hardened attach: the autonomous flow may attach with connectionId=null during discovery, then
            // back-fill with a real id after the user picks one via the connection picker. The facade's
            // attachComponent idempotency tuple includes connectionId — so without this rebind path, the second
            // call creates a DUPLICATE row. We detect the (chat, component, version, env) match ignoring
            // connection and route to setComponentConnection in place. New (component, action, parameters) paths
            // still flow through attachComponent as before.
            Optional<Long> existingComponentBindingId = chatToolFacade.findChatComponentIdIgnoringConnection(
                chat.get()
                    .getId(),
                input.componentName(), componentVersion, environment);

            long chatComponentId;

            if (existingComponentBindingId.isPresent()) {
                chatComponentId = existingComponentBindingId.get();

                chatToolFacade.setComponentConnection(chatComponentId, connectionId);

                metrics.recordAttach("rebound_connection");
            } else {
                chatComponentId = chatToolFacade.attachComponent(
                    chat.get()
                        .getId(),
                    input.componentName(), componentVersion, connectionId, environment);

                metrics.recordAttach("new_component");
            }

            Map<String, ?> parameters = input.parameters() == null ? Map.of() : input.parameters();

            // addTool is upsert-by-name on the (componentBindingId, actionName) pair — re-issuing this with
            // different params updates the row in place. Whether we replaced an existing tool is interesting
            // signal for the LLM's reply, but the facade doesn't currently surface it; v2 follow-up could
            // change addTool to return a structured "createdNew vs updated" enum. For now, default false.
            long chatToolId = chatToolFacade.addTool(
                chatComponentId, input.actionName(), parameters);

            return jsonMapper.writeValueAsString(new AttachChatToolOutput(
                chatToolId, chatComponentId,
                input.componentName() + "_" + input.actionName(), false));
        } catch (JacksonException exception) {
            metrics.recordAttach("error");

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            metrics.recordAttach("error");

            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            metrics.recordAttach("error");

            return ToolErrors.runtimeFailure(
                jsonMapper, AttachChatToolToolCallback.class, TOOL_NAME, exception);
        }
    }

    /**
     * Returns a user-facing error message when the connection's environment does not match the chat's, or {@code null}
     * when the environment matches (or when the connection cannot be loaded — that path falls through to the facade,
     * which produces a clearer downstream error than a synthetic one here).
     */
    private @Nullable String checkConnectionEnvironment(long connectionId, int chatEnvironment) {
        Connection connection;

        try {
            connection = connectionService.getConnection(connectionId);
        } catch (RuntimeException exception) {
            // Connection lookup failed (doesn't exist, deleted between listConnections and this call, transient
            // DB outage). Don't synthesise a mismatch error from this — let the facade attempt the attach so the
            // user sees the canonical "connection not found / facade rejected" message instead of our second-guess.
            return null;
        }

        if (connection == null) {
            // Same rationale as the catch: the service implementation may return null on not-found instead of
            // throwing. Either path falls through to the facade for a canonical error.
            return null;
        }

        if (connection.getEnvironmentId() != chatEnvironment) {
            return "Connection " + connectionId + " belongs to environment " + connection.getEnvironmentId()
                + " but this chat is rooted in environment " + chatEnvironment
                + ". Use listConnectionsForComponent to find a connection in the current environment.";
        }

        return null;
    }

    private static @Nullable Long parseConnectionId(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException exception) {
            // Throw IllegalArgumentException to surface as a tool-error rather than silently dropping;
            // bad connection-id is almost always a hallucination the user needs to know about.
            throw new IllegalArgumentException("connectionId must be a numeric id, was: " + raw);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record AttachChatToolInput(
        String componentName, String actionName, Map<String, Object> parameters, @Nullable String connectionId,
        @Nullable Integer componentVersion) {

        // Defensive-copy via compact constructor — sidesteps SpotBugs EI on the parameters Map.
        public AttachChatToolInput {
            parameters = parameters == null ? null : Map.copyOf(parameters);
        }
    }

    public record AttachChatToolOutput(
        long chatToolId, long chatComponentId, String attachedToolName,
        boolean replacedExistingTool) {
    }
}
