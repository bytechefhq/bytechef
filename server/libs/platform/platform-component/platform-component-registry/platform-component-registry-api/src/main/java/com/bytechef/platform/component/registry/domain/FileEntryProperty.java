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
import com.bytechef.component.definition.Property;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class FileEntryProperty extends ValueProperty<Map<String, ?>> {

    private final List<? extends ValueProperty<?>> properties;

    public FileEntryProperty(Property.FileEntryProperty fileEntryProperty) {
        this.properties = CollectionUtils.map(
            fileEntryProperty.getProperties(),
            valueProperty -> (ValueProperty<?>) toProperty(valueProperty));
    }

    @Override
    public Object accept(PropertyVisitor propertyVisitor) {
        return propertyVisitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof FileEntryProperty that)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        return Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), properties);
    }

    public List<? extends ValueProperty<?>> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "FileEntryProperty{" +
            "properties=" + properties +
            ", controlType=" + controlType +
            ", defaultValue=" + defaultValue +
            ", exampleValue=" + exampleValue +
            "} " + super.toString();
    }
}
