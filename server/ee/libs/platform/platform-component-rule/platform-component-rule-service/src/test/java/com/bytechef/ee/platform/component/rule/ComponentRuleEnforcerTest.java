/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.workspace.JobPrincipalWorkspaceResolver;
import com.bytechef.ee.platform.component.rule.ComponentRule.RuleAction;
import com.bytechef.ee.platform.component.rule.ComponentRule.RulePhase;
import com.bytechef.ee.platform.component.rule.audit.ComponentRuleAuditEvent;
import com.bytechef.ee.platform.component.rule.audit.ComponentRuleAuditPublisher;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.component.rule.ComponentRuleEnforcer.Decision;
import com.bytechef.platform.component.rule.ComponentRuleEnforcer.ToolCall;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Ticker;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 * @author Ivica Cardic
 */
class ComponentRuleEnforcerTest {

    private static final ToolCall SEND_MESSAGE_TO_C05 = new ToolCall(
        "slack", "sendMessage", "SLACK_SEND_MESSAGE", Map.of("channel", "C05QG7RF30A", "text", "hello"), 3L, 42L, 7L,
        null, null, null);

    private final ComponentRuleService componentRuleService = mock(ComponentRuleService.class);
    private final ComponentRuleAuditPublisher componentRuleAuditPublisher = mock(ComponentRuleAuditPublisher.class);
    private final ComponentRuleSettingsService componentRuleSettingsService = mock(ComponentRuleSettingsService.class);
    private final JobPrincipalWorkspaceResolver jobPrincipalWorkspaceResolver = mock(
        JobPrincipalWorkspaceResolver.class);
    private final Evaluator evaluator = SpelEvaluator.builder()
        .build();
    private final ComponentRuleEnforcerImpl componentRuleEnforcer = new ComponentRuleEnforcerImpl(
        componentRuleService, evaluator, componentRuleAuditPublisher, componentRuleSettingsService,
        jobPrincipalWorkspaceResolver);

    @BeforeEach
    void setUp() {
        when(componentRuleSettingsService.getSettings(any())).thenReturn(ComponentRuleSettings.DEFAULT);

        when(jobPrincipalWorkspaceResolver.resolve(any(), any())).thenReturn(null);
    }

