/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.componentrule;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.platform.component.rule.ComponentRule;
import com.bytechef.ee.platform.component.rule.ComponentRuleService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 * @author Ivica Cardic
 */
public class ListComponentRulesToolCallback implements ToolCallback {

    static final String TOOL_NAME = "listComponentRules";

    private static final String DESCRIPTION = """
        List the component rules that govern the current chat's workspace: that workspace's own rules plus the
        rules that apply to every workspace in the tenant. Optionally narrow to one component with componentName.
        Returns each rule's id, component, tool (null means every tool of that component), phase, enforcement
        action, condition, enabled flag and workspaceId (null means the rule applies to every workspace). Read-only.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "componentName": {"type": "string", "description": "Optional — narrow to one component"}
            },
            "required": []
        }""";

    private final ComponentRuleService componentRuleService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListComponentRulesToolCallback(ComponentRuleService componentRuleService) {
        this.componentRuleService = componentRuleService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            ListComponentRulesInput input = jsonMapper.readValue(toolInput, ListComponentRulesInput.class);

            String componentName = input.componentName();

            List<ComponentRule> componentRules = componentName == null || componentName.isBlank()
                ? componentRuleService.getComponentRules()
                : componentRuleService.getComponentRules(componentName);

            AgentToolInvocationContext invocationContext =
                AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            List<ComponentRule> scopedComponentRules = componentRules.stream()
                .filter(componentRule -> componentRule.appliesToWorkspace(workspaceId))
                .toList();

            return jsonMapper.writeValueAsString(scopedComponentRules.stream()
                .map(ListComponentRulesToolCallback::toSummary)
                .toList());
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, ListComponentRulesToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static ComponentRuleSummary toSummary(ComponentRule componentRule) {
        return new ComponentRuleSummary(
            componentRule.getId(), componentRule.getComponentName(), componentRule.getToolName(),
            componentRule.getPhase()
                .name(),
            componentRule.getRuleAction()
                .name(),
            componentRule.getCondition(), componentRule.getDescription(), componentRule.isEnabled(),
            componentRule.getWorkspaceId());
    }

    public record ListComponentRulesInput(@Nullable String componentName) {
    }

    public record ComponentRuleSummary(
        @Nullable Long id, String componentName, @Nullable String toolName, String phase, String ruleAction,
        String condition, @Nullable String description, boolean enabled, @Nullable Long workspaceId) {
    }
}
