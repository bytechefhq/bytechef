/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailTokenSession;
import com.bytechef.ee.platform.ai.guardrails.repository.AiGuardrailTokenSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiGuardrailTokenSessionRetentionJobTest {

    @Test
    void testPurgeExpiredTokenSessionsComputesCutoffFromTheConfiguredRetentionWindow() {
        AiGuardrailTokenSessionRepository repository = mock(AiGuardrailTokenSessionRepository.class);

        when(repository.findByLastModifiedDateBefore(any())).thenReturn(List.of());

        AiGuardrailTokenSessionRetentionJob job = new AiGuardrailTokenSessionRetentionJob(repository, 7);

        Instant before = Instant.now()
            .minus(Duration.ofDays(7));

        job.purgeExpiredTokenSessions();

        Instant after = Instant.now()
            .minus(Duration.ofDays(7));

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(repository).findByLastModifiedDateBefore(cutoffCaptor.capture());

        Instant cutoff = cutoffCaptor.getValue();

        assertThat(cutoff).isBetween(before, after);
    }

    @Test
    void testPurgeExpiredTokenSessionsDeletesEverySessionTheFinderReturns() {
        AiGuardrailTokenSessionRepository repository = mock(AiGuardrailTokenSessionRepository.class);

        AiGuardrailTokenSession firstExpired = new AiGuardrailTokenSession(1L, 10L, "conversation-1", "session-1",
            "enc:one");
        AiGuardrailTokenSession secondExpired = new AiGuardrailTokenSession(2L, 20L, "conversation-2", "session-2",
            "enc:two");
        List<AiGuardrailTokenSession> expired = List.of(firstExpired, secondExpired);

        when(repository.findByLastModifiedDateBefore(any())).thenReturn(expired);

        AiGuardrailTokenSessionRetentionJob job = new AiGuardrailTokenSessionRetentionJob(repository, 30);

        job.purgeExpiredTokenSessions();

        verify(repository).deleteAll(expired);
    }

    @Test
    void testPurgeExpiredTokenSessionsSkipsDeleteWhenNothingHasExpired() {
        AiGuardrailTokenSessionRepository repository = mock(AiGuardrailTokenSessionRepository.class);

        when(repository.findByLastModifiedDateBefore(any())).thenReturn(List.of());

        AiGuardrailTokenSessionRetentionJob job = new AiGuardrailTokenSessionRetentionJob(repository, 30);

        job.purgeExpiredTokenSessions();

        verify(repository, never()).deleteAll(any());
    }
}
