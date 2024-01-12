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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class ObjectProperty extends ValueProperty<Map<String, Object>> {

    private List<? extends Property> additionalProperties;
    private boolean multipleValues;
    private String objectType;
    private List<Option> options;
    private OptionsDataSource optionsDataSource;
    private List<? extends Property> properties;

    private ObjectProperty() {
    }

    public ObjectProperty(com.bytechef.component.definition.Property.ObjectProperty objectProperty) {
        super(objectProperty);

        this.additionalProperties = CollectionUtils.map(
            OptionalUtils.orElse(objectProperty.getAdditionalProperties(), List.of()),
            valueProperty -> (ValueProperty<?>) toProperty(
                (com.bytechef.component.definition.Property) valueProperty));
        this.multipleValues = OptionalUtils.orElse(objectProperty.getMultipleValues(), true);
        this.objectType = OptionalUtils.orElse(objectProperty.getObjectType(), null);
        this.options =
            CollectionUtils.map(OptionalUtils.orElse(objectProperty.getOptions(), List.of()), Option::new);
        this.optionsDataSource = OptionalUtils.mapOrElse(
            objectProperty.getOptionsDataSource(), OptionsDataSource::new, null);
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(objectProperty.getProperties(), List.of()),
            valueProperty -> (ValueProperty<?>) toProperty(
                (com.bytechef.component.definition.Property) valueProperty));
    }

    @Override
    public Object accept(PropertyVisitor propertyVisitor) {
        return propertyVisitor.visit(this);
    }

    public List<? extends Property> getAdditionalProperties() {
        return Collections.unmodifiableList(additionalProperties);
    }

    public boolean isMultipleValues() {
        return multipleValues;
    }

    public Optional<String> getObjectType() {
        return Optional.ofNullable(objectType);
    }

    public List<Option> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public Optional<OptionsDataSource> getOptionsDataSource() {
        return Optional.ofNullable(optionsDataSource);
    }

    public List<? extends Property> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ObjectProperty that))
            return false;
        return multipleValues == that.multipleValues && Objects.equals(additionalProperties, that.additionalProperties)
            && Objects.equals(objectType, that.objectType) && Objects.equals(options, that.options)
            && Objects.equals(optionsDataSource, that.optionsDataSource) && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(additionalProperties, multipleValues, objectType, options, optionsDataSource, properties);
    }

    @Override
    public String toString() {
        return "ObjectProperty{" +
            "additionalProperties=" + additionalProperties +
            ", multipleValues=" + multipleValues +
            ", objectType='" + objectType + '\'' +
            ", options=" + options +
            ", optionsDataSource=" + optionsDataSource +
            ", properties=" + properties +
            ", controlType=" + controlType +
            ", defaultValue=" + defaultValue +
            ", exampleValue=" + exampleValue +
            "} ";
    }
}
