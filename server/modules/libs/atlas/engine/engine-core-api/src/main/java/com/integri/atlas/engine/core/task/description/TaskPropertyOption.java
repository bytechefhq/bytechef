/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.engine.core.task.description;

import static com.integri.atlas.engine.core.task.description.TaskPropertyOptionValue.optionValue;

/**
 * @author Ivica Cardic
 */
public final class TaskPropertyOption {

    private String name;
    private TaskPropertyOptionValue value;
    private String description;

    public static TaskPropertyOption option(String name, int value) {
        return new TaskPropertyOption(name, optionValue(value), null);
    }

    public static TaskPropertyOption option(String name, String value) {
        return new TaskPropertyOption(name, optionValue(value), null);
    }

    public static TaskPropertyOption option(String name, int value, String description) {
        return new TaskPropertyOption(name, optionValue(value), description);
    }

    public static TaskPropertyOption option(String name, String value, String description) {
        return new TaskPropertyOption(name, optionValue(value), description);
    }

    TaskPropertyOption(String name, TaskPropertyOptionValue value, String description) {
        this.name = name;
        this.value = value;
        this.description = description;
    }

    public TaskPropertyOption name(String name) {
        this.name = name;

        return this;
    }

    public TaskPropertyOption description(String description) {
        this.description = description;

        return this;
    }

    public TaskPropertyOption value(TaskPropertyOptionValue value) {
        this.value = value;

        return this;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public TaskPropertyOptionValue getValue() {
        return value;
    }
}
