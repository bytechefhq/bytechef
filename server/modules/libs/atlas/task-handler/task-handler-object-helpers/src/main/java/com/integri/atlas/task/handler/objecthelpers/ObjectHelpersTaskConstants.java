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

package com.integri.atlas.task.handler.objecthelpers;

import static com.integri.atlas.task.definition.model.DSL.ARRAY_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.OBJECT_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.OPERATION;
import static com.integri.atlas.task.definition.model.DSL.STRING_PROPERTY;

import com.integri.atlas.task.definition.model.DSL;
import com.integri.atlas.task.definition.model.TaskDefinition;

/**
 * @author Ivica Cardic
 */
public class ObjectHelpersTaskConstants {

    public static final String SOURCE = "source";
    public static final String OBJECT_HELPERS = "objectHelpers";
    public static final float VERSION_1_0 = 1.0f;
    public static final String STRINGIFY = "stringify";
    public static final String PARSE = "parse";

    public static final TaskDefinition TASK_DEFINITION = DSL
        .createTaskDefinition(OBJECT_HELPERS)
        .displayName("Object Helpers")
        .description("Converts between JSON string and object/array.")
        .version(VERSION_1_0)
        .operations(
            OPERATION(PARSE)
                .displayName("Convert from JSON string")
                .description("Converts the JSON string to object/array.")
                .inputs(
                    STRING_PROPERTY(SOURCE)
                        .displayName("Source")
                        .description("The JSON string to convert to the data.")
                        .required(true)
                )
                .outputs(ARRAY_PROPERTY(), OBJECT_PROPERTY()),
            OPERATION(STRINGIFY)
                .displayName("Convert to JSON string")
                .description("Writes the object/array to a JSON string.")
                .inputs(
                    OBJECT_PROPERTY(SOURCE)
                        .displayName("Source")
                        .description("The data to convert to JSON string.")
                        .required(true)
                        .additionalProperties(true)
                )
                .outputs(STRING_PROPERTY())
        );
}
