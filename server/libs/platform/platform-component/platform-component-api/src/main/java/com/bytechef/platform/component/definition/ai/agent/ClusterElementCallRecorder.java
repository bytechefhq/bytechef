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

package com.bytechef.platform.component.definition.ai.agent;

import com.bytechef.platform.component.definition.ai.agent.ClusterElementCall.Status;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallEvent.Type;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "MS_EXPOSE_REP", "SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR"
})
public final class ClusterElementCallRecorder {

    private static final Logger log = LoggerFactory.getLogger(ClusterElementCallRecorder.class);

    private static final Duration UNDRAINED_CALLS_EXPIRY = Duration.ofHours(1);

    private static final ClusterElementCallRecorder INSTANCE = new ClusterElementCallRecorder();

    private final Cache<String, TaskExecutionCalls> callsByTaskExecution;
    private final Map<String, List<Consumer<ClusterElementCallEvent>>> listenersByJob = new ConcurrentHashMap<>();

    public ClusterElementCallRecorder() {
        this(Ticker.systemTicker());
    }

    ClusterElementCallRecorder(Ticker ticker) {
        this.callsByTaskExecution = Caffeine.newBuilder()
            .expireAfterAccess(UNDRAINED_CALLS_EXPIRY)
            .ticker(ticker)
            .build();
    }

    public static ClusterElementCallRecorder getInstance() {
        return INSTANCE;
    }

    public static Map<String, ClusterElementCall> merge(
        Map<String, ClusterElementCall> existing, Map<String, ClusterElementCall> incoming) {

        Map<String, ClusterElementCall> merged = new LinkedHashMap<>(existing);

        for (ClusterElementCall call : incoming.values()) {
            merged.merge(call.workflowNodeName(), call, ClusterElementCallRecorder::mergeCall);
        }

        return merged;
    }

    public AutoCloseable addListener(long jobId, Consumer<ClusterElementCallEvent> listener) {
        String key = TenantCacheKeyUtils.getKey(jobId);

        List<Consumer<ClusterElementCallEvent>> jobListeners = listenersByJob.computeIfAbsent(
            key, curKey -> new CopyOnWriteArrayList<>());

        jobListeners.add(listener);

        return () -> listenersByJob.computeIfPresent(key, (curKey, listeners) -> {
            listeners.remove(listener);

            return listeners.isEmpty() ? null : listeners;
        });
    }

    public ClusterElementCallRef callStarted(
        long jobId, long taskExecutionId, String workflowNodeName, String componentName, @Nullable String toolName,
        @Nullable String parentId, @Nullable String input) {

        ClusterElementCallRef ref = new ClusterElementCallRef(String.valueOf(UUID.randomUUID()), workflowNodeName);

        record(jobId, taskExecutionId, Type.STARTED, ref, componentName, toolName, parentId, input, null, null, false);

        return ref;
    }

    public void callCompleted(long jobId, long taskExecutionId, ClusterElementCallRef ref, @Nullable String output) {
        record(jobId, taskExecutionId, Type.COMPLETED, ref, null, null, null, null, output, null, false);
    }

    public void callFailed(long jobId, long taskExecutionId, ClusterElementCallRef ref, String error) {
        record(jobId, taskExecutionId, Type.FAILED, ref, null, null, null, null, null, error, false);
    }

    public void callApproved(long jobId, long taskExecutionId, ClusterElementCallRef ref, @Nullable String details) {
        record(jobId, taskExecutionId, Type.COMPLETED, ref, null, null, null, null, details, null, true);
    }

    public void callDenied(long jobId, long taskExecutionId, ClusterElementCallRef ref, @Nullable String details) {
        record(jobId, taskExecutionId, Type.DENIED, ref, null, null, null, null, details, null, false);
    }

