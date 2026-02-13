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

import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class SseStreamBridgeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SseStreamBridgeRegistry.class);

    private final Cache<Long, CopyOnWriteArrayList<SseStreamBridge>> bridges = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build();

    private final Cache<Long, CompletableFuture<Void>> completionFutures = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build();

    public void onSseStreamEvent(SseStreamEvent sseStreamEvent) {
        long jobId = sseStreamEvent.getJobId();

        CopyOnWriteArrayList<SseStreamBridge> sseStreamBridges = bridges.getIfPresent(jobId);

        if (sseStreamBridges == null) {
            return;
        }

        String eventType = sseStreamEvent.getEventType();

        switch (eventType) {
            case SseStreamEvent.EVENT_TYPE_DATA -> {
                for (SseStreamBridge sseStreamBridge : sseStreamBridges) {
                    try {
                        sseStreamBridge.onEvent(sseStreamEvent.getPayload());
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }

            case SseStreamEvent.EVENT_TYPE_COMPLETE -> {
                for (SseStreamBridge sseStreamBridge : sseStreamBridges) {
                    try {
                        sseStreamBridge.onComplete();
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }

            case SseStreamEvent.EVENT_TYPE_ERROR -> {
                String errorMessage = sseStreamEvent.getPayload() != null
                    ? sseStreamEvent.getPayload()
                        .toString()
                    : "Unknown error";

                for (SseStreamBridge sseStreamBridge : sseStreamBridges) {
                    try {
                        sseStreamBridge.onError(new RuntimeException(errorMessage));
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }

            case SseStreamEvent.EVENT_TYPE_JOB_STATUS -> handleJobStatus(jobId, sseStreamEvent);

            case SseStreamEvent.EVENT_TYPE_TASK_STARTED -> {
                for (SseStreamBridge sseStreamBridge : sseStreamBridges) {
                    try {
                        sseStreamBridge.onEvent(sseStreamEvent.getPayload());
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }
                }
            }

            default -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("Unknown SSE stream event type: {}", eventType);
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

    private void handleJobStatus(long jobId, SseStreamEvent sseStreamEvent) {
        Object payload = sseStreamEvent.getPayload();
        String status = payload != null ? payload.toString() : "";

        if ("COMPLETED".equals(status) || "FAILED".equals(status) || "STOPPED".equals(status)) {
            CopyOnWriteArrayList<SseStreamBridge> sseStreamBridges = bridges.getIfPresent(jobId);

            if (sseStreamBridges != null) {
                for (SseStreamBridge sseStreamBridge : sseStreamBridges) {
                    try {
                        if ("FAILED".equals(status)) {
                            sseStreamBridge.onError(new RuntimeException("Job failed"));
                        } else {
                            sseStreamBridge.onComplete();
                        }
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
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
    }

    @SuppressFBWarnings("EI")
    public record Registration(AutoCloseable handle, CompletableFuture<Void> completion) {
    }
}
