/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.componentrule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.platform.component.rule.ComponentRule;
import com.bytechef.ee.platform.component.rule.ComponentRuleService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 * @author Ivica Cardic
 */
class CreateComponentRuleToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();
    private final ComponentRuleService componentRuleService = mock(ComponentRuleService.class);
    private final CreateComponentRuleToolCallback toolCallback =
        new CreateComponentRuleToolCallback(componentRuleService);

    private static ToolContext toolContext(long workspaceId) {
        return new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .build()
                .toToolContext());
    }

    @Test
    void testCallPersistsARuleScopedToTheSessionWorkspace() throws Exception {
        when(componentRuleService.saveComponentRule(any())).thenAnswer(invocation -> {
            ComponentRule componentRule = invocation.getArgument(0);

            componentRule.setId(42L);

            return componentRule;
        });

        String input =
            """
                {"componentName": "slack", "toolName": "sendMessage", "phase": "BEFORE", "ruleAction": "TAG",
                 "condition": "true"}""";

        String result = toolCallback.call(input, toolContext(7L));

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("componentRuleId")
            .asLong()).isEqualTo(42L);

        ArgumentCaptor<ComponentRule> componentRuleCaptor = ArgumentCaptor.forClass(ComponentRule.class);

        verify(componentRuleService).saveComponentRule(componentRuleCaptor.capture());

        ComponentRule savedComponentRule = componentRuleCaptor.getValue();

        assertThat(savedComponentRule.getWorkspaceId()).isEqualTo(7L);
    }

    @Test
    void testCallReturnsErrorAndWritesNothingWhenWorkspaceIsUnresolvable() throws Exception {
        String input =
            """
                {"componentName": "slack", "toolName": "sendMessage", "phase": "BEFORE", "ruleAction": "TAG",
                 "condition": "true"}""";

        String result = toolCallback.call(input);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("Workspace context unavailable");

        verify(componentRuleService, never()).saveComponentRule(any());
    }

    @Test
    void testCallInputSchemaHasNoWorkspaceIdField() {
        assertThat(toolCallback.getToolDefinition()
            .inputSchema()).doesNotContain("workspaceId");
    }
}
