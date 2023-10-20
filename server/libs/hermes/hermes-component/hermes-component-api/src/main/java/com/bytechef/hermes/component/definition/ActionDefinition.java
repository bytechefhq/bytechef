
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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * @author Ivica Cardic
 */
@Schema(
    name = "ActionDefinition",
    description = "An action is a a portion of reusable code that accomplish a specific task. When building a workflow, each action is represented as a task inside the workflow. The task 'type' property is defined as [component name]/v[component version]/[action name]. Action properties are used to set properties of the task inside the workflow.")
public sealed interface ActionDefinition permits ComponentDSL.ModifiableActionDefinition {

    Display getDisplay();

    @Schema(name = "exampleOutput", description = "The example of the action's output.")
    Object getExampleOutput();

    @Schema(name = "metadata", description = "Additional data that can be used during processing.")
    Map<String, Object> getMetadata();

    @Schema(name = "name", description = "The action name.")
    String getName();

    @Schema(name = "output", description = "The output schema of an execution result.")
    List<Property<? extends Property<?>>> getOutput();

    @Schema(name = "properties", description = "The list of action properties.")
    List<Property<?>> getProperties();

    /**
     * The code that should be performed when the action is executed as a task whe running inside the workflow engine.
     *
     * @return an optional perform function implementation
     */
    Optional<BiFunction<Context, ExecutionParameters, Object>> getPerformFunction();
}
