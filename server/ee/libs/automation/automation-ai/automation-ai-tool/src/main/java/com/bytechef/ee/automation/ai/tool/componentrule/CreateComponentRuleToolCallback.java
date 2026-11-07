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
import com.bytechef.ee.platform.component.rule.ComponentRule.RuleAction;
import com.bytechef.ee.platform.component.rule.ComponentRule.RulePhase;
import com.bytechef.ee.platform.component.rule.ComponentRuleService;
import com.bytechef.exception.ConfigurationException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
public class CreateComponentRuleToolCallback implements ToolCallback {

    static final String TOOL_NAME = "createComponentRule";

    private static final String DESCRIPTION = """
        Create a component rule. Call proposeComponentRuleCondition first and confirm the condition with the admin
        before calling this — this tool writes to the database. An AFTER-phase rule must use TAG; BLOCK and
        REQUIRE_APPROVAL are rejected, because the tool has already run by then. Returns the new rule's id.
        The rule always governs the current chat's workspace only — this tool can never create a rule that applies
        to every workspace in the tenant.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "componentName": {"type": "string", "description": "The component the rule governs"},
                    "toolName": {"type": "string", "description": "Optional — omit to govern every tool of the component"},
                    "phase": {"type": "string", "description": "BEFORE or AFTER"},
                    "ruleAction": {"type": "string", "description": "BLOCK, TAG or REQUIRE_APPROVAL. AFTER rules must be TAG."},
                    "condition": {"type": "string", "description": "The formula body, validated by proposeComponentRuleCondition first"},
                    "description": {"type": "string", "description": "The plain-English intent, kept for later re-editing"},
                    "strict": {"type": "boolean", "description": "Fire when the condition cannot be evaluated. Default false."}
                },
                "required": ["componentName", "phase", "ruleAction", "condition"]
            }""";

    private final ComponentRuleService componentRuleService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateComponentRuleToolCallback(ComponentRuleService componentRuleService) {
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
            CreateComponentRuleInput input = jsonMapper.readValue(toolInput, CreateComponentRuleInput.class);

            if (input.componentName() == null || input.componentName()
                .isBlank()) {
                return toolError("componentName is required");
            }

            if (input.condition() == null || input.condition()
                .isBlank()) {
                return toolError("condition is required");
            }

            if (input.phase() == null || input.phase()
                .isBlank()) {
                return toolError("phase is required (BEFORE or AFTER)");
            }

            RulePhase phase;

            try {
                phase = RulePhase.valueOf(input.phase());
            } catch (IllegalArgumentException exception) {
                return toolError("phase must be BEFORE or AFTER, got '" + input.phase() + "'");
            }

            if (input.ruleAction() == null || input.ruleAction()
                .isBlank()) {
                return toolError("ruleAction is required (BLOCK, TAG or REQUIRE_APPROVAL)");
            }

            RuleAction ruleAction;

            try {
                ruleAction = RuleAction.valueOf(input.ruleAction());
            } catch (IllegalArgumentException exception) {
                return toolError(
                    "ruleAction must be BLOCK, TAG or REQUIRE_APPROVAL, got '" + input.ruleAction() + "'");
            }

            if (phase == RulePhase.AFTER && ruleAction != RuleAction.TAG) {
                return toolError(
                    "A rule evaluated in the AFTER phase must use TAG — the tool has already run, so neither a "
                        + "block nor an approval can prevent anything.");
            }

            AgentToolInvocationContext invocationContext =
                AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the AI Hub of a workspace.");
            }

            ComponentRule componentRule = new ComponentRule();

            componentRule.setComponentName(input.componentName());
            componentRule.setToolName(input.toolName());
            componentRule.setWorkspaceId(workspaceId);
            componentRule.setPhase(phase);
            componentRule.setRuleAction(ruleAction);
            componentRule.setCondition(input.condition());
            componentRule.setDescription(input.description());
            componentRule.setStrict(Boolean.TRUE.equals(input.strict()));

            ComponentRule savedComponentRule = componentRuleService.saveComponentRule(componentRule);

            return jsonMapper.writeValueAsString(new CreateComponentRuleOutput(savedComponentRule.getId()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException | ConfigurationException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, CreateComponentRuleToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record CreateComponentRuleInput(
        String componentName, @Nullable String toolName, String phase, String ruleAction, String condition,
        @Nullable String description, @Nullable Boolean strict) {
    }

    public record CreateComponentRuleOutput(@Nullable Long componentRuleId) {
    }
}
