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

import com.bytechef.automation.ai.a2a.server.executor.A2AAgentExecutorImpl;
import com.bytechef.platform.ai.a2a.A2AAgentCardFactory;
import com.bytechef.platform.ai.a2a.A2AAgentDescriptor;
import com.bytechef.platform.ai.a2a.A2AProtocolHandler;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.a2a.json.JsonProcessingException;
import io.a2a.json.JsonUtil;
import io.a2a.spec.AgentCard;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.MessageSendParams;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.math.NumberUtils;
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
    private final A2AAgentExecutorImpl a2aAgentExecutor;
    private final String publicUrl;

    @SuppressFBWarnings("EI")
    A2AServerController(
        A2AAgentCardFactory agentCardFactory, A2AProtocolHandler protocolHandler,
        A2AAgentExecutorImpl a2aAgentExecutor, @Value("${bytechef.public-url}") String publicUrl) {

        this.agentCardFactory = agentCardFactory;
        this.protocolHandler = protocolHandler;
        this.a2aAgentExecutor = a2aAgentExecutor;
        this.publicUrl = publicUrl;
    }

    @GetMapping(value = "/{secretKey}/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> getAgentCard(@PathVariable String secretKey, HttpServletRequest request) throws Exception {
        A2AAgentDescriptor descriptor = a2aAgentExecutor.getAgentDescriptor(
            secretKey, resolveEndpointUrl(secretKey, request));

        AgentCard agentCard = agentCardFactory.create(descriptor);

        return ResponseEntity.ok(JsonUtil.toJson(agentCard));
    }

    @PostMapping(value = "/{secretKey}", consumes = MediaType.APPLICATION_JSON_VALUE)
    Object handleJsonRpc(@PathVariable String secretKey, @RequestBody String body) throws Exception {
        JsonElement root;

        try {
            root = JsonParser.parseString(body);
        } catch (JsonParseException jsonParseException) {
            return toResponseEntity(new JSONRPCErrorResponse(new JSONParseError()));
        }

        Object requestId = extractId(root);

        if (!isValidRequest(root)) {
            return toResponseEntity(new JSONRPCErrorResponse(requestId, new InvalidRequestError()));
        }

        JsonObject rootObject = root.getAsJsonObject();

        String method = rootObject.get("method")
            .getAsString();

        MessageSendParams params = null;
        JsonElement paramsElement = rootObject.get("params");

        if (isMessageMethod(method) && paramsElement != null && !paramsElement.isJsonNull()) {
            try {
                params = JsonUtil.OBJECT_MAPPER.fromJson(paramsElement, MessageSendParams.class);
            } catch (RuntimeException runtimeException) {
                return toResponseEntity(
                    new JSONRPCErrorResponse(requestId,
                        new InvalidParamsError("The message parameters are malformed")));
            }
        }

        if (A2AProtocolHandler.METHOD_STREAM_MESSAGE.equals(method)) {
            return streamJsonRpc(secretKey, requestId, method, params);
        }

        return toResponseEntity(dispatch(secretKey, requestId, method, params, rootObject));
    }

    private static ResponseEntity<String> toResponseEntity(JSONRPCResponse<?> response)
        throws JsonProcessingException {

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(JsonUtil.toJson(response));
    }

    private JSONRPCResponse<?> dispatch(
        String secretKey, Object requestId, @Nullable String method, @Nullable MessageSendParams params,
        JsonObject root) {

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

    private static @Nullable String extractTaskId(JsonObject root) {
        JsonElement paramsElement = root.get("params");

        if (paramsElement == null || !paramsElement.isJsonObject()) {
            return null;
        }

        JsonElement idElement = paramsElement.getAsJsonObject()
            .get("id");

        if (!isString(idElement)) {
            return null;
        }

        return idElement.getAsString();
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
                        .data(JsonUtil.toJson(event), MediaType.APPLICATION_JSON));
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

    private static boolean isValidRequest(JsonElement root) {
        if (!root.isJsonObject()) {
            return false;
        }

        JsonObject rootObject = root.getAsJsonObject();

        JsonElement idElement = rootObject.get("id");
        JsonElement jsonRpcElement = rootObject.get("jsonrpc");
        JsonElement methodElement = rootObject.get("method");

        return isValidId(idElement) && isString(jsonRpcElement) &&
            JSON_RPC_VERSION.equals(jsonRpcElement.getAsString()) && isString(methodElement);
    }

    private static boolean isValidId(@Nullable JsonElement idElement) {
        return idElement == null || idElement.isJsonNull() || isNumber(idElement) || isString(idElement);
    }

    private static boolean isNumber(@Nullable JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive()
            .isNumber();
    }

    private static boolean isString(@Nullable JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive()
            .isString();
    }

    private static boolean isMessageMethod(@Nullable String method) {
        return A2AProtocolHandler.METHOD_SEND_MESSAGE.equals(method) ||
            A2AProtocolHandler.METHOD_STREAM_MESSAGE.equals(method);
    }

    private static @Nullable Object extractId(JsonElement root) {
        if (!root.isJsonObject()) {
            return null;
        }

        JsonElement idElement = root.getAsJsonObject()
            .get("id");

        if (isNumber(idElement)) {
            return NumberUtils.createNumber(idElement.getAsString());
        }

        if (isString(idElement)) {
            return idElement.getAsString();
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
