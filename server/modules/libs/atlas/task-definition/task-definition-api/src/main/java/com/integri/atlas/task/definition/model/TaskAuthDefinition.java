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
 *
 * Used for specifying a task auth type.
 */
public class TaskAuthDefinition {

    private String description;
    private String displayName;
    private String icon;
    private String name;
    private List<TaskProperty<?>> properties;
    private String subtitle;
    private float version = 1;

    TaskAuthDefinition(String name) {
        this.name = name;
    }

    public TaskAuthDefinition description(String description) {
        this.description = description;

        return this;
    }

    public TaskAuthDefinition displayName(String displayName) {
        this.displayName = displayName;

        return this;
    }

    public TaskAuthDefinition icon(String icon) {
        this.icon = icon;

        return this;
    }

    public TaskAuthDefinition properties(TaskProperty<?>... properties) {
        this.properties = List.of(properties);

        return this;
    }

    public TaskAuthDefinition subtitle(String subtitle) {
        this.subtitle = subtitle;

        return this;
    }

    public TaskAuthDefinition version(float version) {
        this.version = version;

        return this;
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

    public List<TaskProperty<?>> getProperties() {
        return properties;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public float getVersion() {
        return version;
    }
}
