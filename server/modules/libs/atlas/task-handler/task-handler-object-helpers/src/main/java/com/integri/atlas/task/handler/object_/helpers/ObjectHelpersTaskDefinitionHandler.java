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

import static com.integri.atlas.task.definition.model.DSL.ARRAY_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.OBJECT_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.OPERATION;
import static com.integri.atlas.task.definition.model.DSL.STRING_PROPERTY;
import static com.integri.atlas.task.handler.object_.helpers.ObjectHelpersTaskConstants.PROPERTY_SOURCE;
import static com.integri.atlas.task.handler.object_.helpers.ObjectHelpersTaskConstants.TASK_OBJECT_HELPERS;

import com.integri.atlas.task.definition.AbstractTaskDefinitionHandler;
import com.integri.atlas.task.definition.model.DSL;
import com.integri.atlas.task.definition.model.TaskDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ObjectHelpersTaskDefinitionHandler extends AbstractTaskDefinitionHandler {

    private static final TaskDefinition TASK_DEFINITION = DSL
        .createTaskDefinition(TASK_OBJECT_HELPERS)
        .displayName("Object Helpers")
        .description("Converts between JSON string and object/array.")
        .operations(
            OPERATION("parse")
                .displayName("Convert from JSON string")
                .description("Converts the JSON string to object/array.")
                .inputs(
                    STRING_PROPERTY(PROPERTY_SOURCE)
                        .displayName("Source")
                        .description("The JSON string to convert to the data.")
                        .required(true)
                )
                .outputs(ARRAY_PROPERTY(), OBJECT_PROPERTY()),
            OPERATION("stringify")
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
