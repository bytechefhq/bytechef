/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.componentrule;

import com.bytechef.ee.platform.component.rule.ComponentRuleConditionStubContext;
import com.bytechef.evaluator.Evaluator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 * @author Ivica Cardic
 */
public class ProposeComponentRuleConditionToolCallback implements ToolCallback {

    static final String TOOL_NAME = "proposeComponentRuleCondition";

    private static final String CONDITION_KEY = "condition";
    private static final String FORMULA_PREFIX = "=";

    private static final String DESCRIPTION = """
        Propose a ByteChef formula condition for a component rule and return it for the admin to review. \
        Saves nothing. Supply condition (the formula BODY, with no leading '=') and a one-sentence \
        explanation of what it matches. The condition is parse-checked and comes back with valid: true, or \
        valid: false plus the parse error, in which case rewrite it and call this tool again. Conditions may \
        reference inputParameters, componentName, toolName, toolCallName, connectionId, and — in the \
        AFTER phase only — output. They may NOT call Java methods (no .startsWith(...), no T(...), no new); \
        use ByteChef's own functions instead: contains, equalsIgnoreCase, indexOf, length, size, split, \
        substring, join.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "condition": {
                    "type": "string",
                    "description": "The formula body, without a leading '='"
                },
                "explanation": {
                    "type": "string",
                    "description": "One sentence describing what this condition matches"
                }
            },
            "required": ["condition"]
        }""";

    private final Evaluator evaluator;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ProposeComponentRuleConditionToolCallback(Evaluator evaluator) {
        this.evaluator = evaluator;
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
        ProposeInput input;

        try {
            input = jsonMapper.readValue(toolInput, ProposeInput.class);
        } catch (RuntimeException exception) {
            return failure(null, "Could not read the tool input: " + exception.getMessage());
        }

        String condition = input.condition();

        if (condition == null || condition.isBlank()) {
            return failure(condition, "condition is required");
        }

        Map<String, Object> result = new LinkedHashMap<>();

        result.put(CONDITION_KEY, condition);

        try {
            evaluator.evaluate(
                Map.of(CONDITION_KEY, FORMULA_PREFIX + condition), ComponentRuleConditionStubContext.get(), false);

            result.put("valid", true);
            result.put("explanation", input.explanation() == null ? "" : input.explanation());
        } catch (RuntimeException exception) {
            result.put("valid", false);
            result.put(
                "error",
                "The condition is not a valid ByteChef formula expression: " + exception.getMessage()
                    + ". Rewrite it using ByteChef functions (contains, equalsIgnoreCase, size) rather than Java "
                    + "method calls.");
        }

        return jsonMapper.writeValueAsString(result);
    }

    private String failure(@Nullable String condition, String error) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put(CONDITION_KEY, condition);
        result.put("valid", false);
        result.put("error", error);

        return jsonMapper.writeValueAsString(result);
    }

    private record ProposeInput(@Nullable String condition, @Nullable String explanation) {
    }
}
