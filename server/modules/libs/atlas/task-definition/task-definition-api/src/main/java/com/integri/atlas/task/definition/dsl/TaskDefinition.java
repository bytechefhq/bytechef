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
 *
 * Used for specifying a task type.
 */
public class TaskDefinition {

    private TaskAuth auth;
    private String description;
    private String displayName;
    private String icon;
    private String name;
    private List<TaskOperation> operations;
    private String subtitle;
    private float version = 1;

    TaskDefinition() {}

    public TaskDefinition auth(TaskPropertyOption... options) {
        this.auth = new TaskAuth().options(options);

        return this;
    }

    public TaskDefinition description(String description) {
        this.description = description;

        return this;
    }

    public TaskDefinition displayName(String displayName) {
        this.displayName = displayName;

        return this;
    }

    public TaskDefinition icon(String icon) {
        this.icon = icon;

        return this;
    }

    public TaskDefinition name(String name) {
        this.name = name;

        return this;
    }

    public TaskDefinition operations(TaskOperation... operations) {
        this.operations = List.of(operations);

        return this;
    }

    public TaskDefinition subtitle(String subtitle) {
        this.subtitle = subtitle;

        return this;
    }

    public TaskDefinition version(float version) {
        this.version = version;

        return this;
    }

    public TaskAuth getAuth() {
        return auth;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public List<TaskOperation> getOperations() {
        return operations;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public float getVersion() {
        return version;
    }
}
