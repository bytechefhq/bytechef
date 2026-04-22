/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRequestLog;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertCondition;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertEvent;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertMetric;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRule;
import com.bytechef.ee.automation.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewayRequestLogRepository;
import com.bytechef.platform.scheduler.event.AlertEvaluationEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link AiObservabilityAlertEvaluator}. Focuses on the top-level event listener — any regression in
 * snooze/enabled gating, the no-data short-circuit, or the fire-path that wires up
 * {@link AiObservabilityNotificationDispatcher#dispatch} is caught here.
 *
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiObservabilityAlertEvaluatorTest {

    @Mock
    private AiGatewayMetrics aiGatewayMetrics;

    @Mock
    private AiGatewayRequestLogRepository aiGatewayRequestLogRepository;

    @Mock
    private AiObservabilityAlertEventService aiObservabilityAlertEventService;

    @Mock
    private AiObservabilityAlertRuleService aiObservabilityAlertRuleService;

    @Mock
    private AiObservabilityNotificationDispatcher aiObservabilityNotificationDispatcher;

    private AiObservabilityAlertEvaluator aiObservabilityAlertEvaluator;

    @BeforeEach
    void setUp() {
        aiObservabilityAlertEvaluator = new AiObservabilityAlertEvaluator(
            aiGatewayMetrics, aiGatewayRequestLogRepository, aiObservabilityAlertEventService,
            aiObservabilityAlertRuleService, aiObservabilityNotificationDispatcher);
    }

    @Test
    void testOnAlertEvaluationSkipsDisabledRule() {
        AiObservabilityAlertRule rule = newRule();

        rule.setEnabled(false);

        when(aiObservabilityAlertRuleService.getAlertRule(1L)).thenReturn(rule);

        aiObservabilityAlertEvaluator.onAlertEvaluation(new AlertEvaluationEvent(1L));

        verify(aiGatewayRequestLogRepository, never())
            .findAllByWorkspaceIdAndCreatedDateBetween(any(), any(), any());

        verify(aiObservabilityNotificationDispatcher, never()).dispatch(any(), any());
    }

    @Test
    void testOnAlertEvaluationSkipsWhileSnoozed() {
        AiObservabilityAlertRule rule = newRule();

        rule.setSnoozedUntil(Instant.now()
            .plusSeconds(3600));

        when(aiObservabilityAlertRuleService.getAlertRule(1L)).thenReturn(rule);

        aiObservabilityAlertEvaluator.onAlertEvaluation(new AlertEvaluationEvent(1L));

        verify(aiGatewayRequestLogRepository, never())
            .findAllByWorkspaceIdAndCreatedDateBetween(any(), any(), any());

        verify(aiObservabilityNotificationDispatcher, never()).dispatch(any(), any());
    }

    @Test
    void testOnAlertEvaluationFiresDispatchAndMetricWhenThresholdBreached() {
        // REQUEST_VOLUME > 0 with one request log in the window => breach, dispatch + metric increment.
        AiObservabilityAlertRule rule = new AiObservabilityAlertRule(
            100L, "volume-rule", AiObservabilityAlertMetric.REQUEST_VOLUME,
            AiObservabilityAlertCondition.GREATER_THAN, BigDecimal.ZERO, 5, 0);

        // Seed the rule's id (normally assigned by Spring Data JDBC on save) so downstream
        // `new AiObservabilityAlertEvent(alertRule.getId(), ...)` has a non-null alertRuleId.
        setRuleId(rule, 42L);

        AiGatewayRequestLog requestLog = new AiGatewayRequestLog("req-1", "gpt-4");

        AiObservabilityAlertEvent savedEvent = new AiObservabilityAlertEvent(42L, BigDecimal.ONE, "msg");

        when(aiObservabilityAlertRuleService.getAlertRule(42L)).thenReturn(rule);
        when(aiGatewayRequestLogRepository.findAllByWorkspaceIdAndCreatedDateBetween(
            any(), any(), any())).thenReturn(List.of(requestLog));
        when(aiObservabilityAlertEventService.create(any())).thenReturn(savedEvent);

        aiObservabilityAlertEvaluator.onAlertEvaluation(new AlertEvaluationEvent(42L));

        verify(aiObservabilityAlertEventService).create(any(AiObservabilityAlertEvent.class));
        verify(aiGatewayMetrics).incrementAlertTriggered("REQUEST_VOLUME");
        verify(aiObservabilityNotificationDispatcher).dispatch(rule, savedEvent);
    }

    @Test
    void testLessThanConditionFiresWhenMetricDropsBelowThreshold() {
        AiObservabilityAlertRule rule = new AiObservabilityAlertRule(
            100L, "volume-under-threshold", AiObservabilityAlertMetric.REQUEST_VOLUME,
            AiObservabilityAlertCondition.LESS_THAN, BigDecimal.valueOf(10), 5, 0);

        setRuleId(rule, 43L);

        AiObservabilityAlertEvent savedEvent = new AiObservabilityAlertEvent(43L, BigDecimal.ONE, "msg");

        when(aiObservabilityAlertRuleService.getAlertRule(43L)).thenReturn(rule);
        when(aiGatewayRequestLogRepository.findAllByWorkspaceIdAndCreatedDateBetween(
            any(), any(), any())).thenReturn(List.of(new AiGatewayRequestLog("req-1", "gpt-4")));
        when(aiObservabilityAlertEventService.create(any())).thenReturn(savedEvent);

        aiObservabilityAlertEvaluator.onAlertEvaluation(new AlertEvaluationEvent(43L));

        verify(aiObservabilityAlertEventService).create(any(AiObservabilityAlertEvent.class));
        verify(aiObservabilityNotificationDispatcher).dispatch(rule, savedEvent);
    }

    @Test
    void testEqualsConditionFiresOnExactMatch() {
        AiObservabilityAlertRule rule = new AiObservabilityAlertRule(
            100L, "volume-exact", AiObservabilityAlertMetric.REQUEST_VOLUME,
            AiObservabilityAlertCondition.EQUALS, BigDecimal.ONE, 5, 0);

        setRuleId(rule, 44L);

        AiObservabilityAlertEvent savedEvent = new AiObservabilityAlertEvent(44L, BigDecimal.ONE, "msg");

        when(aiObservabilityAlertRuleService.getAlertRule(44L)).thenReturn(rule);
        when(aiGatewayRequestLogRepository.findAllByWorkspaceIdAndCreatedDateBetween(
            any(), any(), any())).thenReturn(List.of(new AiGatewayRequestLog("req-1", "gpt-4")));
        when(aiObservabilityAlertEventService.create(any())).thenReturn(savedEvent);

        aiObservabilityAlertEvaluator.onAlertEvaluation(new AlertEvaluationEvent(44L));

        verify(aiObservabilityAlertEventService).create(any(AiObservabilityAlertEvent.class));
        verify(aiObservabilityNotificationDispatcher).dispatch(rule, savedEvent);
    }

    @Test
    void testLessThanConditionDoesNotFireWhenMetricMeetsThreshold() {
        // REQUEST_VOLUME=2, threshold=1, LESS_THAN: 2 is NOT less than 1 — must not fire. Without this negative
        // assertion, a refactor that flipped the comparison operator (>=/<= swapped) would still pass the paired
        // positive tests because they also check the fire path. This guards the operator direction explicitly.
        AiObservabilityAlertRule rule = new AiObservabilityAlertRule(
            100L, "volume-above", AiObservabilityAlertMetric.REQUEST_VOLUME,
            AiObservabilityAlertCondition.LESS_THAN, BigDecimal.ONE, 5, 0);

        setRuleId(rule, 45L);

        when(aiObservabilityAlertRuleService.getAlertRule(45L)).thenReturn(rule);
        when(aiGatewayRequestLogRepository.findAllByWorkspaceIdAndCreatedDateBetween(
            any(), any(), any())).thenReturn(
                List.of(
                    new AiGatewayRequestLog("req-1", "gpt-4"),
                    new AiGatewayRequestLog("req-2", "gpt-4")));

        aiObservabilityAlertEvaluator.onAlertEvaluation(new AlertEvaluationEvent(45L));

        verify(aiObservabilityAlertEventService, never()).create(any(AiObservabilityAlertEvent.class));
        verify(aiObservabilityNotificationDispatcher, never()).dispatch(any(), any());
    }

    @Test
    void testGreaterThanConditionDoesNotFireWhenMetricBelowThreshold() {
        // Symmetric negative assertion for GREATER_THAN.
        AiObservabilityAlertRule rule = new AiObservabilityAlertRule(
            100L, "volume-below", AiObservabilityAlertMetric.REQUEST_VOLUME,
            AiObservabilityAlertCondition.GREATER_THAN, BigDecimal.valueOf(10), 5, 0);

        setRuleId(rule, 46L);

        when(aiObservabilityAlertRuleService.getAlertRule(46L)).thenReturn(rule);
        when(aiGatewayRequestLogRepository.findAllByWorkspaceIdAndCreatedDateBetween(
            any(), any(), any())).thenReturn(List.of(new AiGatewayRequestLog("req-1", "gpt-4")));

        aiObservabilityAlertEvaluator.onAlertEvaluation(new AlertEvaluationEvent(46L));

        verify(aiObservabilityAlertEventService, never()).create(any(AiObservabilityAlertEvent.class));
        verify(aiObservabilityNotificationDispatcher, never()).dispatch(any(), any());
    }

    @Test
    void testEqualsConditionDoesNotFireOnMismatch() {
        // Negative pair for EQUALS: REQUEST_VOLUME=2, threshold=1 — not equal, must not fire.
        AiObservabilityAlertRule rule = new AiObservabilityAlertRule(
            100L, "volume-mismatch", AiObservabilityAlertMetric.REQUEST_VOLUME,
            AiObservabilityAlertCondition.EQUALS, BigDecimal.ONE, 5, 0);

        setRuleId(rule, 47L);

        when(aiObservabilityAlertRuleService.getAlertRule(47L)).thenReturn(rule);
        when(aiGatewayRequestLogRepository.findAllByWorkspaceIdAndCreatedDateBetween(
            any(), any(), any())).thenReturn(
                List.of(
                    new AiGatewayRequestLog("req-1", "gpt-4"),
                    new AiGatewayRequestLog("req-2", "gpt-4")));

        aiObservabilityAlertEvaluator.onAlertEvaluation(new AlertEvaluationEvent(47L));

        verify(aiObservabilityAlertEventService, never()).create(any(AiObservabilityAlertEvent.class));
        verify(aiObservabilityNotificationDispatcher, never()).dispatch(any(), any());
    }

    @Test
    void testEvaluateMetricReturnsNullWhenNoRequestLogsInWindow() {
        AiObservabilityAlertRule rule = newRule();

        when(aiGatewayRequestLogRepository.findAllByWorkspaceIdAndCreatedDateBetween(
            any(), any(), any())).thenReturn(List.of());

        BigDecimal metric = aiObservabilityAlertEvaluator.evaluateMetric(rule);

        assertThat(metric).isNull();

        // Dry-run must not emit events or dispatch notifications.
        verify(aiObservabilityAlertEventService, never()).create(any());
        verify(aiObservabilityNotificationDispatcher, never()).dispatch(any(), any());
    }

    private static AiObservabilityAlertRule newRule() {
        return new AiObservabilityAlertRule(
            100L, "rule", AiObservabilityAlertMetric.REQUEST_VOLUME, AiObservabilityAlertCondition.GREATER_THAN,
            BigDecimal.valueOf(1000), 5, 0);
    }

    private static void setRuleId(AiObservabilityAlertRule rule, long id) {
        try {
            java.lang.reflect.Field idField = AiObservabilityAlertRule.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(rule, id);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed id on test rule", reflectiveOperationException);
        }
    }
}
