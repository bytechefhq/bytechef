/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.approval.repository.AiHubToolApprovalRepository;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubToolApprovalServiceTest {

    private static final long CHAT_ID = 10L;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-02T00:00:00Z"), ZoneOffset.UTC);

    private AiHubToolApprovalRepository repository;
    private AiHubToolApprovalServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(AiHubToolApprovalRepository.class);

        when(repository.save(any(AiHubToolApproval.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service = new AiHubToolApprovalServiceImpl(repository, clock);
    }

    private static AiHubToolApproval pendingApproval() {
        AiHubToolApproval approval = new AiHubToolApproval();

        approval.setId(42L);
        approval.setChatId(CHAT_ID);
        approval.setThreadId("thread-1");
        approval.setRequestedByUserId(3L);
        approval.setToolKind(AiHubToolApproval.ToolKind.CATALOG);
        approval.setToolName("deleteProject");
        approval.setArguments("{}");
        approval.setMode("BUILD");
        approval.setEnvironment(0);

        return approval;
    }

    @Test
    void testCreatePendingStampsPendingStatusAndDefaultExpiry() {
        AiHubToolApproval approval = pendingApproval();

        AiHubToolApproval saved = service.createPending(approval);

        assertThat(saved.getStatus()).isEqualTo(AiHubToolApproval.Status.PENDING);
        assertThat(saved.getExpiresAt()).isEqualTo(Instant.now(clock)
            .plus(Duration.ofHours(24)));
    }

    @Test
    void testCreatePendingKeepsAnExplicitExpiry() {
        AiHubToolApproval approval = pendingApproval();
        Instant expiresAt = Instant.now(clock)
            .plusSeconds(60);

        approval.setExpiresAt(expiresAt);

        assertThat(service.createPending(approval)
            .getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void testFindPendingQueriesByThePendingOrdinal() {
        AiHubToolApproval approval = pendingApproval();

        when(repository.findFirstByChatIdAndStatus(CHAT_ID, AiHubToolApproval.Status.PENDING.ordinal()))
            .thenReturn(Optional.of(approval));

        assertThat(service.findPending(CHAT_ID)).contains(approval);
    }

    @Test
    void testGetThrowsNotFoundForAnUnknownId() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(5L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void testSupersedePendingFlipsEveryPendingRow() {
        AiHubToolApproval first = pendingApproval();
        AiHubToolApproval second = pendingApproval();

        second.setId(43L);

        when(repository.findAllByChatIdAndStatus(CHAT_ID, AiHubToolApproval.Status.PENDING.ordinal()))
            .thenReturn(List.of(first, second));

        int superseded = service.supersedePending(CHAT_ID);

        assertThat(superseded).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(AiHubToolApproval.Status.SUPERSEDED);
        assertThat(second.getStatus()).isEqualTo(AiHubToolApproval.Status.SUPERSEDED);
        assertThat(first.getDecidedAt()).isEqualTo(Instant.now(clock));

        verify(repository, times(2)).save(any(AiHubToolApproval.class));
    }

    @Test
    void testSupersedePendingWithNoPendingRowsSavesNothing() {
        when(repository.findAllByChatIdAndStatus(CHAT_ID, AiHubToolApproval.Status.PENDING.ordinal()))
            .thenReturn(List.of());

        int superseded = service.supersedePending(CHAT_ID);

        assertThat(superseded).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    void testDeleteByChatDeletesEveryRowOfTheChat() {
        service.deleteByChat(CHAT_ID);

        verify(repository).deleteAllByChatId(CHAT_ID);
    }
}
