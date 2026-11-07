/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule;

import com.bytechef.ee.platform.ai.workspace.JobPrincipalWorkspaceResolver;
import com.bytechef.ee.platform.component.rule.ComponentRule.RuleAction;
import com.bytechef.ee.platform.component.rule.ComponentRule.RulePhase;
import com.bytechef.ee.platform.component.rule.audit.ComponentRuleAuditEvent;
import com.bytechef.ee.platform.component.rule.audit.ComponentRuleAuditPublisher;
import com.bytechef.ee.platform.component.rule.audit.ComponentRuleAuditPublisher.ComponentRuleAuditPayload;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.rule.ComponentRuleEnforcer;
import com.bytechef.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ComponentRuleEnforcerImpl implements ComponentRuleEnforcer {

    private static final Duration CACHE_TTL = Duration.ofSeconds(10);
    private static final String CONDITION_KEY = "condition";
    private static final String FORMULA_PREFIX = "=";
    private static final Decision ALLOW = new Decision.Allow();

    private static final Logger log = LoggerFactory.getLogger(ComponentRuleEnforcerImpl.class);

    private record TenantComponentWorkspaceKey(String tenantId, String componentName, @Nullable Long workspaceId) {
    }

    private record TenantWorkspaceKey(String tenantId, @Nullable Long workspaceId) {
    }

    private record MatchedComponentRule(ComponentRule componentRule, boolean strictFallback) {
    }

    private record MatchResult(boolean matched, boolean strictFallback) {

        private static final MatchResult NO_MATCH = new MatchResult(false, false);
        private static final MatchResult MATCHED = new MatchResult(true, false);
        private static final MatchResult STRICT_FALLBACK = new MatchResult(true, true);
    }

    private final Cache<TenantComponentWorkspaceKey, List<ComponentRule>> componentRulesCache;
    private final Cache<TenantWorkspaceKey, ComponentRuleSettings> componentRuleSettingsCache;
    private final ComponentRuleAuditPublisher componentRuleAuditPublisher;
    private final ComponentRuleService componentRuleService;
    private final ComponentRuleSettingsService componentRuleSettingsService;
    private final Evaluator evaluator;
    private final JobPrincipalWorkspaceResolver jobPrincipalWorkspaceResolver;

    @Autowired
    public ComponentRuleEnforcerImpl(
        ComponentRuleService componentRuleService, Evaluator evaluator,
        ComponentRuleAuditPublisher componentRuleAuditPublisher,
        ComponentRuleSettingsService componentRuleSettingsService,
        JobPrincipalWorkspaceResolver jobPrincipalWorkspaceResolver) {

        this(componentRuleService, evaluator, componentRuleAuditPublisher, componentRuleSettingsService,
            jobPrincipalWorkspaceResolver, Ticker.systemTicker());
    }

    ComponentRuleEnforcerImpl(
        ComponentRuleService componentRuleService, Evaluator evaluator,
        ComponentRuleAuditPublisher componentRuleAuditPublisher,
        ComponentRuleSettingsService componentRuleSettingsService,
        JobPrincipalWorkspaceResolver jobPrincipalWorkspaceResolver, Ticker ticker) {

        this.componentRuleService = componentRuleService;
        this.evaluator = evaluator;
        this.componentRuleAuditPublisher = componentRuleAuditPublisher;
        this.componentRuleSettingsService = componentRuleSettingsService;
        this.jobPrincipalWorkspaceResolver = jobPrincipalWorkspaceResolver;
        this.componentRulesCache = Caffeine.newBuilder()
            .expireAfterWrite(CACHE_TTL)
            .ticker(ticker)
            .build();
        this.componentRuleSettingsCache = Caffeine.newBuilder()
            .expireAfterWrite(CACHE_TTL)
            .ticker(ticker)
            .build();
    }

    @Override
    public Decision checkBeforeCall(ToolCall toolCall) {
        Long workspaceId = jobPrincipalWorkspaceResolver.resolve(toolCall.platformType(), toolCall.jobPrincipalId());

        if (getCachedComponentRules(toolCall.componentName(), workspaceId)
            .isEmpty()) {

            return ALLOW;
        }

        List<MatchedComponentRule> matchingComponentRules = getMatchingComponentRules(
            toolCall, workspaceId, RulePhase.BEFORE, buildEvaluationContext(toolCall, null));

        if (matchingComponentRules.isEmpty()) {
            return ALLOW;
        }

        ComponentRuleSettings componentRuleSettings = getCachedComponentRuleSettings(workspaceId);

        if (componentRuleSettings.observeMode()) {

            for (MatchedComponentRule matchedComponentRule : matchingComponentRules) {
                publish(
                    ComponentRuleAuditEvent.RULE_OBSERVED, matchedComponentRule, toolCall, RulePhase.BEFORE,
                    matchedComponentRule.componentRule()
                        .getRuleAction()
                        .name());
            }

            return ALLOW;
        }

        for (MatchedComponentRule matchedComponentRule : matchingComponentRules) {
            if (matchedComponentRule.componentRule()
                .getRuleAction() == RuleAction.BLOCK) {

                publish(ComponentRuleAuditEvent.RULE_BLOCKED, matchedComponentRule, toolCall, RulePhase.BEFORE);

                return new Decision.Block(
                    "Tool '%s' of component '%s' was blocked by an administrator rule."
                        .formatted(toolCall.toolName(), toolCall.componentName()));
            }
        }

        List<Long> approvalRuleIds = new ArrayList<>();

        for (MatchedComponentRule matchedComponentRule : matchingComponentRules) {
            if (matchedComponentRule.componentRule()
                .getRuleAction() == RuleAction.REQUIRE_APPROVAL) {

                approvalRuleIds.add(matchedComponentRule.componentRule()
                    .getId());
            }
        }

        if (!approvalRuleIds.isEmpty() && toolCall.approvedBy() == null) {
            for (MatchedComponentRule matchedComponentRule : matchingComponentRules) {
                if (matchedComponentRule.componentRule()
                    .getRuleAction() == RuleAction.REQUIRE_APPROVAL) {

                    publish(
                        ComponentRuleAuditEvent.RULE_APPROVAL_REQUESTED, matchedComponentRule, toolCall,
                        RulePhase.BEFORE);
                }
            }

            return new Decision.RequireApproval(
                List.copyOf(approvalRuleIds), "Approve tool call: " + toolCall.toolCallName(),
                buildApprovalDescription(matchingComponentRules, toolCall), Instant.now()
                    .plus(Duration.ofHours(componentRuleSettings.approvalExpiresInHours())));
        }

        for (MatchedComponentRule matchedComponentRule : matchingComponentRules) {
            if (matchedComponentRule.componentRule()
                .getRuleAction() == RuleAction.TAG) {

                publish(ComponentRuleAuditEvent.RULE_TAGGED, matchedComponentRule, toolCall, RulePhase.BEFORE);
            }
        }

        return ALLOW;
    }

    @Override
    public void recordAfterCall(ToolCall toolCall, @Nullable Object output) {
        try {
            Long workspaceId = jobPrincipalWorkspaceResolver.resolve(
                toolCall.platformType(), toolCall.jobPrincipalId());

            List<MatchedComponentRule> matchingComponentRules = getMatchingComponentRules(
                toolCall, workspaceId, RulePhase.AFTER, buildEvaluationContext(toolCall, output));

            for (MatchedComponentRule matchedComponentRule : matchingComponentRules) {
                publish(ComponentRuleAuditEvent.RULE_TAGGED, matchedComponentRule, toolCall, RulePhase.AFTER);
            }
        } catch (RuntimeException exception) {
            log.warn(
                "Could not evaluate AFTER-phase rules for {}#{}", toolCall.componentName(), toolCall.toolName(),
                exception);
        }
    }

    @Override
    public void recordApprovalResolution(
        List<Long> ruleIds, ToolCall toolCall, boolean approved, @Nullable String approvedBy) {

        try {
            ComponentRuleAuditEvent componentRuleAuditEvent = approved
                ? ComponentRuleAuditEvent.RULE_APPROVED : ComponentRuleAuditEvent.RULE_REJECTED;

            for (Long ruleId : ruleIds) {
                componentRuleAuditPublisher.publish(
                    componentRuleAuditEvent,
                    new ComponentRuleAuditPayload(
                        ruleId, toolCall.componentName(), toolCall.toolName(), toolCall.toolCallName(),
                        RulePhase.BEFORE.name(), toolCall.jobId(), toolCall.taskExecutionId(), approvedBy, null,
                        false));
            }
        } catch (RuntimeException exception) {
            log.warn("Could not record the approval resolution for rules {}", ruleIds, exception);
        }
    }

    private List<MatchedComponentRule> getMatchingComponentRules(
        ToolCall toolCall, @Nullable Long workspaceId, RulePhase rulePhase,
        Map<String, Object> evaluationContext) {

        List<ComponentRule> componentRules = getCachedComponentRules(toolCall.componentName(), workspaceId);

        if (componentRules.isEmpty()) {
            return List.of();
        }

        List<MatchedComponentRule> matchingComponentRules = new ArrayList<>();

        for (ComponentRule componentRule : componentRules) {
            if (componentRule.getPhase() != rulePhase || !componentRule.appliesToTool(toolCall.toolName())) {
                continue;
            }

            MatchResult matchResult = matches(componentRule, evaluationContext);

            if (matchResult.matched()) {
                matchingComponentRules.add(new MatchedComponentRule(componentRule, matchResult.strictFallback()));
            }
        }

        return matchingComponentRules;
    }

    private ComponentRuleSettings getCachedComponentRuleSettings(@Nullable Long workspaceId) {
        TenantWorkspaceKey tenantWorkspaceKey = new TenantWorkspaceKey(
            TenantContext.getCurrentTenantId(), workspaceId);

        return componentRuleSettingsCache.get(
            tenantWorkspaceKey, key -> componentRuleSettingsService.getSettings(key.workspaceId()));
    }

    private List<ComponentRule> getCachedComponentRules(String componentName, @Nullable Long workspaceId) {
        TenantComponentWorkspaceKey tenantComponentWorkspaceKey = new TenantComponentWorkspaceKey(
            TenantContext.getCurrentTenantId(), componentName, workspaceId);

        return componentRulesCache.get(
            tenantComponentWorkspaceKey,
            key -> componentRuleService.getEnabledComponentRules(key.componentName(), key.workspaceId()));
    }

    private MatchResult matches(ComponentRule componentRule, Map<String, Object> evaluationContext) {
        try {
            Map<String, ?> evaluated = evaluator.evaluate(
                Map.of(CONDITION_KEY, FORMULA_PREFIX + componentRule.getCondition()), evaluationContext, true);

            Object condition = evaluated.get(CONDITION_KEY);

            if (Boolean.TRUE.equals(condition)) {
                return MatchResult.MATCHED;
            }

            if (Boolean.FALSE.equals(condition)) {
                return MatchResult.NO_MATCH;
            }

            return componentRule.isStrict() ? MatchResult.STRICT_FALLBACK : MatchResult.NO_MATCH;
        } catch (RuntimeException exception) {
            log.warn(
                "Could not evaluate the condition of component rule id={}", componentRule.getId(), exception);

            return componentRule.isStrict() ? MatchResult.STRICT_FALLBACK : MatchResult.NO_MATCH;
        }
    }

    private static Map<String, Object> buildEvaluationContext(ToolCall toolCall, @Nullable Object output) {
        Map<String, Object> evaluationContext = new HashMap<>();

        evaluationContext.put("inputParameters", toolCall.inputParameters());
        evaluationContext.put("componentName", toolCall.componentName());
        evaluationContext.put("toolName", toolCall.toolName());
        evaluationContext.put("toolCallName", toolCall.toolCallName());
        evaluationContext.put("connectionId", toolCall.connectionId());

        if (output != null) {
            evaluationContext.put("output", output);
        }

        return evaluationContext;
    }

    private static String buildApprovalDescription(
        List<MatchedComponentRule> matchingComponentRules, ToolCall toolCall) {

        String reasons = matchingComponentRules.stream()
            .map(MatchedComponentRule::componentRule)
            .filter(componentRule -> componentRule.getRuleAction() == RuleAction.REQUIRE_APPROVAL)
            .map(ComponentRuleEnforcerImpl::describeReason)
            .collect(Collectors.joining("; "));

        return "Tool '%s' of component '%s' requires approval: %s"
            .formatted(toolCall.toolName(), toolCall.componentName(), reasons);
    }

    private static String describeReason(ComponentRule componentRule) {
        String description = componentRule.getDescription();

        if (description == null || description.isBlank()) {
            return componentRule.getCondition();
        }

        return description;
    }

    private void publish(
        ComponentRuleAuditEvent componentRuleAuditEvent, MatchedComponentRule matchedComponentRule,
        ToolCall toolCall, RulePhase rulePhase) {

        publish(componentRuleAuditEvent, matchedComponentRule, toolCall, rulePhase, null);
    }

    private void publish(
        ComponentRuleAuditEvent componentRuleAuditEvent, MatchedComponentRule matchedComponentRule,
        ToolCall toolCall, RulePhase rulePhase, @Nullable String wouldHave) {

        ComponentRule componentRule = matchedComponentRule.componentRule();

        componentRuleAuditPublisher.publish(
            componentRuleAuditEvent,
            new ComponentRuleAuditPayload(
                componentRule.getId(), toolCall.componentName(), toolCall.toolName(), toolCall.toolCallName(),
                rulePhase.name(), toolCall.jobId(), toolCall.taskExecutionId(), null, wouldHave,
                matchedComponentRule.strictFallback()));
    }
}
