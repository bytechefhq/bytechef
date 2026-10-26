/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.violation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailViolation;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailViolationAction;
import com.bytechef.ee.platform.ai.guardrails.repository.AiGuardrailViolationRepository;
import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiGuardrailViolationRecorderTest {

    private static final String MATCHED_VALUE = "bob@acme.io";

    private static final String PRINCIPAL = "admin@localhost.com";

    private final AiGuardrailViolationRepository repository = mock(AiGuardrailViolationRepository.class);
    private final List<String> events = new ArrayList<>();

    @Test
    void testNoMatchedValueReachesAWrittenRecord() {
        AiGuardrailViolationRecorder recorder = recorder(1000);

        when(repository.countForWorkspaceSince(any(), any())).thenReturn(0L);

        recorder.submit(
            List.of(new SensitiveSpan(SensitiveKind.PII, "EMAIL_ADDRESS", 5, 16, 0.9)), true,
            AiGuardrailViolationAction.REDACTED, "ai_agent", 42L, 1, PRINCIPAL, events::add);

        ArgumentCaptor<AiGuardrailViolation> captor = ArgumentCaptor.forClass(AiGuardrailViolation.class);

        verify(repository).save(captor.capture());

        AiGuardrailViolation violation = captor.getValue();

        String serialized = violation.getCategory() + "|" + violation.getKind() + "|" + violation.getSpanStart() +
            "|" + violation.getSpanLength() + "|" + violation.getConfidence() + "|" + violation.getAction() + "|" +
            violation.getSurface() + "|" + violation.getWorkspaceId() + "|" + violation.getEnvironment() + "|" +
            violation.getPrincipal() + "|" + violation;

        assertThat(serialized).doesNotContain(MATCHED_VALUE);

        assertThat(serialized).contains(PRINCIPAL);
    }

    @Test
    void testTheRecordLocatesTheMatchItRefusesToReproduce() {
        AiGuardrailViolationRecorder recorder = recorder(1000);

        when(repository.countForWorkspaceSince(any(), any())).thenReturn(0L);

        recorder.submit(
            List.of(new SensitiveSpan(SensitiveKind.PII, "EMAIL_ADDRESS", 5, 16, 0.9)), true,
            AiGuardrailViolationAction.REDACTED, "ai_agent", 42L, 1, null, events::add);

        ArgumentCaptor<AiGuardrailViolation> captor = ArgumentCaptor.forClass(AiGuardrailViolation.class);

        verify(repository).save(captor.capture());

        AiGuardrailViolation violation = captor.getValue();

        assertThat(violation.getCategory()).isEqualTo("EMAIL_ADDRESS");
        assertThat(violation.getSpanStart()).isEqualTo(5);
        assertThat(violation.getSpanLength()).isEqualTo(11);
        assertThat(events).containsExactly("violation_record_written");
    }

    @Test
    void testRecordingIsOffUnlessTheWorkspaceEnabledIt() {
        AiGuardrailViolationRecorder recorder = recorder(1000);

        recorder.submit(
            List.of(new SensitiveSpan(SensitiveKind.PII, "EMAIL_ADDRESS", 5, 16, 0.9)), false,
            AiGuardrailViolationAction.REDACTED, "ai_agent", 42L, 1, null, events::add);

        verify(repository, never()).save(any());
        verify(repository, never()).countForWorkspaceSince(any(), any());
        assertThat(events).isEmpty();
    }

    @Test
    void testAFailingWriteNeitherThrowsNorGoesUnreported() {
        AiGuardrailViolationRecorder recorder = recorder(1000);

        when(repository.countForWorkspaceSince(any(), any())).thenThrow(new IllegalStateException("db down"));

        assertThatCode(
            () -> recorder.submit(
                List.of(new SensitiveSpan(SensitiveKind.PII, "EMAIL_ADDRESS", 5, 16, 0.9)), true,
                AiGuardrailViolationAction.REDACTED, "ai_agent", 42L, 1, null, events::add))
                    .doesNotThrowAnyException();

        assertThat(events).containsExactly("violation_record_dropped");
    }

    @Test
    void testTheDailyCapStopsWritesAndFiresItsEventExactlyOnce() {
        AiGuardrailViolationRecorder recorder = recorder(10);

        when(repository.countForWorkspaceSince(any(), any())).thenReturn(10L);

        for (int index = 0; index < 5; index++) {
            recorder.submit(
                List.of(new SensitiveSpan(SensitiveKind.PII, "EMAIL_ADDRESS", 5, 16, 0.9)), true,
                AiGuardrailViolationAction.REDACTED, "ai_agent", 42L, 1, null, events::add);
        }

        verify(repository, never()).save(any());
        assertThat(events).containsExactly("violation_records_capped");
    }

    @Test
    void testAnAllowedRecordIsDistinguishableFromAnEnforcedOne() {
        AiGuardrailViolationRecorder recorder = recorder(1000);

        when(repository.countForWorkspaceSince(any(), any())).thenReturn(0L);

        recorder.submit(
            List.of(new SensitiveSpan(SensitiveKind.PII, "EMAIL_ADDRESS", 5, 16, 0.9)), true,
            AiGuardrailViolationAction.ALLOWED, "ai_agent", 42L, 1, null, events::add);

        ArgumentCaptor<AiGuardrailViolation> captor = ArgumentCaptor.forClass(AiGuardrailViolation.class);

        verify(repository).save(captor.capture());

        assertThat(captor.getValue()
            .getAction()).isEqualTo(AiGuardrailViolationAction.ALLOWED);
    }

    @Test
    void testNothingIsSubmittedWhenNoSpansWereDetected() {
        AiGuardrailViolationRecorder recorder = recorder(1000);

        recorder.submit(
            List.of(), true, AiGuardrailViolationAction.REDACTED, "ai_agent", 42L, 1, null, events::add);

        verify(repository, never()).countForWorkspaceSince(any(), any());
        assertThat(events).isEmpty();
    }

    @Test
    void testTheNullWorkspaceBucketIsCappedToo() {
        AiGuardrailViolationRecorder recorder = recorder(10);

        when(repository.countForWorkspaceSince(any(), any())).thenReturn(10L);

        recorder.submit(
            List.of(new SensitiveSpan(SensitiveKind.PII, "EMAIL_ADDRESS", 5, 16, 0.9)), true,
            AiGuardrailViolationAction.REDACTED, "ai_agent", null, null, null, events::add);

        verify(repository, never()).save(any());
        assertThat(events).containsExactly("violation_records_capped");
    }

    private AiGuardrailViolationRecorder recorder(int dailyCap) {
        return new AiGuardrailViolationRecorder(repository, sameThreadExecutor(), dailyCap);
    }

    private static ExecutorService sameThreadExecutor() {
        return new AbstractExecutorService() {

            @Override
            public void execute(Runnable command) {
                command.run();
            }

            @Override
            public void shutdown() {
            }

            @Override
            public List<Runnable> shutdownNow() {
                return List.of();
            }

            @Override
            public boolean isShutdown() {
                return false;
            }

            @Override
            public boolean isTerminated() {
                return false;
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) {
                return true;
            }
        };
    }
}
