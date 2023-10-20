/*
 * Copyright 2021 <your company/name>.
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

package com.integri.atlas.task.handler.object_.helpers;

import static com.integri.atlas.task.definition.dsl.DSL.ARRAY_PROPERTY;
import static com.integri.atlas.task.definition.dsl.DSL.OBJECT_PROPERTY;
import static com.integri.atlas.task.definition.dsl.DSL.OPERATION;
import static com.integri.atlas.task.definition.dsl.DSL.STRING_PROPERTY;
import static com.integri.atlas.task.handler.object_.helpers.ObjectHelpersTaskConstants.*;
import static com.integri.atlas.task.handler.object_.helpers.ObjectHelpersTaskConstants.Operation;
import static com.integri.atlas.task.handler.object_.helpers.ObjectHelpersTaskConstants.PROPERTY_SOURCE;

import com.integri.atlas.task.definition.TaskDefinitionHandler;
import com.integri.atlas.task.definition.dsl.DSL;
import com.integri.atlas.task.definition.dsl.TaskDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ObjectHelpersTaskDefinitionHandler implements TaskDefinitionHandler {

    private static final TaskDefinition TASK_DEFINITION = DSL
        .create(TASK_OBJECT_HELPERS)
        .displayName("Object Helpers")
        .description("Converts between JSON string and object/array.")
        .actions(
            OPERATION(Operation.JSON_PARSE.name())
                .displayName("Convert from JSON string")
                .description("Converts the JSON string to object/array.")
                .inputs(
                    STRING_PROPERTY(PROPERTY_SOURCE)
                        .displayName("Source")
                        .description("The JSON string to convert to the data.")
                        .required(true)
                )
                .outputs(ARRAY_PROPERTY(), OBJECT_PROPERTY()),
            OPERATION(Operation.JSON_STRINGIFY.name())
                .displayName("Convert to JSON string")
                .description("Writes the object/array to a JSON string.")
                .inputs(
                    OBJECT_PROPERTY(PROPERTY_SOURCE)
                        .displayName("Source")
                        .description("The data to convert to JSON string.")
                        .required(true)
                        .additionalProperties(true)
                )
                .outputs(STRING_PROPERTY())
        );

    @Override
    public TaskDefinition getTaskDefinition() {
        return TASK_DEFINITION;
    }
}
