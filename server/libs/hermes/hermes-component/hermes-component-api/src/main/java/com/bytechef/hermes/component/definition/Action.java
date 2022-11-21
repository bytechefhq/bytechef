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

import com.bytechef.hermes.component.PerformFunction;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Schema(
        name = "Action",
        description =
                "An action is a a portion of reusable code that accomplish a specific task. When building a workflow, each action is represented as a task inside the workflow. The task 'type' property is defined as [component name]/v[component version]/[action name]. Action properties are used to set properties of the task inside the workflow.")
public final class Action {

    public static final String ACTION = "action";
    private Display display;
    private Object exampleOutput;
    private Map<String, Object> metadata;
    private String name;
    private List<Property<? extends Property<?>>> output;
    private List<Property<?>> properties;

    @JsonIgnore
    private PerformFunction performFunction;

    private Action() {}

    public Action(String name) {
        this.name = name;
    }

    public Action display(Display display) {
        this.display = display;

        return this;
    }

    public Action exampleOutput(Object exampleOutput) {
        this.exampleOutput = exampleOutput;

        return this;
    }

    @SuppressWarnings("unchecked")
    public Action metadata(String key, String value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }

        this.metadata.put(key, value);

        return this;
    }

    @SuppressFBWarnings("EI2")
    public Action metadata(Map<String, Object> metadata) {
        this.metadata = metadata;

        return this;
    }

    public Action output(Property... output) {
        this.output = List.of(output);

        return this;
    }

    public Action performFunction(PerformFunction performFunction) {
        this.performFunction = performFunction;

        return this;
    }

    public Action properties(Property... properties) {
        this.properties = List.of(properties);

        return this;
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

    public PerformFunction getPerformFunction() {
        return performFunction;
    }
}
