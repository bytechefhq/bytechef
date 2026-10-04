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
import com.bytechef.platform.webhook.executor.SseStreamBridgeRegistry;
import com.bytechef.platform.webhook.executor.SseStreamBridgeRegistry.Registration;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
     * {@code Accept} header: a request whose most preferred Accept type is {@code text/event-stream} (as sent by
     * {@code EventSource}) is routed to {@link #resumeStreaming(String, Map)} instead, which streams the resumed turn's
     * Server-Sent Events. A request that accepts any type stays here.
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
            case STREAMING_NOT_ALLOWED -> throw new IllegalStateException(
                "A non-streaming resume cannot be refused for streaming");
        };
    }

    @SuppressFBWarnings(
        value = "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
        justification = "CSRF disabled for external resume callbacks")
    @RequestMapping(
        method = {
            RequestMethod.GET, RequestMethod.POST
        },
        value = "/job/resume/{id}",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> resumeStreaming(
        @PathVariable String id, @RequestBody(required = false) Map<String, Object> data) {

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        SseEmitterStreamBridge bridge = new SseEmitterStreamBridge(emitter);

        AtomicReference<Registration> registrationReference = new AtomicReference<>();

        JobResumeOutcome outcome;

        try {
            outcome = jobResumeFacade.resumeJobStreaming(
                id, data == null ? Map.of() : data,
                jobId -> {
                    Registration registration = sseStreamBridgeRegistry.registerForResume(jobId, bridge);

                    registrationReference.set(registration);

                    registration.completion()
                        .whenComplete((unused, throwable) -> closeQuietly(registration.handle()));
                });
        } catch (RuntimeException exception) {
            closeRegistrationIfPresent(registrationReference);

            throw exception;
        }

        return switch (outcome) {
            case OK -> ResponseEntity.ok(emitter);
            case INVALID_ID -> ResponseEntity.badRequest()
                .build();
            case GONE -> ResponseEntity.status(HttpStatus.GONE)
                .build();
            case STREAMING_NOT_ALLOWED -> ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .build();
        };
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
}
