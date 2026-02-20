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
import static com.bytechef.component.workflow.constant.WorkflowConstants.CALLABLE_RESPONSE;
import static com.bytechef.component.workflow.constant.WorkflowConstants.OUTPUT_SCHEMA;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.WebhookResponse;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class WorkflowCallableResponseAction {

    public static final String RESPONSE = "response";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CALLABLE_RESPONSE)
        .title("Callable Workflow Response")
        .description(
            "Respond and send back data to the calling workflow. Must be the last step in a callable workflow.")
        .properties(
            string(OUTPUT_SCHEMA)
                .label("Output Schema")
                .placeholder("Edit Output schema")
                .description("The schema definition for the response data sent back to the calling workflow.")
                .controlType(JSON_SCHEMA_BUILDER),
            dynamicProperties(RESPONSE)
                .description("The response data to send back to the calling workflow.")
                .propertiesLookupDependsOn(OUTPUT_SCHEMA)
                .properties(WorkflowCallableResponseAction::responseProperties)
                .required(true))
        .output(WorkflowCallableResponseAction::output)
        .perform(WorkflowCallableResponseAction::perform);

    protected static OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Map<String, ?> response = inputParameters.getMap(RESPONSE, Map.of());

        if (response.isEmpty()) {
            return null;
        }

        return OutputResponse.of(response);
    }

    protected static WebhookResponse perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Map<String, ?> response = inputParameters.getMap(RESPONSE, Map.of());

        return WebhookResponse.json(response);
    }

    protected static List<? extends ValueProperty<?>> responseProperties(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ActionContext actionContext) {

        String outputSchema = inputParameters.getString(OUTPUT_SCHEMA);

        if (outputSchema == null) {
            return List.of();
        }

        ModifiableValueProperty<?, ?> property = (ModifiableValueProperty<?, ?>) actionContext.outputSchema(
            outputSchemaFunction -> outputSchemaFunction.getOutputSchema(OUTPUT_SCHEMA, outputSchema));

        if (property == null) {
            return List.of();
        }

        return List.of(property);
    }
}
