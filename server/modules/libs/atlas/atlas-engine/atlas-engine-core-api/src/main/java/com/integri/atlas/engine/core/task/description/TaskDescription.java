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

import java.util.List;

/**
 * @author Ivica Cardic
 *
 * Used for describing a task type.
 */
public class TaskDescription {

    private TaskAuthentication authentication;
    private String description;
    private String displayName;
    private String name;
    private String icon;
    private List<TaskProperty<?>> properties;
    private String subtitle;
    private float version = 1;

    public static TaskDescription task(String name) {
        return new TaskDescription().name(name);
    }

    public TaskDescription authentication(TaskAuthentication authentication) {
        this.authentication = authentication;

        return this;
    }

    public TaskDescription description(String description) {
        this.description = description;

        return this;
    }

    public TaskDescription displayName(String displayName) {
        this.displayName = displayName;

        return this;
    }

    public TaskDescription name(String name) {
        this.name = name;

        return this;
    }

    public TaskDescription icon(String icon) {
        this.icon = icon;

        return this;
    }

    public TaskDescription properties(TaskProperty... properties) {
        this.properties = List.of(properties);

        return this;
    }

    public TaskDescription subtitle(String subtitle) {
        this.subtitle = subtitle;

        return this;
    }

    public TaskDescription version(float version) {
        this.version = version;

        return this;
    }

    public TaskAuthentication getAuthentication() {
        return authentication;
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

    public String getIcon() {
        return icon;
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
