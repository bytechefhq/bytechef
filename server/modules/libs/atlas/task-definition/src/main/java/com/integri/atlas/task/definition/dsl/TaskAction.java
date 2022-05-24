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

package com.integri.atlas.task.definition.dsl;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public class TaskAction {

    private String description;
    private String name;
    private List<TaskProperty<?>> inputs;
    private List<TaskProperty<?>> outputs;
    private String displayName;

    TaskAction(String name) {
        this.name = name;
    }

    public TaskAction description(String description) {
        this.description = description;

        return this;
    }

    public TaskAction displayName(String displayName) {
        this.displayName = displayName;

        return this;
    }

    public TaskAction inputs(TaskProperty<?>... inputs) {
        this.inputs = List.of(inputs);

        return this;
    }

    public TaskAction outputs(TaskProperty<?>... outputs) {
        this.outputs = List.of(outputs);

        return this;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getName() {
        return name;
    }

    public List<TaskProperty<?>> getInputs() {
        return inputs;
    }

    public List<TaskProperty<?>> getOutputs() {
        return outputs;
    }
}
