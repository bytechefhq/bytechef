/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.mcp.server;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpNotificationHandler;
import io.modelcontextprotocol.server.McpRequestHandler;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.DefaultMcpStreamableServerSessionFactory;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ErrorCodes;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
import io.modelcontextprotocol.util.ToolInputValidator;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * A request-scoped MCP server that serves a per-session tool set resolved dynamically through a {@code toolFilter}.
 *
 * <p>
 * The MCP SDK's standard {@code McpAsyncServer} serves a single static tool list to every client session. ByteChef's
 * MCP endpoints are multi-tenant: one transport serves many logical servers distinguished by a secret key carried in
 * the {@link io.modelcontextprotocol.spec.McpTransportContext}. The {@code toolFilter} is invoked on every
 * {@code tools/list} and {@code tools/call} request, resolving the caller's visible and callable tools from the current
 * {@link McpAsyncServerExchange}.
 *
 * <p>
 * Rather than forking {@code McpAsyncServer}, this class composes the MCP SDK's public primitives
 * ({@link DefaultMcpStreamableServerSessionFactory}, {@link McpRequestHandler}, {@link ToolInputValidator}), which are
 * stable as of SDK 2.0.0. It implements only the request methods ByteChef serves: {@code ping}, {@code tools/list},
 * {@code tools/call}, {@code resources/list} and {@code resources/read} over a static, session-independent resource
 * list (e.g. MCP App UI resources), plus empty {@code resources/templates/list} and {@code prompts/list} handlers (for
 * parity with the advertised capabilities) and a no-op {@code logging/setLevel} acknowledgement.
 *
 * @author Ivica Cardic
 */
public class FilterableMcpAsyncServer {

    private static final Logger log = LoggerFactory.getLogger(FilterableMcpAsyncServer.class);

    private final McpJsonMapper jsonMapper;

    private final JsonSchemaValidator jsonSchemaValidator;

    private final boolean validateToolInputs;

    private final McpSchema.ServerCapabilities serverCapabilities;

    private final McpSchema.Implementation serverInfo;

    private final String instructions;

    private final List<String> protocolVersions;

    private final Function<McpAsyncServerExchange, List<McpServerFeatures.AsyncToolSpecification>> toolFilter;

    private final List<McpServerFeatures.AsyncResourceSpecification> resourceSpecifications;

    private final Duration requestTimeout;

    private final Map<String, McpRequestHandler<?>> requestHandlers;

    private final Map<String, McpNotificationHandler> notificationHandlers;

    /**
     * Build the transport-agnostic filtering core. Attach it to a transport with {@link #attachStreamable} and/or
     * {@link #attachSse}.
     *
     * @param jsonMapper             The JsonMapper to use for JSON serialization/deserialization.
     * @param serverInfo             The server implementation information.
     * @param serverCapabilities     The server capabilities configuration.
     * @param instructions           Optional instructions for the server.
     * @param requestTimeout         The request timeout duration.
     * @param jsonSchemaValidator    The JSON schema validator.
     * @param validateToolInputs     Whether to validate tool call arguments against the tool input schema.
     * @param toolFilter             The tool filter function, or null to serve no tools.
     * @param resourceSpecifications Static resources served to every session, or null to serve none.
     * @param protocolVersions       The protocol versions the server supports.
     */
    FilterableMcpAsyncServer(
        McpJsonMapper jsonMapper, McpSchema.Implementation serverInfo,
        McpSchema.ServerCapabilities serverCapabilities, String instructions, Duration requestTimeout,
        JsonSchemaValidator jsonSchemaValidator, boolean validateToolInputs,
        Function<McpAsyncServerExchange, List<McpServerFeatures.AsyncToolSpecification>> toolFilter,
        List<McpServerFeatures.AsyncResourceSpecification> resourceSpecifications, List<String> protocolVersions) {

        this.jsonMapper = jsonMapper;
        this.serverInfo = serverInfo;
        this.serverCapabilities = serverCapabilities != null
            ? serverCapabilities.mutate()
                .logging()
                .build()
            : null;
        this.instructions = instructions;
        this.requestTimeout = requestTimeout;
        this.jsonSchemaValidator = jsonSchemaValidator;
        this.validateToolInputs = validateToolInputs;
        this.toolFilter = toolFilter != null ? toolFilter : exchange -> List.of();
        this.resourceSpecifications = resourceSpecifications != null ? List.copyOf(resourceSpecifications) : List.of();
        this.protocolVersions = protocolVersions;

        this.requestHandlers = prepareRequestHandlers();
        this.notificationHandlers = prepareNotificationHandlers();
    }

