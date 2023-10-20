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

package com.integri.atlas.task.definition.model;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public final class TaskPropertyOption {

    private String description;
    private DisplayOption displayOption;
    private String name;
    private Object value;

    TaskPropertyOption(String name, int value, String description) {
        this.name = name;
        this.value = value;
        this.description = description;
    }

    TaskPropertyOption(String name, String value, String description) {
        this.name = name;
        this.value = value;
        this.description = description;
    }

    public TaskPropertyOption description(String description) {
        this.description = description;

        return this;
    }

    public TaskPropertyOption displayOption(DisplayOption.DisplayOptionEntry... displayOptionEntries) {
        this.displayOption = DisplayOption.build(List.of(displayOptionEntries));

        return this;
    }

    public String getDescription() {
        return description;
    }

    public DisplayOption getDisplayOption() {
        return displayOption;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }
}
