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

package com.bytechef.component.workflow.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.JSON_SCHEMA_BUILDER;
import static com.bytechef.component.workflow.constant.WorkflowConstants.RESPONSE;
import static com.bytechef.platform.component.constant.WorkflowConstants.OUTPUT_SCHEMA;
import static com.bytechef.platform.component.constant.WorkflowConstants.RESPONSE_TO_AI_MODEL_CALL;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.workflow.util.WorkflowResponseUtils;

/**
 * @author Ivica Cardic
 */
public class WorkflowResponseToAiModelCallAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(RESPONSE_TO_AI_MODEL_CALL)
        .title("Response to AI Model Call")
        .description(
            "Respond and send back data to the calling AI model. Must be the last step in a tool callable workflow.")
        .properties(
            string(OUTPUT_SCHEMA)
                .label("Output Schema")
                .placeholder("Edit Output schema")
                .description("The schema definition for the response data sent back to the AI model.")
                .controlType(JSON_SCHEMA_BUILDER),
            dynamicProperties(RESPONSE)
                .description("The response data to send back to the AI model.")
                .propertiesLookupDependsOn(OUTPUT_SCHEMA)
                .properties(WorkflowResponseUtils::responseProperties)
                .required(true))
        .output(WorkflowResponseUtils::actionOutput)
        .perform(WorkflowResponseUtils::perform);
}
