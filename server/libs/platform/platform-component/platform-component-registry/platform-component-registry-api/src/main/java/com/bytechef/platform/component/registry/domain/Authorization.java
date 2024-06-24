/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.component.registry.domain;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class Authorization {

    private String description;
    private List<String> detectOn;
    private String name;
    private List<? extends Property> properties;
    private List<Object> refreshOn;
    private String title;
    private AuthorizationType type;

    private Authorization() {
    }

    public Authorization(com.bytechef.component.definition.Authorization authorization) {
        this.description = OptionalUtils.orElse(authorization.getDescription(), null);
        this.detectOn = OptionalUtils.orElse(authorization.getDetectOn(), List.of());
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(authorization.getProperties(), List.of()),
            valueProperty -> (ValueProperty<?>) Property.toProperty(valueProperty));

        this.type = authorization.getType();

        this.name = type.getName();

        this.title = OptionalUtils.orElse(authorization.getTitle(), name);

        this.refreshOn = OptionalUtils.orElse(authorization.getRefreshOn(), List.of(401));
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public List<? extends Property> getProperties() {
        return properties;
    }

    public String getTitle() {
        return title;
    }

    public AuthorizationType getType() {
        return type;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Authorization that)) {
            return false;
        }

        return Objects.equals(description, that.description) && Objects.equals(detectOn, that.detectOn) &&
            Objects.equals(name, that.name) && Objects.equals(properties, that.properties) &&
            Objects.equals(refreshOn, that.refreshOn) && Objects.equals(title, that.title) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, name, properties, title, type);
    }

    public List<String> getDetectOn() {
        return detectOn;
    }

    public List<Object> getRefreshOn() {
        return refreshOn;
    }

    @Override
    public String toString() {
        return "Authorization{" +
            "name='" + name + '\'' +
            ", type=" + type +
            ", title='" + title + '\'' +
            ", description='" + description + '\'' +
            ", properties=" + properties +
            ", refreshOn=" + refreshOn +
            ", detectOn=" + detectOn +
            '}';
    }
}
