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

package com.bytechef.hermes.descriptor.domain;

import java.util.List;

/**
 * @author Ivica Cardic
 *     <p>Used for specifying a task type.
 */
public class TaskDescriptor {

    private Authentication auth;
    private String category;
    private String description;
    private String displayName;
    private String icon;
    private String name;
    private List<TaskOperation> operations;
    private String subtitle;
    private float version = 1.0f;
    private String[] tags;

    TaskDescriptor() {}

    public TaskDescriptor auth(TaskPropertyOption... options) {
        this.auth = new Authentication().options(options);

        return this;
    }

    public TaskDescriptor category(String category) {
        this.category = category;

        return this;
    }

    public TaskDescriptor description(String description) {
        this.description = description;

        return this;
    }

    public TaskDescriptor displayName(String displayName) {
        this.displayName = displayName;

        return this;
    }

    public TaskDescriptor icon(String icon) {
        this.icon = icon;

        return this;
    }

    public TaskDescriptor name(String name) {
        this.name = name;

        return this;
    }

    public TaskDescriptor operations(TaskOperation... operations) {
        this.operations = List.of(operations);

        return this;
    }

    public TaskDescriptor subtitle(String subtitle) {
        this.subtitle = subtitle;

        return this;
    }

    public TaskDescriptor tags(String... tags) {
        this.tags = tags;

        return this;
    }

    public TaskDescriptor version(float version) {
        this.version = version;

        return this;
    }

    public Authentication getAuth() {
        return auth;
    }

    public String getCategory() {
        return category;
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

    public String[] getTags() {
        return tags;
    }

    public float getVersion() {
        return version;
    }
}
