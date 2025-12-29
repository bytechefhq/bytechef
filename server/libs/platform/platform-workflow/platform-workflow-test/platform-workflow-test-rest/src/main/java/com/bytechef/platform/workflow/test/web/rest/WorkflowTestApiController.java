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
import com.bytechef.platform.workflow.test.facade.WorkflowTestFacade;
import com.bytechef.platform.workflow.test.web.rest.model.WorkflowTestExecutionModel;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    private final ConversionService conversionService;
    private final Cache<String, SseEmitter> emitter = createCache();
    private final Cache<String, List<AutoCloseable>> listenerHandles = createCache();
    private final int maxPendingEvents;
    private final Cache<String, List<SseEmitter.SseEventBuilder>> pendingEvents = createCache();
    private final Cache<String, CompletableFuture<WorkflowTestExecutionModel>> runs = createCache();
    private final TempFileStorage tempFileStorage;
    private final WorkflowTestFacade workflowTestFacade;

    @Autowired
    @SuppressFBWarnings("EI")
    public WorkflowTestApiController(
        ConversionService conversionService, TempFileStorage tempFileStorage, WorkflowTestFacade workflowTestFacade) {

        this(conversionService, tempFileStorage, workflowTestFacade, 100);
    }

    @SuppressFBWarnings("EI")
    public WorkflowTestApiController(
        ConversionService conversionService, TempFileStorage tempFileStorage, WorkflowTestFacade workflowTestFacade,
        int maxPendingEvents) {

        this.conversionService = conversionService;
        this.tempFileStorage = tempFileStorage;
        this.workflowTestFacade = workflowTestFacade;
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

        CompletableFuture<WorkflowTestExecutionModel> future = runs.getIfPresent(key);

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

        registerEmitter(key, jobId, emitter);

        CompletableFuture.runAsync(() -> sendToEmitter(
            key, createEvent("start", Map.of("jobId", String.valueOf(jobId)))));

        future.whenComplete((result, throwable) -> {
            try {
                if (throwable instanceof CancellationException) {
                    sendToEmitter(key, createEvent("error", "Aborted"));
                } else if (throwable != null) {
                    sendToEmitter(
                        key, createEvent("error", Objects.toString(throwable.getMessage(), "An error occurred")));
                } else {
                    sendToEmitter(key, createEvent("result", result));
                }
            } finally {
                completeAndClearEmitter(key);
                unregisterListeners(key);
            }
        });

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

        CompletableFuture<WorkflowTestExecutionModel> future = runs.getIfPresent(key);

        if (future != null && !future.isDone()) {
            future.cancel(true);
        } else {
            workflowTestFacade.stopTest(Long.parseLong(jobId));

            try {
                sendToEmitter(key, createEvent("error", "Aborted"));
            } finally {
                runs.invalidate(key);
                pendingEvents.invalidate(key);
                completeAndClearEmitter(key);
                unregisterListeners(key);
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
    public SseEmitter testWorkflow(
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
        long jobId = workflowTestFacade.startTestWorkflow(id, inputs, environmentId);

        String key = TenantCacheKeyUtils.getKey(jobId);

        registerEmitter(key, jobId, emitter);

        CompletableFuture.runAsync(() -> sendToEmitter(
            key, createEvent("start", Map.of("jobId", String.valueOf(jobId)))));

        String currentTenantId = TenantContext.getCurrentTenantId();

        CompletableFuture<WorkflowTestExecutionModel> future =
            CompletableFuture.supplyAsync(() -> TenantContext.callWithTenantId(
                currentTenantId, () -> Objects.requireNonNull(conversionService.convert(
                    workflowTestFacade.awaitTestResult(jobId), WorkflowTestExecutionModel.class))));

        runs.put(key, future);

        future.whenComplete((result, throwable) -> {
            try {
                if (throwable instanceof CancellationException) {
                    workflowTestFacade.stopTest(jobId);

                    sendToEmitter(key, createEvent("error", "Aborted"));
                } else if (throwable != null) {
                    sendToEmitter(
                        key, createEvent("error", Objects.toString(throwable.getMessage(), "An error occurred")));
                } else {
                    sendToEmitter(key, createEvent("result", result));
                }
            } finally {
                runs.invalidate(key);
                pendingEvents.invalidate(key);
                completeAndClearEmitter(key);
                unregisterListeners(key);
            }
        });

        return emitter;
    }

    private static Map<String, Object> getInputs(@Nullable TestWorkflowRequest testWorkflowRequest) {
        return (testWorkflowRequest != null && testWorkflowRequest.inputs() != null)
            ? testWorkflowRequest.inputs() : Map.of();
    }

    private void registerListenersIfAbsent(String key, long jobId) {
        listenerHandles.get(key, k -> {
            List<AutoCloseable> handles = new ArrayList<>();

            handles.add(workflowTestFacade.addJobStatusListener(
                jobId, (event) -> sendToEmitter(key, createEvent("job", JsonUtils.write(event)))));

            handles.add(
                workflowTestFacade.addTaskStartedListener(
                    jobId, (event) -> sendToEmitter(key, createEvent("task", JsonUtils.write(event)))));

            handles.add(
                workflowTestFacade.addTaskExecutionCompleteListener(
                    jobId, (event) -> sendToEmitter(key, createEvent("task", JsonUtils.write(event)))));

            handles.add(
                workflowTestFacade.addErrorListener(
                    jobId, (event) -> sendToEmitter(key, createEvent("error", JsonUtils.write(event)))));

            return handles;
        });
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
            .data(data);
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

    private void registerEmitter(String key, long jobId, SseEmitter emitter) {
        this.emitter.put(key, emitter);

        List<SseEmitter.SseEventBuilder> bufferedEvents = pendingEvents.getIfPresent(key);

        if (bufferedEvents != null) {
            pendingEvents.invalidate(key);

            for (SseEmitter.SseEventBuilder eventBuilder : bufferedEvents) {
                sendToEmitter(key, eventBuilder);
            }
        }

        registerListenersIfAbsent(key, jobId);
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
        } catch (Exception ex) {
            if (logger.isTraceEnabled()) {
                logger.trace(ex.getMessage(), ex);
            }

            this.emitter.invalidate(key);
        }
    }

    private void unregisterListeners(String key) {
        List<AutoCloseable> handles = listenerHandles.getIfPresent(key);

        if (handles != null) {
            listenerHandles.invalidate(key);

            for (AutoCloseable handle : handles) {
                try {
                    handle.close();
                } catch (Exception exception) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(exception.getMessage(), exception);
                    }
                }
            }
        }
    }

    @SuppressFBWarnings("EI")
    public record TestWorkflowRequest(@Nullable Map<String, Object> inputs) {
    }
}
