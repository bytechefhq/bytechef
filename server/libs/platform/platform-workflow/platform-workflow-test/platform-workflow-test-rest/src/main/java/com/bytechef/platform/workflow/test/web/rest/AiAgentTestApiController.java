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

package com.bytechef.platform.workflow.test.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.StringUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.workflow.test.facade.AiAgentTestFacade;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
class AiAgentTestApiController {

    private static final Logger logger = LoggerFactory.getLogger(AiAgentTestApiController.class);

    private final Cache<String, SseEmitter> activeTests = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .maximumSize(100)
        .build();
    private final AiAgentTestFacade aiAgentTestFacade;
    private final Executor executor;

    @SuppressFBWarnings("EI")
    AiAgentTestApiController(AiAgentTestFacade aiAgentTestFacade, TaskExecutor taskExecutor) {
        this.aiAgentTestFacade = aiAgentTestFacade;
        this.executor = taskExecutor;
    }

    @PostMapping(
        value = "/ai-agent-tests",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter testAiAgent(@RequestBody AiAgentTestRequest aiAgentTestRequest) {
        SseEmitter sseEmitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));

        String testId = String.valueOf(UUID.randomUUID());

        activeTests.put(testId, sseEmitter);

        sseEmitter.onCompletion(() -> activeTests.invalidate(testId));
        sseEmitter.onTimeout(() -> activeTests.invalidate(testId));
        sseEmitter.onError(throwable -> activeTests.invalidate(testId));

        CompletableFuture.runAsync(() -> {
            try {
                sendEvent(sseEmitter, "start", Map.of("testId", testId));

                Object result = aiAgentTestFacade.executeAiAgentAction(
                    aiAgentTestRequest.workflowId(), aiAgentTestRequest.workflowNodeName(),
                    aiAgentTestRequest.environmentId(), aiAgentTestRequest.conversationId(),
                    aiAgentTestRequest.message(), aiAgentTestRequest.attachments());

                if (result instanceof ActionDefinition.SseEmitterHandler sseEmitterHandler) {
                    AiAgentSseEmitterBridge bridge = new AiAgentSseEmitterBridge(sseEmitter, testId);

                    sseEmitterHandler.handle(bridge);
                } else if (result instanceof Map<?, ?> resultMap && resultMap.containsKey("toolExecutions")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> toolExecutions =
                        (List<Map<String, Object>>) resultMap.get("toolExecutions");

                    for (Map<String, Object> toolExecution : toolExecutions) {
                        sendEvent(sseEmitter, "tool_execution", toolExecution);
                    }

                    sendEvent(
                        sseEmitter, "result", resultMap.get("response") != null ? resultMap.get("response") : "");

                    completeEmitter(sseEmitter, testId);
                } else {
                    sendEvent(sseEmitter, "result", result != null ? result : "");

                    completeEmitter(sseEmitter, testId);
                }
            } catch (Exception exception) {
                logger.warn(
                    "AI agent test failed for workflow '{}', node '{}': {}",
                    StringUtils.sanitize(aiAgentTestRequest.workflowId()),
                    StringUtils.sanitize(aiAgentTestRequest.workflowNodeName()),
                    StringUtils.sanitize(exception.getMessage()), exception);

                sendEvent(sseEmitter, "error", getRootCauseMessage(exception));

                completeEmitter(sseEmitter, testId);
            }
        }, executor);

