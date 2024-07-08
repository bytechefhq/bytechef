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

import static com.bytechef.component.definition.ComponentDSL.object;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableValueProperty;
import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.platform.registry.util.SchemaUtils;
import com.bytechef.platform.registry.util.SchemaUtils.SchemaPropertyFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public record PropertyFactory(Object value) implements SchemaPropertyFactory {

    @Override
    public BaseValueProperty<?> create(String name, Class<? extends BaseProperty> baseValueProperty) {
        if (baseValueProperty == BaseProperty.BaseArrayProperty.class) {
            return getArrayProperty(name);
        } else if (baseValueProperty == BaseProperty.BaseBooleanProperty.class) {
            return ComponentDSL.bool(name);
        } else if (baseValueProperty == BaseProperty.BaseDateProperty.class) {
            return ComponentDSL.date(name);
        } else if (baseValueProperty == BaseProperty.BaseDateTimeProperty.class) {
            return ComponentDSL.dateTime(name);
        } else if (baseValueProperty == BaseProperty.BaseFileEntryProperty.class) {
            return ComponentDSL.fileEntry(name);
        } else if (baseValueProperty == BaseProperty.BaseIntegerProperty.class) {
            return ComponentDSL.integer(name);
        } else if (baseValueProperty == BaseProperty.BaseNullProperty.class) {
            return ComponentDSL.nullable(name);
        } else if (baseValueProperty == BaseProperty.BaseNumberProperty.class) {
            return ComponentDSL.number(name);
        } else if (baseValueProperty == BaseProperty.BaseObjectProperty.class) {
            return getObjectProperty(name);
        } else if (baseValueProperty == BaseProperty.BaseStringProperty.class) {
            return ComponentDSL.string(name);
        } else if (baseValueProperty == BaseProperty.BaseTimeProperty.class) {
            return ComponentDSL.time(name);
        } else {
            return object(name);
        }
    }

    private ModifiableArrayProperty getArrayProperty(String name) {
        ModifiableArrayProperty arrayProperty;
        Class<?> valueClass = value.getClass();

        if (valueClass.isArray()) {
            arrayProperty = ComponentDSL.array(name);
        } else {
            arrayProperty = ComponentDSL.array(name);

            List<?> list = (List<?>) value;

            if (!list.isEmpty()) {
                arrayProperty.items(
                    (ModifiableValueProperty<?, ?>) SchemaUtils.getOutputSchema(
                        null, list.getFirst(), new PropertyFactory(list.getFirst())));
            }
        }

        return arrayProperty;
    }

    private ModifiableObjectProperty getObjectProperty(String name) {
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
                (String) entry.getKey(), entry.getValue(), new PropertyFactory(entry.getValue())));
        }

        return objectProperty.properties(properties);
    }
}
