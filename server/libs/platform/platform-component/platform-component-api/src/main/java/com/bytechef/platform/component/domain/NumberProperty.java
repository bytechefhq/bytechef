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
import com.bytechef.component.definition.Property;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public class NumberProperty extends ValueProperty<Double> implements OptionsDataSourceAware {

    private Integer maxNumberPrecision;
    private Double maxValue;
    private Integer minNumberPrecision;
    private Double minValue;
    private Integer numberPrecision;
    private List<Option> options;
    private OptionsDataSource optionsDataSource;

    private NumberProperty() {
    }

    public NumberProperty(Property.NumberProperty numberProperty) {
        super(numberProperty);

        this.maxNumberPrecision = OptionalUtils.orElse(numberProperty.getMaxNumberPrecision(), null);
        this.maxValue = OptionalUtils.orElse(numberProperty.getMaxValue(), null);
        this.minNumberPrecision = OptionalUtils.orElse(numberProperty.getMinNumberPrecision(), null);
        this.minValue = OptionalUtils.orElse(numberProperty.getMinValue(), null);
        this.numberPrecision = OptionalUtils.orElse(numberProperty.getNumberPrecision(), null);
        this.options = CollectionUtils.map(
            OptionalUtils.orElse(numberProperty.getOptions(), List.of()), Option::new);
        this.optionsDataSource = OptionalUtils.mapOrElse(
            numberProperty.getOptionsDataSource(), OptionsDataSource::new, null);
    }

    @Override
    public Object accept(PropertyVisitor propertyVisitor) {
        return propertyVisitor.visit(this);
    }

    public Integer getMaxNumberPrecision() {
        return maxNumberPrecision;
    }

    @Nullable
    public Double getMaxValue() {
        return maxValue;
    }

    @Nullable
    public Integer getMinNumberPrecision() {
        return minNumberPrecision;
    }

    @Nullable
    public Double getMinValue() {
        return minValue;
    }

    @Nullable
    public Integer getNumberPrecision() {
        return numberPrecision;
    }

    public List<Option> getOptions() {
        return Collections.unmodifiableList(options);
    }

    @Nullable
    public OptionsDataSource getOptionsDataSource() {
        return optionsDataSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof NumberProperty that))
            return false;
        return Objects.equals(maxValue, that.maxValue) && Objects.equals(minValue, that.minValue)
            && Objects.equals(numberPrecision, that.numberPrecision) && Objects.equals(options, that.options)
            && Objects.equals(optionsDataSource, that.optionsDataSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxValue, minValue, numberPrecision, options, optionsDataSource);
    }

    @Override
    public String toString() {
        return "NumberProperty{" +
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
            ", optionsDataSource=" + optionsDataSource +
            ", options=" + options +
            ", numberPrecision=" + numberPrecision +
            ", minValue=" + minValue +
            ", minNumberPrecision=" + minNumberPrecision +
            ", maxValue=" + maxValue +
            ", maxNumberPrecision=" + maxNumberPrecision +
            "} " + super.toString();
    }
}
