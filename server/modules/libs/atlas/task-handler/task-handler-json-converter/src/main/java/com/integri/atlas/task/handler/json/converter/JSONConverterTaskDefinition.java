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

package com.integri.atlas.task.handler.json.converter;

import static com.integri.atlas.task.definition.dsl.TaskParameterValue.parameterValues;
import static com.integri.atlas.task.definition.dsl.TaskProperty.JSON_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.SELECT_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.STRING_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.show;
import static com.integri.atlas.task.definition.dsl.TaskPropertyOption.option;
import static com.integri.atlas.task.handler.json.converter.JSONConverterTaskConstants.*;
import static com.integri.atlas.task.handler.json.converter.JSONConverterTaskConstants.Operation;
import static com.integri.atlas.task.handler.json.converter.JSONConverterTaskConstants.PROPERTY_INPUT;
import static com.integri.atlas.task.handler.json.converter.JSONConverterTaskConstants.PROPERTY_OPERATION;

import com.integri.atlas.task.definition.TaskDefinition;
import com.integri.atlas.task.definition.dsl.TaskSpecification;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class JSONConverterTaskDefinition implements TaskDefinition {

    public static final TaskSpecification TASK_SPECIFICATION = TaskSpecification
        .create(TASK_JSON_CONVERTER)
        .displayName("JSON Converter")
        .description("Converts between JSON string and object/array.")
        .properties(
            SELECT_PROPERTY(PROPERTY_OPERATION)
                .displayName("Operation")
                .description("The operation to perform.")
                .options(
                    option(
                        "Convert from JSON string",
                        Operation.FROM_JSON.name(),
                        "Converts the JSON string to object/array."
                    ),
                    option(
                        "Convert to JSON string",
                        Operation.TO_JSON.name(),
                        "Writes the object/array to a JSON string."
                    )
                )
                .defaultValue(Operation.FROM_JSON.name())
                .required(true),
            STRING_PROPERTY(PROPERTY_INPUT)
                .displayName("Input")
                .description("JSON string to convert to the data.")
                .displayOption(show(PROPERTY_OPERATION, parameterValues(Operation.FROM_JSON.name())))
                .required(true),
            JSON_PROPERTY(PROPERTY_INPUT)
                .displayName("Input")
                .description("The data to convert to JSON string.")
                .displayOption(show(PROPERTY_OPERATION, parameterValues(Operation.TO_JSON.name())))
                .required(true)
        );

    @Override
    public TaskSpecification getSpecification() {
        return TASK_SPECIFICATION;
    }
}
