
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
import com.bytechef.hermes.definition.Property.ObjectProperty;

import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class ObjectPropertyDTO extends ValuePropertyDTO<Object> {

    private final List<? extends PropertyDTO> additionalProperties;
    private final boolean multipleValues;
    private final String objectType;
    private final List<OptionDTO> options;
    private final OptionsDataSourceDTO optionsDataSource;
    private final List<? extends PropertyDTO> properties;

    public ObjectPropertyDTO(ObjectProperty objectProperty) {
        super(objectProperty);

        this.additionalProperties = CollectionUtils.map(
            OptionalUtils.orElse(objectProperty.getAdditionalProperties(), List.of()),
            PropertyDTO::toPropertyDTO);
        this.multipleValues = OptionalUtils.orElse(objectProperty.getMultipleValues(), true);
        this.objectType = OptionalUtils.orElse(objectProperty.getObjectType(), null);
        this.options =
            CollectionUtils.map(OptionalUtils.orElse(objectProperty.getOptions(), List.of()), OptionDTO::new);
        this.optionsDataSource = OptionalUtils.mapOrElse(
            objectProperty.getOptionsDataSource(), OptionsDataSourceDTO::new, null);
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(objectProperty.getProperties(), List.of()), PropertyDTO::toPropertyDTO);
    }

    @Override
    public Object accept(PropertyVisitor propertyVisitor) {
        return propertyVisitor.visit(this);
    }

    public List<? extends PropertyDTO> getAdditionalProperties() {
        return additionalProperties;
    }

    public boolean isMultipleValues() {
        return multipleValues;
    }

    public Optional<String> getObjectType() {
        return Optional.ofNullable(objectType);
    }

    public List<OptionDTO> getOptions() {
        return options;
    }

    public Optional<OptionsDataSourceDTO> getOptionsDataSource() {
        return Optional.ofNullable(optionsDataSource);
    }

    public List<? extends PropertyDTO> getProperties() {
        return properties;
    }
}
