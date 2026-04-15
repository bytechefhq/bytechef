/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.ai.agenticai.constant;

import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;

import com.bytechef.component.ai.llm.constant.LLMConstants;
import com.bytechef.component.definition.Property;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class AgenticAiConstants {

    public static final String AGENTIC_AI = "agenticAi";
    public static final String RUN = "run";
    public static final String GOAL_DESCRIPTION = "goalDescription";
    public static final String GOAL_OUTPUT_BINDING = "goalOutputBinding";
    public static final String GOAL_MODE = "goalMode";
    public static final String GOAL_MODE_STRUCTURAL = "STRUCTURAL";
    public static final String GOAL_MODE_SMART = "SMART";
    public static final String ACTION_NAME = "actionName";
    public static final String ACTION_DESCRIPTION = "actionDescription";
    public static final String ACTION_PROMPT = "actionPrompt";
    public static final String INPUT_BINDING = "inputBinding";
    public static final String OUTPUT_BINDING = "outputBinding";
    public static final String ACTION_COST = "actionCost";
    public static final double DEFAULT_ACTION_COST = 1.0;

    public static final List<Property> RUN_PROPERTIES = List.of(
        string(GOAL_DESCRIPTION)
            .label("Goal Description")
            .description("Describe the goal the agentic AI should achieve using the configured actions.")
            .required(true),
        string(GOAL_OUTPUT_BINDING)
            .label("Goal Output Binding")
            .description(
                "The output binding that, once produced on the blackboard, satisfies the goal. " +
                    "Must match the output binding of at least one configured action.")
            .expressionEnabled(false)
            .required(true),
        string(GOAL_MODE)
            .label("Goal Mode")
            .description(
                "Structural: the goal is satisfied as soon as the goal output binding is produced. " +
                    "Smart: additionally asks an LLM to judge whether the produced value actually " +
                    "satisfies the goal description; the planner may backtrack and try alternative " +
                    "action paths if not. Smart mode adds an LLM call per evaluation.")
            .options(
                option("Structural", GOAL_MODE_STRUCTURAL),
                option("Smart (experimental)", GOAL_MODE_SMART))
            .defaultValue(GOAL_MODE_STRUCTURAL)
            .expressionEnabled(false)
            .required(true),
        LLMConstants.SYSTEM_PROMPT_PROPERTY,
        LLMConstants.RESPONSE_PROPERTY);

    public static final List<Property> ACTION_PROPERTIES = List.of(
        string(ACTION_NAME)
            .label("Action Name")
            .description("A unique name for this action.")
            .expressionEnabled(false)
            .required(true),
        string(ACTION_DESCRIPTION)
            .label("Description")
            .description("What this action does. Used by the GOAP planner.")
            .controlType(TEXT_AREA)
            .expressionEnabled(false)
            .required(true),
        string(ACTION_PROMPT)
            .label("Prompt")
            .description("The prompt template for the LLM. Use {input} to reference the input data.")
            .controlType(TEXT_AREA)
            .required(true),
        string(INPUT_BINDING)
            .label("Input Binding")
            .description(
                "The name of the input this action needs from a previous action's output. " +
                    "Use 'userGoal' for the first action that takes the user's goal directly.")
            .expressionEnabled(false)
            .required(true),
        string(OUTPUT_BINDING)
            .label("Output Binding")
            .description(
                "The name of the output this action produces. Must match another action's input binding " +
                    "or the goal's required output.")
            .expressionEnabled(false)
            .required(true),
        number(ACTION_COST)
            .label("Cost")
            .description(
                "GOAP edge weight for this action. When multiple actions produce the same output " +
                    "binding, the planner prefers the path with the lowest total cost. Use higher " +
                    "values to discourage this action and lower values to encourage it. Defaults to " +
                    "1.0.")
            .defaultValue(DEFAULT_ACTION_COST)
            .expressionEnabled(false)
            .required(false));
}
