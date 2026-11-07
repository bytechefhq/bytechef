/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.RiskLevel;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.ee.platform.component.rule.ComponentRule;
import com.bytechef.ee.platform.component.rule.ComponentRule.RuleAction;
import com.bytechef.ee.platform.component.rule.ComponentRule.RulePhase;
import com.bytechef.ee.platform.component.rule.ComponentRuleService;
import com.bytechef.ee.platform.component.rule.ComponentRuleSettings;
import com.bytechef.ee.platform.component.rule.ComponentRuleSettingsService;
import com.bytechef.ee.platform.component.rule.web.graphql.ComponentRuleGraphQlController.ComponentRuleItem;
import com.bytechef.ee.platform.component.rule.web.graphql.ComponentRuleGraphQlController.ComponentRuleSettingsItem;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 * @author Ivica Cardic
 */
class ComponentRuleGraphQlControllerTest {

    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final ComponentRuleService componentRuleService = mock(ComponentRuleService.class);
    private final ComponentRuleSettingsService componentRuleSettingsService = mock(ComponentRuleSettingsService.class);
    private final ComponentRuleGraphQlController controller = new ComponentRuleGraphQlController(
        clusterElementDefinitionService, componentDefinitionService, componentRuleService,
        componentRuleSettingsService);

    @BeforeEach
    void authenticateAsTenantAdmin() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "admin", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testComponentRulesWithoutAComponentNameListsEveryRule() {
        ComponentDefinition componentDefinition = slackComponentDefinition();

        when(componentRuleService.getComponentRules()).thenReturn(List.of(newComponentRule(1L)));
        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(componentDefinition));

        List<ComponentRuleItem> result = controller.componentRules(null, null);

        assertThat(result).hasSize(1);

        ComponentRuleItem componentRuleItem = result.getFirst();

