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
 *     <p>Used for specifying a task auth type.
 */
public class AuthenticationDescriptor {

    private String description;
    private String displayName;
    private String icon;
    private String name;
    private List<TaskProperty<?>> properties;
    private String subtitle;

    AuthenticationDescriptor(String name) {
        this.name = name;
    }

    public AuthenticationDescriptor description(String description) {
        this.description = description;

        return this;
    }

    public AuthenticationDescriptor displayName(String displayName) {
        this.displayName = displayName;

        return this;
    }

    public AuthenticationDescriptor icon(String icon) {
        this.icon = icon;

        return this;
    }

    public AuthenticationDescriptor properties(TaskProperty<?>... properties) {
        this.properties = List.of(properties);

        return this;
    }

    public AuthenticationDescriptor subtitle(String subtitle) {
        this.subtitle = subtitle;

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
}