        return sseEmitter;
    }

    @PostMapping(value = "/ai-agent-tests/{testId}/stop")
    public ResponseEntity<Void> stopAiAgentTest(@PathVariable String testId) {
        SseEmitter sseEmitter = activeTests.getIfPresent(testId);

        if (sseEmitter != null) {
            sendEvent(sseEmitter, "error", "Aborted");

            completeEmitter(sseEmitter, testId);
        }

        return ResponseEntity.ok()
            .build();
    }

    private static String getRootCauseMessage(Throwable throwable) {
        Throwable rootCause = throwable;

        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        String message = rootCause.getMessage();

        if (message != null) {
            return message;
        }

        Class<? extends Throwable> rootCauseClass = rootCause.getClass();

        return "An unexpected error occurred: " + rootCauseClass.getSimpleName();
    }

    private void completeEmitter(SseEmitter sseEmitter, String testId) {
        activeTests.invalidate(testId);

        try {
            sseEmitter.complete();
        } catch (Exception exception) {
            if (logger.isTraceEnabled()) {
                logger.trace(exception.getMessage(), exception);
            }
        }
    }

    private void sendEvent(SseEmitter sseEmitter, String name, Object data) {
        try {
            sseEmitter.send(
                SseEmitter.event()
                    .name(name)
                    .data(data instanceof String ? JsonUtils.write(data) : data));
        } catch (Exception exception) {
            if (logger.isTraceEnabled()) {
                logger.trace("Failed to send SSE '{}' event: {}", name, exception.getMessage(), exception);
            }
        }
    }

    @SuppressFBWarnings("EI")
    record AiAgentTestRequest(
        String workflowId, String workflowNodeName, long environmentId, String conversationId, String message,
        List<Object> attachments) {

        AiAgentTestRequest {
            Objects.requireNonNull(workflowId, "workflowId");
            Objects.requireNonNull(workflowNodeName, "workflowNodeName");
            Objects.requireNonNull(conversationId, "conversationId");
            Objects.requireNonNull(message, "message");

            if (attachments == null) {
                attachments = List.of();
            }
        }
    }

    private class AiAgentSseEmitterBridge implements ActionDefinition.SseEmitterHandler.SseEmitter {

        private final StringBuffer accumulatedContent = new StringBuffer();
        private volatile boolean completed;
        private final SseEmitter springSseEmitter;
        private final String testId;

        AiAgentSseEmitterBridge(SseEmitter springSseEmitter, String testId) {
            this.springSseEmitter = springSseEmitter;
            this.testId = testId;
        }

        @Override
        public void addTimeoutListener(Runnable timeoutListener) {
            springSseEmitter.onTimeout(() -> {
                activeTests.invalidate(testId);

                timeoutListener.run();
            });
        }

        @Override
        public void complete() {
            if (completed) {
                return;
            }

            completed = true;

            sendEvent(springSseEmitter, "result", accumulatedContent.toString());

            completeEmitter(springSseEmitter, testId);
        }

        @Override
        public void error(Throwable throwable) {
            if (completed) {
                return;
            }

            completed = true;

            sendEvent(springSseEmitter, "error", getRootCauseMessage(throwable));

            completeEmitter(springSseEmitter, testId);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void send(Object data) {
            if (!(data instanceof Map<?, ?> map)) {
                if (data instanceof String stringData) {
                    accumulatedContent.append(stringData);
                }

                sendEvent(springSseEmitter, "stream", data);

                return;
            }

            Object eventType = map.get(AiAgentSseEventType.EVENT_TYPE);

            if (AiAgentSseEventType.TOOL_EXECUTION.equals(eventType)) {
                Map<String, Object> eventData = new LinkedHashMap<>((Map<String, Object>) map);

                eventData.remove(AiAgentSseEventType.EVENT_TYPE);

                if (logger.isTraceEnabled()) {
                    logger.trace("Sending tool_execution SSE event: toolName={}", eventData.get("toolName"));
                }

                sendEvent(springSseEmitter, AiAgentSseEventType.TOOL_EXECUTION, eventData);
            } else if (AiAgentSseEventType.ASK_USER_QUESTION.equals(eventType)) {
                Map<String, Object> eventData = new LinkedHashMap<>((Map<String, Object>) map);

                eventData.remove(AiAgentSseEventType.EVENT_TYPE);

                sendEvent(springSseEmitter, AiAgentSseEventType.ASK_USER_QUESTION, eventData);
            } else {
                sendEvent(springSseEmitter, "stream", data);
            }
        }
    }
}
