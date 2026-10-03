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
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.a2a.spec.AgentCard;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.MessageSendParams;
import io.a2a.util.Utils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/api/automation/a2a")
class A2AServerController {

    private static final String JSON_RPC_VERSION = "2.0";
    private static final long STREAM_TIMEOUT_MS = JobCompletionAwaiter.DEFAULT_SYNC_TIMEOUT.plusSeconds(10)
        .toMillis();

    private static final Logger log = LoggerFactory.getLogger(A2AServerController.class);

    private final A2AAgentCardFactory agentCardFactory;
    private final A2AProtocolHandler protocolHandler;
    private final AutomationA2AServerFacade automationA2AServerFacade;
    private final String publicUrl;

    @SuppressFBWarnings("EI")
    A2AServerController(
        A2AAgentCardFactory agentCardFactory, A2AProtocolHandler protocolHandler,
        AutomationA2AServerFacade automationA2AServerFacade, @Value("${bytechef.public-url:}") String publicUrl) {

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
        JsonNode root;

        try {
            root = Utils.OBJECT_MAPPER.readTree(body);
        } catch (JsonProcessingException jsonProcessingException) {
            return toResponseEntity(new JSONRPCErrorResponse(new JSONParseError()));
        }

        Object requestId = extractId(root);

        if (!isValidRequest(root)) {
            return toResponseEntity(new JSONRPCErrorResponse(requestId, new InvalidRequestError()));
        }

        String method = root.get("method")
            .asText();

        MessageSendParams params = null;

        if (isMessageMethod(method) && root.hasNonNull("params")) {
            try {
                params = Utils.OBJECT_MAPPER.treeToValue(root.get("params"), MessageSendParams.class);
            } catch (JsonProcessingException jsonProcessingException) {
                return toResponseEntity(
                    new JSONRPCErrorResponse(requestId,
                        new InvalidParamsError("The message parameters are malformed")));
            }
        }

        if (A2AProtocolHandler.METHOD_STREAM_MESSAGE.equals(method)) {
            return streamJsonRpc(secretKey, requestId, method, params);
        }

        return toResponseEntity(dispatch(secretKey, requestId, method, params, root));
    }

    private static ResponseEntity<String> toResponseEntity(JSONRPCResponse<?> response)
        throws JsonProcessingException {

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(Utils.OBJECT_MAPPER.writeValueAsString(response));
    }

    private JSONRPCResponse<?> dispatch(
        String secretKey, Object requestId, @Nullable String method, @Nullable MessageSendParams params,
        JsonNode root) {

        if (A2AProtocolHandler.METHOD_GET_TASK.equals(method) || A2AProtocolHandler.METHOD_CANCEL_TASK.equals(method)) {
            String taskId = extractTaskId(root);

            if (taskId == null) {
                return new JSONRPCErrorResponse(requestId, new InvalidParamsError("A task id string is required"));
            }

            if (A2AProtocolHandler.METHOD_GET_TASK.equals(method)) {
                return protocolHandler.handleGetTask(secretKey, requestId, taskId);
            }

            return protocolHandler.handleCancelTask(secretKey, requestId, taskId);
        }

        return protocolHandler.handle(secretKey, requestId, method, params);
    }

    private static @Nullable String extractTaskId(JsonNode root) {
        JsonNode paramsNode = root.get("params");

        if (paramsNode == null || !paramsNode.isObject()) {
            return null;
        }

        JsonNode idNode = paramsNode.get("id");

        if (idNode == null || !idNode.isTextual()) {
            return null;
        }

        return idNode.asText();
    }

    private SseEmitter streamJsonRpc(
        String secretKey, @Nullable Object requestId, String method, @Nullable MessageSendParams params) {

        SseEmitter sseEmitter = new SseEmitter(STREAM_TIMEOUT_MS);

        AtomicBoolean closed = new AtomicBoolean();

        sseEmitter.onCompletion(() -> closed.set(true));
        sseEmitter.onError(throwable -> closed.set(true));
        sseEmitter.onTimeout(() -> {
            closed.set(true);

            sseEmitter.complete();
        });

        protocolHandler.handleStream(secretKey, requestId, method, params, new A2AProtocolHandler.StreamSink() {

            @Override
            public void send(JSONRPCResponse<?> event) throws Exception {
                if (closed.get()) {
                    log.debug("Skipping an event of the A2A stream for request {}: the stream is closed", requestId);

                    return;
                }

                sseEmitter.send(
                    SseEmitter.event()
                        .data(Utils.OBJECT_MAPPER.writeValueAsString(event), MediaType.APPLICATION_JSON));
            }

            @Override
            public void complete() {
                if (!closed.get()) {
                    sseEmitter.complete();
                }
            }

            @Override
            public void completeWithError(Throwable throwable) {
                if (!closed.get()) {
                    sseEmitter.completeWithError(throwable);
                }
            }
        });

        return sseEmitter;
    }

    private static boolean isValidRequest(JsonNode root) {
        if (!root.isObject()) {
            return false;
        }

        JsonNode idNode = root.get("id");
        JsonNode jsonRpcNode = root.get("jsonrpc");
        JsonNode methodNode = root.get("method");

        return isValidId(idNode) && jsonRpcNode != null && jsonRpcNode.isTextual() &&
            JSON_RPC_VERSION.equals(jsonRpcNode.asText()) &&
            methodNode != null && methodNode.isTextual();
    }

    private static boolean isValidId(@Nullable JsonNode idNode) {
        return idNode == null || idNode.isNull() || idNode.isNumber() || idNode.isTextual();
    }

    private static boolean isMessageMethod(@Nullable String method) {
        return A2AProtocolHandler.METHOD_SEND_MESSAGE.equals(method) ||
            A2AProtocolHandler.METHOD_STREAM_MESSAGE.equals(method);
    }

    private static @Nullable Object extractId(JsonNode root) {
        if (!root.isObject()) {
            return null;
        }

        JsonNode idNode = root.get("id");

        if (idNode == null || idNode.isNull()) {
            return null;
        }

        if (idNode.isNumber()) {
            return idNode.numberValue();
        }

        if (idNode.isTextual()) {
            return idNode.asText();
        }

        return null;
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
