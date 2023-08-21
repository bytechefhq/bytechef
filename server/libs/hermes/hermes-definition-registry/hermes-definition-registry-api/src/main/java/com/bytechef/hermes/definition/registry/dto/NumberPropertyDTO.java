
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

package com.bytechef.hermes.definition.registry.dto;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.definition.Property.NumberProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class NumberPropertyDTO extends ValuePropertyDTO<Double> {

    private Integer maxValue;
    private Integer minValue;
    private Integer numberPrecision;
    private List<OptionDTO> options;
    private OptionsDataSourceDTO optionsDataSource;

    private NumberPropertyDTO() {
    }

    public NumberPropertyDTO(NumberProperty numberProperty) {
        super(numberProperty);

        this.maxValue = OptionalUtils.orElse(numberProperty.getMaxValue(), null);
        this.minValue = OptionalUtils.orElse(numberProperty.getMinValue(), null);
        this.numberPrecision = OptionalUtils.orElse(numberProperty.getNumberPrecision(), null);
        this.options = CollectionUtils.map(
            OptionalUtils.orElse(numberProperty.getOptions(), List.of()), OptionDTO::new);
        this.optionsDataSource = OptionalUtils.mapOrElse(
            numberProperty.getOptionsDataSource(), OptionsDataSourceDTO::new, null);
    }

    @Override
    public Object accept(PropertyVisitor propertyVisitor) {
        return propertyVisitor.visit(this);
    }

    public Optional<Integer> getMaxValue() {
        return Optional.ofNullable(maxValue);
    }

    public Optional<Integer> getMinValue() {
        return Optional.ofNullable(minValue);
    }

    public Optional<Integer> getNumberPrecision() {
        return Optional.ofNullable(numberPrecision);
    }

    public List<OptionDTO> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public Optional<OptionsDataSourceDTO> getOptionsDataSource() {
        return Optional.ofNullable(optionsDataSource);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof NumberPropertyDTO that))
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
        return "NumberPropertyDTO{" +
            "maxValue=" + maxValue +
            ", minValue=" + minValue +
            ", numberPrecision=" + numberPrecision +
            ", options=" + options +
            ", optionsDataSource=" + optionsDataSource +
            ", controlType=" + controlType +
            ", defaultValue=" + defaultValue +
            ", exampleValue=" + exampleValue +
            "} ";
    }
}
