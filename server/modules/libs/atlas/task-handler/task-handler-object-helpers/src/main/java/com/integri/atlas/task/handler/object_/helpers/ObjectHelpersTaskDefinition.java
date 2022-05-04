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

import static com.integri.atlas.task.definition.dsl.TaskParameterValue.parameterValues;
import static com.integri.atlas.task.definition.dsl.TaskProperty.JSON_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.SELECT_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.STRING_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.show;
import static com.integri.atlas.task.definition.dsl.TaskPropertyOption.option;
import static com.integri.atlas.task.handler.object_.helpers.ObjectHelpersTaskConstants.*;
import static com.integri.atlas.task.handler.object_.helpers.ObjectHelpersTaskConstants.Operation;
import static com.integri.atlas.task.handler.object_.helpers.ObjectHelpersTaskConstants.PROPERTY_OPERATION;
import static com.integri.atlas.task.handler.object_.helpers.ObjectHelpersTaskConstants.PROPERTY_SOURCE;

import com.integri.atlas.task.definition.TaskDefinition;
import com.integri.atlas.task.definition.dsl.TaskSpecification;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ObjectHelpersTaskDefinition implements TaskDefinition {

    public static final TaskSpecification TASK_SPECIFICATION = TaskSpecification
        .create(TASK_OBJECT_HELPERS)
        .displayName("Object Helpers")
        .description("Converts between JSON string and object/array.")
        .properties(
            SELECT_PROPERTY(PROPERTY_OPERATION)
                .displayName("Operation")
                .description("The operation to perform.")
                .options(
                    option(
                        "Convert from JSON string",
                        Operation.JSON_PARSE.name(),
                        "Converts the JSON string to object/array."
                    ),
                    option(
                        "Convert to JSON string",
                        Operation.JSON_STRINGIFY.name(),
                        "Writes the object/array to a JSON string."
                    )
                )
                .defaultValue(Operation.JSON_PARSE.name())
                .required(true),
            STRING_PROPERTY(PROPERTY_SOURCE)
                .displayName("Source")
                .description("The JSON string to convert to the data.")
                .displayOption(show(PROPERTY_OPERATION, parameterValues(Operation.JSON_PARSE.name())))
                .required(true),
            JSON_PROPERTY(PROPERTY_SOURCE)
                .displayName("Source")
                .description("The data to convert to JSON string.")
                .displayOption(show(PROPERTY_OPERATION, parameterValues(Operation.JSON_STRINGIFY.name())))
                .required(true)
        );

    @Override
    public TaskSpecification getSpecification() {
        return TASK_SPECIFICATION;
    }
}
