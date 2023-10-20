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
import com.bytechef.hermes.component.web.rest.model.ComponentActionInputsInnerModel;
import com.bytechef.hermes.component.web.rest.model.DateTimePropertyModel;
import com.bytechef.hermes.component.web.rest.model.IntegerPropertyModel;
import com.bytechef.hermes.component.web.rest.model.NullPropertyModel;
import com.bytechef.hermes.component.web.rest.model.NumberPropertyModel;
import com.bytechef.hermes.component.web.rest.model.ObjectPropertyModel;
import com.bytechef.hermes.component.web.rest.model.OptionPropertyModel;
import com.bytechef.hermes.component.web.rest.model.PropertyOptionValueModel;
import com.bytechef.hermes.component.web.rest.model.StringPropertyModel;
import com.bytechef.hermes.definition.Property;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.mapstruct.Mapper;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = ComponentDefinitionMapperSpringConfig.class)
public interface ComponentPropertyMapper extends Converter<Property<?>, ComponentActionInputsInnerModel> {

    default ComponentActionInputsInnerModel convert(Property<?> property) {
        return switch (property.getType()) {
            case ANY -> map((Property.AnyProperty) property);
            case ARRAY -> map((Property.ArrayProperty) property);
            case BOOLEAN -> map((Property.BooleanProperty) property);
            case DATE_TIME -> map((Property.DateTimeProperty) property);
            case INTEGER -> map((Property.IntegerProperty) property);
            case NULL -> map((Property.NullProperty) property);
            case NUMBER -> map((Property.NumberProperty) property);
            case OBJECT -> map((Property.ObjectProperty) property);
            case OPTION -> map((Property.OptionProperty) property);
            case STRING -> map((Property.StringProperty) property);
        };
    }

    default PropertyOptionValueModel map(Object value) {
        return new ValuePropertyOptionValueModel(value);
    }

    AnyPropertyModel map(Property.AnyProperty property);

    ArrayPropertyModel map(Property.ArrayProperty property);

    BooleanPropertyModel map(Property.BooleanProperty property);

    DateTimePropertyModel map(Property.DateTimeProperty property);

    IntegerPropertyModel map(Property.IntegerProperty property);

    NullPropertyModel map(Property.NullProperty property);

    NumberPropertyModel map(Property.NumberProperty property);

    ObjectPropertyModel map(Property.ObjectProperty property);

    OptionPropertyModel map(Property.OptionProperty property);

    StringPropertyModel map(Property.StringProperty property);

    @JsonComponent
    class PropertyOptionValueModelSerializer extends JsonSerializer<PropertyOptionValueModel> {

        @Override
        public void serialize(
                PropertyOptionValueModel propertyOptionValueModel,
                JsonGenerator jsonGenerator,
                SerializerProvider serializerProvider)
                throws IOException {
            Object value = ((ValuePropertyOptionValueModel) propertyOptionValueModel).value();

            if (value == null) {
                jsonGenerator.writeNull();
            } else {
                if (value instanceof String) {
                    jsonGenerator.writeString((String) value);
                } else if (value instanceof Boolean) {
                    jsonGenerator.writeBoolean((Boolean) value);
                } else {
                    jsonGenerator.writeNumber((Integer) value);
                }
            }
        }
    }

    record ValuePropertyOptionValueModel(Object value) implements PropertyOptionValueModel {}
}
