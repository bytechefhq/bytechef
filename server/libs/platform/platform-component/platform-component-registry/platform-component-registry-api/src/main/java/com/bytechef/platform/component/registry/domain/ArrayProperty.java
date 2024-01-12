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
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class ArrayProperty extends ValueProperty<List<Object>> {

    private List<? extends Property> items;
    private Long maxItems;
    private Long minItems;
    private boolean multipleValues; // Defaults to true
    private List<Option> options;
    private OptionsDataSource optionsDataSource;

    private ArrayProperty() {
    }

    public ArrayProperty(com.bytechef.component.definition.Property.ArrayProperty arrayProperty) {
        super(arrayProperty);

        this.items = CollectionUtils.map(
            OptionalUtils.orElse(arrayProperty.getItems(), List.of()),
            valueProperty -> (ValueProperty<?>) toProperty(
                (com.bytechef.component.definition.Property) valueProperty));
        this.maxItems = OptionalUtils.orElse(arrayProperty.getMaxItems(), null);
        this.minItems = OptionalUtils.orElse(arrayProperty.getMinItems(), null);
        this.multipleValues = OptionalUtils.orElse(arrayProperty.getMultipleValues(), true);
        this.options = CollectionUtils.map(OptionalUtils.orElse(arrayProperty.getOptions(), List.of()), Option::new);
        this.optionsDataSource = OptionalUtils.mapOrElse(
            arrayProperty.getOptionsDataSource(), OptionsDataSource::new, null);
    }

    @Override
    public Object accept(PropertyVisitor propertyVisitor) {
        return propertyVisitor.visit(this);
    }

    public List<? extends Property> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean isMultipleValues() {
        return multipleValues;
    }

    public Optional<Long> getMaxItems() {
        return Optional.ofNullable(maxItems);
    }

    public Optional<Long> getMinItems() {
        return Optional.ofNullable(minItems);
    }

    public List<Option> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public Optional<OptionsDataSource> getOptionsDataSource() {
        return Optional.ofNullable(optionsDataSource);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ArrayProperty that))
            return false;
        return multipleValues == that.multipleValues && Objects.equals(items, that.items)
            && Objects.equals(options, that.options) && Objects.equals(optionsDataSource, that.optionsDataSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items, multipleValues, options, optionsDataSource);
    }

    @Override
    public String toString() {
        return "ArrayProperty{" +
            "items=" + items +
            ", multipleValues=" + multipleValues +
            ", options=" + options +
            ", optionsDataSource=" + optionsDataSource +
            ", controlType=" + controlType +
            ", defaultValue=" + defaultValue +
            ", exampleValue=" + exampleValue +
            "} ";
    }
}
