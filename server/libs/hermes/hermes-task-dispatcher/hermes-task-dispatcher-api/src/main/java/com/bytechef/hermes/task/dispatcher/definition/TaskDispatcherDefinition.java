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

package com.bytechef.hermes.task.dispatcher.definition;

import static com.bytechef.hermes.task.dispatcher.constants.Versions.VERSION_1;

import com.bytechef.hermes.definition.Definition;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Used for specifying a task dispatcher description.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
@Schema(
        name = "TaskDispatcherDefinition",
        description = "A task dispatcher defines a strategy for dispatching tasks to be executed.")
public final class TaskDispatcherDefinition implements Definition {

    private Display display;
    private String name;
    private List<Property<? extends Property<?>>> output;
    protected List<Property<?>> properties;
    private Resources resources;
    private int version = VERSION_1;
    protected List<Property<?>> taskProperties;

    private TaskDispatcherDefinition() {}

    public TaskDispatcherDefinition(String name) {
        this.name = name;
    }

    public TaskDispatcherDefinition display(Display display) {
        this.display = display;

        return this;
    }

    public TaskDispatcherDefinition display(Resources resources) {
        this.resources = resources;

        return this;
    }

    public TaskDispatcherDefinition output(Property... output) {
        this.output = List.of(output);

        return this;
    }

    public TaskDispatcherDefinition properties(Property<?>... properties) {
        this.properties = List.of(properties);

        return this;
    }

    public TaskDispatcherDefinition version(int version) {
        this.version = version;

        return this;
    }

    public TaskDispatcherDefinition taskProperties(Property<?>... taskProperties) {
        this.taskProperties = List.of(taskProperties);

        return this;
    }

    @Override
    public Display getDisplay() {
        return display;
    }

    @Override
    @Schema(name = "name", description = "The connection name.")
    public String getName() {
        return name;
    }

    @Schema(name = "output", description = "The output schema of a task dispatching result.")
    public List<Property<? extends Property<?>>> getOutput() {
        return output;
    }

    @Schema(name = "properties", description = "Properties of the connection.")
    public List<Property<?>> getProperties() {
        return properties;
    }

    @Override
    public Resources getResources() {
        return resources;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Schema(name = "taskProperties", description = "Properties used to define tasks to be dispatched.")
    public List<Property<?>> getTaskProperties() {
        return taskProperties;
    }
}