    @Test
    void testNoRulesAllows() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(List.of());

        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05)).isInstanceOf(Decision.Allow.class);

        verify(componentRuleAuditPublisher, never()).publish(any(), any());
    }

    @Test
    void testMatchingBeforeBlockRuleReturnsABlockDecision() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(
                newComponentRule(
                    1L, "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK,
                    "inputParameters['channel'] == 'C05QG7RF30A'")));

        Decision decision = componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05);

        assertThat(decision).isInstanceOfSatisfying(
            Decision.Block.class, block -> assertThat(block.reason()).contains("blocked by an administrator rule"));

        verify(componentRuleAuditPublisher).publish(eq(ComponentRuleAuditEvent.RULE_BLOCKED), any());
    }

    @Test
    void testNonMatchingBeforeBlockRuleAllowsTheCall() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(
                newComponentRule(
                    1L, "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK,
                    "inputParameters['channel'] == 'C99NOMATCH'")));

        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05)).isInstanceOf(Decision.Allow.class);

        verify(componentRuleAuditPublisher, never()).publish(any(), any());
    }

    @Test
    void testMatchingBeforeTagRuleAllowsTheCallAndAudits() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(
                newComponentRule(
                    2L, "sendMessage", RulePhase.BEFORE, RuleAction.TAG,
                    "contains(inputParameters['channel'], 'C05')")));

        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05)).isInstanceOf(Decision.Allow.class);

        verify(componentRuleAuditPublisher).publish(eq(ComponentRuleAuditEvent.RULE_TAGGED), any());
    }

    @Test
    void testBlockWinsOverTagOnTheSameCall() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(
                newComponentRule(2L, "sendMessage", RulePhase.BEFORE, RuleAction.TAG, "true"),
                newComponentRule(1L, "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "true")));

        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05)).isInstanceOf(Decision.Block.class);

        verify(componentRuleAuditPublisher).publish(eq(ComponentRuleAuditEvent.RULE_BLOCKED), any());
        verify(componentRuleAuditPublisher, never()).publish(eq(ComponentRuleAuditEvent.RULE_TAGGED), any());
    }

    @Test
    void testNullToolNameRuleAppliesToEveryTool() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(newComponentRule(3L, null, RulePhase.BEFORE, RuleAction.BLOCK, "true")));

        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05)).isInstanceOf(Decision.Block.class);
    }

    @Test
    void testRuleScopedToAnotherToolDoesNotFire() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(newComponentRule(4L, "deleteMessage", RulePhase.BEFORE, RuleAction.BLOCK, "true")));

        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05)).isInstanceOf(Decision.Allow.class);
    }

    @Test
    void testToolCallNameIsAvailableInTheConditionContext() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(
                newComponentRule(
                    13L, null, RulePhase.BEFORE, RuleAction.BLOCK, "toolCallName == 'SLACK_SEND_MESSAGE'")));

        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05)).isInstanceOf(Decision.Block.class);
    }

    @Test
    void testAfterRuleSeesOutputInItsContext() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(
                newComponentRule(
                    5L, "sendMessage", RulePhase.AFTER, RuleAction.TAG, "output['ok'] == false")));

        componentRuleEnforcer.recordAfterCall(SEND_MESSAGE_TO_C05, Map.of("ok", false));

        verify(componentRuleAuditPublisher).publish(eq(ComponentRuleAuditEvent.RULE_TAGGED), any());
    }

    @Test
    void testOutputIsNotAvailableInTheBeforePhase() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(
                newComponentRule(
                    11L, "sendMessage", RulePhase.BEFORE, RuleAction.TAG, "output['ok'] == false")));

        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05)).isInstanceOf(Decision.Allow.class);

        verify(componentRuleAuditPublisher, never()).publish(any(), any());
    }

    @Test
    void testBeforeRuleIsNotEvaluatedInTheAfterPhase() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(newComponentRule(6L, "sendMessage", RulePhase.BEFORE, RuleAction.TAG, "true")));

        componentRuleEnforcer.recordAfterCall(SEND_MESSAGE_TO_C05, Map.of());

        verify(componentRuleAuditPublisher, never()).publish(any(), any());
    }

    @Test
    void testAfterPhaseNeverThrows() {
        when(componentRuleService.getEnabledComponentRules("slack", null))
            .thenThrow(new IllegalStateException("database is down"));

        assertThatCode(() -> componentRuleEnforcer.recordAfterCall(SEND_MESSAGE_TO_C05, Map.of()))
            .doesNotThrowAnyException();
    }

    @Test
    void testUnresolvableConditionDoesNotBlock() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(
                newComponentRule(
                    7L, "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "missingRoot['whatever'] == 1")));

        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05)).isInstanceOf(Decision.Allow.class);
    }

    @Test
    void testRulesAreFetchedOncePerComponentAcrossBothPhases() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(List.of());

        componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05);
        componentRuleEnforcer.recordAfterCall(SEND_MESSAGE_TO_C05, Map.of());
        componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05);

        verify(componentRuleService, times(1)).getEnabledComponentRules("slack", null);
    }

    @Test
    void testAuditPayloadCarriesTheFiringRule() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(newComponentRule(9L, "sendMessage", RulePhase.BEFORE, RuleAction.TAG, "true")));

        componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05);

        ArgumentCaptor<ComponentRuleAuditPublisher.ComponentRuleAuditPayload> payloadCaptor =
            ArgumentCaptor.forClass(ComponentRuleAuditPublisher.ComponentRuleAuditPayload.class);

        verify(componentRuleAuditPublisher).publish(eq(ComponentRuleAuditEvent.RULE_TAGGED), payloadCaptor.capture());

        ComponentRuleAuditPublisher.ComponentRuleAuditPayload payload = payloadCaptor.getValue();

        assertThat(payload.ruleId()).isEqualTo(9L);
        assertThat(payload.componentName()).isEqualTo("slack");
        assertThat(payload.toolName()).isEqualTo("sendMessage");
        assertThat(payload.toolCallName()).isEqualTo("SLACK_SEND_MESSAGE");
        assertThat(payload.phase()).isEqualTo("BEFORE");
        assertThat(payload.jobId()).isEqualTo(42L);
        assertThat(payload.taskExecutionId()).isEqualTo(7L);
    }

    @Test
    void testMatchingApprovalRuleReturnsEveryMatchingRuleId() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(
                newComponentRule(1L, "sendMessage", RulePhase.BEFORE, RuleAction.REQUIRE_APPROVAL, "true"),
                newComponentRule(2L, null, RulePhase.BEFORE, RuleAction.REQUIRE_APPROVAL, "true")));

        Decision decision = componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05);

        assertThat(decision).isInstanceOfSatisfying(
            Decision.RequireApproval.class,
            requireApproval -> assertThat(requireApproval.ruleIds()).containsExactlyInAnyOrder(1L, 2L));

        verify(componentRuleAuditPublisher, times(2))
            .publish(eq(ComponentRuleAuditEvent.RULE_APPROVAL_REQUESTED), any());
    }

    @Test
    void testBlockWinsOverApproval() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(
                newComponentRule(1L, "sendMessage", RulePhase.BEFORE, RuleAction.REQUIRE_APPROVAL, "true"),
                newComponentRule(2L, "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "true")));

        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05)).isInstanceOf(Decision.Block.class);
    }

    @Test
    void testAnApprovedCallDoesNotRaiseTheApprovalAgain() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(newComponentRule(1L, "sendMessage", RulePhase.BEFORE, RuleAction.REQUIRE_APPROVAL, "true")));

        ToolCall approvedToolCall = new ToolCall(
            "slack", "sendMessage", "SLACK_SEND_MESSAGE", Map.of("channel", "C05QG7RF30A"), 3L, 42L, 7L, "@jane",
            null, null);

        assertThat(componentRuleEnforcer.checkBeforeCall(approvedToolCall)).isInstanceOf(Decision.Allow.class);
    }

    @Test
    void testAnApprovedCallStillBlocks() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(newComponentRule(1L, "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "true")));

        ToolCall approvedToolCall = new ToolCall(
            "slack", "sendMessage", "SLACK_SEND_MESSAGE", Map.of("channel", "C05QG7RF30A"), 3L, 42L, 7L, "@jane",
            null, null);

        assertThat(componentRuleEnforcer.checkBeforeCall(approvedToolCall)).isInstanceOf(Decision.Block.class);
    }

    @Test
    void testRecordApprovalResolutionAudits() {
        componentRuleEnforcer.recordApprovalResolution(List.of(1L), SEND_MESSAGE_TO_C05, true, "@jane");

        verify(componentRuleAuditPublisher).publish(eq(ComponentRuleAuditEvent.RULE_APPROVED), any());
    }

    @Test
    void testObserveModeAllowsAndAuditsAWouldBeBlock() {
        when(componentRuleSettingsService.getSettings(null)).thenReturn(new ComponentRuleSettings(true, 1440));
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(newComponentRule(1L, "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "true")));

        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05)).isInstanceOf(Decision.Allow.class);

        ArgumentCaptor<ComponentRuleAuditPublisher.ComponentRuleAuditPayload> payloadCaptor =
            ArgumentCaptor.forClass(ComponentRuleAuditPublisher.ComponentRuleAuditPayload.class);

        verify(componentRuleAuditPublisher).publish(
            eq(ComponentRuleAuditEvent.RULE_OBSERVED), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()
            .wouldHave()).isEqualTo("BLOCK");
    }

    @Test
    void testObserveModeNeverRaisesAnApproval() {
        when(componentRuleSettingsService.getSettings(null)).thenReturn(new ComponentRuleSettings(true, 1440));
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(newComponentRule(1L, "sendMessage", RulePhase.BEFORE, RuleAction.REQUIRE_APPROVAL, "true")));

        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05)).isInstanceOf(Decision.Allow.class);

        verify(componentRuleAuditPublisher).publish(eq(ComponentRuleAuditEvent.RULE_OBSERVED), any());
        verify(componentRuleAuditPublisher, never())
            .publish(eq(ComponentRuleAuditEvent.RULE_APPROVAL_REQUESTED), any());
    }

    @Test
    void testAStrictRuleFiresWhenItsConditionCannotBeResolved() {
        ComponentRule componentRule = newComponentRule(
            7L, "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "missingRoot['whatever'] == 1");

        componentRule.setStrict(true);

        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(List.of(componentRule));

        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05)).isInstanceOf(Decision.Block.class);
    }

    @Test
    void testAStrictFallbackIsMarkedInTheAuditPayload() {
        ComponentRule componentRule = newComponentRule(
            8L, "sendMessage", RulePhase.BEFORE, RuleAction.TAG, "missingRoot['whatever'] == 1");

        componentRule.setStrict(true);

        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(List.of(componentRule));

        componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05);

        ArgumentCaptor<ComponentRuleAuditPublisher.ComponentRuleAuditPayload> payloadCaptor =
            ArgumentCaptor.forClass(ComponentRuleAuditPublisher.ComponentRuleAuditPayload.class);

        verify(componentRuleAuditPublisher).publish(eq(ComponentRuleAuditEvent.RULE_TAGGED), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()
            .strictFallback()).isTrue();
    }

    @Test
    void testANonStrictRuleStillFailsOpen() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(
                newComponentRule(
                    9L, "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "missingRoot['whatever'] == 1")));

        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05)).isInstanceOf(Decision.Allow.class);
    }

    @Test
    void testAStrictRuleWithALegitimatelyFalseConditionDoesNotFire() {
        ComponentRule componentRule = newComponentRule(
            10L, "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "inputParameters['channel'] == 'C99NOMATCH'");

        componentRule.setStrict(true);

        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(List.of(componentRule));

        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05)).isInstanceOf(Decision.Allow.class);

        verify(componentRuleAuditPublisher, never()).publish(any(), any());
    }

    @Test
    void testTheCacheRefetchesAfterItsTtlExpires() {
        FakeTicker fakeTicker = new FakeTicker();
        ComponentRuleEnforcerImpl expiringComponentRuleEnforcer = new ComponentRuleEnforcerImpl(
            componentRuleService, evaluator, componentRuleAuditPublisher, componentRuleSettingsService,
            jobPrincipalWorkspaceResolver, fakeTicker);

        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(List.of());

        expiringComponentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05);

        fakeTicker.advanceSeconds(9);

        expiringComponentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05);

        verify(componentRuleService, times(1)).getEnabledComponentRules("slack", null);

        fakeTicker.advanceSeconds(2);

        expiringComponentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05);

        verify(componentRuleService, times(2)).getEnabledComponentRules("slack", null);
    }

    @Test
    void testTheTenantSettingsAreCachedBesideTheRuleList() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(newComponentRule(1L, "sendMessage", RulePhase.BEFORE, RuleAction.REQUIRE_APPROVAL, "true")));

        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05))
            .isInstanceOf(Decision.RequireApproval.class);
        assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05))
            .isInstanceOf(Decision.RequireApproval.class);

        verify(componentRuleSettingsService, times(1)).getSettings(null);
    }

    @Test
    void testCacheIsScopedByTenant() {
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(newComponentRule(12L, "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "true")));

        TenantContext.runWithTenantId(
            "tenant_a",
            () -> assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05))
                .isInstanceOf(Decision.Block.class));

        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(List.of());

        TenantContext.runWithTenantId(
            "tenant_b",
            () -> assertThat(componentRuleEnforcer.checkBeforeCall(SEND_MESSAGE_TO_C05))
                .isInstanceOf(Decision.Allow.class));
    }

    @Test
    void testRulesAreFetchedForTheResolvedWorkspace() {
        when(jobPrincipalWorkspaceResolver.resolve(PlatformType.AUTOMATION, 5L)).thenReturn(42L);
        when(componentRuleService.getEnabledComponentRules("slack", 42L)).thenReturn(
            List.of(newComponentRule(1L, "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "true")));

        assertThat(componentRuleEnforcer.checkBeforeCall(inWorkspace(5L))).isInstanceOf(Decision.Block.class);
    }

    @Test
    void testAnUnresolvableWorkspaceStillEvaluatesTenantWideRules() {
        when(jobPrincipalWorkspaceResolver.resolve(any(), any())).thenReturn(null);
        when(componentRuleService.getEnabledComponentRules("slack", null)).thenReturn(
            List.of(newComponentRule(1L, "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "true")));

        assertThat(componentRuleEnforcer.checkBeforeCall(inWorkspace(5L))).isInstanceOf(Decision.Block.class);
    }

    @Test
    void testTheCacheIsScopedByWorkspace() {
        when(jobPrincipalWorkspaceResolver.resolve(PlatformType.AUTOMATION, 5L)).thenReturn(42L);
        when(jobPrincipalWorkspaceResolver.resolve(PlatformType.AUTOMATION, 6L)).thenReturn(99L);
        when(componentRuleService.getEnabledComponentRules("slack", 42L)).thenReturn(
            List.of(newComponentRule(1L, "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "true")));
        when(componentRuleService.getEnabledComponentRules("slack", 99L)).thenReturn(List.of());

        assertThat(componentRuleEnforcer.checkBeforeCall(inWorkspace(5L))).isInstanceOf(Decision.Block.class);
        assertThat(componentRuleEnforcer.checkBeforeCall(inWorkspace(6L))).isInstanceOf(Decision.Allow.class);
    }

    @Test
    void testObserveModeInOneWorkspaceDoesNotSuppressEnforcementInAnother() {
        long observingWorkspaceId = 201L;
        long enforcingWorkspaceId = 202L;

        when(jobPrincipalWorkspaceResolver.resolve(PlatformType.AUTOMATION, 11L)).thenReturn(observingWorkspaceId);
        when(jobPrincipalWorkspaceResolver.resolve(PlatformType.AUTOMATION, 12L)).thenReturn(enforcingWorkspaceId);

        ComponentRule blockingRule = newComponentRule(1L, "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "true");

        when(componentRuleService.getEnabledComponentRules("slack", observingWorkspaceId))
            .thenReturn(List.of(blockingRule));
        when(componentRuleService.getEnabledComponentRules("slack", enforcingWorkspaceId))
            .thenReturn(List.of(blockingRule));

        when(componentRuleSettingsService.getSettings(observingWorkspaceId))
            .thenReturn(new ComponentRuleSettings(true, 1440));
        when(componentRuleSettingsService.getSettings(enforcingWorkspaceId))
            .thenReturn(new ComponentRuleSettings(false, 1440));

        assertThat(componentRuleEnforcer.checkBeforeCall(inWorkspace(11L))).isInstanceOf(Decision.Allow.class);
        assertThat(componentRuleEnforcer.checkBeforeCall(inWorkspace(12L))).isInstanceOf(Decision.Block.class);
    }

    private static ToolCall inWorkspace(long jobPrincipalId) {
        return new ToolCall(
            "slack", "sendMessage", "SLACK_SEND_MESSAGE", Map.of("channel", "C05QG7RF30A"), 3L, 42L, null, null,
            jobPrincipalId, PlatformType.AUTOMATION);
    }

    private static final class FakeTicker implements Ticker {

        private long nanos;

        @Override
        public long read() {
            return nanos;
        }

        private void advanceSeconds(long seconds) {
            nanos += TimeUnit.SECONDS.toNanos(seconds);
        }
    }

    private static ComponentRule newComponentRule(
        long id, String toolName, RulePhase rulePhase, RuleAction ruleAction, String condition) {

        ComponentRule componentRule = new ComponentRule();

        componentRule.setId(id);
        componentRule.setComponentName("slack");
        componentRule.setToolName(toolName);
        componentRule.setPhase(rulePhase);
        componentRule.setRuleAction(ruleAction);
        componentRule.setCondition(condition);
        componentRule.setEnabled(true);

        return componentRule;
    }
}
