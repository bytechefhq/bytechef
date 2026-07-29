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

package com.bytechef.platform.webhook.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.webhook.executor.SseStreamBridgeRegistry;
import com.bytechef.platform.webhook.executor.SseStreamBridgeRegistry.Registration;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Public controller for anonymous suspended-job resume webhook callbacks. Authentication is provided by the
 * cryptographically signed token embedded in the path, so the endpoint lives outside the {@code /api/**} namespace —
 * alongside other anonymous webhook-style callbacks — and is permitted in the Order-4 filter chain where CSRF is
 * disabled by convention.
 *
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class JobResumeController {

    private static final Logger log = LoggerFactory.getLogger(JobResumeController.class);

    private static final long SSE_TIMEOUT = TimeUnit.MINUTES.toMillis(30);

    private final JobResumeFacade jobResumeFacade;
    private final SseStreamBridgeRegistry sseStreamBridgeRegistry;

    @SuppressFBWarnings("EI")
    public JobResumeController(JobResumeFacade jobResumeFacade, SseStreamBridgeRegistry sseStreamBridgeRegistry) {
        this.jobResumeFacade = jobResumeFacade;
        this.sseStreamBridgeRegistry = sseStreamBridgeRegistry;
    }

    /**
     * Resumes a suspended job via anonymous webhook callback (typically submitted by the approval form in the SPA).
     *
     * <p>
     * <b>Security Note:</b> CSRF protection is not required for this endpoint. Resume callbacks may come from external
     * sources (e.g., email links) that cannot include CSRF tokens. Security is maintained through cryptographic resume
     * tokens that are verified before processing.
     *
     * <p>
     * The {@code produces} attribute is intentionally unset so Spring content-negotiates by the request's
     * {@code Accept} header: a request that accepts {@code text/event-stream} is routed to
     * {@link #resumeStreaming(String, Map)} instead, which streams the resumed turn's Server-Sent Events.
     */
    @SuppressFBWarnings(
        value = "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
        justification = "CSRF disabled for external resume callbacks")
    @RequestMapping(method = {
        RequestMethod.GET, RequestMethod.POST
    }, value = "/job/resume/{id}")
    public ResponseEntity<Void> resume(
        @PathVariable String id, @RequestBody(required = false) Map<String, Object> data) {

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(id, data);

        return switch (outcome) {
            case OK -> ResponseEntity.noContent()
                .build();
            case INVALID_ID -> ResponseEntity.badRequest()
                .build();
            case GONE -> ResponseEntity.status(HttpStatus.GONE)
                .build();
        };
    }

    /**
     * Resumes a suspended job and streams the resumed turn's Server-Sent Events back over the same
     * {@code /job/resume/{id}} endpoint. Selected by Spring content negotiation when the request accepts
     * {@code text/event-stream}; otherwise {@link #resume(String, Map)} handles the callback and returns
     * {@code 204 No Content}.
     *
     * <p>
     * A resumed streaming-agent turn runs asynchronously on a worker: the worker's
     * {@code SseStreamTaskExecutionPostOutputProcessor} emits each streamed event onto the {@code SSE_STREAM_EVENTS}
     * message route, where {@link SseStreamBridgeRegistry} fans it out to bridges registered by numeric job id. This
     * method registers an {@link SseEmitter}-backed {@link SseStreamBridge} for the resumed job — within the
     * {@code jobIdConsumer} callback, which the facade invokes after token validation but before the resume is
     * triggered, so the bridge is in place before the first delta is emitted.
     *
     * <p>
     * Invalid or already-gone resume ids complete the emitter with an error rather than leaving the stream hanging.
     *
     * <p>
     * <b>Security Note:</b> CSRF protection is not required — see {@link #resume(String, Map)}.
     */
    @SuppressFBWarnings(
        value = "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
        justification = "CSRF disabled for external resume callbacks")
    @RequestMapping(
        method = {
            RequestMethod.GET, RequestMethod.POST
        },
        value = "/job/resume/{id}",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter resumeStreaming(
        @PathVariable String id, @RequestBody(required = false) Map<String, Object> data) {

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        JobResumeSseStreamBridge bridge = new JobResumeSseStreamBridge(emitter);

        // Capture the bridge registration so we can close it if dispatch fails between registration and worker
        // dispatch. Without this, `registration.completion()` only resolves on a terminal job-status event — which
        // never arrives if the worker never started — and the bridge entry leaks until Caffeine evicts it.
        AtomicReference<Registration> registrationReference = new AtomicReference<>();

        try {
            JobResumeOutcome outcome = jobResumeFacade.resumeJobStreaming(
                id, data == null ? Map.of() : data,
                jobId -> {
                    Registration registration = sseStreamBridgeRegistry.register(jobId, bridge);

                    registrationReference.set(registration);

                    registration.completion()
                        .whenComplete((unused, throwable) -> {
                            if (throwable != null) {
                                log.warn("Resumed turn for job {} completed exceptionally", jobId, throwable);

                                bridge.onError(throwable);
                            }

                            closeQuietly(registration.handle());
                        });
                });

            // On OK the resumed turn is dispatched and the bridge relays streamed events as they arrive on the
            // SSE_STREAM_EVENTS route; the emitter is completed by the bridge's onComplete/onError. On a failure
            // outcome the bridge is completed with an error so the SSE client sees a terminal event instead of a
            // hanging stream — and we close the registration handle here because the completion future will never
            // resolve (no worker ran, so no terminal job-status event arrives).
            if (outcome == JobResumeOutcome.INVALID_ID) {
                bridge.onError(new IllegalArgumentException("Invalid resume id."));

                closeRegistrationIfPresent(registrationReference);
            } else if (outcome == JobResumeOutcome.GONE) {
                bridge.onError(new IllegalStateException("Job can no longer be resumed."));

                closeRegistrationIfPresent(registrationReference);
            }
        } catch (Exception exception) {
            log.warn("Failed to dispatch resumed turn for id '{}'", id, exception);

            bridge.onError(exception);

            closeRegistrationIfPresent(registrationReference);
        }

        return emitter;
    }

    private static void closeRegistrationIfPresent(AtomicReference<Registration> registrationReference) {
        Registration registration = registrationReference.get();

        if (registration != null) {
            closeQuietly(registration.handle());
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception exception) {
            log.warn("Failed to close resume bridge registration handle", exception);
        }
    }

    private static void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(
                SseEmitter.event()
                    .name(name)
                    .data(data instanceof String ? JsonUtils.write(data) : data));
        } catch (AsyncRequestNotUsableException exception) {
            // Client disconnected mid-stream; expected on tab close / navigate-away. Log at DEBUG so it doesn't
            // pollute production logs but is still discoverable.
            log.debug("SSE send skipped — async request no longer usable (client disconnected): {}",
                exception.getMessage());
        } catch (Exception exception) {
            log.warn("Failed to send SSE event '{}' on resume stream", name, exception);
        }
    }

    /**
     * Relays a resumed turn's streamed payloads to an SSE client. Events arrive on message-broker threads concurrently
     * with completion events and client-initiated disconnects, so callbacks are idempotent and skip sends once the
     * emitter is closed. Mirrors {@code WebhookTriggerController.WebhookSseStreamBridge}.
     */
    private static class JobResumeSseStreamBridge implements SseStreamBridge {

        private final AtomicBoolean completed = new AtomicBoolean();
        private final SseEmitter emitter;

        private JobResumeSseStreamBridge(SseEmitter emitter) {
            this.emitter = emitter;

            emitter.onCompletion(() -> completed.set(true));
            emitter.onTimeout(() -> completed.set(true));
            emitter.onError(throwable -> completed.set(true));
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onEvent(Object payload) {
            if (completed.get()) {
                return;
            }

            if (payload instanceof Map<?, ?> map) {
                // AI Agent SSE event types (approval_request, ask_user_question, tool_execution) become named
                // events — mirroring WebhookSseStreamBridge — so a resumed turn's interactive events reach the
                // same client eventHandlers as on the original webhook stream, instead of arriving mangled as
                // generic `stream` payloads.
                if (map.containsKey(AiAgentSseEventType.EVENT_TYPE)) {
                    String eventType = (String) map.get(AiAgentSseEventType.EVENT_TYPE);

                    Map<String, Object> eventData = new LinkedHashMap<>((Map<String, Object>) map);

                    eventData.remove(AiAgentSseEventType.EVENT_TYPE);

                    sendEvent(emitter, eventType, eventData);

                    return;
                }

                if (map.containsKey("event")) {
                    String event = (String) map.get("event");
                    Object data = map.entrySet()
                        .stream()
                        .filter(entry -> !"event".equals(entry.getKey()))
                        .findFirst()
                        .map(Map.Entry::getValue)
                        .orElse(null);

                    sendEvent(emitter, event, data);

                    return;
                }
            }

            sendEvent(emitter, "stream", payload);
        }

        @Override
        public void onComplete() {
            if (!completed.compareAndSet(false, true)) {
                return;
            }

            try {
                emitter.complete();
            } catch (Exception exception) {
                log.debug("SseEmitter.complete() failed (likely already completed): {}", exception.getMessage());
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (!completed.compareAndSet(false, true)) {
                return;
            }

            try {
                sendEvent(emitter, "error", Objects.toString(throwable.getMessage(), "An error occurred"));
            } finally {
                try {
                    emitter.complete();
                } catch (Exception exception) {
                    log.debug("SseEmitter.complete() failed after onError (likely already completed): {}",
                        exception.getMessage());
                }
            }
        }
    }
}
