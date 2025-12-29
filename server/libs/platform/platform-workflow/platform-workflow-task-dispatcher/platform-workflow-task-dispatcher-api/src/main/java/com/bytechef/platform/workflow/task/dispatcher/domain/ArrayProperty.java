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

package com.bytechef.platform.workflow.task.dispatcher.domain;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class ArrayProperty extends ValueProperty<List<?>> {

    private List<? extends Property> items;
    private Long maxItems;
    private Long minItems;
    private boolean multipleValues; // Defaults to true
    private List<Option> options;

    private ArrayProperty() {
    }

    public ArrayProperty(
        com.bytechef.platform.workflow.task.dispatcher.definition.Property.ArrayProperty arrayProperty) {

        super(arrayProperty);

        this.items = CollectionUtils.map(
            OptionalUtils.orElse(arrayProperty.getItems(), List.of()),
            property -> (Property) toProperty(property));
        this.maxItems = OptionalUtils.orElse(arrayProperty.getMaxItems(), null);
        this.minItems = OptionalUtils.orElse(arrayProperty.getMinItems(), null);
        this.multipleValues = OptionalUtils.orElse(arrayProperty.getMultipleValues(), true);
        this.options = CollectionUtils.map(OptionalUtils.orElse(arrayProperty.getOptions(), List.of()), Option::new);
    }

    @Override
    public Object accept(PropertyVisitor propertyVisitor) {
        return propertyVisitor.visit(this);
    }

    public boolean isMultipleValues() {
        return multipleValues;
    }

    public List<? extends Property> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Nullable
    public Long getMaxItems() {
        return maxItems;
    }

    @Nullable
    public Long getMinItems() {
        return minItems;
    }

    public List<Option> getOptions() {
        return Collections.unmodifiableList(options);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ArrayProperty that)) {
            return false;
        }

        return multipleValues == that.multipleValues && Objects.equals(items, that.items) &&
            Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items, multipleValues, options);
    }

    @Override
    public String toString() {
        return "ArrayProperty{" +
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
            ", options=" + options +
            ", multipleValues=" + multipleValues +
            ", minItems=" + minItems +
            ", maxItems=" + maxItems +
            ", items=" + items +
            "} " + super.toString();
    }
}
