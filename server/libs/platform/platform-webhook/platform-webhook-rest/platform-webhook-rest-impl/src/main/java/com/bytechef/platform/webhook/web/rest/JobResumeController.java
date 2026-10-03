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
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
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
 * Public controller for anonymous suspended-job resume webhook callbacks. Authentication is provided by the unguessable
 * resume id embedded in the path, which must match the id stored on the suspended job, so the endpoint lives outside
 * the {@code /api/**} namespace — alongside other anonymous webhook-style callbacks — and is permitted in the Order-4
 * filter chain where CSRF is disabled by convention.
 *
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class JobResumeController {

    private static final Logger log = LoggerFactory.getLogger(JobResumeController.class);

    private static final long SSE_TIMEOUT = TimeUnit.MINUTES.toMillis(30);

    private static final Duration NOT_YET_SUSPENDED_MAX_POLL_INTERVAL = Duration.ofSeconds(2);
    private static final Duration NOT_YET_SUSPENDED_POLL_INTERVAL = Duration.ofMillis(200);
    private static final Duration NOT_YET_SUSPENDED_WAIT = Duration.ofSeconds(10);

    private final JobResumeFacade jobResumeFacade;
    private final Duration notYetSuspendedPollInterval;
    private final Duration notYetSuspendedWait;
    private final SseStreamBridgeRegistry sseStreamBridgeRegistry;

    @Autowired
    @SuppressFBWarnings("EI")
    public JobResumeController(JobResumeFacade jobResumeFacade, SseStreamBridgeRegistry sseStreamBridgeRegistry) {
        this(jobResumeFacade, sseStreamBridgeRegistry, NOT_YET_SUSPENDED_WAIT, NOT_YET_SUSPENDED_POLL_INTERVAL);
    }

    @SuppressFBWarnings("EI")
    JobResumeController(
        JobResumeFacade jobResumeFacade, SseStreamBridgeRegistry sseStreamBridgeRegistry, Duration notYetSuspendedWait,
        Duration notYetSuspendedPollInterval) {

        this.jobResumeFacade = jobResumeFacade;
        this.notYetSuspendedPollInterval = notYetSuspendedPollInterval;
        this.notYetSuspendedWait = notYetSuspendedWait;
        this.sseStreamBridgeRegistry = sseStreamBridgeRegistry;
    }

    /**
     * Resumes a suspended job via anonymous webhook callback, submitted by the approval form in the SPA, by a chat
     * client posting the user's answer without streaming, or by an email link.
     *
     * <p>
     * <b>Security Note:</b> CSRF protection is not required for this endpoint. Resume callbacks may come from external
     * sources (e.g., email links) that cannot include CSRF tokens. Security is maintained through unguessable resume
     * ids that are compared in constant time with the id stored on the suspended job before processing.
     *
     * <p>
     * The {@code produces} attribute is intentionally unset so Spring content-negotiates by the request's
     * {@code Accept} header: a request whose most preferred Accept type is {@code text/event-stream} (as the chat
     * client sends when it posts the user's answer) is routed to {@link #resumeStreaming(String, Map)} instead, which
     * streams the resumed turn's Server-Sent Events. A request that accepts any type stays here.
     *
     * <p>
     * An answer can arrive before the job has finished suspending (the chat client receives the question while the
     * agent's task is still completing), so while the job is not yet suspended the resume is retried, with a growing
     * interval, for up to 10 seconds before answering 409.
     */
    @SuppressFBWarnings(
        value = "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
        justification = "CSRF disabled for external resume callbacks")
    @RequestMapping(method = {
        RequestMethod.GET, RequestMethod.POST
    }, value = "/job/resume/{id}")
    public ResponseEntity<Void> resume(
        @PathVariable String id, @RequestBody(required = false) Map<String, Object> data) {

        JobResumeOutcome outcome;

        try {
            outcome = retryWhileNotYetSuspended(
                () -> jobResumeFacade.resumeJob(id, data), JobResumeOutcome.NOT_YET_SUSPENDED::equals);
        } catch (RuntimeException exception) {
            if (!isOptimisticLockingFailure(exception)) {
                throw exception;
            }

            log.debug("A concurrent request resumed the job first", exception);

            outcome = JobResumeOutcome.GONE;
        }

        if (outcome == JobResumeOutcome.OK) {
            return ResponseEntity.noContent()
                .build();
        }

        return ResponseEntity.status(getErrorStatus(outcome))
            .build();
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
            outcome = retryWhileNotYetSuspended(
                () -> jobResumeFacade.resumeJobStreaming(
                    id, data == null ? Map.of() : data,
                    jobId -> {
                        Registration registration = sseStreamBridgeRegistry.registerForResume(jobId, bridge);

                        registrationReference.set(registration);

                        registration.completion()
                            .whenComplete((unused, throwable) -> registration.close());
                    }),
                JobResumeOutcome.NOT_YET_SUSPENDED::equals);
        } catch (RuntimeException exception) {
            closeRegistrationIfPresent(registrationReference);

            if (!isOptimisticLockingFailure(exception)) {
                throw exception;
            }

            log.debug("A concurrent request resumed the job first", exception);

            outcome = JobResumeOutcome.GONE;
        }

        emitter.onCompletion(() -> closeRegistrationIfPresent(registrationReference));
        emitter.onTimeout(() -> closeRegistrationIfPresent(registrationReference));
        emitter.onError(throwable -> closeRegistrationIfPresent(registrationReference));

        if (outcome == JobResumeOutcome.OK) {
            return ResponseEntity.ok(emitter);
        }

        return ResponseEntity.status(getErrorStatus(outcome))
            .build();
    }

    private static HttpStatus getErrorStatus(JobResumeOutcome outcome) {
        return switch (outcome) {
            case INVALID_ID -> HttpStatus.BAD_REQUEST;
            case GONE -> HttpStatus.GONE;
            case JOB_FAILED -> HttpStatus.UNPROCESSABLE_CONTENT;
            case NOT_YET_SUSPENDED -> HttpStatus.CONFLICT;
            case STREAMING_NOT_ALLOWED -> HttpStatus.NOT_ACCEPTABLE;
            case OK -> throw new IllegalArgumentException("A successful resume has no error status");
        };
    }

    private static boolean isOptimisticLockingFailure(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof OptimisticLockingFailureException) {
                return true;
            }
        }

        return false;
    }

    private <T> T retryWhileNotYetSuspended(Supplier<T> resumeSupplier, Predicate<T> notYetSuspended) {
        Instant deadline = Instant.now()
            .plus(notYetSuspendedWait);

        Duration pollInterval = notYetSuspendedPollInterval;
        T outcome = resumeSupplier.get();

        while (notYetSuspended.test(outcome) && Instant.now()
            .isBefore(deadline)) {

            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException interruptedException) {
                Thread currentThread = Thread.currentThread();

                currentThread.interrupt();

                return outcome;
            }

            outcome = resumeSupplier.get();

            pollInterval = min(pollInterval.multipliedBy(2), NOT_YET_SUSPENDED_MAX_POLL_INTERVAL);
        }

        return outcome;
    }

    private static Duration min(Duration duration, Duration otherDuration) {
        return duration.compareTo(otherDuration) <= 0 ? duration : otherDuration;
    }

    private static void closeRegistrationIfPresent(AtomicReference<Registration> registrationReference) {
        Registration registration = registrationReference.get();

        if (registration != null) {
            registration.close();
        }
    }
}
