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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.StringUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
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

    private final Cache<String, SseEmitter> activeTests = createCache();
    private final ActionDefinitionFacade actionDefinitionFacade;
    private final Evaluator evaluator;
    private final Executor executor;
    private final TempFileStorage tempFileStorage;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    AiAgentTestApiController(
        ActionDefinitionFacade actionDefinitionFacade, Evaluator evaluator, TaskExecutor taskExecutor,
        TempFileStorage tempFileStorage, WorkflowNodeOutputFacade workflowNodeOutputFacade,
        WorkflowService workflowService, WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.evaluator = evaluator;
        this.executor = taskExecutor;
        this.tempFileStorage = tempFileStorage;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @PostMapping(
        value = "/ai-agent-tests",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter testAiAgent(@RequestBody AiAgentTestRequest aiAgentTestRequest) {
        SseEmitter sseEmitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));

        String testId = String.valueOf(UUID.randomUUID());

        activeTests.put(testId, sseEmitter);

        sseEmitter.onCompletion(() -> activeTests.invalidate(testId));
        sseEmitter.onTimeout(() -> activeTests.invalidate(testId));
        sseEmitter.onError(throwable -> activeTests.invalidate(testId));

        CompletableFuture.runAsync(() -> {
            try {
                sendEvent(sseEmitter, "start", Map.of("testId", testId));

                String workflowId = aiAgentTestRequest.workflowId();
                String workflowNodeName = aiAgentTestRequest.workflowNodeName();
                long environmentId = aiAgentTestRequest.environmentId();

                Workflow workflow = workflowService.getWorkflow(workflowId);

                WorkflowTask workflowTask = workflow.getTasks(true)
                    .stream()
                    .filter(task -> Objects.equals(task.getName(), workflowNodeName))
                    .findFirst()
                    .orElseThrow(
                        () -> new IllegalArgumentException(
                            "Workflow task not found: %s".formatted(workflowNodeName)));

                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

                Map<String, Object> taskParameters = new HashMap<>(workflowTask.getParameters());

                taskParameters.put("conversationId", aiAgentTestRequest.conversationId());
                taskParameters.put("userPrompt", aiAgentTestRequest.message());

                taskParameters.put("attachments", aiAgentTestRequest.attachments());

                Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
                    workflowId, environmentId);

                Map<String, ?> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
                    workflowId, workflowNodeName, environmentId);

                @SuppressWarnings("unchecked")
                Map<String, Object> evaluatedParameters = (Map<String, Object>) evaluator.evaluate(
                    taskParameters, MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs));

                if (evaluatedParameters.containsKey("attachments")) {
                    evaluatedParameters.put(
                        "attachments",
                        TestAttachmentUtils.getFileEntries(tempFileStorage, evaluatedParameters));
                }

                List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections =
                    workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                        workflowId, workflowNodeName, environmentId);

                Map<String, Long> connectionIds = MapUtils.toMap(
                    workflowTestConfigurationConnections,
                    WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                    WorkflowTestConfigurationConnection::getConnectionId);

                Map<String, ?> extensions = workflowTask.getExtensions();

                Object result = actionDefinitionFacade.executePerform(
                    workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(), null, null, null,
                    workflowId, evaluatedParameters, connectionIds, extensions, environmentId, null, true);

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

                String errorMessage = exception.getMessage() != null
                    ? exception.getMessage()
                    : "An unexpected error occurred: " + exception.getClass()
                        .getSimpleName();

                sendEvent(sseEmitter, "error", errorMessage);

                completeEmitter(sseEmitter, testId);
            }
        }, executor);

        return sseEmitter;
    }

    @PostMapping(value = "/ai-agent-tests/{testId}/stop")
    ResponseEntity<Void> stopAiAgentTest(@PathVariable String testId) {
        SseEmitter sseEmitter = activeTests.getIfPresent(testId);

        if (sseEmitter != null) {
            sendEvent(sseEmitter, "error", "Aborted");

            completeEmitter(sseEmitter, testId);
        }

        return ResponseEntity.ok()
            .build();
    }

    private static <K, V> Cache<K, V> createCache() {
        return Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();
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
        String workflowId,
        String workflowNodeName,
        long environmentId,
        String conversationId,
        String message,
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

            String errorMessage = throwable.getMessage() != null
                ? throwable.getMessage()
                : "An unexpected error occurred: " + throwable.getClass()
                    .getSimpleName();

            sendEvent(springSseEmitter, "error", errorMessage);

            completeEmitter(springSseEmitter, testId);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void send(Object data) {
            if (data instanceof Map<?, ?> map && "tool_execution".equals(map.get("__eventType"))) {
                Map<String, Object> eventData = new LinkedHashMap<>((Map<String, Object>) map);

                eventData.remove("__eventType");

                if (logger.isTraceEnabled()) {
                    logger.trace("Sending tool_execution SSE event: toolName={}", eventData.get("toolName"));
                }

                sendEvent(springSseEmitter, "tool_execution", eventData);
            } else {
                if (data instanceof String stringData) {
                    accumulatedContent.append(stringData);
                }

                sendEvent(springSseEmitter, "stream", data);
            }
        }
    }
}
