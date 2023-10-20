
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

package com.bytechef.hermes.definition.registry.web.rest.mapper;

import com.bytechef.hermes.definition.registry.dto.AnyPropertyDTO;
import com.bytechef.hermes.definition.registry.dto.ArrayPropertyDTO;
import com.bytechef.hermes.definition.registry.dto.BooleanPropertyDTO;
import com.bytechef.hermes.definition.registry.dto.DatePropertyDTO;
import com.bytechef.hermes.definition.registry.dto.DateTimePropertyDTO;
import com.bytechef.hermes.definition.registry.dto.DynamicPropertiesPropertyDTO;
import com.bytechef.hermes.definition.registry.dto.IntegerPropertyDTO;
import com.bytechef.hermes.definition.registry.dto.NullPropertyDTO;
import com.bytechef.hermes.definition.registry.dto.NumberPropertyDTO;
import com.bytechef.hermes.definition.registry.dto.ObjectPropertyDTO;
import com.bytechef.hermes.definition.registry.dto.OptionDTO;
import com.bytechef.hermes.definition.registry.dto.OptionsDataSourceDTO;
import com.bytechef.hermes.definition.registry.dto.PropertyDTO;
import com.bytechef.hermes.definition.registry.dto.StringPropertyDTO;
import com.bytechef.hermes.definition.registry.dto.TimePropertyDTO;
import com.bytechef.hermes.definition.registry.web.rest.mapper.config.DefinitionMapperSpringConfig;
import com.bytechef.hermes.definition.registry.web.rest.model.AnyPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ArrayPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.BooleanPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DatePropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DateTimePropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DynamicPropertiesPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.IntegerPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.NullPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.NumberPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ObjectPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.OptionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.OptionsDataSourceModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.StringPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.TimePropertyModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Mapper(config = DefinitionMapperSpringConfig.class)
public interface PropertyMapper extends Converter<PropertyDTO, PropertyModel>, PropertyDTO.PropertyVisitor {

    @Override
    default PropertyModel convert(PropertyDTO property) {
        return (PropertyModel) property.accept(this);
    }

    @Override
    default AnyPropertyModel visit(AnyPropertyDTO anyPropertyDTO) {
        return map(anyPropertyDTO);
    }

    @Override
    default ArrayPropertyModel visit(ArrayPropertyDTO arrayPropertyDTO) {
        return map(arrayPropertyDTO);
    }

    @Override
    default BooleanPropertyModel visit(BooleanPropertyDTO booleanPropertyDTO) {
        return map(booleanPropertyDTO);
    }

    @Override
    default DatePropertyModel visit(DatePropertyDTO datePropertyDTO) {
        return map(datePropertyDTO);
    }

    @Override
    default DateTimePropertyModel visit(DateTimePropertyDTO dateTimePropertyDTO) {
        return map(dateTimePropertyDTO);
    }

    @Override
    default DynamicPropertiesPropertyModel visit(DynamicPropertiesPropertyDTO dynamicPropertiesPropertyDTO) {
        return map(dynamicPropertiesPropertyDTO);
    }

    @Override
    default IntegerPropertyModel visit(IntegerPropertyDTO integerPropertyDTO) {
        return map(integerPropertyDTO);
    }

    @Override
    default NullPropertyModel visit(NullPropertyDTO nullPropertyDTO) {
        return map(nullPropertyDTO);
    }

    @Override
    default NumberPropertyModel visit(NumberPropertyDTO numberPropertyDTO) {
        return map(numberPropertyDTO);
    }

    @Override
    default ObjectPropertyModel visit(ObjectPropertyDTO objectPropertyDTO) {
        return map(objectPropertyDTO);
    }

    @Override
    default StringPropertyModel visit(StringPropertyDTO stringPropertyDTO) {
        return map(stringPropertyDTO);
    }

    @Override
    default TimePropertyModel visit(TimePropertyDTO timePropertyDTO) {
        return map(timePropertyDTO);
    }

    AnyPropertyModel map(AnyPropertyDTO anyPropertyDTO);

    ArrayPropertyModel map(ArrayPropertyDTO arrayProperty);

    BooleanPropertyModel map(BooleanPropertyDTO booleanProperty);

    DatePropertyModel map(DatePropertyDTO dateProperty);

    DateTimePropertyModel map(DateTimePropertyDTO dateTimeProperty);

    DynamicPropertiesPropertyModel map(DynamicPropertiesPropertyDTO dynamicPropertiesProperty);

    IntegerPropertyModel map(IntegerPropertyDTO integerProperty);

    @Mapping(target = "exampleValue", ignore = true)
    @Mapping(target = "defaultValue", ignore = true)
    @Mapping(target = "controlType", ignore = true)
    NullPropertyModel map(NullPropertyDTO nullProperty);

    NumberPropertyModel map(NumberPropertyDTO numberProperty);

    ObjectPropertyModel map(ObjectPropertyDTO objectProperty);

    OptionsDataSourceModel map(OptionsDataSourceDTO optionsDataSource);

    StringPropertyModel map(StringPropertyDTO stringProperty);

    TimePropertyModel map(TimePropertyDTO timeProperty);

    OptionModel map(OptionDTO option);

    default List<PropertyModel> map(List<? extends PropertyDTO> properties) {
        if (CollectionUtils.isEmpty(properties)) {
            return Collections.emptyList();
        } else {
            return properties.stream()
                .map(this::convert)
                .toList();
        }
    }

    default Boolean mapToBoolean(Optional<Boolean> optional) {
        return optional.orElse(null);
    }

    default Integer mapToInteger(Optional<Integer> optional) {
        return optional.orElse(null);
    }

    default JsonNullable<Object> mapToJsonNullable(Object value) {
        return JsonNullable.of(value);
    }

    default JsonNullable<Object> mapToJsonNullable(Optional<?> optional) {
        return JsonNullable.of(optional.orElse(null));
    }

    default LocalDate mapToLocalDate(Optional<LocalDate> optional) {
        return optional.orElse(null);
    }

    default LocalDateTime mapToLocalDateTime(Optional<LocalDateTime> optional) {
        return optional.orElse(null);
    }

    default LocalTime mapToLocalTime(Optional<LocalTime> optional) {
        return optional.orElse(null);
    }

    default Object mapToObject(Optional<Object> optional) {
        return optional.orElse(null);
    }

    default OptionDTO mapToOption(Optional<OptionDTO> optional) {
        return optional.orElse(null);
    }

    default OptionsDataSourceModel mapToOptionsDataSourceModel(Optional<OptionsDataSourceDTO> optional) {
        return optional.map(this::map)
            .orElse(null);
    }

    default String mapToString(Optional<String> optional) {
        return optional.orElse(null);
    }
}
