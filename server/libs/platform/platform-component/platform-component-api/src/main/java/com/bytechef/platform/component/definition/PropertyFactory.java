/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.component.definition;

import static com.bytechef.component.definition.ComponentDsl.object;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.platform.util.SchemaUtils;
import com.bytechef.platform.util.SchemaUtils.SchemaPropertyFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public record PropertyFactory() implements SchemaPropertyFactory {

    public static final PropertyFactory PROPERTY_FACTORY = new PropertyFactory();

    @Override
    public BaseValueProperty<?> create(String name, Object value, Class<? extends BaseProperty> baseValueProperty) {
        if (baseValueProperty == BaseProperty.BaseArrayProperty.class) {
            return getArrayProperty(name, value);
        } else if (baseValueProperty == BaseProperty.BaseBooleanProperty.class) {
            return ComponentDsl.bool(name);
        } else if (baseValueProperty == BaseProperty.BaseDateProperty.class) {
            return ComponentDsl.date(name);
        } else if (baseValueProperty == BaseProperty.BaseDateTimeProperty.class) {
            return ComponentDsl.dateTime(name);
        } else if (baseValueProperty == BaseProperty.BaseFileEntryProperty.class) {
            return ComponentDsl.fileEntry(name);
        } else if (baseValueProperty == BaseProperty.BaseIntegerProperty.class) {
            return ComponentDsl.integer(name);
        } else if (baseValueProperty == BaseProperty.BaseNullProperty.class) {
            return ComponentDsl.nullable(name);
        } else if (baseValueProperty == BaseProperty.BaseNumberProperty.class) {
            return ComponentDsl.number(name);
        } else if (baseValueProperty == BaseProperty.BaseObjectProperty.class) {
            return getObjectProperty(name, value);
        } else if (baseValueProperty == BaseProperty.BaseStringProperty.class) {
            return ComponentDsl.string(name);
        } else if (baseValueProperty == BaseProperty.BaseTimeProperty.class) {
            return ComponentDsl.time(name);
        } else {
            return object(name);
        }
    }

    private ModifiableArrayProperty getArrayProperty(String name, Object value) {
        ModifiableArrayProperty arrayProperty;
        Class<?> valueClass = value.getClass();

        if (valueClass.isArray()) {
            arrayProperty = ComponentDsl.array(name);
        } else {
            arrayProperty = ComponentDsl.array(name);

            List<?> list = (List<?>) value;

            if (!list.isEmpty()) {
                arrayProperty.items(
                    (ModifiableValueProperty<?, ?>) SchemaUtils.getOutputSchema(
                        null, list.getFirst(), PROPERTY_FACTORY));
            }
        }

        return arrayProperty;
    }

    private ModifiableObjectProperty getObjectProperty(String name, Object value) {
        ModifiableObjectProperty objectProperty = object(name);

        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        Map<?, ?> map;

        if (value instanceof Map<?, ?>) {
            map = (Map<?, ?>) value;
        } else {
            map = ConvertUtils.convertValue(value, Map.class);
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            properties.add((ModifiableValueProperty<?, ?>) SchemaUtils.getOutputSchema(
                (String) entry.getKey(), entry.getValue(), PROPERTY_FACTORY));
        }

        return objectProperty.properties(properties);
    }
}
