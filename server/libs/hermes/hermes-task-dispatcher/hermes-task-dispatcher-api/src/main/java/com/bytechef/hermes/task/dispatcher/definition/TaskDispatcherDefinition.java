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
import java.util.List;

/**
 * Used for specifying a task dispatcher description.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public final class TaskDispatcherDefinition implements Definition {

    private Display display;
    private String name;
    protected List<Property<?>> inputs;
    private Resources resources;
    private int version = VERSION_1;

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

    public TaskDispatcherDefinition inputs(Property<?>... inputs) {
        this.inputs = List.of(inputs);

        return this;
    }

    public TaskDispatcherDefinition version(int version) {
        this.version = version;

        return this;
    }

    @Override
    public Display getDisplay() {
        return display;
    }

    public List<Property<?>> getInputs() {
        return inputs;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Resources getResources() {
        return resources;
    }

    @Override
    public int getVersion() {
        return version;
    }
}
