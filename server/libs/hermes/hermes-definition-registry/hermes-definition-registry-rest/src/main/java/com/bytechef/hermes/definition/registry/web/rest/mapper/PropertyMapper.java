
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

import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.registry.web.rest.mapper.config.DefinitionMapperSpringConfig;
import com.bytechef.hermes.definition.registry.web.rest.model.ArrayPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.BooleanPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DatePropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DateTimePropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DynamicPropertiesPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.IntegerPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.NumberPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ObjectPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.OneOfPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.StringPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.TimePropertyModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

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

    NumberPropertyModel map(Property.NumberProperty numberProperty);

    OneOfPropertyModel map(Property.OneOfProperty oneOfProperty);

    ObjectPropertyModel map(Property.ObjectProperty objectProperty);

    StringPropertyModel map(Property.StringProperty stringProperty);

    TimePropertyModel map(Property.TimeProperty timeProperty);

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
