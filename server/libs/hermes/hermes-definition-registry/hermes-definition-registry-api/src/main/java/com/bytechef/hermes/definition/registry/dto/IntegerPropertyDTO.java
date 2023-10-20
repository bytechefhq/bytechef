
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
import com.bytechef.hermes.definition.Property.IntegerProperty;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class IntegerPropertyDTO extends ValuePropertyDTO<Integer> {

    private Integer maxValue;
    private Integer minValue;
    private List<OptionDTO> options;
    private OptionsDataSourceDTO optionsDataSource;

    private IntegerPropertyDTO() {
    }

    public IntegerPropertyDTO(IntegerProperty integerProperty) {
        super(integerProperty);

        this.maxValue = OptionalUtils.orElse(integerProperty.getMaxValue(), null);
        this.minValue = OptionalUtils.orElse(integerProperty.getMinValue(), null);
        this.options = CollectionUtils.map(
            OptionalUtils.orElse(integerProperty.getOptions(), List.of()), OptionDTO::new);
        this.optionsDataSource = OptionalUtils.mapOrElse(
            integerProperty.getOptionsDataSource(), OptionsDataSourceDTO::new, null);
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

    public List<OptionDTO> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public Optional<OptionsDataSourceDTO> getOptionsDataSource() {
        return Optional.ofNullable(optionsDataSource);
    }
}
