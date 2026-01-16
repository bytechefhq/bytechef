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
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.workflow.test.dto.WorkflowTestExecutionDTO;
import com.bytechef.platform.workflow.test.facade.TestWorkflowExecutor;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
public class WorkflowTestApiController implements WorkflowTestApi {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowTestApiController.class);

    private static final Pattern IMAGE_PATTERN = Pattern.compile("data:image/[^;]+;base64,([^\\s]+)");
    private static final Pattern TEXT_PATTERN = Pattern.compile(
        "<attachment[^>]*>(.*?)</attachment>", Pattern.DOTALL);

    private final Cache<String, SseEmitter> emitter = createCache();
    private final int maxPendingEvents;
    private final Cache<String, List<SseEmitter.SseEventBuilder>> pendingEvents = createCache();
    private final Cache<String, CompletableFuture<WorkflowTestExecutionDTO>> workflowExecutions = createCache();
    private final TempFileStorage tempFileStorage;
    private final TestWorkflowExecutor testWorkflowExecutor;

    @Autowired
    @SuppressFBWarnings("EI")
    public WorkflowTestApiController(TempFileStorage tempFileStorage, TestWorkflowExecutor testWorkflowExecutor) {
        this(tempFileStorage, testWorkflowExecutor, 100);
    }

    @SuppressFBWarnings("EI")
    public WorkflowTestApiController(
        TempFileStorage tempFileStorage, TestWorkflowExecutor testWorkflowExecutor, int maxPendingEvents) {

        this.tempFileStorage = tempFileStorage;
        this.testWorkflowExecutor = testWorkflowExecutor;
        this.maxPendingEvents = Math.max(1, maxPendingEvents);
    }

    /**
     * Attaches a workflow test to the provided job identifier, enabling real-time streaming of workflow events to the
     * client via a Server-Sent Events (SSE) emitter.
     *
     * @param jobId the unique identifier of the job for which the workflow test is being attached
     * @return an {@link SseEmitter} instance that streams events such as start, error, and result to the client
     */
    @GetMapping(value = "/workflow-tests/{jobId}/attach", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter attachWorkflowTest(@PathVariable Long jobId) {
        final String key = TenantCacheKeyUtils.getKey(jobId);
        final SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));

        CompletableFuture<WorkflowTestExecutionDTO> future = workflowExecutions.getIfPresent(key);

        if (future == null) {
            List<SseEmitter.SseEventBuilder> bufferedEvents = pendingEvents.getIfPresent(key);

            if (bufferedEvents != null && !bufferedEvents.isEmpty()) {
                pendingEvents.invalidate(key);

                try {
                    for (SseEmitter.SseEventBuilder eventBuilder : bufferedEvents) {
                        try {
                            emitter.send(eventBuilder);
                        } catch (IOException exception) {
                            if (logger.isTraceEnabled()) {
                                logger.trace(
                                    "Failed to send buffered SSE event for job {}: {}", jobId, exception.getMessage(),
                                    exception);
                            }

                            try {
                                emitter.send(createEvent("error", "Failed to deliver buffered events"));
                            } catch (IOException ioException) {
                                if (logger.isTraceEnabled()) {
                                    logger.trace(
                                        "Failed to send SSE error event for job {}: {}", jobId,
                                        ioException.getMessage(), ioException);
                                }

                                break;
                            }
                        }
                    }
                } finally {
                    emitter.complete();
                }
            } else {
                if (bufferedEvents != null) {
                    pendingEvents.invalidate(key);
                }

                try {
                    emitter.send(createEvent("error", "Not running"));
                } catch (Exception exception) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(exception.getMessage(), exception);
                    }
                } finally {
                    emitter.complete();
                }
            }

            return emitter;
        }

        registerEmitter(key, emitter);

        CompletableFuture.runAsync(() -> sendToEmitter(
            key, createEvent("start", Map.of("jobId", String.valueOf(jobId)))));

        return emitter;
    }

    /**
     * Stops a running workflow test associated with the specified job ID. The method performs a best-effort attempt to
     * stop the test, invalidates associated resources, and notifies subscribers of the stop event.
     *
     * @param jobId The unique identifier of the workflow test job to stop.
     * @return A ResponseEntity with no content, indicating the stop operation has been processed.
     */
    @Override
    @PostMapping(value = "/workflow-tests/{jobId}/stop")
    public ResponseEntity<Void> stopWorkflowTest(@PathVariable String jobId) {
        if (!jobId.matches("\\d+")) {
            return ResponseEntity.badRequest()
                .build();
        }

        final String key = TenantCacheKeyUtils.getKey(jobId);

        CompletableFuture<WorkflowTestExecutionDTO> future = workflowExecutions.getIfPresent(key);

        if (future != null && !future.isDone()) {
            future.cancel(true);
        } else {
            testWorkflowExecutor.stop(Long.parseLong(jobId));

            try {
                sendToEmitter(key, createEvent("error", "Aborted"));
            } finally {
                whenComplete(key);
            }
        }

        return ResponseEntity.ok()
            .build();
    }

    /**
     * Initiates a test execution for a specified workflow and returns an {@code SseEmitter} to stream real-time events
     * related to the workflow testing process, such as the start, results, or errors.
     *
     * @param id                  the identifier of the workflow to test
     * @param environmentId       the identifier of the environment in which the test is to be run
     * @param testWorkflowRequest an optional request object containing inputs for the test execution; if null or
     *                            missing inputs, defaults will be applied
     * @return an {@code SseEmitter} to stream events related to the workflow testing process
     */
    @PostMapping(
        value = "/workflows/{id}/tests", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startWorkflowTest(
        @PathVariable String id, @RequestParam("environmentId") Long environmentId,
        @Nullable @RequestBody TestWorkflowRequest testWorkflowRequest) {

        Map<String, Object> inputs = getInputs(testWorkflowRequest);

        if (inputs.containsKey("trigger_1")) {
            Map<String, Object> trigger1 = MapUtils.getRequiredMap(inputs, "trigger_1", new TypeReference<>() {});

            if (trigger1.containsKey("attachments")) {
                trigger1.put("attachments", getFileEntries(trigger1));
            } else {
                trigger1.put("attachments", List.of());
            }

            inputs.put("trigger_1", trigger1);
        }

        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));

        testWorkflowExecutor.executeAsync(
            id, inputs, environmentId, (key) -> registerEmitter(key, emitter), WebhookSseStreamBridge::new,
            workflowExecutions::put, this::whenComplete);

        return emitter;
    }

    private static Map<String, Object> getInputs(@Nullable TestWorkflowRequest testWorkflowRequest) {
        return (testWorkflowRequest != null && testWorkflowRequest.inputs() != null)
            ? testWorkflowRequest.inputs() : Map.of();
    }

    private void completeAndClearEmitter(String key) {
        SseEmitter emitter = this.emitter.getIfPresent(key);

        if (emitter != null) {
            this.emitter.invalidate(key);

            try {
                emitter.complete();
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    private static <K, V> Cache<K, V> createCache() {
        return Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();
    }

    private SseEmitter.SseEventBuilder createEvent(String name, Object data) {
        return SseEmitter.event()
            .name(name)
            .data(data instanceof String ? JsonUtils.write(data) : data);
    }

    private List<FileEntry> getFileEntries(Map<String, Object> trigger1) {
        List<Map<String, Object>> attachments = MapUtils.getRequiredList(
            trigger1, "attachments", new TypeReference<>() {});

        List<FileEntry> fileEntries = new ArrayList<>();

        for (Map<String, Object> attachment : attachments) {
            List<Map<String, String>> content = MapUtils.getRequiredList(
                attachment, "content", new TypeReference<>() {});
            String contentType = MapUtils.getString(attachment, "contentType");
            String name = (String) attachment.get("name");

            if (contentType.startsWith("text/")) {
                Matcher matcher = TEXT_PATTERN.matcher(MapUtils.getString(content.getFirst(), "text"));

                if (matcher.find()) {
                    String text = matcher.group(1);

                    fileEntries.add(tempFileStorage.storeFileContent(name, text));
                }
            } else {
                Matcher matcher = IMAGE_PATTERN.matcher(MapUtils.getString(content.getFirst(), "image"));

                if (matcher.find()) {
                    fileEntries.add(tempFileStorage.storeFileContent(
                        name, new ByteArrayInputStream(EncodingUtils.base64Decode(matcher.group(1)))));
                }
            }
        }

        return fileEntries;
    }

    private void registerEmitter(String key, SseEmitter emitter) {
        this.emitter.put(key, emitter);

        List<SseEmitter.SseEventBuilder> bufferedEvents = pendingEvents.getIfPresent(key);

        if (bufferedEvents != null) {
            pendingEvents.invalidate(key);

            for (SseEmitter.SseEventBuilder eventBuilder : bufferedEvents) {
                sendToEmitter(key, eventBuilder);
            }
        }
    }

    private void sendToEmitter(String key, SseEmitter.SseEventBuilder event) {
        SseEmitter emitter = this.emitter.getIfPresent(key);

        if (emitter == null) {
            List<SseEmitter.SseEventBuilder> sseEventBuilders = Objects.requireNonNull(
                pendingEvents.get(key, k -> new ArrayList<>()));

            if (sseEventBuilders.size() >= maxPendingEvents) {
                sseEventBuilders.removeFirst();
            }

            sseEventBuilders.add(event);

            return;
        }

        try {
            emitter.send(event);
        } catch (Exception exception) {
            if (logger.isTraceEnabled()) {
                logger.trace(exception.getMessage(), exception);
            }

            this.emitter.invalidate(key);
        }
    }

    private void whenComplete(String key) {
        workflowExecutions.invalidate(key);
        pendingEvents.invalidate(key);
        completeAndClearEmitter(key);
    }

    @SuppressFBWarnings("EI")
    public record TestWorkflowRequest(@Nullable Map<String, Object> inputs) {
    }

    /**
     * Bridge that broadcasts streamed payloads to all currently attached SSE clients for this job key.
     */
    private class WebhookSseStreamBridge implements SseStreamBridge {

        private final String key;

        private WebhookSseStreamBridge(String key) {
            this.key = key;
        }

        @Override
        public void onEvent(Object payload) {
            if (payload instanceof Map<?, ?> map && map.containsKey("event")) {
                String event = (String) map.get("event");
                Object data = map.entrySet()
                    .stream()
                    .filter(entry -> !"event".equals(entry.getKey()))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .orElse(null);

                sendToEmitter(key, createEvent(event, data));
            } else {
                sendToEmitter(key, createEvent("stream", payload));

            }
        }

        @Override
        public void onComplete() {
            // do not complete overall SSE; a single action stream completion should not close the job stream
        }
    }
}
