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

package com.bytechef.platform.webhook.executor;

import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.github.benmanes.caffeine.cache.Ticker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes a job's SSE stream events to the SSE connections registered for the job. A registration made with
 * {@link #register(long, SseStreamBridge)} receives events right away and ends when the job completes, fails or stops
 * ({@code STOPPED}). A {@code STOPPED} status reaches the bridge as {@link SseStreamBridge#onSuspend()} unless the
 * event marks it as not suspended, in which case it reaches the bridge as {@link SseStreamBridge#onComplete()}.
 *
 * <p>
 * A registration for a resumed job, made with {@link #registerForResume(long, SseStreamBridge)}, stays pending so it
 * does not receive events left over from the run before the suspend. It is activated right away when this registry has
 * already seen the job suspend (remembered for 30 minutes); otherwise when the resumed task starts, when the job
 * suspends, stops, completes or fails, or two minutes after the job's first pending registration, by which time any
 * leftover events have been delivered.
 *
 * @author Ivica Cardic
 */
public class SseStreamBridgeRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseStreamBridgeRegistry.class);

    private static final Duration PENDING_REGISTRATION_TIMEOUT = Duration.ofMinutes(2);

    private final Cache<Long, CopyOnWriteArrayList<SseStreamBridge>> bridges = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build();

    private final Cache<Long, CompletableFuture<Void>> completionFutures = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build();

    private final Cache<Long, List<PendingRegistration>> pendingRegistrations;

    private final Cache<Long, Boolean> stoppedJobIds = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .build();

    public SseStreamBridgeRegistry() {
        this(PENDING_REGISTRATION_TIMEOUT, Ticker.systemTicker(), ForkJoinPool.commonPool());
    }

    SseStreamBridgeRegistry(Duration pendingRegistrationTimeout, Ticker ticker, Executor executor) {
        pendingRegistrations = Caffeine.newBuilder()
            .expireAfterWrite(pendingRegistrationTimeout)
            .executor(executor)
            .scheduler(Scheduler.systemScheduler())
            .ticker(ticker)
            .<Long, List<PendingRegistration>>removalListener((jobId, jobPendingRegistrations, removalCause) -> {
                if (removalCause.wasEvicted() && jobPendingRegistrations != null) {
                    for (PendingRegistration pendingRegistration : jobPendingRegistrations) {
                        pendingRegistration.activate();
                    }
                }
            })
            .build();
    }

    public void onSseStreamEvent(SseStreamEvent sseStreamEvent) {
        long jobId = sseStreamEvent.getJobId();

        String eventType = sseStreamEvent.getEventType();

        if (SseStreamEvent.EVENT_TYPE_TASK_STARTED.equals(eventType)) {
            activatePendingRegistrations(jobId);
        }

        if (SseStreamEvent.EVENT_TYPE_JOB_STATUS.equals(eventType)) {
            handleJobStatus(jobId, sseStreamEvent);

            return;
        }

        CopyOnWriteArrayList<SseStreamBridge> sseStreamBridges = bridges.getIfPresent(jobId);

        if (sseStreamBridges == null || sseStreamBridges.isEmpty()) {
            if (isAskUserQuestionEvent(sseStreamEvent.getPayload())) {
                log.warn(
                    "The '{}' event of job {} was dropped because no SSE connection is registered for the job",
                    AiAgentSseEventType.ASK_USER_QUESTION, jobId);
            }

            return;
        }

        switch (eventType) {
            case SseStreamEvent.EVENT_TYPE_DATA -> {
                for (SseStreamBridge sseStreamBridge : sseStreamBridges) {
                    try {
                        sseStreamBridge.onEvent(sseStreamEvent.getPayload());
                    } catch (Exception exception) {
                        if (log.isTraceEnabled()) {
                            log.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }

            case SseStreamEvent.EVENT_TYPE_COMPLETE -> {
                for (SseStreamBridge sseStreamBridge : sseStreamBridges) {
                    try {
                        sseStreamBridge.onComplete();
                    } catch (Exception exception) {
                        if (log.isTraceEnabled()) {
                            log.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }

            case SseStreamEvent.EVENT_TYPE_ERROR -> {
                Object payload = sseStreamEvent.getPayload();

                String errorMessage = payload != null ? payload.toString() : "Unknown error";

                for (SseStreamBridge sseStreamBridge : sseStreamBridges) {
                    try {
                        sseStreamBridge.onError(new RuntimeException(errorMessage));
                    } catch (Exception exception) {
                        if (log.isTraceEnabled()) {
                            log.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }

            case SseStreamEvent.EVENT_TYPE_TASK_STARTED -> {
                for (SseStreamBridge sseStreamBridge : sseStreamBridges) {
                    try {
                        sseStreamBridge.onEvent(sseStreamEvent.getPayload());
                    } catch (Exception exception) {
                        if (log.isTraceEnabled()) {
                            log.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }

            default -> {
                if (log.isDebugEnabled()) {
                    log.debug("Unknown SSE stream event type: {}", eventType);
                }
            }
        }
    }

    public Registration register(long jobId, SseStreamBridge sseStreamBridge) {
        CopyOnWriteArrayList<SseStreamBridge> sseStreamBridges = bridges.get(
            jobId, key -> new CopyOnWriteArrayList<>());

        sseStreamBridges.add(sseStreamBridge);

        CompletableFuture<Void> completionFuture = completionFutures.get(jobId, key -> new CompletableFuture<>());

        AutoCloseable handle = () -> {
            CopyOnWriteArrayList<SseStreamBridge> currentBridges = bridges.getIfPresent(jobId);

            if (currentBridges != null) {
                currentBridges.remove(sseStreamBridge);
            }
        };

        return new Registration(handle, completionFuture);
    }

    public synchronized Registration registerForResume(long jobId, SseStreamBridge sseStreamBridge) {
        PendingRegistration pendingRegistration = new PendingRegistration(jobId, sseStreamBridge);

        if (stoppedJobIds.getIfPresent(jobId) != null) {
            stoppedJobIds.invalidate(jobId);

            pendingRegistration.activate();
        } else {
            List<PendingRegistration> jobPendingRegistrations = pendingRegistrations.get(
                jobId, key -> new CopyOnWriteArrayList<>());

            jobPendingRegistrations.add(pendingRegistration);
        }

        return new Registration(pendingRegistration::close, pendingRegistration.completion);
    }

    void cleanUp() {
        pendingRegistrations.cleanUp();
    }

    private synchronized void activatePendingRegistrations(long jobId) {
        stoppedJobIds.invalidate(jobId);

        List<PendingRegistration> jobPendingRegistrations = pendingRegistrations.getIfPresent(jobId);

        if (jobPendingRegistrations == null) {
            return;
        }

        pendingRegistrations.invalidate(jobId);

        for (PendingRegistration pendingRegistration : jobPendingRegistrations) {
            pendingRegistration.activate();
        }
    }

    private void handleJobStatus(long jobId, SseStreamEvent sseStreamEvent) {
        Object payload = sseStreamEvent.getPayload();
        String status = payload != null ? payload.toString() : "";

        boolean suspended = "STOPPED".equals(status) &&
            !Boolean.FALSE.equals(sseStreamEvent.getMetadata(SseStreamEvent.METADATA_SUSPENDED));

        if ("COMPLETED".equals(status) || "FAILED".equals(status) || ("STOPPED".equals(status) && !suspended)) {
            activatePendingRegistrations(jobId);
        }

        if ("COMPLETED".equals(status) || "FAILED".equals(status) || "STOPPED".equals(status)) {
            CopyOnWriteArrayList<SseStreamBridge> sseStreamBridges = bridges.getIfPresent(jobId);

            if (sseStreamBridges != null) {
                for (SseStreamBridge sseStreamBridge : sseStreamBridges) {
                    try {
                        if ("FAILED".equals(status)) {
                            sseStreamBridge.onError(new RuntimeException(getJobFailedMessage(sseStreamEvent)));
                        } else if (suspended) {
                            sseStreamBridge.onSuspend();
                        } else {
                            sseStreamBridge.onComplete();
                        }
                    } catch (Exception exception) {
                        if (log.isTraceEnabled()) {
                            log.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }

            CompletableFuture<Void> completionFuture = completionFutures.getIfPresent(jobId);

            if (completionFuture != null) {
                completionFuture.complete(null);
            }

            bridges.invalidate(jobId);
            completionFutures.invalidate(jobId);
        }

        if (suspended) {
            onJobStopped(jobId);
        }
    }

    private static String getJobFailedMessage(SseStreamEvent sseStreamEvent) {
        if (sseStreamEvent.getMetadata(SseStreamEvent.METADATA_ERROR_MESSAGE) instanceof String errorMessage &&
            !errorMessage.isBlank()) {

            return errorMessage;
        }

        return "Job failed";
    }

    private static boolean isAskUserQuestionEvent(@Nullable Object payload) {
        return payload instanceof Map<?, ?> map &&
            AiAgentSseEventType.ASK_USER_QUESTION.equals(map.get(AiAgentSseEventType.EVENT_TYPE));
    }

    private synchronized void onJobStopped(long jobId) {
        if (pendingRegistrations.getIfPresent(jobId) == null) {
            stoppedJobIds.put(jobId, Boolean.TRUE);
        } else {
            activatePendingRegistrations(jobId);
        }
    }

    private final class PendingRegistration {

        private final CompletableFuture<Void> completion = new CompletableFuture<>();
        private final long jobId;
        private final SseStreamBridge sseStreamBridge;
        private @Nullable AutoCloseable activeHandle;
        private boolean closed;

        private PendingRegistration(long jobId, SseStreamBridge sseStreamBridge) {
            this.jobId = jobId;
            this.sseStreamBridge = sseStreamBridge;
        }

        private void activate() {
            synchronized (this) {
                if (closed || activeHandle != null) {
                    return;
                }

                Registration registration = register(jobId, sseStreamBridge);

                activeHandle = registration.handle();

                registration.completion()
                    .whenComplete((unused, throwable) -> completion.complete(null));
            }
        }

        private void close() throws Exception {
            AutoCloseable handle;

            synchronized (this) {
                closed = true;
                handle = activeHandle;
            }

            if (handle == null) {
                List<PendingRegistration> jobPendingRegistrations = pendingRegistrations.getIfPresent(jobId);

                if (jobPendingRegistrations != null) {
                    jobPendingRegistrations.remove(this);
                }
            } else {
                handle.close();
            }
        }
    }

    @SuppressFBWarnings("EI")
    public record Registration(AutoCloseable handle, CompletableFuture<Void> completion) {
    }
}
