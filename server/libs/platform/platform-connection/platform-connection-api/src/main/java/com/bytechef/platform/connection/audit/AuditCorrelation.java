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

package com.bytechef.platform.connection.audit;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Thread-local carrier for a correlation ID that groups related connection audit events.
 *
 * <p>
 * The typical use case is a facade method (e.g. {@code setConnectionProjects}) that triggers several nested audited
 * mutations ({@code shareConnectionToProject}, {@code revokeConnectionFromProject}). The parent establishes a
 * correlation scope and every child audit event picks up the same ID via {@link ConnectionAuditAspect}, so an auditor
 * reviewing the stream can reassemble the parent/child relationship without relying on temporal proximity.
 *
 * <p>
 * Correlation IDs are modelled as a typed {@link CorrelationId} record so a call site cannot accidentally
 * {@code push(someOtherStringId)}. {@link #current()} returns the serialized string form (non-null only inside a scope)
 * for ergonomic use in audit payload maps, while internal storage preserves the typed value.
 *
 * <p>
 * The slot is push/pop-stack-safe via {@link #push(CorrelationId)} + {@link #pop(CorrelationId)}: callers capture the
 * previous value at push, restore it at pop. Nested establishments stack without clobbering.
 *
 * <p>
 * <b>Cross-thread propagation.</b> Storage is a plain {@link ThreadLocal} — NOT {@link InheritableThreadLocal}. Work
 * dispatched to another thread (e.g. {@code @Async}, {@code CompletableFuture.runAsync}, Reactor scheduler) does
 * <em>not</em> inherit the correlation ID automatically, because pooled worker threads are recycled across unrelated
 * requests and inheritable leakage across those boundaries is far worse than the temporary loss of correlation context.
 * Callers that genuinely need correlation on a worker thread must use {@link #capture()} on the submitting thread, then
 * {@link #wrap(Runnable)} / {@link #wrap(Callable)} the work they hand off — or, equivalently, bracket the worker code
 * with {@link #restore(Captured)}.
 *
 * @author Ivica Cardic
 */
public final class AuditCorrelation {

    private static final ThreadLocal<CorrelationId> CURRENT = new ThreadLocal<>();

    private AuditCorrelation() {
    }

    /**
     * Returns a fresh correlation ID suitable for {@link #push(CorrelationId)}.
     */
    public static CorrelationId newId() {
        return new CorrelationId(
            UUID.randomUUID()
                .toString());
    }

    /**
     * Returns the correlation ID active on the current thread in string form (the shape audit payload maps expect), or
     * {@code null} when no scope is active.
     */
    public static String current() {
        CorrelationId correlationId = CURRENT.get();

        return correlationId == null ? null : correlationId.value();
    }

    /**
     * Returns the currently-active correlation ID in typed form, or {@code null} when no scope is active. Prefer
     * {@link #current()} when the caller is about to write into an audit payload (which is a
     * {@code Map<String, Object>}); use this overload when the caller needs to re-push the current ID onto another
     * thread.
     */
    public static CorrelationId currentId() {
        return CURRENT.get();
    }

    /**
     * Sets the current correlation ID and returns the previous value so the caller can restore it via
     * {@link #pop(CorrelationId)}. Always pair with pop in a try/finally.
     */
    public static CorrelationId push(CorrelationId correlationId) {
        Objects.requireNonNull(correlationId, "correlationId");

        CorrelationId previous = CURRENT.get();

        CURRENT.set(correlationId);

        return previous;
    }

    /**
     * Restores the previous correlation ID captured from {@link #push(CorrelationId)}. Clears the slot entirely when
     * {@code previous} is null, to avoid retaining a null sentinel on the ThreadLocal.
     */
    public static void pop(CorrelationId previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    /**
     * Convenience for callers that need to run non-throwing work within a correlation scope.
     */
    public static void runWithCorrelationId(CorrelationId correlationId, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");

        try (Scope ignored = open(correlationId)) {
            runnable.run();
        }
    }

    /**
     * Opens a correlation scope bound to the current thread. The returned {@link Scope} is {@link AutoCloseable} so a
     * {@code try-with-resources} block guarantees the previous value is restored even on exception paths. Prefer this
     * over manual {@link #push(CorrelationId)} / {@link #pop(CorrelationId)} pairs — a missed {@code finally} leaves
     * the correlation ID leaked into the next request on a pooled worker thread.
     *
     * <p>
     * Nested scopes stack; closing an inner scope restores the outer scope's ID.
     */
    public static Scope open(CorrelationId correlationId) {
        return new Scope(push(correlationId));
    }

    /**
     * Opens a correlation scope with a freshly-generated ID.
     */
    public static Scope openNew() {
        return open(newId());
    }

    /**
     * Captures the correlation ID currently bound to <em>this</em> thread so it can be restored on a different thread
     * later. Call on the submitting thread; pair with {@link #restore(Captured)} (or use {@link #wrap(Runnable)} /
     * {@link #wrap(Callable)}) on the worker. Safe to call when no scope is active — the returned {@link Captured} then
     * carries {@code null} and restoring it clears the worker slot rather than leaking an unrelated ID from the pool.
     */
    public static Captured capture() {
        return new Captured(CURRENT.get());
    }

    /**
     * Restores a previously {@link #capture() captured} correlation ID on the current thread and returns an
     * {@link Scope} that reverses the effect on close. Always use inside a {@code try-with-resources} block so the
     * worker thread's slot cannot leak across recycled executions on a pooled thread.
     */
    public static Scope restore(Captured captured) {
        Objects.requireNonNull(captured, "captured");

        CorrelationId previous = CURRENT.get();

        if (captured.correlationId == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(captured.correlationId);
        }

        return new Scope(previous);
    }

    /**
     * Wraps a {@link Runnable} so it executes on its eventual worker thread under the correlation ID captured <em>at
     * wrap time</em>. Idiomatic use:
     *
     * <pre>{@code
     * executor.submit(AuditCorrelation.wrap(() -> auditedWork()));
     * }</pre>
     *
     * The worker restores the captured ID before running, then clears it in a finally block so the pooled thread does
     * not carry a stale ID into its next task.
     */
    public static Runnable wrap(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");

        Captured captured = capture();

        return () -> {
            try (Scope ignored = restore(captured)) {
                runnable.run();
            }
        };
    }

    /**
     * {@link Callable} variant of {@link #wrap(Runnable)} for use with {@link java.util.concurrent.ExecutorService} and
     * {@link java.util.concurrent.CompletableFuture#supplyAsync} pipelines that return a value.
     */
    public static <T> Callable<T> wrap(Callable<T> callable) {
        Objects.requireNonNull(callable, "callable");

        Captured captured = capture();

        return () -> {
            try (Scope ignored = restore(captured)) {
                return callable.call();
            }
        };
    }

    /**
     * Opaque handle returned by {@link #capture()} so callers cannot accidentally pass an arbitrary
     * {@link CorrelationId} (which has a different lifecycle — a captured ID may be absent, whereas a CorrelationId
     * must be non-blank) into the cross-thread restore path.
     */
    public static final class Captured {
        @org.jspecify.annotations.Nullable
        private final CorrelationId correlationId;

        private Captured(@org.jspecify.annotations.Nullable CorrelationId correlationId) {
            this.correlationId = correlationId;
        }
    }

    /**
     * Typed correlation identifier. Wraps a non-blank UUID-shaped string so that a call site cannot accidentally push
     * an unrelated string identifier (principal, connection id stringified, etc.) into the correlation slot. Equality
     * and hashing are value-based.
     */
    public record CorrelationId(String value) {

        public CorrelationId {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("correlationId value must be non-blank");
            }
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * {@link AutoCloseable} handle that restores the prior correlation ID on close. Returned by
     * {@link #open(CorrelationId)} and {@link #openNew()}; always use inside a {@code try-with-resources} block so
     * restoration cannot be skipped.
     */
    public static final class Scope implements AutoCloseable {

        private final CorrelationId previous;
        private boolean closed;

        private Scope(CorrelationId previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }

            closed = true;

            pop(previous);
        }
    }
}
