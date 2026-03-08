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

package com.bytechef.component.workflow.trigger;

import static com.bytechef.ai.tool.constant.ToolConstants.TOOL_DESCRIPTION;
import static com.bytechef.ai.tool.constant.ToolConstants.TOOL_NAME;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.Property.ControlType.JSON_SCHEMA_BUILDER;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;
import static com.bytechef.platform.component.constant.WorkflowConstants.INPUT_SCHEMA;
import static com.bytechef.platform.component.constant.WorkflowConstants.NEW_AI_MODEL_CALL;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.workflow.util.WorkflowResponseUtils;

/**
 * @author Ivica Cardic
 */
public class WorkflowNewAiModelCallTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger(NEW_AI_MODEL_CALL)
        .title("New AI Model Call")
        .description(
            "Exposes this workflow as a tool with a custom name and description. Define the input schema to specify what data the AI model should provide.")
        .type(TriggerType.CALLABLE)
        .workflowSyncExecution(true)
        .properties(
            string(TOOL_NAME)
                .label("Name")
                .description("The tool name exposed to the AI model.")
                .required(true),
            string(TOOL_DESCRIPTION)
                .label("Description")
                .description("The tool description exposed to the AI model.")
                .controlType(TEXT_AREA)
                .required(true),
            string(INPUT_SCHEMA)
                .label("Inputs")
                .placeholder("Edit Inputs schema")
                .description("The schema definition for the input data this tool expects from the AI model.")
                .controlType(JSON_SCHEMA_BUILDER))
        .output(WorkflowResponseUtils::triggerOutput);
}
