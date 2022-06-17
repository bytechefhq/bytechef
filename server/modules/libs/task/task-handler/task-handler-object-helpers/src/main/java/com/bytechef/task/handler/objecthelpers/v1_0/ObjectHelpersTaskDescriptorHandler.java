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

package com.bytechef.task.handler.objecthelpers.v1_0;

import static com.bytechef.hermes.descriptor.domain.DSL.ANY_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.ARRAY_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.BOOLEAN_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.NUMBER_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.OBJECT_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.OPERATION;
import static com.bytechef.hermes.descriptor.domain.DSL.STRING_PROPERTY;

import com.bytechef.hermes.descriptor.domain.DSL;
import com.bytechef.hermes.descriptor.domain.TaskDescriptor;
import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import com.bytechef.task.handler.objecthelpers.ObjectHelpersTaskConstants;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ObjectHelpersTaskDescriptorHandler implements TaskDescriptorHandler {

    public static final TaskDescriptor TASK_DESCRIPTOR = DSL.createTaskDescriptor(
                    ObjectHelpersTaskConstants.OBJECT_HELPERS)
            .displayName("Object Helpers")
            .description("Converts between JSON string and object/array.")
            .version(ObjectHelpersTaskConstants.VERSION_1_0)
            .operations(
                    OPERATION(ObjectHelpersTaskConstants.JSON_PARSE)
                            .displayName("Convert from JSON string")
                            .description("Converts the JSON string to object/array.")
                            .inputs(STRING_PROPERTY(ObjectHelpersTaskConstants.SOURCE)
                                    .displayName("Source")
                                    .description("The JSON string to convert to the data.")
                                    .required(true))
                            .outputs(ARRAY_PROPERTY(), BOOLEAN_PROPERTY(), NUMBER_PROPERTY(), OBJECT_PROPERTY()),
                    OPERATION(ObjectHelpersTaskConstants.JSON_STRINGIFY)
                            .displayName("Convert to JSON string")
                            .description("Writes the object/array to a JSON string.")
                            .inputs(ANY_PROPERTY(ObjectHelpersTaskConstants.SOURCE)
                                    .displayName("Source")
                                    .description("The data to convert to JSON string.")
                                    .types(
                                            ARRAY_PROPERTY(),
                                            BOOLEAN_PROPERTY(),
                                            NUMBER_PROPERTY(),
                                            OBJECT_PROPERTY(),
                                            STRING_PROPERTY())
                                    .required(true))
                            .outputs(STRING_PROPERTY()));

    @Override
    public TaskDescriptor getTaskDescriptor() {
        return TASK_DESCRIPTOR;
    }
}
