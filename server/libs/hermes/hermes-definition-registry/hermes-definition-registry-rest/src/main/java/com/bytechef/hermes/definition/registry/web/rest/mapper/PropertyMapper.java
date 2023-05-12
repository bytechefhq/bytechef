
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

import com.bytechef.hermes.definition.Option;
import com.bytechef.hermes.definition.OptionsDataSource;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.registry.web.rest.mapper.config.DefinitionMapperSpringConfig;
import com.bytechef.hermes.definition.registry.web.rest.model.ArrayPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.BooleanPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DatePropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DateTimePropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DynamicPropertiesPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.IntegerPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.NullPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.NumberPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ObjectPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.OneOfPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.OptionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.OptionsDataSourceModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.StringPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.TimePropertyModel;
import org.mapstruct.Mapper;
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
public interface PropertyMapper extends Converter<Property<?>, PropertyModel>, Property.PropertyVisitor {

    @Override
    default PropertyModel convert(Property property) {
        return (PropertyModel) property.accept(this);
    }

    @Override
    default ArrayPropertyModel visit(Property.ArrayProperty arrayProperty) {
        return map(arrayProperty);
    }

    @Override
    default BooleanPropertyModel visit(Property.BooleanProperty booleanProperty) {
        return map(booleanProperty);
    }

    @Override
    default DatePropertyModel visit(Property.DateProperty dateProperty) {
        return map(dateProperty);
    }

    @Override
    default DateTimePropertyModel visit(Property.DateTimeProperty dateTimeProperty) {
        return map(dateTimeProperty);
    }

    @Override
    default DynamicPropertiesPropertyModel visit(Property.DynamicPropertiesProperty dynamicPropertiesProperty) {
        return map(dynamicPropertiesProperty);
    }

    @Override
    default IntegerPropertyModel visit(Property.IntegerProperty integerProperty) {
        return map(integerProperty);
    }

    @Override
    default NullPropertyModel visit(Property.NullProperty nullProperty) {
        return map(nullProperty);
    }

    @Override
    default NumberPropertyModel visit(Property.NumberProperty numberProperty) {
        return map(numberProperty);
    }

    @Override
    default OneOfPropertyModel visit(Property.OneOfProperty oneOfProperty) {
        return map(oneOfProperty);
    }

    @Override
    default ObjectPropertyModel visit(Property.ObjectProperty objectProperty) {
        return map(objectProperty);
    }

    @Override
    default StringPropertyModel visit(Property.StringProperty stringProperty) {
        return map(stringProperty);
    }

    @Override
    default TimePropertyModel visit(Property.TimeProperty timeProperty) {
        return map(timeProperty);
    }

    ArrayPropertyModel map(Property.ArrayProperty arrayProperty);

    BooleanPropertyModel map(Property.BooleanProperty booleanProperty);

    DatePropertyModel map(Property.DateProperty dateProperty);

    DateTimePropertyModel map(Property.DateTimeProperty dateTimeProperty);

    DynamicPropertiesPropertyModel map(Property.DynamicPropertiesProperty dynamicPropertiesProperty);

    IntegerPropertyModel map(Property.IntegerProperty integerProperty);

    NullPropertyModel map(Property.NullProperty nullProperty);

    NumberPropertyModel map(Property.NumberProperty numberProperty);

    ObjectPropertyModel map(Property.ObjectProperty objectProperty);

    OneOfPropertyModel map(Property.OneOfProperty oneOfProperty);

    OptionsDataSourceModel map(OptionsDataSource optionsDataSource);

    StringPropertyModel map(Property.StringProperty stringProperty);

    TimePropertyModel map(Property.TimeProperty timeProperty);

    OptionModel map(Option<?> option);

    default Boolean mapToBoolean(Optional<Boolean> optional) {
        return optional.orElse(null);
    }

    default Integer mapToInteger(Optional<Integer> optional) {
        return optional.orElse(null);
    }

    default JsonNullable<Object> mapToJsonNullable(Object value) {
        return JsonNullable.of(value);
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

    default OptionsDataSourceModel mapToOptionsDataSourceModel(Optional<OptionsDataSource> optional) {
        return optional.map(this::map)
            .orElse(null);
    }

    default List<PropertyModel> mapToProperties(Optional<List<? extends Property<?>>> optional) {
        return optional.map(
            properties -> properties.stream()
                .map(this::convert)
                .toList())
            .orElse(Collections.emptyList());
    }

    default String mapToString(Optional<String> optional) {
        return optional.orElse(null);
    }

    default List<OptionModel> mapToOptions(Optional<List<Option<?>>> optional) {
        return optional.map(options -> options.stream()
            .map(this::map)
            .toList())
            .orElse(Collections.emptyList());
    }

    default List<PropertyModel> map(List<? extends Property<?>> properties) {
        if (CollectionUtils.isEmpty(properties)) {
            return Collections.emptyList();
        } else {
            return properties.stream()
                .map(this::convert)
                .toList();
        }
    }
}
