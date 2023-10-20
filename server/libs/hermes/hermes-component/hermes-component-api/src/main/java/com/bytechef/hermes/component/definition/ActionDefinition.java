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
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author Ivica Cardic
 */
@Schema(
        name = "ActionDefinition",
        description =
                "An action is a a portion of reusable code that accomplish a specific task. When building a workflow, each action is represented as a task inside the workflow. The task 'type' property is defined as [component name]/v[component version]/[action name]. Action properties are used to set properties of the task inside the workflow.")
public sealed class ActionDefinition permits ComponentDSL.ModifiableActionDefinition {

    public static final String ACTION = "action";

    protected Display display;
    protected Object exampleOutput;
    protected Map<String, Object> metadata;
    protected String name;
    protected List<Property<? extends Property<?>>> output;
    protected List<Property<?>> properties;

    @JsonIgnore
    protected BiFunction<Context, ExecutionParameters, Object> performFunction;

    protected ActionDefinition(String name) {
        this.name = name;
    }

    public Display getDisplay() {
        return display;
    }

    @Schema(name = "exampleOutput", description = "The example of the action's output.")
    public Object getExampleOutput() {
        return exampleOutput;
    }

    @Schema(name = "metadata", description = "Additional data that can be used during processing.")
    public Map<String, Object> getMetadata() {
        return metadata == null ? null : new HashMap<>(metadata);
    }

    @Schema(name = "name", description = "The action name.")
    public String getName() {
        return name;
    }

    @Schema(name = "output", description = "The output schema of an execution result.")
    public List<Property<? extends Property<?>>> getOutput() {
        return output;
    }

    @Schema(name = "properties", description = "The list of action properties.")
    public List<Property<?>> getProperties() {
        return properties;
    }

    /**
     * The code that should be performed when the action is executed as a task whe running inside the workflow engine.
     *
     * @return a perform function implementation
     */
    public BiFunction<Context, ExecutionParameters, Object> getPerformFunction() {
        return performFunction;
    }
}
