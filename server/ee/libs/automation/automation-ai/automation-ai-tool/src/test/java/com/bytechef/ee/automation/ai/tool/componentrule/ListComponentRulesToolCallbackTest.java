/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.componentrule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.platform.component.rule.ComponentRule;
import com.bytechef.ee.platform.component.rule.ComponentRule.RuleAction;
import com.bytechef.ee.platform.component.rule.ComponentRule.RulePhase;
import com.bytechef.ee.platform.component.rule.ComponentRuleService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 * @author Ivica Cardic
 */
class ListComponentRulesToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();
    private final ComponentRuleService componentRuleService = mock(ComponentRuleService.class);
    private final ListComponentRulesToolCallback toolCallback =
        new ListComponentRulesToolCallback(componentRuleService);

    private static ToolContext toolContext(long workspaceId) {
        return new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .build()
                .toToolContext());
    }

    private static ComponentRule rule(long id, String componentName, Long workspaceId) {
        ComponentRule componentRule = new ComponentRule();

        componentRule.setId(id);
        componentRule.setComponentName(componentName);
        componentRule.setToolName("sendMessage");
        componentRule.setWorkspaceId(workspaceId);
        componentRule.setPhase(RulePhase.BEFORE);
        componentRule.setRuleAction(RuleAction.TAG);
        componentRule.setCondition("true");
        componentRule.setEnabled(true);

        return componentRule;
    }

    private static List<Long> ids(JsonNode arrayNode) {
        List<Long> ids = new ArrayList<>();

        for (JsonNode item : arrayNode) {
            ids.add(item.get("id")
                .asLong());
        }

        return ids;
    }

    @Test
    void testCallReturnsTheSessionWorkspaceRulesPlusTenantWideAndExcludesOtherWorkspaces() throws Exception {
        ComponentRule ownWorkspaceRule = rule(1L, "slack", 7L);
        ComponentRule tenantWideRule = rule(2L, "slack", null);
        ComponentRule otherWorkspaceRule = rule(3L, "slack", 8L);

        when(componentRuleService.getComponentRules())
            .thenReturn(List.of(ownWorkspaceRule, tenantWideRule, otherWorkspaceRule));

        String result = toolCallback.call("{}", toolContext(7L));

        assertThat(ids(jsonMapper.readTree(result))).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void testCallNarrowsToTenantWideOnlyWhenWorkspaceIsUnresolvable() throws Exception {
        ComponentRule workspaceRule = rule(1L, "slack", 7L);
        ComponentRule tenantWideRule = rule(2L, "slack", null);

        when(componentRuleService.getComponentRules()).thenReturn(List.of(workspaceRule, tenantWideRule));

        String result = toolCallback.call("{}");

        assertThat(ids(jsonMapper.readTree(result))).containsExactly(2L);
    }

    @Test
    void testCallSummaryCarriesWorkspaceIdSoTheModelCanTellRulesApart() throws Exception {
        when(componentRuleService.getComponentRules()).thenReturn(List.of(rule(1L, "slack", 7L)));

        String result = toolCallback.call("{}", toolContext(7L));

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get(0)
            .get("workspaceId")
            .asLong()).isEqualTo(7L);
    }

    @Test
    void testCallSummaryWorkspaceIdIsNullForATenantWideRule() throws Exception {
        when(componentRuleService.getComponentRules()).thenReturn(List.of(rule(1L, "slack", null)));

        String result = toolCallback.call("{}", toolContext(7L));

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get(0)
            .get("workspaceId")
            .isNull()).isTrue();
    }
}
