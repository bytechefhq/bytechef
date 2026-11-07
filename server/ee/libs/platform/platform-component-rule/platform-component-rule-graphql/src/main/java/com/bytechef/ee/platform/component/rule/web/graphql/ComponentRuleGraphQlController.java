/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule.web.graphql;

import com.bytechef.component.definition.RiskLevel;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.ee.platform.component.rule.ComponentRule;
import com.bytechef.ee.platform.component.rule.ComponentRule.RuleAction;
import com.bytechef.ee.platform.component.rule.ComponentRule.RulePhase;
import com.bytechef.ee.platform.component.rule.ComponentRuleService;
import com.bytechef.ee.platform.component.rule.ComponentRuleSettings;
import com.bytechef.ee.platform.component.rule.ComponentRuleSettingsService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.rule.ToolRiskLevelResolver;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
public class ComponentRuleGraphQlController {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ComponentDefinitionService componentDefinitionService;
    private final ComponentRuleService componentRuleService;
    private final ComponentRuleSettingsService componentRuleSettingsService;

    @SuppressFBWarnings("EI2")
    public ComponentRuleGraphQlController(
        ClusterElementDefinitionService clusterElementDefinitionService,
        ComponentDefinitionService componentDefinitionService, ComponentRuleService componentRuleService,
        ComponentRuleSettingsService componentRuleSettingsService) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.componentDefinitionService = componentDefinitionService;
        this.componentRuleService = componentRuleService;
        this.componentRuleSettingsService = componentRuleSettingsService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<ComponentRuleItem> componentRules(
        @Argument @Nullable String componentName, @Argument @Nullable String workspaceId) {

        List<ComponentRule> componentRules = componentName == null
            ? componentRuleService.getComponentRules()
            : componentRuleService.getComponentRules(componentName);

        if (componentRules.isEmpty()) {
            return List.of();
        }

        Long resolvedWorkspaceId = parseId(workspaceId);

        if (resolvedWorkspaceId != null) {
            componentRules = componentRules.stream()
                .filter(componentRule -> componentRule.appliesToWorkspace(resolvedWorkspaceId))
                .toList();
        }

        if (componentRules.isEmpty()) {
            return List.of();
        }

        Map<String, ComponentDefinition> componentDefinitionsByName = getComponentDefinitionsByName();

        return componentRules.stream()
            .map(componentRule -> toItem(componentRule, componentDefinitionsByName))
            .sorted(
                Comparator.comparing(ComponentRuleItem::sortKey, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(ComponentRuleItem::id))
            .toList();
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ComponentRuleItem saveComponentRule(
        @Argument @Nullable String id, @Argument String componentName, @Argument @Nullable String toolName,
        @Argument RulePhase phase, @Argument RuleAction ruleAction, @Argument String condition,
        @Argument @Nullable String description, @Argument boolean enabled, @Argument boolean strict,
        @Argument @Nullable String workspaceId) {

        Long resolvedWorkspaceId = parseId(workspaceId);

        ComponentRule componentRule = new ComponentRule();

        componentRule.setId(parseId(id));
        componentRule.setComponentName(componentName);
        componentRule.setToolName(toolName);
        componentRule.setWorkspaceId(resolvedWorkspaceId);
        componentRule.setPhase(phase);
        componentRule.setRuleAction(ruleAction);
        componentRule.setCondition(condition);
        componentRule.setDescription(description);
        componentRule.setEnabled(enabled);
        componentRule.setStrict(strict);

        ComponentRule savedComponentRule = componentRuleService.saveComponentRule(componentRule);

        return toItem(savedComponentRule, getComponentDefinitionsByName());
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteComponentRule(@Argument String id) {
        long parsedId = Long.parseLong(id);

        componentRuleService.deleteComponentRule(parsedId);

        return true;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ComponentRuleSettingsItem componentRuleSettings(@Argument @Nullable String workspaceId) {
        Long resolvedWorkspaceId = parseId(workspaceId);

        ComponentRuleSettings componentRuleSettings = componentRuleSettingsService.getSettings(resolvedWorkspaceId);

        boolean inherited = resolvedWorkspaceId != null
            && componentRuleSettingsService.fetchWorkspaceOverride(resolvedWorkspaceId)
                .isEmpty();

        return toSettingsItem(componentRuleSettings, inherited);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ComponentRuleSettingsItem updateComponentRuleSettings(
        @Argument boolean observeMode, @Argument int approvalExpiresInHours,
        @Argument @Nullable String workspaceId) {

        ComponentRuleSettings savedComponentRuleSettings = componentRuleSettingsService.saveSettings(
            new ComponentRuleSettings(observeMode, approvalExpiresInHours), parseId(workspaceId));

        return toSettingsItem(savedComponentRuleSettings, false);
    }

    private Map<String, ComponentDefinition> getComponentDefinitionsByName() {
        return componentDefinitionService.getComponentDefinitions()
            .stream()
            .collect(
                Collectors.toMap(
                    ComponentDefinition::getName, Function.identity(),
                    ComponentRuleGraphQlController::preferHighestVersion));
    }

    private static ComponentDefinition preferHighestVersion(
        ComponentDefinition firstComponentDefinition, ComponentDefinition secondComponentDefinition) {

        return firstComponentDefinition.getVersion() >= secondComponentDefinition.getVersion()
            ? firstComponentDefinition
            : secondComponentDefinition;
    }

    @Nullable
    private static Long parseId(@Nullable String id) {
        return id == null || id.isBlank() ? null : Long.parseLong(id);
    }

    private static ComponentRuleSettingsItem toSettingsItem(
        ComponentRuleSettings componentRuleSettings, boolean inherited) {

        return new ComponentRuleSettingsItem(
            componentRuleSettings.observeMode(), componentRuleSettings.approvalExpiresInHours(), inherited);
    }

    private ComponentRuleItem toItem(
        ComponentRule componentRule, Map<String, ComponentDefinition> componentDefinitionsByName) {

        ComponentDefinition componentDefinition = componentDefinitionsByName.get(componentRule.getComponentName());

        return new ComponentRuleItem(
            String.valueOf(componentRule.getId()), componentRule.getComponentName(),
            componentDefinition == null ? null : componentDefinition.getTitle(),
            componentDefinition == null ? null : componentDefinition.getIcon(), componentRule.getToolName(),
            toToolRiskLevel(componentRule, componentDefinition), componentRule.getPhase(),
            componentRule.getRuleAction(), componentRule.getCondition(), componentRule.getDescription(),
            componentRule.isEnabled(), componentRule.isStrict(),
            componentRule.getWorkspaceId() == null ? null : String.valueOf(componentRule.getWorkspaceId()));
    }

    @Nullable
    private RiskLevel toToolRiskLevel(
        ComponentRule componentRule, @Nullable ComponentDefinition componentDefinition) {

        String toolName = componentRule.getToolName();

        if (toolName == null) {
            return null;
        }

        if (componentDefinition == null) {
            return ToolRiskLevelResolver.resolve(null, toolName);
        }

        try {
            ClusterElementDefinition clusterElementDefinition =
                clusterElementDefinitionService.getClusterElementDefinition(
                    componentRule.getComponentName(), componentDefinition.getVersion(), toolName,
                    BaseToolFunction.TOOLS.name());

            return clusterElementDefinition.getRiskLevel();
        } catch (RuntimeException exception) {
            return ToolRiskLevelResolver.resolve(null, toolName);
        }
    }

    public record ComponentRuleItem(
        String id, String componentName, @Nullable String componentTitle, @Nullable String componentIcon,
        @Nullable String toolName, @Nullable RiskLevel toolRiskLevel, RulePhase phase, RuleAction ruleAction,
        String condition, @Nullable String description, boolean enabled, boolean strict,
        @Nullable String workspaceId) {

        String sortKey() {
            return componentTitle == null ? componentName : componentTitle;
        }
    }

    public record ComponentRuleSettingsItem(boolean observeMode, int approvalExpiresInHours, boolean inherited) {
    }
}
