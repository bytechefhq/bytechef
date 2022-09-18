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

package com.bytechef.task.handler.function.v1_0;

import static com.bytechef.hermes.descriptor.model.DSL.ARRAY_PROPERTY;
import static com.bytechef.hermes.descriptor.model.DSL.OBJECT_PROPERTY;
import static com.bytechef.hermes.descriptor.model.DSL.OPERATION;
import static com.bytechef.hermes.descriptor.model.DSL.STRING_PROPERTY;
import static com.bytechef.task.handler.function.FunctionTaskConstants.*;

import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import com.bytechef.hermes.descriptor.model.DSL;
import com.bytechef.hermes.descriptor.model.TaskDescriptor;

/**
 * @author Matija Petanjek
 */
public class FunctionTaskDescriptorHandler implements TaskDescriptorHandler {

    private static final TaskDescriptor TASK_DESCRIPTOR = DSL.createTaskDescriptor(FUNCTION)
            .displayName(FUNCTION)
            .description(
                    "Executes user-defined code. User can write custom workflow logic in Java, JavaScript or Python programming languages.")
            .version(VERSION_1_0)
            .operations(
                    OPERATION(JAVA)
                            .displayName(JAVA)
                            .description("Executes custom Java code.")
                            .inputs(
                                    ARRAY_PROPERTY(CONTEXT)
                                            .displayName(CONTEXT)
                                            .description("Initialize parameter values used in the custom code.")
                                            .defaultValue("")
                                            .placeholder("Add parameter")
                                            .items(OBJECT_PROPERTY()
                                                    .displayName(PARAMETER)
                                                    .properties(
                                                            STRING_PROPERTY()
                                                                    .displayName("Parameter name")
                                                                    .description(
                                                                            "Name of the parameter in the custom code")
                                                                    .defaultValue(""),
                                                            OBJECT_PROPERTY()
                                                                    .displayName("Parameter value")
                                                                    .description(
                                                                            "Value of the parameter. You can define static or dynamic values.")
                                                                    .defaultValue(""))),
                                    STRING_PROPERTY(SOURCE)
                                            .displayName("Java code")
                                            .description("Add your Java custom logic here.")
                                            .defaultValue(""))
                            .outputs(OBJECT_PROPERTY()),
                    OPERATION(JS)
                            .displayName(JS)
                            .description("Executes custom JavaScript code.")
                            .inputs(
                                    ARRAY_PROPERTY(CONTEXT)
                                            .displayName(CONTEXT)
                                            .description("Initialize parameter values used in the custom code.")
                                            .defaultValue("")
                                            .placeholder("Add parameter")
                                            .items(OBJECT_PROPERTY()
                                                    .displayName(PARAMETER)
                                                    .properties(
                                                            STRING_PROPERTY()
                                                                    .displayName("Parameter name")
                                                                    .description(
                                                                            "Name of the parameter in the custom code")
                                                                    .defaultValue(""),
                                                            OBJECT_PROPERTY()
                                                                    .displayName("Parameter value")
                                                                    .description(
                                                                            "Value of the parameter. You can define static or dynamic values.")
                                                                    .defaultValue(""))),
                                    STRING_PROPERTY(SOURCE)
                                            .displayName("JavaScript code")
                                            .description("Add your Javascript custom logic here.")
                                            .defaultValue(""))
                            .outputs(OBJECT_PROPERTY()),
                    OPERATION(PYTHON)
                            .displayName(PYTHON)
                            .description("Executes custom Python code.")
                            .inputs(
                                    ARRAY_PROPERTY(CONTEXT)
                                            .displayName(CONTEXT)
                                            .description("Initialize parameter values used in the custom code.")
                                            .defaultValue("")
                                            .placeholder("Add parameter")
                                            .items(OBJECT_PROPERTY()
                                                    .displayName(PARAMETER)
                                                    .properties(
                                                            STRING_PROPERTY()
                                                                    .displayName("Parameter name")
                                                                    .description(
                                                                            "Name of the parameter in the custom code")
                                                                    .defaultValue(""),
                                                            OBJECT_PROPERTY()
                                                                    .displayName("Parameter value")
                                                                    .description(
                                                                            "Value of the parameter. You can define static or dynamic values.")
                                                                    .defaultValue(""))),
                                    STRING_PROPERTY(SOURCE)
                                            .displayName("Python code")
                                            .description("Add your Python custom logic here.")
                                            .defaultValue(""))
                            .outputs(OBJECT_PROPERTY()));

    @Override
    public TaskDescriptor getTaskDescriptor() {
        return TASK_DESCRIPTOR;
    }
}
