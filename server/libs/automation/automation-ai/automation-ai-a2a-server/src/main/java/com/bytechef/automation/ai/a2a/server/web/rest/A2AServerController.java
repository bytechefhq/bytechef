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

package com.bytechef.automation.ai.a2a.server.web.rest;

import com.bytechef.automation.ai.a2a.server.facade.AutomationA2AServerFacade;
import com.bytechef.platform.ai.a2a.A2AAgentCardFactory;
import com.bytechef.platform.ai.a2a.A2AAgentDescriptor;
import com.bytechef.platform.ai.a2a.A2AProtocolHandler;
import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.a2a.spec.AgentCard;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.MessageSendParams;
import io.a2a.util.Utils;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Exposes ByteChef agent-backed workflows over the A2A (Agent2Agent) protocol.
 *
 * <p>
 * Two endpoints per A2A server secret key:
 * <ul>
 * <li>{@code GET /api/automation/a2a/{secretKey}/.well-known/agent-card.json} — the discovery agent card.</li>
 * <li>{@code POST /api/automation/a2a/{secretKey}} — the JSON-RPC surface, handling {@code message/send},
 * {@code message/stream} (SSE), {@code tasks/get}, and {@code tasks/cancel}.</li>
 * </ul>
 * Both are authenticated by the secret-key API-key stack (see the A2A security configurer). CSRF is intentionally
 * disabled for these endpoints — external A2A clients cannot present CSRF tokens; security is the API key.
 * </p>
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/api/automation/a2a")
class A2AServerController {

    // Slightly above the 300s workflow sync timeout so the SSE emitter outlives a full agent run.
    private static final long STREAM_TIMEOUT_MS = 310_000L;

    private final A2AAgentCardFactory agentCardFactory;
    private final A2AProtocolHandler protocolHandler;
    private final AutomationA2AServerFacade automationA2AServerFacade;
    private final String publicUrl;
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "a2a-stream");

        thread.setDaemon(true);

        return thread;
    });

    @SuppressFBWarnings("EI")
    A2AServerController(
        A2AAgentCardFactory agentCardFactory, A2AProtocolHandler protocolHandler,
        AutomationA2AServerFacade automationA2AServerFacade, @Value("${bytechef.webhook.url:}") String publicUrl) {

        this.agentCardFactory = agentCardFactory;
        this.protocolHandler = protocolHandler;
        this.automationA2AServerFacade = automationA2AServerFacade;
        this.publicUrl = publicUrl;
    }

    @GetMapping(value = "/{secretKey}/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> getAgentCard(@PathVariable String secretKey, HttpServletRequest request) throws Exception {
        A2AAgentDescriptor descriptor = automationA2AServerFacade.getAgentDescriptor(
            secretKey, resolveEndpointUrl(secretKey, request));

        AgentCard agentCard = agentCardFactory.create(descriptor);

        return ResponseEntity.ok(Utils.OBJECT_MAPPER.writeValueAsString(agentCard));
    }

    @PostMapping(value = "/{secretKey}", consumes = MediaType.APPLICATION_JSON_VALUE)
    Object handleJsonRpc(@PathVariable String secretKey, @RequestBody String body) throws Exception {
        JsonNode root = Utils.OBJECT_MAPPER.readTree(body);

        Object requestId = extractId(root);
        String method = root.hasNonNull("method") ? root.get("method")
            .asText() : null;

        MessageSendParams params = null;

        if (isMessageMethod(method) && root.hasNonNull("params")) {
            params = Utils.OBJECT_MAPPER.treeToValue(root.get("params"), MessageSendParams.class);
        }

        // message/stream returns an SSE stream of status events; message/send (and everything else) returns a single
        // JSON-RPC response.
        if (A2AProtocolHandler.METHOD_STREAM_MESSAGE.equals(method)) {
            return streamJsonRpc(secretKey, requestId, method, params);
        }

        JSONRPCResponse<?> response = dispatch(secretKey, requestId, method, params, root);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(Utils.OBJECT_MAPPER.writeValueAsString(response));
    }

    private JSONRPCResponse<?> dispatch(
        String secretKey, Object requestId, @Nullable String method, @Nullable MessageSendParams params,
        JsonNode root) {

        if (A2AProtocolHandler.METHOD_GET_TASK.equals(method)) {
            return protocolHandler.handleGetTask(requestId, extractTaskId(root));
        }

        if (A2AProtocolHandler.METHOD_CANCEL_TASK.equals(method)) {
            return protocolHandler.handleCancelTask(requestId, extractTaskId(root));
        }

        return protocolHandler.handle(secretKey, requestId, method, params);
    }

    private static @Nullable String extractTaskId(JsonNode root) {
        JsonNode paramsNode = root.get("params");

        if (paramsNode == null || !paramsNode.hasNonNull("id")) {
            return null;
        }

        return paramsNode.get("id")
            .asText();
    }

    @PreDestroy
    void shutdown() {
        streamExecutor.shutdown();
    }

    private SseEmitter streamJsonRpc(
        String secretKey, Object requestId, String method, MessageSendParams params) {

        SseEmitter sseEmitter = new SseEmitter(STREAM_TIMEOUT_MS);

        streamExecutor.execute(() -> {
            try {
                protocolHandler.handleStream(
                    secretKey, requestId, method, params,
                    event -> sseEmitter.send(
                        SseEmitter.event()
                            .data(Utils.OBJECT_MAPPER.writeValueAsString(event), MediaType.APPLICATION_JSON)));

                sseEmitter.complete();
            } catch (Exception exception) {
                sseEmitter.completeWithError(exception);
            }
        });

        return sseEmitter;
    }

    private static boolean isMessageMethod(@Nullable String method) {
        return A2AProtocolHandler.METHOD_SEND_MESSAGE.equals(method) ||
            A2AProtocolHandler.METHOD_STREAM_MESSAGE.equals(method);
    }

    private static Object extractId(JsonNode root) {
        JsonNode idNode = root.get("id");

        if (idNode == null || idNode.isNull()) {
            return null;
        }

        if (idNode.isNumber()) {
            return idNode.numberValue();
        }

        return idNode.asText();
    }

    private String resolveEndpointUrl(String secretKey, HttpServletRequest request) {
        if (publicUrl != null && !publicUrl.isBlank()) {
            return publicUrl + "/api/automation/a2a/" + secretKey;
        }

        String requestUrl = request.getRequestURL()
            .toString();

        return requestUrl.replace("/.well-known/agent-card.json", "");
    }
}
