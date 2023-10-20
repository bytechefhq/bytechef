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

package com.bytechef.hermes.component.web.rest.mapper;

import com.bytechef.hermes.component.web.rest.mapper.config.ComponentDefinitionMapperSpringConfig;
import com.bytechef.hermes.component.web.rest.model.AnyPropertyModel;
import com.bytechef.hermes.component.web.rest.model.ArrayPropertyModel;
import com.bytechef.hermes.component.web.rest.model.BooleanPropertyModel;
import com.bytechef.hermes.component.web.rest.model.ConnectionDefinitionPropertiesInnerModel;
import com.bytechef.hermes.component.web.rest.model.DateTimePropertyModel;
import com.bytechef.hermes.component.web.rest.model.IntegerPropertyModel;
import com.bytechef.hermes.component.web.rest.model.NumberPropertyModel;
import com.bytechef.hermes.component.web.rest.model.ObjectPropertyModel;
import com.bytechef.hermes.component.web.rest.model.OptionPropertyModel;
import com.bytechef.hermes.component.web.rest.model.StringPropertyModel;
import com.bytechef.hermes.definition.Property;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = ComponentDefinitionMapperSpringConfig.class)
public interface ConnectionPropertyMapper extends Converter<Property<?>, ConnectionDefinitionPropertiesInnerModel> {

    default ConnectionDefinitionPropertiesInnerModel convert(Property<?> property) {
        return switch (property.getType()) {
            case ANY -> map((Property.AnyProperty) property);
            case ARRAY -> map((Property.ArrayProperty) property);
            case BOOLEAN -> map((Property.BooleanProperty) property);
            case DATE_TIME -> map((Property.DateTimeProperty) property);
            case INTEGER -> map((Property.IntegerProperty) property);
            case NULL -> throw new IllegalStateException();
            case NUMBER -> map((Property.NumberProperty) property);
            case OBJECT -> map((Property.ObjectProperty) property);
            case OPTION -> map((Property.OptionProperty) property);
            case STRING -> map((Property.StringProperty) property);
        };
    }

    AnyPropertyModel map(Property.AnyProperty property);

    ArrayPropertyModel map(Property.ArrayProperty property);

    BooleanPropertyModel map(Property.BooleanProperty property);

    DateTimePropertyModel map(Property.DateTimeProperty property);

    IntegerPropertyModel map(Property.IntegerProperty property);

    NumberPropertyModel map(Property.NumberProperty property);

    ObjectPropertyModel map(Property.ObjectProperty property);

    OptionPropertyModel map(Property.OptionProperty property);

    StringPropertyModel map(Property.StringProperty property);
}
