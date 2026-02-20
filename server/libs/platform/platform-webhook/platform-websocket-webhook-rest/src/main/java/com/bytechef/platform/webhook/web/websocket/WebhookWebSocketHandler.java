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

package com.bytechef.platform.webhook.web.websocket;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * WebSocket handler for webhook connections with workflow execution streaming support. Establishes connections at
 * /webhooks/{id} and streams workflow execution events in real-time using the JobSyncExecutor bridge pattern similar to
 * SSE streaming.
 *
 * @author Ivica Cardic
 */
@Component
public class WebhookWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebhookWebSocketHandler.class);
    private static final int MAX_PENDING_EVENTS = 100;

    private final CallSessionRegistry callSessionRegistry;
    private final JobFacade jobFacade;
    private final ObjectMapper objectMapper;
    private final WebhookWorkflowExecutor webhookWorkflowExecutor;

    private final Cache<String, AutoCloseable> streamHandles;
    private final Cache<String, List<Map<String, Object>>> pendingEvents;
    private final Cache<String, String> sessionIdToCallSid;

    @SuppressFBWarnings("EI")
    public WebhookWebSocketHandler(
        CallSessionRegistry callSessionRegistry, JobFacade jobFacade, ObjectMapper objectMapper,
        WebhookWorkflowExecutor webhookWorkflowExecutor) {

        this.callSessionRegistry = callSessionRegistry;
        this.jobFacade = jobFacade;
        this.objectMapper = objectMapper;
        this.webhookWorkflowExecutor = webhookWorkflowExecutor;

        this.streamHandles = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .removalListener((key, value, cause) -> {
                if (value != null) {
                    closeHandle((AutoCloseable) value);
                }
            })
            .build();

        this.pendingEvents = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

        this.sessionIdToCallSid = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        String webhookId = extractId(uri);
        String sessionKey = session.getId();
        String callSid = extractCallSid(uri);
        String subWorkflowId = extractQueryParam(uri, "subWorkflowId");

        logger.info(
            "WebSocket connection established for webhook: {}, sessionId: {}, callSid: {}, subWorkflowId: {}",
            webhookId, sessionKey, callSid, subWorkflowId);

        if (callSid != null) {

            if (callSessionRegistry.hasSession(callSid)) {
                logger.info("Reusing existing session for callSid: {}", callSid);

                Optional<CallSessionRegistry.CallSession> existingSessionOpt = callSessionRegistry.getSessionByCallSid(
                    callSid);

                if (existingSessionOpt.isPresent()) {
                    CallSessionRegistry.CallSession existingCallSession = existingSessionOpt.get();
                    Optional<WebSocketSession> existingWebSocketSessionOpt =
                        callSessionRegistry.getWebSocketSessionById(
                            existingCallSession.getWebSocketSessionId());

                    if (existingWebSocketSessionOpt.isPresent() && existingWebSocketSessionOpt.get()
                        .isOpen()) {

                        WebSocketSession existingWebSocketSession = existingWebSocketSessionOpt.get();

                        logger.info(
                            "Existing session is still open, reusing: callSid={}, existingSessionId={}", callSid,
                            existingWebSocketSession.getId());

                        Map<String, Object> connected = new LinkedHashMap<>();

                        connected.put("event", "connected");
                        connected.put("id", webhookId);
                        connected.put("callSid", callSid);
                        connected.put("reused", true);

                        sendMessage(session, connected);

                        session.close(CloseStatus.NORMAL.withReason("Session already exists for this callSid"));

                        return;
                    } else {
                        logger.info("Existing session is closed, creating new session: callSid={}", callSid);

                        callSessionRegistry.removeSessionByCallSid(callSid);
                    }
                }
            }

            CallSessionRegistry.CallMetadata metadata = new CallSessionRegistry.CallMetadata(
                null, null, null, null);

            callSessionRegistry.registerSession(callSid, session, metadata);
            sessionIdToCallSid.put(sessionKey, callSid);

            if (subWorkflowId != null && !subWorkflowId.isBlank()) {
                startSubWorkflow(callSid, subWorkflowId, webhookId);
            }
        }

        Map<String, Object> connected = new LinkedHashMap<>();
        connected.put("event", "connected");
        connected.put("id", webhookId);

        if (callSid != null) {
            connected.put("callSid", callSid);
        }

        sendMessage(session, connected);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionKey = session.getId();
        String payload = message.getPayload();

        if (logger.isDebugEnabled()) {
            logger.debug("Webhook WS inbound: sessionId={}, payload={}", sessionKey, payload);
        }

        try {
            Map<String, Object> request = objectMapper.readValue(payload, new TypeReference<>() {});

            String action = (String) request.get("action");

            if ("execute".equals(action)) {
                executeWorkflow(session, request);
            } else {
                Map<String, Object> ack = new LinkedHashMap<>();
                ack.put("event", "ack");
                ack.put("data", payload);

                sendMessage(session, ack);
            }
        } catch (Exception e) {
            logger.error("Error processing WebSocket message", e);

            Map<String, Object> error = new LinkedHashMap<>();
            error.put("event", "error");
            error.put("message", "Error processing message: " + e.getMessage());

            sendMessage(session, error);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionKey = session.getId();

        logger.info("WebSocket connection closed: sessionId={}, status={}", sessionKey, status);

        String callSid = sessionIdToCallSid.getIfPresent(sessionKey);

        if (callSid != null) {
            callSessionRegistry.removeSessionByCallSid(callSid);
            sessionIdToCallSid.invalidate(sessionKey);
        }

        AutoCloseable handle = streamHandles.getIfPresent(sessionKey);

        if (handle != null) {
            closeHandle(handle);
            streamHandles.invalidate(sessionKey);
        }

        pendingEvents.invalidate(sessionKey);
    }

    private void executeWorkflow(WebSocketSession session, Map<String, Object> request) throws Exception {
        String sessionKey = session.getId();
        URI uri = session.getUri();
        String webhookId = extractId(uri);

        Map<String, Object> start = new LinkedHashMap<>();
        start.put("event", "start");
        start.put("message", "Workflow execution started");
        start.put("webhookId", webhookId);

        sendMessage(session, start);

        try {
            WebhookRequest webhookRequest = buildWebhookRequest(request);

            WorkflowExecutionId workflowExecutionId = (WorkflowExecutionId) request.get("workflowExecutionId");

            if (workflowExecutionId == null) {
                throw new IllegalArgumentException("workflowExecutionId is required");
            }

            WebSocketStreamBridge streamBridge = new WebSocketStreamBridge(sessionKey);

            webhookWorkflowExecutor.executeAsync(workflowExecutionId, webhookRequest, streamBridge);

//
//            streamHandles.put(sessionKey, webhookWorkflowExecutor.addSseStreamBridge(jobId, streamBridge));
//
//            if (logger.isDebugEnabled()) {
//                logger.debug("Started workflow execution");
//            }

        } catch (Exception e) {
            logger.error("Error executing workflow", e);

            Map<String, Object> error = new LinkedHashMap<>();
            error.put("event", "error");
            error.put("message", e.getMessage());

            sendMessage(session, error);
        }
    }

    private WebhookRequest buildWebhookRequest(Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, List<String>> headers = (Map<String, List<String>>) request.getOrDefault(
            "headers", new LinkedHashMap<>());

        @SuppressWarnings("unchecked")
        Map<String, List<String>> parameters = (Map<String, List<String>>) request.getOrDefault(
            "parameters", new LinkedHashMap<>());

        Object bodyContent = request.getOrDefault("body", new LinkedHashMap<>());

        WebhookRequest.WebhookBodyImpl body = new WebhookRequest.WebhookBodyImpl(
            bodyContent,
            WebhookRequest.WebhookBodyImpl.ContentType.JSON,
            "application/json",
            bodyContent.toString());

        String methodName = (String) request.getOrDefault("method", "POST");

        WebhookMethod method;

        try {
            method = WebhookMethod.valueOf(methodName.toUpperCase());
        } catch (IllegalArgumentException e) {
            method = WebhookMethod.POST;
        }

        return new WebhookRequest(headers, parameters, body, method);
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> data) {
        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            String json = JsonUtils.write(data);

            session.sendMessage(new TextMessage(json));

            if (logger.isDebugEnabled()) {
                logger.debug("Sent WebSocket message: sessionId={}, event={}", session.getId(), data.get("event"));
            }
        } catch (IOException e) {
            logger.error("Failed to send WebSocket message to session: {}", session.getId(), e);
        }
    }

    private static String extractId(URI uri) {
        if (uri == null) {
            return null;
        }

        String path = uri.getPath();

        if (path == null) {
            return null;
        }

        String[] segments = path.split("/");

        for (int i = segments.length - 1; i >= 0; i--) {
            String seg = segments[i];

            if (!seg.isEmpty() && !"webhooks".equals(seg)) {
                return seg;
            }
        }

        return null;
    }

    private static String extractCallSid(URI uri) {
        return extractQueryParam(uri, "callSid");
    }

    private static String extractQueryParam(URI uri, String paramName) {
        if (uri == null || paramName == null) {
            return null;
        }

        String query = uri.getQuery();

        if (query == null || query.isEmpty()) {
            return null;
        }

        String[] params = query.split("&");

        for (String param : params) {
            String[] keyValue = param.split("=");

            if (keyValue.length == 2 && paramName.equals(keyValue[0])) {
                return keyValue[1];
            }
        }

        return null;
    }

    private void startSubWorkflow(String callSid, String subWorkflowId, String mainWorkflowExecutionId) {
        Optional<CallSessionRegistry.CallSession> callSessionOpt = callSessionRegistry.getSessionByCallSid(callSid);

        if (callSessionOpt.isEmpty()) {
            logger.warn("Cannot start sub-workflow: no session found for callSid={}", callSid);

            return;
        }

        CallSessionRegistry.CallSession callSession = callSessionOpt.get();

        callSession.setSubWorkflowId(subWorkflowId);
        callSession.setWorkflowExecutionId(mainWorkflowExecutionId);

        Thread.startVirtualThread(() -> {
            try {
                logger.info("Starting sub-workflow: callSid={}, subWorkflowId={}", callSid, subWorkflowId);

                Map<String, Object> inputs = new LinkedHashMap<>();

                inputs.put("callSid", callSid);
                inputs.put("mainWorkflowExecutionId", mainWorkflowExecutionId);

                JobParametersDTO jobParameters = new JobParametersDTO(subWorkflowId, inputs);

                long subJobId = jobFacade.createJob(jobParameters);

                callSession.setSubJobId(subJobId);

                logger.info(
                    "Sub-workflow started: callSid={}, subWorkflowId={}, subJobId={}", callSid, subWorkflowId,
                    subJobId);
            } catch (Exception exception) {
                logger.error("Failed to start sub-workflow: callSid={}, subWorkflowId={}", callSid, subWorkflowId,
                    exception);
            }
        });
    }

    private static void closeHandle(AutoCloseable handle) {
        try {
            handle.close();
        } catch (Exception exception) {
            if (logger.isTraceEnabled()) {
                logger.trace("Failed to close handle", exception);
            }
        }
    }

    private class WebSocketStreamBridge implements SseStreamBridge {

        private final String sessionKey;

        public WebSocketStreamBridge(String sessionKey) {
            this.sessionKey = sessionKey;
        }

        @Override
        public void onEvent(Object payload) {
            String callSid = sessionIdToCallSid.getIfPresent(sessionKey);

            if (callSid != null) {
                Optional<WebSocketSession> sessionOpt = callSessionRegistry.getWebSocketSession(callSid);

                if (sessionOpt.isPresent() && sessionOpt.get()
                    .isOpen()) {

                    WebSocketSession session = sessionOpt.get();

                    Map<String, Object> event = new LinkedHashMap<>();
                    event.put("event", "stream");
                    event.put("data", payload);

                    sendMessage(session, event);
                } else {
                    bufferEvent(payload);
                }
            } else {
                bufferEvent(payload);
            }
        }

        @Override
        public void onComplete() {
            String callSid = sessionIdToCallSid.getIfPresent(sessionKey);

            if (callSid != null) {
                Optional<WebSocketSession> sessionOpt = callSessionRegistry.getWebSocketSession(callSid);

                if (sessionOpt.isPresent() && sessionOpt.get()
                    .isOpen()) {

                    WebSocketSession session = sessionOpt.get();

                    Map<String, Object> complete = new LinkedHashMap<>();
                    complete.put("event", "complete");
                    complete.put("message", "Workflow execution completed");

                    sendMessage(session, complete);

                    try {
                        session.close(CloseStatus.NORMAL);
                    } catch (IOException e) {
                        logger.error("Error closing WebSocket session: {}", sessionKey, e);
                    }
                }

                callSessionRegistry.removeSessionByCallSid(callSid);
                sessionIdToCallSid.invalidate(sessionKey);
            }

            AutoCloseable handle = streamHandles.getIfPresent(sessionKey);

            if (handle != null) {
                closeHandle(handle);
                streamHandles.invalidate(sessionKey);
            }

            pendingEvents.invalidate(sessionKey);
        }

        @Override
        public void onError(Throwable throwable) {
            String callSid = sessionIdToCallSid.getIfPresent(sessionKey);

            WebSocketSession session = null;

            if (callSid != null) {
                Optional<WebSocketSession> sessionOpt = callSessionRegistry.getWebSocketSession(callSid);

                session = sessionOpt.orElse(null);
            }

            if (session != null && session.isOpen()) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("event", "error");

                if (throwable instanceof CancellationException) {
                    error.put("message", "Workflow execution aborted");
                } else {
                    error.put("message", throwable.getMessage());
                }

                sendMessage(session, error);

                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (IOException e) {
                    logger.error("Error closing WebSocket session: {}", sessionKey, e);
                }
            }

            if (callSid != null) {
                callSessionRegistry.removeSessionByCallSid(callSid);
                sessionIdToCallSid.invalidate(sessionKey);
            }

            AutoCloseable handle = streamHandles.getIfPresent(sessionKey);

            if (handle != null) {
                closeHandle(handle);
                streamHandles.invalidate(sessionKey);
            }

            pendingEvents.invalidate(sessionKey);
        }

        private void bufferEvent(Object payload) {
            List<Map<String, Object>> events = pendingEvents.get(sessionKey, key -> new ArrayList<>());

            if (events.size() < MAX_PENDING_EVENTS) {
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("event", "stream");
                event.put("data", payload);
                events.add(event);
            }
        }
    }
}
