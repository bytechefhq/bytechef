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
import com.bytechef.commons.util.OptionalUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class ObjectProperty extends ValueProperty<Map<String, ?>> implements OptionsDataSourceAware {

    private List<? extends Property> additionalProperties;
    private boolean multipleValues;
    private List<Option> options;
    private OptionsDataSource optionsDataSource;
    private List<? extends Property> properties;

    private ObjectProperty() {
    }

    public ObjectProperty(com.bytechef.component.definition.Property.ObjectProperty objectProperty) {
        super(objectProperty);

        this.additionalProperties = CollectionUtils.map(
            OptionalUtils.orElse(objectProperty.getAdditionalProperties(), List.of()),
            valueProperty -> (ValueProperty<?>) toProperty(valueProperty));
        this.multipleValues = OptionalUtils.orElse(objectProperty.getMultipleValues(), true);
        this.options =
            CollectionUtils.map(OptionalUtils.orElse(objectProperty.getOptions(), List.of()), Option::new);
        this.optionsDataSource = OptionalUtils.mapOrElse(
            objectProperty.getOptionsDataSource(), OptionsDataSource::new, null);
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(objectProperty.getProperties(), List.of()),
            valueProperty -> (ValueProperty<?>) toProperty(valueProperty));
    }

    @Override
    public Object accept(PropertyVisitor propertyVisitor) {
        return propertyVisitor.visit(this);
    }

    public List<? extends Property> getAdditionalProperties() {
        return Collections.unmodifiableList(additionalProperties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ObjectProperty that))
            return false;
        return multipleValues == that.multipleValues && Objects.equals(additionalProperties, that.additionalProperties)
            && Objects.equals(options, that.options)
            && Objects.equals(optionsDataSource, that.optionsDataSource) && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(additionalProperties, multipleValues, options, optionsDataSource, properties);
    }

    public boolean isMultipleValues() {
        return multipleValues;
    }

    public List<Option> getOptions() {
        return Collections.unmodifiableList(options);
    }

    @Nullable
    public OptionsDataSource getOptionsDataSource() {
        return optionsDataSource;
    }

    public List<? extends Property> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    @Override
    public String toString() {
        return "ObjectProperty{" +
            "name='" + name + '\'' +
            ", type=" + type +
            ", controlType=" + controlType +
            ", required=" + required +
            ", hidden=" + hidden +
            ", expressionEnabled=" + expressionEnabled +
            ", displayCondition='" + displayCondition + '\'' +
            ", description='" + description + '\'' +
            ", advancedOption=" + advancedOption +
            ", exampleValue=" + exampleValue +
            ", defaultValue=" + defaultValue +
            ", properties=" + properties +
            ", optionsDataSource=" + optionsDataSource +
            ", options=" + options +
            ", multipleValues=" + multipleValues +
            ", additionalProperties=" + additionalProperties +
            "} " + super.toString();
    }
}
