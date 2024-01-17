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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableValueProperty;
import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.platform.registry.util.OutputSchemaUtils;
import com.bytechef.platform.registry.util.OutputSchemaUtils.OutputSchemaValuePropertyFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
record ValuePropertyFactory(Object value) implements OutputSchemaValuePropertyFactory {

    @Override
    @SuppressWarnings("rawtypes")
    public BaseValueProperty<?> create(String name, Class<? extends BaseValueProperty> baseValueProperty) {
        if (baseValueProperty == BaseProperty.BaseArrayProperty.class) {
            return getArrayProperty(name, value);
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
        } else if (baseValueProperty == BaseProperty.BaseNumberProperty.class) {
            return ComponentDSL.number(name);
        } else if (baseValueProperty == BaseProperty.BaseObjectProperty.class) {
            return getObjectProperty();
        } else if (baseValueProperty == BaseProperty.BaseStringProperty.class) {
            return ComponentDSL.string(name);
        } else if (baseValueProperty == BaseProperty.BaseTimeProperty.class) {
            return ComponentDSL.time(name);
        } else {
            return ComponentDSL.object(name);
        }
    }

    private ModifiableArrayProperty getArrayProperty(String name, Object value) {
        ModifiableArrayProperty arrayProperty;
        Class<?> valueClass = value.getClass();

        if (valueClass.isArray()) {
            arrayProperty = ComponentDSL.array(name);
        } else {
            arrayProperty = ComponentDSL.array(name);

            List<?> list = (List<?>) value;

            if (!list.isEmpty()) {
                arrayProperty.items(
                    (ModifiableValueProperty<?, ?>) OutputSchemaUtils.getOutputSchemaDefinition(null, list.getFirst(),
                        this));
            }
        }

        return arrayProperty;
    }

    private ModifiableObjectProperty getObjectProperty() {
        ModifiableObjectProperty objectProperty = ComponentDSL.object();

        List<BaseValueProperty<?>> properties = new ArrayList<>();
        Map<?, ?> map = (Map<?, ?>) value;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            properties.add(OutputSchemaUtils.getOutputSchemaDefinition(
                (String) entry.getKey(), entry.getValue(), this));
        }

        return objectProperty.properties(
            CollectionUtils.map(properties, property -> (ModifiableValueProperty<?, ?>) property));
    }
}