    public ClusterElementCallRef recordOutcome(
        long jobId, long taskExecutionId, String workflowNodeName, @Nullable String componentName,
        @Nullable String parentId, ClusterElementCallRecord.Status status, @Nullable String details,
        @Nullable String error) {

        if (status != ClusterElementCallRecord.Status.FAILED && status != ClusterElementCallRecord.Status.BLOCKED &&
            status != ClusterElementCallRecord.Status.REDACTED && status != ClusterElementCallRecord.Status.WITHHELD &&
            status != ClusterElementCallRecord.Status.DENIED) {

            throw new IllegalArgumentException("Not an outcome status: " + status);
        }

        ClusterElementCallRef ref = new ClusterElementCallRef(String.valueOf(UUID.randomUUID()), workflowNodeName);

        TaskExecutionCalls taskExecutionCalls = callsByTaskExecution.get(
            TenantCacheKeyUtils.getKey(taskExecutionId), key -> new TaskExecutionCalls());

        synchronized (taskExecutionCalls) {
            ClusterElementCall merged = taskExecutionCalls.recordOutcomeAggregate(
                workflowNodeName, Status.valueOf(status.name()), ClusterElementCallRecord.cap(error));
            ClusterElementCallRecord callRecord = taskExecutionCalls.recordOutcomeCall(
                ref, componentName, parentId, status, details, error);

            notifyListeners(new ClusterElementCallEvent(
                jobId, taskExecutionId, Type.valueOf(status.name()), merged, callRecord));
        }

        return ref;
    }

