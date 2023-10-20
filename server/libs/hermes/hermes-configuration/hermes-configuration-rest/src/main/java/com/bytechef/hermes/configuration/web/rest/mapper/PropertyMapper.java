
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

package com.bytechef.hermes.configuration.web.rest.mapper;

import com.bytechef.hermes.configuration.web.rest.mapper.config.ConfigurationMapperSpringConfig;
import com.bytechef.hermes.definition.registry.domain.Property;
import com.bytechef.hermes.definition.registry.domain.AnyProperty;
import com.bytechef.hermes.definition.registry.domain.ArrayProperty;
import com.bytechef.hermes.definition.registry.domain.BooleanProperty;
import com.bytechef.hermes.definition.registry.domain.DateProperty;
import com.bytechef.hermes.definition.registry.domain.DateTimeProperty;
import com.bytechef.hermes.definition.registry.domain.DynamicPropertiesProperty;
import com.bytechef.hermes.definition.registry.domain.IntegerProperty;
import com.bytechef.hermes.definition.registry.domain.NullProperty;
import com.bytechef.hermes.definition.registry.domain.NumberProperty;
import com.bytechef.hermes.definition.registry.domain.ObjectProperty;
import com.bytechef.hermes.definition.registry.domain.OptionsDataSource;
import com.bytechef.hermes.definition.registry.domain.StringProperty;
import com.bytechef.hermes.definition.registry.domain.TimeProperty;
import com.bytechef.hermes.configuration.web.rest.model.AnyPropertyModel;
import com.bytechef.hermes.configuration.web.rest.model.ArrayPropertyModel;
import com.bytechef.hermes.configuration.web.rest.model.BooleanPropertyModel;
import com.bytechef.hermes.configuration.web.rest.model.DatePropertyModel;
import com.bytechef.hermes.configuration.web.rest.model.DateTimePropertyModel;
import com.bytechef.hermes.configuration.web.rest.model.DynamicPropertiesPropertyModel;
import com.bytechef.hermes.configuration.web.rest.model.IntegerPropertyModel;
import com.bytechef.hermes.configuration.web.rest.model.NullPropertyModel;
import com.bytechef.hermes.configuration.web.rest.model.NumberPropertyModel;
import com.bytechef.hermes.configuration.web.rest.model.ObjectPropertyModel;
import com.bytechef.hermes.configuration.web.rest.model.OptionsDataSourceModel;
import com.bytechef.hermes.configuration.web.rest.model.PropertyModel;
import com.bytechef.hermes.configuration.web.rest.model.StringPropertyModel;
import com.bytechef.hermes.configuration.web.rest.model.TimePropertyModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@Mapper(config = ConfigurationMapperSpringConfig.class, uses = {
    OptionalMapper.class
})
public interface PropertyMapper extends Converter<Property, PropertyModel>, Property.PropertyVisitor {

    @Override
    default PropertyModel convert(Property property) {
        return (PropertyModel) property.accept(this);
    }

    @Override
    default AnyPropertyModel visit(AnyProperty anyProperty) {
        return map(anyProperty);
    }

    @Override
    default ArrayPropertyModel visit(ArrayProperty arrayProperty) {
        return map(arrayProperty);
    }

    @Override
    default BooleanPropertyModel visit(BooleanProperty booleanProperty) {
        return map(booleanProperty);
    }

    @Override
    default DatePropertyModel visit(DateProperty dateProperty) {
        return map(dateProperty);
    }

    @Override
    default DateTimePropertyModel visit(DateTimeProperty dateTimeProperty) {
        return map(dateTimeProperty);
    }

    @Override
    default DynamicPropertiesPropertyModel visit(DynamicPropertiesProperty dynamicPropertiesProperty) {
        return map(dynamicPropertiesProperty);
    }

    @Override
    default IntegerPropertyModel visit(IntegerProperty integerProperty) {
        return map(integerProperty);
    }

    @Override
    default NullPropertyModel visit(NullProperty nullProperty) {
        return map(nullProperty);
    }

    @Override
    default NumberPropertyModel visit(NumberProperty numberProperty) {
        return map(numberProperty);
    }

    @Override
    default ObjectPropertyModel visit(ObjectProperty objectProperty) {
        return map(objectProperty);
    }

    @Override
    default StringPropertyModel visit(StringProperty stringProperty) {
        return map(stringProperty);
    }

    @Override
    default TimePropertyModel visit(TimeProperty timeProperty) {
        return map(timeProperty);
    }

    AnyPropertyModel map(AnyProperty anyPropertyDTO);

    ArrayPropertyModel map(ArrayProperty arrayProperty);

    BooleanPropertyModel map(BooleanProperty booleanProperty);

    DatePropertyModel map(DateProperty dateProperty);

    DateTimePropertyModel map(DateTimeProperty dateTimeProperty);

    DynamicPropertiesPropertyModel map(DynamicPropertiesProperty dynamicPropertiesProperty);

    IntegerPropertyModel map(IntegerProperty integerProperty);

    @Mapping(target = "exampleValue", ignore = true)
    @Mapping(target = "defaultValue", ignore = true)
    @Mapping(target = "controlType", ignore = true)
    NullPropertyModel map(NullProperty nullProperty);

    NumberPropertyModel map(NumberProperty numberProperty);

    ObjectPropertyModel map(ObjectProperty objectProperty);

    OptionsDataSourceModel map(OptionsDataSource optionsDataSource);

    StringPropertyModel map(StringProperty stringProperty);

    TimePropertyModel map(TimeProperty timeProperty);

    default List<PropertyModel> map(List<? extends Property> properties) {
        if (CollectionUtils.isEmpty(properties)) {
            return Collections.emptyList();
        } else {
            return properties.stream()
                .map(this::convert)
                .toList();
        }
    }
}
