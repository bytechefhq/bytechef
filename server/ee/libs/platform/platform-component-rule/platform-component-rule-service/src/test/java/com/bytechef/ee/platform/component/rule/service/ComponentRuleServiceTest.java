/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.component.rule.ComponentRule;
import com.bytechef.ee.platform.component.rule.ComponentRule.RuleAction;
import com.bytechef.ee.platform.component.rule.ComponentRule.RulePhase;
import com.bytechef.ee.platform.component.rule.ComponentRuleErrorType;
import com.bytechef.ee.platform.component.rule.repository.ComponentRuleRepository;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.exception.ConfigurationException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 * @author Ivica Cardic
 */
final class ComponentRuleServiceTest {

    private final ComponentRuleRepository componentRuleRepository = mock(ComponentRuleRepository.class);
    private final Evaluator evaluator = SpelEvaluator.builder()
        .build();
    private final ComponentRuleServiceImpl componentRuleService =
        new ComponentRuleServiceImpl(objectProvider(componentRuleRepository), evaluator);

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ComponentRuleRepository> objectProvider(
        ComponentRuleRepository componentRuleRepository) {

        ObjectProvider<ComponentRuleRepository> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(componentRuleRepository);

        return objectProvider;
    }

    @Test
    void testSaveRejectsBlockInAfterPhase() {
        ComponentRule componentRule = newComponentRule(RulePhase.AFTER, RuleAction.BLOCK, "output['ok'] == false");

        assertThatThrownBy(() -> componentRuleService.saveComponentRule(componentRule))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("AFTER");

        verify(componentRuleRepository, never()).save(any());
    }

    @Test
    void testSaveRejectsUnparseableCondition() {
        ComponentRule componentRule = newComponentRule(RulePhase.BEFORE, RuleAction.BLOCK, "inputParameters[[[");

        assertThatThrownBy(() -> componentRuleService.saveComponentRule(componentRule))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("condition");

        verify(componentRuleRepository, never()).save(any());
    }

    @Test
    void testSaveRejectsConditionCallingAJavaMethod() {
        ComponentRule componentRule = newComponentRule(
            RulePhase.BEFORE, RuleAction.TAG, "inputParameters['channel'].startsWith('C05')");

        assertThatThrownBy(() -> componentRuleService.saveComponentRule(componentRule))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("condition");
    }

    @Test
    void testSaveAcceptsAnEvaluatorFunctionCondition() {
        ComponentRule componentRule = newComponentRule(
            RulePhase.BEFORE, RuleAction.TAG, "contains(inputParameters['channel'], 'C05')");

        when(componentRuleRepository.save(componentRule)).thenReturn(componentRule);

        assertThat(componentRuleService.saveComponentRule(componentRule)).isSameAs(componentRule);
    }

    @Test
    void testSaveAcceptsBlockInBeforePhase() {
        ComponentRule componentRule = newComponentRule(
            RulePhase.BEFORE, RuleAction.BLOCK, "inputParameters['recordId'] != null");

        when(componentRuleRepository.save(componentRule)).thenReturn(componentRule);

        assertThat(componentRuleService.saveComponentRule(componentRule)).isSameAs(componentRule);
    }

    @Test
    void testSaveWithAnIdLoadsAndMutatesThePersistedInstanceRatherThanTheDetachedOne() {
        ComponentRule persistedComponentRule = newComponentRule(RulePhase.BEFORE, RuleAction.BLOCK, "true");

        persistedComponentRule.setId(5L);

        when(componentRuleRepository.findById(5L)).thenReturn(Optional.of(persistedComponentRule));
        when(componentRuleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ComponentRule detachedComponentRule = newComponentRule(
            RulePhase.BEFORE, RuleAction.TAG, "contains(inputParameters['channel'], 'C05')");

        detachedComponentRule.setId(5L);
        detachedComponentRule.setDescription("updated");
        detachedComponentRule.setEnabled(false);

        ComponentRule result = componentRuleService.saveComponentRule(detachedComponentRule);

        ArgumentCaptor<ComponentRule> savedComponentRuleCaptor = ArgumentCaptor.forClass(ComponentRule.class);

        verify(componentRuleRepository).save(savedComponentRuleCaptor.capture());

        assertThat(savedComponentRuleCaptor.getValue()).isSameAs(persistedComponentRule);
        assertThat(result).isSameAs(persistedComponentRule);
        assertThat(persistedComponentRule.getRuleAction()).isEqualTo(RuleAction.TAG);
        assertThat(persistedComponentRule.getCondition()).isEqualTo("contains(inputParameters['channel'], 'C05')");
        assertThat(persistedComponentRule.getDescription()).isEqualTo("updated");
        assertThat(persistedComponentRule.isEnabled()).isFalse();
    }

    @Test
    void testSaveWithAnUnknownIdThrowsRatherThanInserting() {
        ComponentRule componentRule = newComponentRule(RulePhase.BEFORE, RuleAction.TAG, "true");

        componentRule.setId(999L);

        when(componentRuleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> componentRuleService.saveComponentRule(componentRule))
            .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> {
                assertThat(configurationException.getEntityClass()).isEqualTo(ComponentRule.class);
                assertThat(configurationException.getErrorKey())
                    .isEqualTo(ComponentRuleErrorType.COMPONENT_RULE_NOT_FOUND.getErrorKey());
            });

        verify(componentRuleRepository, never()).save(any());
    }

