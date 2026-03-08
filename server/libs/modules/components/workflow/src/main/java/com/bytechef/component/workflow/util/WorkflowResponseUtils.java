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

package com.bytechef.component.workflow.util;

import static com.bytechef.component.workflow.constant.WorkflowConstants.RESPONSE;
import static com.bytechef.platform.component.constant.WorkflowConstants.INPUT_SCHEMA;
import static com.bytechef.platform.component.constant.WorkflowConstants.OUTPUT_SCHEMA;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.CallableResponse;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class WorkflowResponseUtils {

    private WorkflowResponseUtils() {
    }

    public static OutputResponse actionOutput(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Map<String, ?> response = inputParameters.getMap(RESPONSE, Map.of());

        if (response.isEmpty()) {
            return null;
        }

        return OutputResponse.of(response);
    }

    public static CallableResponse perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return new CallableResponse(inputParameters.getMap(RESPONSE, Map.of()));
    }

    public static List<? extends ValueProperty<?>> responseProperties(
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

        if (property instanceof ModifiableObjectProperty objectProperty) {
            return objectProperty.getProperties()
                .orElse(List.of());
        }

        return List.of(property);
    }

    public static OutputResponse triggerOutput(
        Parameters inputParameters, Parameters connectionParameters, TriggerContext context) {

        String inputSchema = inputParameters.getString(INPUT_SCHEMA);

        if (inputSchema != null) {
            ModifiableValueProperty<?, ?> input = (ModifiableValueProperty<?, ?>) context.outputSchema(
                outputSchema -> outputSchema.getOutputSchema(INPUT_SCHEMA, inputSchema));

            if (input != null) {
                return OutputResponse.of(input);
            }
        }

        return null;
    }
}
