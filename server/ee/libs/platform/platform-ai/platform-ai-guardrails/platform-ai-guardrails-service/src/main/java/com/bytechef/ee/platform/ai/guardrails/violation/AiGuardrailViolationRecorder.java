/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.violation;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailViolation;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailViolationAction;
import com.bytechef.ee.platform.ai.guardrails.repository.AiGuardrailViolationRepository;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AiGuardrailViolationRecorder {

    private static final Logger log = LoggerFactory.getLogger(AiGuardrailViolationRecorder.class);

    private final AiGuardrailViolationRepository aiGuardrailViolationRepository;
    private final ExecutorService executorService;
    private final int dailyCap;

    private final Map<CapKey, AtomicBoolean> capReported = new ConcurrentHashMap<>();

    @Autowired
    @SuppressFBWarnings("EI")
    public AiGuardrailViolationRecorder(
        AiGuardrailViolationRepository aiGuardrailViolationRepository,
        @Value("${bytechef.ai.guardrails.violation.daily-cap:100000}") int dailyCap,
        @Value("${bytechef.ai.guardrails.violation.queue-capacity:1000}") int queueCapacity) {

        this(
            aiGuardrailViolationRepository,
            new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueCapacity),
                runnable -> {
                    Thread thread = new Thread(runnable, "guardrail-violation-recorder");

                    thread.setDaemon(true);

                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy()),
            dailyCap);
    }

    AiGuardrailViolationRecorder(
        AiGuardrailViolationRepository aiGuardrailViolationRepository, ExecutorService executorService, int dailyCap) {

        this.aiGuardrailViolationRepository = aiGuardrailViolationRepository;
        this.executorService = executorService;
        this.dailyCap = dailyCap;
    }

    public void submit(
        List<SensitiveSpan> spans, boolean enabled, AiGuardrailViolationAction action, String surface,
        @Nullable Long workspaceId, @Nullable Integer environment, @Nullable String principal,
        Consumer<String> recordEvent) {

        if (!enabled || spans.isEmpty()) {
            return;
        }

        try {
            executorService.execute(() -> write(spans, action, surface, workspaceId, environment, principal,
                recordEvent));
        } catch (RejectedExecutionException rejectedExecutionException) {
            recordEvent.accept("violation_record_dropped");

            log.debug("Guardrail violation record dropped: writer queue full");
        }
    }

    private void write(
        List<SensitiveSpan> spans, AiGuardrailViolationAction action, String surface, @Nullable Long workspaceId,
        @Nullable Integer environment, @Nullable String principal, Consumer<String> recordEvent) {

        try {
            Instant since = Instant.now()
                .minus(Duration.ofDays(1));

            long written = aiGuardrailViolationRepository.countForWorkspaceSince(workspaceId, since);

            if (written + spans.size() > dailyCap) {
                reportCapOnce(workspaceId, recordEvent);

                return;
            }

            for (SensitiveSpan span : spans) {
                aiGuardrailViolationRepository.save(
                    new AiGuardrailViolation(
                        span.category(), span.kind()
                            .ordinal(),
                        span.start(), span.length(),
                        BigDecimal.valueOf(span.confidence())
                            .setScale(2, RoundingMode.HALF_UP),
                        action, surface, workspaceId, environment, principal));
            }

            recordEvent.accept("violation_record_written");
        } catch (RuntimeException runtimeException) {
            recordEvent.accept("violation_record_dropped");

            log.warn("Guardrail violation record write failed; continuing without it", runtimeException);
        }
    }

    private void reportCapOnce(@Nullable Long workspaceId, Consumer<String> recordEvent) {
        CapKey capKey = new CapKey(workspaceId, Instant.now()
            .truncatedTo(ChronoUnit.DAYS));

        AtomicBoolean reported = capReported.computeIfAbsent(capKey, key -> new AtomicBoolean());

        if (reported.compareAndSet(false, true)) {
            recordEvent.accept("violation_records_capped");

            log.info(
                "Guardrail violation recording capped for workspace {} at {} records/day; further records today are " +
                    "not persisted",
                workspaceId, dailyCap);

            capReported.keySet()
                .removeIf(key -> !key.day()
                    .equals(capKey.day()));
        }
    }

    private record CapKey(@Nullable Long workspaceId, Instant day) {
    }
}