    @Test
    void testSaveRejectsAConditionCallingAnUnknownFunction() {
        ComponentRule componentRule = newComponentRule(
            RulePhase.BEFORE, RuleAction.TAG, "frobnicate(inputParameters['channel'], 'C05')");

        assertThatThrownBy(() -> componentRuleService.saveComponentRule(componentRule))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("condition");

        verify(componentRuleRepository, never()).save(any());
    }

    @Test
    void testGetEnabledComponentRulesReturnsTheWorkspacesRulesAndTheTenantWideOnes() {
        ComponentRule tenantWideComponentRule = newComponentRule(RulePhase.BEFORE, RuleAction.TAG, "true");
        ComponentRule workspaceComponentRule = newComponentRule(RulePhase.BEFORE, RuleAction.BLOCK, "true");

        workspaceComponentRule.setWorkspaceId(42L);

        when(componentRuleRepository.findAllEnabledForWorkspace("slack", 42L))
            .thenReturn(List.of(tenantWideComponentRule, workspaceComponentRule));

        assertThat(componentRuleService.getEnabledComponentRules("slack", 42L))
            .containsExactly(tenantWideComponentRule, workspaceComponentRule);
    }

    @Test
    void testANullWorkspaceReturnsOnlyTheTenantWideRules() {
        ComponentRule tenantWideComponentRule = newComponentRule(RulePhase.BEFORE, RuleAction.TAG, "true");

        when(componentRuleRepository.findAllEnabledForWorkspace("slack", null))
            .thenReturn(List.of(tenantWideComponentRule));

        assertThat(componentRuleService.getEnabledComponentRules("slack", null))
            .containsExactly(tenantWideComponentRule);
    }

    @Test
    void testDeleteDelegatesToRepository() {
        componentRuleService.deleteComponentRule(7L);

        verify(componentRuleRepository).deleteById(7L);
    }

    @Test
    void testGetComponentRuleReturnsTheStoredRule() {
        ComponentRule persistedComponentRule = newComponentRule(RulePhase.BEFORE, RuleAction.BLOCK, "true");

        persistedComponentRule.setId(5L);

        when(componentRuleRepository.findById(5L)).thenReturn(Optional.of(persistedComponentRule));

        assertThat(componentRuleService.getComponentRule(5L)).isEqualTo(persistedComponentRule);
    }

    @Test
    void testGetComponentRuleThrowsWhenTheIdDoesNotExist() {
        when(componentRuleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> componentRuleService.getComponentRule(999L))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                configurationException -> assertThat(configurationException.getErrorKey())
                    .isEqualTo(ComponentRuleErrorType.COMPONENT_RULE_NOT_FOUND.getErrorKey()));
    }

    @Test
    void testGetEnabledComponentRulesFailsOpenToAnEmptyListWhenNoRepositoryIsAvailable() {
        ComponentRuleServiceImpl componentRuleServiceWithNoRepository =
            new ComponentRuleServiceImpl(objectProvider(null), evaluator);

        assertThat(componentRuleServiceWithNoRepository.getEnabledComponentRules("slack", null)).isEmpty();
    }

    @Test
    void testGetComponentRulesThrowsWhenNoRepositoryIsAvailable() {
        ComponentRuleServiceImpl componentRuleServiceWithNoRepository =
            new ComponentRuleServiceImpl(objectProvider(null), evaluator);

        assertThatThrownBy(componentRuleServiceWithNoRepository::getComponentRules)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testErrorTypeOnBlockAfterRejection() {
        ComponentRule componentRule = newComponentRule(RulePhase.AFTER, RuleAction.BLOCK, "true");

        assertThatThrownBy(() -> componentRuleService.saveComponentRule(componentRule))
            .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> {
                assertThat(configurationException.getEntityClass()).isEqualTo(ComponentRule.class);
                assertThat(configurationException.getErrorKey()).isEqualTo(100);
            });
    }

    @Test
    void testSaveRejectsRequireApprovalInAfterPhase() {
        ComponentRule componentRule = newComponentRule(RulePhase.AFTER, RuleAction.REQUIRE_APPROVAL, "true");

        assertThatThrownBy(() -> componentRuleService.saveComponentRule(componentRule))
            .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> {
                assertThat(configurationException.getEntityClass()).isEqualTo(ComponentRule.class);
                assertThat(configurationException.getErrorKey())
                    .isEqualTo(ComponentRuleErrorType.APPROVAL_AFTER_UNSUPPORTED.getErrorKey());
            });

        verify(componentRuleRepository, never()).save(any());
    }

    @Test
    void testSaveCarriesTheStrictFlagOntoThePersistedInstance() {
        ComponentRule persistedComponentRule = newComponentRule(RulePhase.BEFORE, RuleAction.BLOCK, "true");

        persistedComponentRule.setId(5L);

        when(componentRuleRepository.findById(5L)).thenReturn(Optional.of(persistedComponentRule));
        when(componentRuleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ComponentRule detachedComponentRule = newComponentRule(RulePhase.BEFORE, RuleAction.BLOCK, "true");

        detachedComponentRule.setId(5L);
        detachedComponentRule.setStrict(true);

        componentRuleService.saveComponentRule(detachedComponentRule);

        assertThat(persistedComponentRule.isStrict()).isTrue();
    }

    private static ComponentRule newComponentRule(RulePhase rulePhase, RuleAction ruleAction, String condition) {
        ComponentRule componentRule = new ComponentRule();

        componentRule.setComponentName("slack");
        componentRule.setToolName("sendMessage");
        componentRule.setPhase(rulePhase);
        componentRule.setRuleAction(ruleAction);
        componentRule.setCondition(condition);
        componentRule.setEnabled(true);

        return componentRule;
    }
}