    /**
     * Installs the filtering core's session factory on a Streamable HTTP transport provider.
     */
    public void attachStreamable(McpStreamableServerTransportProvider transportProvider) {
        transportProvider.setSessionFactory(new DefaultMcpStreamableServerSessionFactory(requestTimeout,
            this::asyncInitializeRequestHandler, requestHandlers, notificationHandlers, sessionId -> Mono.empty(),
            jsonSchemaValidator));
    }

    /**
     * Installs the filtering core's session factory on an HTTP+SSE transport provider. The same core may be attached to
     * many SSE providers (e.g. one per secret key); the request/notification handlers are stateless and resolve the
     * caller's tools per request from the {@link McpAsyncServerExchange} transport context.
     */
    public void attachSse(McpServerTransportProvider transportProvider) {
        transportProvider.setSessionFactory(sessionTransport -> new McpServerSession(
            UUID.randomUUID()
                .toString(),
            requestTimeout, sessionTransport, this::asyncInitializeRequestHandler, requestHandlers,
            notificationHandlers, Mono::empty, jsonSchemaValidator));
    }

    private Map<String, McpRequestHandler<?>> prepareRequestHandlers() {
        Map<String, McpRequestHandler<?>> requestHandlers = new HashMap<>();

        // Ping MUST respond with an empty data, but not NULL response.
        requestHandlers.put(McpSchema.METHOD_PING, (exchange, params) -> Mono.just(Map.of()));

        if (serverCapabilities == null) {
            return requestHandlers;
        }

        if (serverCapabilities.tools() != null) {
            requestHandlers.put(McpSchema.METHOD_TOOLS_LIST, toolsListRequestHandler());
            requestHandlers.put(McpSchema.METHOD_TOOLS_CALL, toolsCallRequestHandler());
        }

        // Resources are static and session-independent (e.g. MCP App UI resources); resource templates stay empty so
        // clients that honor the advertised capability receive an empty list rather than a method-not-found.
        if (serverCapabilities.resources() != null) {
            requestHandlers.put(McpSchema.METHOD_RESOURCES_LIST,
                (exchange, params) -> Mono.just(
                    McpSchema.ListResourcesResult.builder(
                        resourceSpecifications.stream()
                            .map(McpServerFeatures.AsyncResourceSpecification::resource)
                            .toList())
                        .build()));
            requestHandlers.put(McpSchema.METHOD_RESOURCES_READ, resourcesReadRequestHandler());
            requestHandlers.put(McpSchema.METHOD_RESOURCES_TEMPLATES_LIST,
                (exchange, params) -> Mono.just(McpSchema.ListResourceTemplatesResult.builder(List.of())
                    .build()));
        }

        if (serverCapabilities.prompts() != null) {
            requestHandlers.put(McpSchema.METHOD_PROMPT_LIST,
                (exchange, params) -> Mono.just(McpSchema.ListPromptsResult.builder(List.of())
                    .build()));
        }

        // Acknowledge logging level changes without broadcasting; ByteChef tools emit no MCP logging notifications.
        if (serverCapabilities.logging() != null) {
            requestHandlers.put(McpSchema.METHOD_LOGGING_SET_LEVEL, (exchange, params) -> Mono.just(Map.of()));
        }

        return requestHandlers;
    }

    private Map<String, McpNotificationHandler> prepareNotificationHandlers() {
        Map<String, McpNotificationHandler> notificationHandlers = new HashMap<>();

        notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_INITIALIZED, (exchange, params) -> Mono.empty());

