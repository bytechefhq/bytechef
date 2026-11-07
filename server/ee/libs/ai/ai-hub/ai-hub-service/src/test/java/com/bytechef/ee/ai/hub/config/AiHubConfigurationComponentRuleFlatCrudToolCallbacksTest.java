/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.tool.componentrule.ComponentRuleToolCallbacksFactory;
import com.bytechef.ee.platform.component.rule.ComponentRuleService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 * @author Ivica Cardic
 */
class AiHubConfigurationComponentRuleFlatCrudToolCallbacksTest {

    private static final Set<String> EXPECTED_READ_NAMES = Set.of(
        "listComponentRules", "describeComponentToolParameters", "proposeComponentRuleCondition");

    private static final Set<String> EXPECTED_WRITE_NAMES = Set.of(
        "listComponentRules", "describeComponentToolParameters", "proposeComponentRuleCondition",
        "createComponentRule");

    @Test
    void testReadModeReturnsExactlyTheThreeReadNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.componentRuleFlatCrudToolCallbacks(present(), false);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testWriteModeAddsTheCreateTool() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.componentRuleFlatCrudToolCallbacks(present(), true);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_WRITE_NAMES);
    }

    @Test
    void testAbsentFactoryReturnsEmptyListForBothModes() {
        assertThat(AiHubConfiguration.componentRuleFlatCrudToolCallbacks(absent(), false)).isEmpty();
        assertThat(AiHubConfiguration.componentRuleFlatCrudToolCallbacks(absent(), true)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ComponentRuleToolCallbacksFactory> present() {
        ComponentRuleToolCallbacksFactory factory = new ComponentRuleToolCallbacksFactory(
            mock(ComponentRuleService.class), mock(ComponentDefinitionService.class),
            mock(ClusterElementDefinitionService.class), mock(Evaluator.class));

        ObjectProvider<ComponentRuleToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(factory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ComponentRuleToolCallbacksFactory> absent() {
        return mock(ObjectProvider.class);
    }
}
