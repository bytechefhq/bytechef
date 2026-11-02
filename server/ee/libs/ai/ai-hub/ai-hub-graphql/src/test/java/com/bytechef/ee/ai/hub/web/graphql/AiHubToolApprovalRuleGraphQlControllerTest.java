/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.approval.AiHubToolApproval;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalRule;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalRuleFacade;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubToolApprovalRuleGraphQlControllerTest {

    @Test
    void testAiHubToolApprovalRulesDelegatesToFacade() {
        AiHubToolApprovalRuleFacade toolApprovalRuleFacade = mock(AiHubToolApprovalRuleFacade.class);

        AiHubToolApprovalRule rule = mock(AiHubToolApprovalRule.class);

        when(toolApprovalRuleFacade.getRules(7L)).thenReturn(List.of(rule));

        AiHubToolApprovalRuleGraphQlController controller =
            new AiHubToolApprovalRuleGraphQlController(toolApprovalRuleFacade);

        List<AiHubToolApprovalRule> result = controller.aiHubToolApprovalRules(7L);

        assertThat(result).containsExactly(rule);
        verify(toolApprovalRuleFacade).getRules(7L);
    }

    @Test
    void testAiHubToolApprovalDefaultToolNamesDelegatesToFacade() {
        AiHubToolApprovalRuleFacade toolApprovalRuleFacade = mock(AiHubToolApprovalRuleFacade.class);

        when(toolApprovalRuleFacade.getDefaultToolNames()).thenReturn(List.of("deleteProject"));

        AiHubToolApprovalRuleGraphQlController controller =
            new AiHubToolApprovalRuleGraphQlController(toolApprovalRuleFacade);

        List<String> result = controller.aiHubToolApprovalDefaultToolNames();

        assertThat(result).containsExactly("deleteProject");
        verify(toolApprovalRuleFacade).getDefaultToolNames();
    }

    @Test
    void testCreateAiHubToolApprovalRuleDelegatesWithSameArguments() {
        AiHubToolApprovalRuleFacade toolApprovalRuleFacade = mock(AiHubToolApprovalRuleFacade.class);

        AiHubToolApprovalRule created = mock(AiHubToolApprovalRule.class);

        when(
            toolApprovalRuleFacade.createRule(
                7L, AiHubToolApproval.ToolKind.CATALOG, "deleteProject", AiHubToolApprovalRule.Mode.REQUIRE))
                    .thenReturn(created);

        AiHubToolApprovalRuleGraphQlController controller =
            new AiHubToolApprovalRuleGraphQlController(toolApprovalRuleFacade);

        AiHubToolApprovalRule result = controller.createAiHubToolApprovalRule(
            7L, AiHubToolApproval.ToolKind.CATALOG, "deleteProject", AiHubToolApprovalRule.Mode.REQUIRE);

        assertThat(result).isSameAs(created);
        verify(toolApprovalRuleFacade).createRule(
            7L, AiHubToolApproval.ToolKind.CATALOG, "deleteProject", AiHubToolApprovalRule.Mode.REQUIRE);
    }

    @Test
    void testDeleteAiHubToolApprovalRuleDelegatesAndReturnsTrue() {
        AiHubToolApprovalRuleFacade toolApprovalRuleFacade = mock(AiHubToolApprovalRuleFacade.class);

        AiHubToolApprovalRuleGraphQlController controller =
            new AiHubToolApprovalRuleGraphQlController(toolApprovalRuleFacade);

        boolean result = controller.deleteAiHubToolApprovalRule(7L, 42L);

        assertThat(result).isTrue();
        verify(toolApprovalRuleFacade).deleteRule(7L, 42L);
    }

    @Test
    void testToolApprovalRuleToolKindResolverReturnsEnumName() {
        AiHubToolApprovalRuleFacade toolApprovalRuleFacade = mock(AiHubToolApprovalRuleFacade.class);

        AiHubToolApprovalRule rule = new AiHubToolApprovalRule();

        rule.setToolKind(AiHubToolApproval.ToolKind.COMPONENT);

        AiHubToolApprovalRuleGraphQlController controller =
            new AiHubToolApprovalRuleGraphQlController(toolApprovalRuleFacade);

        assertThat(controller.toolApprovalRuleToolKind(rule)).isEqualTo("COMPONENT");
    }

    @Test
    void testToolApprovalRuleModeResolverReturnsEnumName() {
        AiHubToolApprovalRuleFacade toolApprovalRuleFacade = mock(AiHubToolApprovalRuleFacade.class);

        AiHubToolApprovalRule rule = new AiHubToolApprovalRule();

        rule.setMode(AiHubToolApprovalRule.Mode.EXEMPT);

        AiHubToolApprovalRuleGraphQlController controller =
            new AiHubToolApprovalRuleGraphQlController(toolApprovalRuleFacade);

        assertThat(controller.toolApprovalRuleMode(rule)).isEqualTo("EXEMPT");
    }
}
