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

/**
 * @author Ivica Cardic
 */
public class TaskCredential {

    private String name;
    private Boolean required;
    private DisplayOption displayOption;

    public static TaskCredential credential() {
        return new TaskCredential();
    }

    public static TaskCredential credential(String name) {
        return TaskCredential.credential().name(name);
    }

    public TaskCredential name(String name) {
        this.name = name;

        return this;
    }

    public TaskCredential required(Boolean required) {
        this.required = required;

        return this;
    }

    public TaskCredential displayOption(DisplayOption displayOption) {
        this.displayOption = displayOption;

        return this;
    }

    public String getName() {
        return name;
    }

    public Boolean getRequired() {
        return required;
    }

    public DisplayOption getDisplayOption() {
        return displayOption;
    }
}