        return notificationHandlers;
    }

    private Mono<McpSchema.InitializeResult> asyncInitializeRequestHandler(
        McpSchema.InitializeRequest initializeRequest) {

        return Mono.defer(() -> {
            log.info("Client initialize request - Protocol: {}, Capabilities: {}, Info: {}",
                initializeRequest.protocolVersion(), initializeRequest.capabilities(),
                initializeRequest.clientInfo());

            // The server MUST respond with the highest protocol version it supports if it does not support the
            // requested (e.g. Client) version.
            String serverProtocolVersion = this.protocolVersions.get(this.protocolVersions.size() - 1);

            if (this.protocolVersions.contains(initializeRequest.protocolVersion())) {
                // If the server supports the requested protocol version, it MUST respond with the same version.
                serverProtocolVersion = initializeRequest.protocolVersion();
            } else {
                log.warn(
                    "Client requested unsupported protocol version: {}, "
                        + "so the server will suggest the {} version instead",
                    initializeRequest.protocolVersion(), serverProtocolVersion);
            }

            return Mono.just(new McpSchema.InitializeResult(serverProtocolVersion, this.serverCapabilities,
                this.serverInfo, this.instructions));
        });
    }

    private McpRequestHandler<McpSchema.ListToolsResult> toolsListRequestHandler() {
        return (exchange, params) -> {
            List<Tool> toolList = this.toolFilter.apply(exchange)
                .stream()
                .map(McpServerFeatures.AsyncToolSpecification::tool)
                .toList();

            return Mono.just(McpSchema.ListToolsResult.builder(toolList)
                .build());
        };
    }

    private McpRequestHandler<McpSchema.ReadResourceResult> resourcesReadRequestHandler() {
        return (exchange, params) -> {
            McpSchema.ReadResourceRequest readResourceRequest = jsonMapper.convertValue(params,
                new TypeRef<McpSchema.ReadResourceRequest>() {});

            Optional<McpServerFeatures.AsyncResourceSpecification> resourceSpecification =
                resourceSpecifications.stream()
                    .filter(specification -> Objects.equals(
                        readResourceRequest.uri(),
                        specification.resource()
                            .uri()))
                    .findFirst();

            if (resourceSpecification.isEmpty()) {
                return Mono.error(McpError.builder(ErrorCodes.RESOURCE_NOT_FOUND)
                    .message("Unknown resource: " + readResourceRequest.uri())
                    .data("Resource not found: " + readResourceRequest.uri())
                    .build());
            }

            return resourceSpecification.get()
                .readHandler()
                .apply(exchange, readResourceRequest);
        };
    }

    private McpRequestHandler<CallToolResult> toolsCallRequestHandler() {
        return (exchange, params) -> {
            McpSchema.CallToolRequest callToolRequest = jsonMapper.convertValue(params,
                new TypeRef<McpSchema.CallToolRequest>() {});

            Optional<McpServerFeatures.AsyncToolSpecification> toolSpecification = this.toolFilter.apply(exchange)
                .stream()
                .filter(toolSpec -> callToolRequest.name()
                    .equals(toolSpec.tool()
                        .name()))
                .findAny();

            if (toolSpecification.isEmpty()) {
                return Mono.error(McpError.builder(ErrorCodes.INVALID_PARAMS)
                    .message("Unknown tool: " + callToolRequest.name())
                    .data("Tool not found: " + callToolRequest.name())
                    .build());
            }

            Tool tool = toolSpecification.get()
                .tool();

            CallToolResult validationError = ToolInputValidator.validate(
                tool, callToolRequest.arguments(), this.validateToolInputs, this.jsonSchemaValidator);

            if (validationError != null) {
                return Mono.just(validationError);
            }

            return toolSpecification.get()
                .callHandler()
                .apply(exchange, callToolRequest)
                .map(result -> validateStructuredOutput(tool, callToolRequest, result));
        };
    }

    private CallToolResult validateStructuredOutput(
        Tool tool, McpSchema.CallToolRequest request, CallToolResult result) {

        Map<String, Object> outputSchema = tool.outputSchema();

        if (outputSchema == null || Boolean.TRUE.equals(result.isError())) {
            // No validation is required when there is no output schema, or when the tool already returned an error.
            return result;
        }

        if (result.structuredContent() == null) {
            String message = "Tool (" + request.name() + ") response missing structured content which is expected "
                + "when calling tool with non-empty outputSchema";

            log.warn(message);

            return CallToolResult.builder()
                .content(List.of(McpSchema.TextContent.builder(message)
                    .build()))
                .isError(true)
                .build();
        }

        var validation = this.jsonSchemaValidator.validate(outputSchema, result.structuredContent());

        if (!validation.valid()) {
            String message = "Tool (" + request.name() + ") output validation failed: " + validation.errorMessage();

            log.warn(message);

            return CallToolResult.builder()
                .content(List.of(McpSchema.TextContent.builder(message)
                    .build()))
                .isError(true)
                .build();
        }

        return result;
    }
}
