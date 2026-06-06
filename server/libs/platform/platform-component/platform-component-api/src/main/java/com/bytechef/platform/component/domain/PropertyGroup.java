/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.component.domain;

import com.bytechef.commons.util.CollectionUtils;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class PropertyGroup {

    private String label;
    private String name;
    private List<? extends Property> properties;

    private PropertyGroup() {
    }

    public PropertyGroup(com.bytechef.component.definition.PropertyGroup propertyGroup) {
        this.label = propertyGroup.getLabel()
            .orElse(null);
        this.name = propertyGroup.getName();
        this.properties = CollectionUtils.map(propertyGroup.getProperties(), Property::toProperty);
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public List<? extends Property> getProperties() {
        return properties == null ? List.of() : Collections.unmodifiableList(properties);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PropertyGroup that)) {
            return false;
        }

        return Objects.equals(label, that.label) && Objects.equals(name, that.name) &&
            Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, name, properties);
    }

    @Override
    public String toString() {
        return "PropertyGroup{" +
            "name='" + name + '\'' +
            ", label='" + label + '\'' +
            ", properties=" + properties +
            '}';
    }
}