        assertThat(componentRuleItem.id()).isEqualTo("1");
        assertThat(componentRuleItem.componentName()).isEqualTo("slack");
        assertThat(componentRuleItem.componentTitle()).isEqualTo("Slack");
        assertThat(componentRuleItem.toolName()).isEqualTo("sendMessage");
        assertThat(componentRuleItem.phase()).isEqualTo(RulePhase.BEFORE);
        assertThat(componentRuleItem.ruleAction()).isEqualTo(RuleAction.TAG);
        assertThat(componentRuleItem.enabled()).isTrue();
    }

    @Test
    void testComponentRulesWithAComponentNameNarrows() {
        ComponentDefinition componentDefinition = slackComponentDefinition();

        when(componentRuleService.getComponentRules("slack")).thenReturn(List.of(newComponentRule(1L)));
        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(componentDefinition));

        assertThat(controller.componentRules("slack", null)).hasSize(1);

        verify(componentRuleService).getComponentRules("slack");
    }

    @Test
    void testComponentRulesWithWorkspaceIdReturnsWorkspaceScopedAndTenantWideRules() {
        ComponentDefinition componentDefinition = slackComponentDefinition();

        ComponentRule workspace42Rule = newComponentRule(1L);

        workspace42Rule.setWorkspaceId(42L);

        ComponentRule workspace7Rule = newComponentRule(2L);

        workspace7Rule.setWorkspaceId(7L);

        ComponentRule tenantWideRule = newComponentRule(3L);

        when(componentRuleService.getComponentRules())
            .thenReturn(List.of(workspace42Rule, workspace7Rule, tenantWideRule));
        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(componentDefinition));

        List<ComponentRuleItem> result = controller.componentRules(null, "42");

        assertThat(result)
            .extracting(ComponentRuleItem::id)
            .containsExactlyInAnyOrder("1", "3");
    }

    @Test
    void testComponentRuleWithoutToolNameHasNullToolRiskLevel() {
        ComponentDefinition componentDefinition = slackComponentDefinition();
        ComponentRule componentRule = newComponentRule(1L);

        componentRule.setToolName(null);

        when(componentRuleService.getComponentRules()).thenReturn(List.of(componentRule));
        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(componentDefinition));

        List<ComponentRuleItem> result = controller.componentRules(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()
            .toolRiskLevel()).isNull();

        verifyNoInteractions(clusterElementDefinitionService);
    }

    @Test
    void testComponentRuleWithKnownToolResolvesRiskLevelFromClusterElementDefinition() {
        ComponentDefinition componentDefinition = slackComponentDefinition();
        ComponentRule componentRule = newComponentRule(1L);

        componentRule.setStrict(true);

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getRiskLevel()).thenReturn(RiskLevel.CRITICAL);
        when(componentRuleService.getComponentRules()).thenReturn(List.of(componentRule));
        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(componentDefinition));
        when(clusterElementDefinitionService.getClusterElementDefinition("slack", 1, "sendMessage",
            BaseToolFunction.TOOLS.name()))
                .thenReturn(clusterElementDefinition);

        List<ComponentRuleItem> result = controller.componentRules(null, null);

        assertThat(result).hasSize(1);

        ComponentRuleItem componentRuleItem = result.getFirst();

        assertThat(componentRuleItem.toolRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(componentRuleItem.strict()).isTrue();
    }

    @Test
    void testComponentRuleFallsBackToNameInferredRiskLevelWhenToolIsMissing() {
        ComponentDefinition componentDefinition = slackComponentDefinition();
        ComponentRule componentRule = newComponentRule(1L);

        when(componentRuleService.getComponentRules()).thenReturn(List.of(componentRule));
        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(componentDefinition));
        when(clusterElementDefinitionService.getClusterElementDefinition("slack", 1, "sendMessage",
            BaseToolFunction.TOOLS.name()))
                .thenThrow(new IllegalStateException("slack no longer ships sendMessage"));

        List<ComponentRuleItem> result = controller.componentRules(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()
            .toolRiskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void testSaveComponentRuleWithoutAnIdCreates() {
        ComponentDefinition componentDefinition = slackComponentDefinition();

        when(componentRuleService.saveComponentRule(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(componentDefinition));

        controller.saveComponentRule(
            null, "slack", "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "inputParameters['x'] == 1",
            "block when x is one", true, false, "1");

        ArgumentCaptor<ComponentRule> componentRuleCaptor = ArgumentCaptor.forClass(ComponentRule.class);

        verify(componentRuleService).saveComponentRule(componentRuleCaptor.capture());

        ComponentRule componentRule = componentRuleCaptor.getValue();

        assertThat(componentRule.getId()).isNull();
        assertThat(componentRule.getComponentName()).isEqualTo("slack");
        assertThat(componentRule.getCondition()).isEqualTo("inputParameters['x'] == 1");
        assertThat(componentRule.getDescription()).isEqualTo("block when x is one");
    }

    @Test
    void testSaveComponentRuleWithAnIdUpdatesInPlace() {
        ComponentDefinition componentDefinition = slackComponentDefinition();

        when(componentRuleService.saveComponentRule(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(componentDefinition));

        controller.saveComponentRule(
            "5", "slack", null, RulePhase.AFTER, RuleAction.TAG, "output['ok'] == false", null, false, false, "1");

        ArgumentCaptor<ComponentRule> componentRuleCaptor = ArgumentCaptor.forClass(ComponentRule.class);

        verify(componentRuleService).saveComponentRule(componentRuleCaptor.capture());

        ComponentRule componentRule = componentRuleCaptor.getValue();

        assertThat(componentRule.getId()).isEqualTo(5L);
        assertThat(componentRule.getToolName()).isNull();
        assertThat(componentRule.getPhase()).isEqualTo(RulePhase.AFTER);
        assertThat(componentRule.isEnabled()).isFalse();
    }

    @Test
    void testSaveComponentRuleCarriesStrictAndRequireApproval() {
        ComponentDefinition componentDefinition = slackComponentDefinition();

        when(componentRuleService.saveComponentRule(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(componentDefinition));

        controller.saveComponentRule(
            null, "slack", "sendMessage", RulePhase.BEFORE, RuleAction.REQUIRE_APPROVAL,
            "inputParameters['x'] == 1", "approve when x is one", true, true, "1");

        ArgumentCaptor<ComponentRule> componentRuleCaptor = ArgumentCaptor.forClass(ComponentRule.class);

        verify(componentRuleService).saveComponentRule(componentRuleCaptor.capture());

        ComponentRule componentRule = componentRuleCaptor.getValue();

        assertThat(componentRule.getRuleAction()).isEqualTo(RuleAction.REQUIRE_APPROVAL);
        assertThat(componentRule.isStrict()).isTrue();
    }

    @Test
    void testSaveComponentRuleCarriesTheWorkspace() {
        ComponentDefinition componentDefinition = slackComponentDefinition();

        when(componentRuleService.saveComponentRule(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(componentDefinition));

        controller.saveComponentRule(
            null, "slack", "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "true", null, true, false, "42");

        ArgumentCaptor<ComponentRule> componentRuleCaptor = ArgumentCaptor.forClass(ComponentRule.class);

        verify(componentRuleService).saveComponentRule(componentRuleCaptor.capture());

        assertThat(componentRuleCaptor.getValue()
            .getWorkspaceId()).isEqualTo(42L);
    }

    @Test
    void testANullWorkspaceSavesATenantWideRule() {
        ComponentDefinition componentDefinition = slackComponentDefinition();

        when(componentRuleService.saveComponentRule(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(componentDefinition));

        controller.saveComponentRule(
            null, "slack", "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "true", null, true, false, null);

        ArgumentCaptor<ComponentRule> componentRuleCaptor = ArgumentCaptor.forClass(ComponentRule.class);

        verify(componentRuleService).saveComponentRule(componentRuleCaptor.capture());

        assertThat(componentRuleCaptor.getValue()
            .getWorkspaceId()).isNull();
    }

    @Test
    void testClearingAnExistingRuleWorkspaceWidensItToEveryWorkspace() {
        ComponentDefinition componentDefinition = slackComponentDefinition();

        when(componentRuleService.saveComponentRule(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(componentDefinition));

        controller.saveComponentRule(
            "5", "slack", "sendMessage", RulePhase.BEFORE, RuleAction.BLOCK, "true", null, true, false, null);

        ArgumentCaptor<ComponentRule> componentRuleCaptor = ArgumentCaptor.forClass(ComponentRule.class);

        verify(componentRuleService).saveComponentRule(componentRuleCaptor.capture());

        assertThat(componentRuleCaptor.getValue()
            .getWorkspaceId()).isNull();
    }

    @Test
    void testComponentRuleSettingsReadsTheRequestedWorkspace() {
        when(componentRuleSettingsService.getSettings(42L)).thenReturn(new ComponentRuleSettings(true, 24));
        when(componentRuleSettingsService.fetchWorkspaceOverride(42L)).thenReturn(Optional.empty());

        ComponentRuleSettingsItem componentRuleSettingsItem = controller.componentRuleSettings("42");

        assertThat(componentRuleSettingsItem.observeMode()).isTrue();
        assertThat(componentRuleSettingsItem.approvalExpiresInHours()).isEqualTo(24);
    }

    @Test
    void testComponentRuleSettingsIsInheritedWhenTheWorkspaceHasNoOverrideOfItsOwn() {
        when(componentRuleSettingsService.getSettings(42L)).thenReturn(new ComponentRuleSettings(true, 24));
        when(componentRuleSettingsService.fetchWorkspaceOverride(42L)).thenReturn(Optional.empty());

        assertThat(controller.componentRuleSettings("42")
            .inherited()).isTrue();
    }

    @Test
    void testComponentRuleSettingsIsNotInheritedWhenTheWorkspaceOverrideEqualsTheTenantDefault() {
        ComponentRuleSettings settingsThatHappenToMatchTheTenantDefault = new ComponentRuleSettings(true, 24);

        when(componentRuleSettingsService.getSettings(42L)).thenReturn(settingsThatHappenToMatchTheTenantDefault);
        when(componentRuleSettingsService.fetchWorkspaceOverride(42L))
            .thenReturn(Optional.of(settingsThatHappenToMatchTheTenantDefault));

        assertThat(controller.componentRuleSettings("42")
            .inherited()).isFalse();
    }

    @Test
    void testComponentRuleSettingsIsNeverInheritedForANullWorkspace() {
        when(componentRuleSettingsService.getSettings(null)).thenReturn(new ComponentRuleSettings(true, 24));

        assertThat(controller.componentRuleSettings(null)
            .inherited()).isFalse();

        verify(componentRuleSettingsService, never()).fetchWorkspaceOverride(any());
    }

    @Test
    void testComponentRuleSettingsRoundTrip() {
        when(componentRuleSettingsService.getSettings(null)).thenReturn(new ComponentRuleSettings(true, 24));

        assertThat(controller.componentRuleSettings(null)
            .observeMode()).isTrue();

        when(componentRuleSettingsService.saveSettings(new ComponentRuleSettings(false, 12), null))
            .thenReturn(new ComponentRuleSettings(false, 12));

        ComponentRuleSettingsItem updated = controller.updateComponentRuleSettings(false, 12, null);

        verify(componentRuleSettingsService).saveSettings(new ComponentRuleSettings(false, 12), null);

        assertThat(updated.observeMode()).isFalse();
        assertThat(updated.approvalExpiresInHours()).isEqualTo(12);
    }

    @Test
    void testUpdateComponentRuleSettingsIsNeverInherited() {
        when(componentRuleSettingsService.saveSettings(new ComponentRuleSettings(true, 6), 42L))
            .thenReturn(new ComponentRuleSettings(true, 6));

        assertThat(controller.updateComponentRuleSettings(true, 6, "42")
            .inherited()).isFalse();
    }

    @Test
    void testDeleteComponentRuleDelegates() {
        ComponentRule workspaceScopedRule = newComponentRule(5L);

        workspaceScopedRule.setWorkspaceId(42L);

        when(componentRuleService.getComponentRule(5L)).thenReturn(workspaceScopedRule);

        assertThat(controller.deleteComponentRule("5")).isTrue();

        verify(componentRuleService).deleteComponentRule(5L);
    }

    @Test
    void testDeletingATenantWideRuleSucceeds() {
        when(componentRuleService.getComponentRule(5L)).thenReturn(newComponentRule(5L));

        assertThat(controller.deleteComponentRule("5")).isTrue();

        verify(componentRuleService).deleteComponentRule(5L);
    }

    private static ComponentDefinition slackComponentDefinition() {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getName()).thenReturn("slack");
        when(componentDefinition.getTitle()).thenReturn("Slack");
        when(componentDefinition.getIcon()).thenReturn("slack.svg");
        when(componentDefinition.getVersion()).thenReturn(1);

        return componentDefinition;
    }

    private static ComponentRule newComponentRule(long id) {
        ComponentRule componentRule = new ComponentRule();

        componentRule.setId(id);
        componentRule.setComponentName("slack");
        componentRule.setToolName("sendMessage");
        componentRule.setPhase(RulePhase.BEFORE);
        componentRule.setRuleAction(RuleAction.TAG);
        componentRule.setCondition("true");
        componentRule.setEnabled(true);

        return componentRule;
    }
}
