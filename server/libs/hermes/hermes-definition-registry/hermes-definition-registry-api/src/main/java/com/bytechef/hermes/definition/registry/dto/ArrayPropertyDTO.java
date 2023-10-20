
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
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.definition.Property.ArrayProperty;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class ArrayPropertyDTO extends ValuePropertyDTO<Object[]> {

    private List<? extends PropertyDTO> items;
    private boolean multipleValues; // Defaults to true
    private List<OptionDTO> options;
    private OptionsDataSourceDTO optionsDataSource;

    private ArrayPropertyDTO() {
    }

    public ArrayPropertyDTO(ArrayProperty arrayProperty) {
        super(arrayProperty);

        this.items = CollectionUtils.map(
            OptionalUtils.orElse(arrayProperty.getItems(), List.of(ComponentDSL.string())),
            valueProperty -> (ValuePropertyDTO<?>) PropertyDTO.toPropertyDTO(valueProperty));
        this.multipleValues = OptionalUtils.orElse(arrayProperty.getMultipleValues(), true);
        this.options = CollectionUtils.map(OptionalUtils.orElse(arrayProperty.getOptions(), List.of()), OptionDTO::new);
        this.optionsDataSource = OptionalUtils.mapOrElse(
            arrayProperty.getOptionsDataSource(), OptionsDataSourceDTO::new, null);
    }

    @Override
    public Object accept(PropertyVisitor propertyVisitor) {
        return propertyVisitor.visit(this);
    }

    public List<? extends PropertyDTO> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean isMultipleValues() {
        return multipleValues;
    }

    public List<OptionDTO> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public Optional<OptionsDataSourceDTO> getOptionsDataSource() {
        return Optional.ofNullable(optionsDataSource);
    }
}