    public boolean claimFailure(long taskExecutionId, Throwable throwable) {
        TaskExecutionCalls taskExecutionCalls = callsByTaskExecution.get(
            TenantCacheKeyUtils.getKey(taskExecutionId), key -> new TaskExecutionCalls());

        synchronized (taskExecutionCalls) {
            Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());

            for (Throwable current = throwable; current != null && visited.add(current); current = current
                .getCause()) {

                if (taskExecutionCalls.claimedFailures.contains(current)) {
                    return false;
                }
            }

            visited.clear();

            for (Throwable current = throwable; current != null && visited.add(current); current = current
                .getCause()) {

                taskExecutionCalls.claimedFailures.add(current);
            }

            return true;
        }
    }

    public void callSuspended(long jobId, long taskExecutionId, ClusterElementCallRef ref) {
        TaskExecutionCalls taskExecutionCalls = callsByTaskExecution.get(
            TenantCacheKeyUtils.getKey(taskExecutionId), key -> new TaskExecutionCalls());

        synchronized (taskExecutionCalls) {
            taskExecutionCalls.suspend(ref);
        }
    }

    public Optional<ClusterElementCallRef> findSuspendedCall(long taskExecutionId) {
        TaskExecutionCalls taskExecutionCalls = callsByTaskExecution.getIfPresent(
            TenantCacheKeyUtils.getKey(taskExecutionId));

        if (taskExecutionCalls == null) {
            return Optional.empty();
        }

        synchronized (taskExecutionCalls) {
            return Optional.ofNullable(taskExecutionCalls.suspendedRef);
        }
    }

    public ClusterElementCallDrain drain(long taskExecutionId) {
        Map<String, TaskExecutionCalls> taskExecutionCallsMap = callsByTaskExecution.asMap();

        TaskExecutionCalls taskExecutionCalls =
            taskExecutionCallsMap.remove(TenantCacheKeyUtils.getKey(taskExecutionId));

        if (taskExecutionCalls == null) {
            return new ClusterElementCallDrain(Map.of(), List.of());
        }

        synchronized (taskExecutionCalls) {
            return new ClusterElementCallDrain(
                new HashMap<>(taskExecutionCalls.calls), new ArrayList<>(taskExecutionCalls.records.values()));
        }
    }

    private static ClusterElementCall mergeCall(ClusterElementCall existing, ClusterElementCall incoming) {
        boolean keepExisting = existing.status() == Status.FAILED ||
            ((existing.status() == Status.BLOCKED || existing.status() == Status.WITHHELD ||
                existing.status() == Status.DENIED) &&
                (incoming.status() == Status.COMPLETED || incoming.status() == Status.STARTED ||
                    incoming.status() == Status.REDACTED));

        Instant endDate = incoming.endDate() == null ? existing.endDate() : incoming.endDate();

        return new ClusterElementCall(
            existing.workflowNodeName(), keepExisting ? existing.status() : incoming.status(), existing.startDate(),
            endDate, keepExisting ? existing.error() : incoming.error());
    }

    private void record(
        long jobId, long taskExecutionId, Type type, ClusterElementCallRef ref, @Nullable String componentName,
        @Nullable String toolName, @Nullable String parentId, @Nullable String input, @Nullable String output,
        @Nullable String error, boolean approved) {

        TaskExecutionCalls taskExecutionCalls = callsByTaskExecution.get(
            TenantCacheKeyUtils.getKey(taskExecutionId), key -> new TaskExecutionCalls());

        boolean listened = listenersByJob.containsKey(TenantCacheKeyUtils.getKey(jobId));

        synchronized (taskExecutionCalls) {
            ClusterElementCall merged = taskExecutionCalls.record(type, ref.workflowNodeName(), error);
            ClusterElementCallRecord callRecord = taskExecutionCalls.recordCall(
                type, ref, componentName, toolName, parentId, listened ? input : null,
                listened || approved || type == Type.DENIED ? output : null, error, approved);

            Status status = merged.status();

            notifyListeners(
                new ClusterElementCallEvent(jobId, taskExecutionId, Type.valueOf(status.name()), merged, callRecord));
        }
    }

    private void notifyListeners(ClusterElementCallEvent event) {
        List<Consumer<ClusterElementCallEvent>> listeners = listenersByJob.get(
            TenantCacheKeyUtils.getKey(event.jobId()));

        if (listeners == null) {
            return;
        }

        for (Consumer<ClusterElementCallEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (RuntimeException exception) {
                if (log.isTraceEnabled()) {
                    log.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    private static final class TaskExecutionCalls {

        private final Map<String, ClusterElementCall> calls = new LinkedHashMap<>();
        private final Map<String, Integer> inFlightCounts = new HashMap<>();
        private final Map<String, ClusterElementCallRecord> records = new LinkedHashMap<>();
        private final Set<Throwable> claimedFailures = Collections.newSetFromMap(new IdentityHashMap<>());
        private @Nullable ClusterElementCallRef suspendedRef;

        private void suspend(ClusterElementCallRef ref) {
            ClusterElementCallRecord existing = records.get(ref.id());

            if (existing != null) {
                records.put(
                    ref.id(),
                    new ClusterElementCallRecord(
                        existing.id(), existing.parentId(), existing.workflowNodeName(), existing.componentName(),
                        existing.toolName(), ClusterElementCallRecord.Status.SUSPENDED, existing.startDate(),
                        existing.endDate(), existing.input(), existing.output(), existing.error()));
            } else {
                records.put(
                    ref.id(),
                    new ClusterElementCallRecord(
                        ref.id(), null, ref.workflowNodeName(), null, null, ClusterElementCallRecord.Status.SUSPENDED,
                        null, null, null, null, null));
            }

            suspendedRef = ref;
        }

        private ClusterElementCallRecord recordCall(
            Type type, ClusterElementCallRef ref, @Nullable String componentName, @Nullable String toolName,
            @Nullable String parentId, @Nullable String input, @Nullable String output, @Nullable String error,
            boolean approved) {

            Instant now = Instant.now();

            ClusterElementCallRecord existing = records.get(ref.id());

            ClusterElementCallRecord transition = switch (type) {
                case STARTED -> new ClusterElementCallRecord(
                    ref.id(), parentId, ref.workflowNodeName(), componentName, toolName,
                    ClusterElementCallRecord.Status.STARTED, now, null, ClusterElementCallRecord.cap(input), null,
                    null);
                case COMPLETED -> new ClusterElementCallRecord(
                    ref.id(), null, ref.workflowNodeName(), null, null,
                    approved ? ClusterElementCallRecord.Status.APPROVED : ClusterElementCallRecord.Status.COMPLETED,
                    null, now, null, ClusterElementCallRecord.cap(output), null);
                case FAILED -> new ClusterElementCallRecord(
                    ref.id(), null, ref.workflowNodeName(), null, null, ClusterElementCallRecord.Status.FAILED, null,
                    now, null, null, ClusterElementCallRecord.cap(error));
                case DENIED -> new ClusterElementCallRecord(
                    ref.id(), null, ref.workflowNodeName(), null, null, ClusterElementCallRecord.Status.DENIED, null,
                    now, null, ClusterElementCallRecord.cap(output), null);
                case BLOCKED, REDACTED, WITHHELD -> new ClusterElementCallRecord(
                    ref.id(), parentId, ref.workflowNodeName(), componentName, toolName,
                    ClusterElementCallRecord.Status.valueOf(type.name()), now, now, null,
                    ClusterElementCallRecord.cap(output), ClusterElementCallRecord.cap(error));
            };

            ClusterElementCallRecord stored = transition;

            if (existing != null) {
                List<ClusterElementCallRecord> mergedRecords = ClusterElementCallRecord.merge(
                    List.of(existing), List.of(transition));

                stored = mergedRecords.getFirst();
            }

            records.put(ref.id(), stored);

            if (type != Type.STARTED && suspendedRef != null && Objects.equals(suspendedRef.id(), ref.id())) {

                suspendedRef = null;
            }

            return stored;
        }

        private ClusterElementCall record(Type type, String workflowNodeName, @Nullable String error) {
            Instant now = Instant.now();

            int inFlightCount = inFlightCounts.getOrDefault(workflowNodeName, 0);

            if (type == Type.STARTED) {
                inFlightCount++;
            } else if (type == Type.COMPLETED || type == Type.FAILED || type == Type.DENIED) {
                inFlightCount = Math.max(inFlightCount - 1, 0);
            }

            inFlightCounts.put(workflowNodeName, inFlightCount);

            ClusterElementCall transition = switch (type) {
                case STARTED -> new ClusterElementCall(workflowNodeName, Status.STARTED, now, null, null);
                case COMPLETED -> new ClusterElementCall(
                    workflowNodeName, inFlightCount > 0 ? Status.STARTED : Status.COMPLETED, now, now, null);
                case FAILED -> new ClusterElementCall(workflowNodeName, Status.FAILED, now, now, error);
                case BLOCKED, REDACTED, WITHHELD, DENIED -> new ClusterElementCall(
                    workflowNodeName, Status.valueOf(type.name()), now, now, error);
            };

            return calls.merge(workflowNodeName, transition, ClusterElementCallRecorder::mergeCall);
        }

        private ClusterElementCall recordOutcomeAggregate(
            String workflowNodeName, Status status, @Nullable String error) {

            Instant now = Instant.now();

            return calls.merge(
                workflowNodeName, new ClusterElementCall(workflowNodeName, status, now, now, error),
                ClusterElementCallRecorder::mergeCall);
        }

        private ClusterElementCallRecord recordOutcomeCall(
            ClusterElementCallRef ref, @Nullable String componentName, @Nullable String parentId,
            ClusterElementCallRecord.Status status, @Nullable String details, @Nullable String error) {

            Instant now = Instant.now();

            ClusterElementCallRecord outcome = new ClusterElementCallRecord(
                ref.id(), parentId, ref.workflowNodeName(), componentName, null, status, now, now, null,
                ClusterElementCallRecord.cap(details), ClusterElementCallRecord.cap(error));

            records.put(ref.id(), outcome);

            return outcome;
        }
    }
}
