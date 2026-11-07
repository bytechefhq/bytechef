/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule.service;

import com.bytechef.ee.platform.component.rule.ComponentRule;
import com.bytechef.ee.platform.component.rule.ComponentRule.RuleAction;
import com.bytechef.ee.platform.component.rule.ComponentRule.RulePhase;
import com.bytechef.ee.platform.component.rule.ComponentRuleConditionStubContext;
import com.bytechef.ee.platform.component.rule.ComponentRuleErrorType;
import com.bytechef.ee.platform.component.rule.ComponentRuleService;
import com.bytechef.ee.platform.component.rule.repository.ComponentRuleRepository;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ComponentRuleServiceImpl implements ComponentRuleService {

    private static final String CONDITION_KEY = "condition";
    private static final String FORMULA_PREFIX = "=";

    private static final Logger log = LoggerFactory.getLogger(ComponentRuleServiceImpl.class);
    private static final AtomicBoolean NO_REPOSITORY_WARNED = new AtomicBoolean();

    private final @Nullable ComponentRuleRepository componentRuleRepository;
    private final Evaluator evaluator;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public ComponentRuleServiceImpl(
        ObjectProvider<ComponentRuleRepository> componentRuleRepositoryProvider, Evaluator evaluator) {

        this.componentRuleRepository = componentRuleRepositoryProvider.getIfAvailable();
        this.evaluator = evaluator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComponentRule> getComponentRules() {
        return StreamSupport.stream(
            requireRepository().findAll()
                .spliterator(),
            false)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComponentRule> getComponentRules(String componentName) {
        return requireRepository().findAllByComponentName(componentName);
    }

    @Override
    @Transactional(readOnly = true)
    public ComponentRule getComponentRule(long id) {
        return requireRepository().findById(id)
            .orElseThrow(
                () -> new ConfigurationException(
                    "Component rule with id " + id + " does not exist.",
                    ComponentRuleErrorType.COMPONENT_RULE_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComponentRule> getEnabledComponentRules(String componentName, @Nullable Long workspaceId) {
        if (componentRuleRepository == null) {
            if (NO_REPOSITORY_WARNED.compareAndSet(false, true)) {
                log.warn(
                    "No ComponentRuleRepository is available on this app (no local database) — component rules " +
                        "will not be enforced here until this app can reach the rule data remotely. Failing open.");
            }

            return List.of();
        }

        return componentRuleRepository.findAllEnabledForWorkspace(componentName, workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ComponentRule saveComponentRule(ComponentRule componentRule) {
        if (componentRule.getPhase() == RulePhase.AFTER && componentRule.getRuleAction() == RuleAction.BLOCK) {
            throw new ConfigurationException(
                "A rule evaluated in the AFTER phase cannot BLOCK — the action has already run. Use TAG instead.",
                ComponentRuleErrorType.BLOCK_AFTER_UNSUPPORTED);
        }

        if (componentRule.getPhase() == RulePhase.AFTER &&
            componentRule.getRuleAction() == RuleAction.REQUIRE_APPROVAL) {

            throw new ConfigurationException(
                "A rule evaluated in the AFTER phase cannot require approval — the tool has already run. Use TAG " +
                    "instead.",
                ComponentRuleErrorType.APPROVAL_AFTER_UNSUPPORTED);
        }

        validateCondition(componentRule.getCondition());

        Long id = componentRule.getId();

        ComponentRuleRepository repository = requireRepository();

        if (id == null) {
            return repository.save(componentRule);
        }

        ComponentRule persistedComponentRule = repository.findById(id)
            .orElseThrow(
                () -> new ConfigurationException(
                    "Component rule with id " + id + " does not exist.",
                    ComponentRuleErrorType.COMPONENT_RULE_NOT_FOUND));

        persistedComponentRule.setComponentName(componentRule.getComponentName());
        persistedComponentRule.setToolName(componentRule.getToolName());
        persistedComponentRule.setWorkspaceId(componentRule.getWorkspaceId());
        persistedComponentRule.setPhase(componentRule.getPhase());
        persistedComponentRule.setRuleAction(componentRule.getRuleAction());
        persistedComponentRule.setCondition(componentRule.getCondition());
        persistedComponentRule.setDescription(componentRule.getDescription());
        persistedComponentRule.setEnabled(componentRule.isEnabled());
        persistedComponentRule.setStrict(componentRule.isStrict());

        return repository.save(persistedComponentRule);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteComponentRule(long id) {
        requireRepository().deleteById(id);
    }

    private ComponentRuleRepository requireRepository() {
        if (componentRuleRepository == null) {
            throw new IllegalStateException(
                "No ComponentRuleRepository is available on this app (no local database); component rule " +
                    "authoring is only supported where the rule data is persisted.");
        }

        return componentRuleRepository;
    }

    private void validateCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            throw new ConfigurationException(
                "A rule condition must not be empty.", ComponentRuleErrorType.INVALID_CONDITION);
        }

        try {
            evaluator.evaluate(
                Map.of(CONDITION_KEY, FORMULA_PREFIX + condition), ComponentRuleConditionStubContext.get(), false);
        } catch (RuntimeException exception) {
            throw new ConfigurationException(
                "The rule condition is not a valid expression: " + exception.getMessage(), exception,
                ComponentRuleErrorType.INVALID_CONDITION);
        }
    }
}
