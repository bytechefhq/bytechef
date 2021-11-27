/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.core.task.description;

import static com.integri.atlas.engine.core.task.description.TaskPropertyOptionValue.optionValue;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public sealed interface TaskPropertyOption
    permits TaskProperty, TaskPropertyOption.TaskPropertyOptionItem, TaskPropertyOption.TaskPropertyGroup {
    static TaskPropertyOption group(String name, String displayName, List<TaskProperty> properties) {
        return new TaskPropertyGroup(name, displayName, properties);
    }

    static TaskPropertyOption option(String name, int value) {
        return new TaskPropertyOptionItem(name, TaskPropertyOptionValue.optionValue(value), null);
    }

    static TaskPropertyOption option(String name, String value) {
        return new TaskPropertyOptionItem(name, TaskPropertyOptionValue.optionValue(value), null);
    }

    static TaskPropertyOption option(String name, int value, String description) {
        return new TaskPropertyOptionItem(name, TaskPropertyOptionValue.optionValue(value), description);
    }

    static TaskPropertyOption option(String name, String value, String description) {
        return new TaskPropertyOptionItem(name, TaskPropertyOptionValue.optionValue(value), description);
    }

    record TaskPropertyGroup(String name, String displayName, List<TaskProperty> properties)
        implements TaskPropertyOption {
        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<TaskProperty> getProperties() {
            return properties;
        }
    }

    record TaskPropertyOptionItem(String name, TaskPropertyOptionValue value, String description)
        implements TaskPropertyOption {
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
}
