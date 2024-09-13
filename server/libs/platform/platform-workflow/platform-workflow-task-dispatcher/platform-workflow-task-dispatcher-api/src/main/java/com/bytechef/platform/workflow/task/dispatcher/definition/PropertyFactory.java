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

package com.bytechef.platform.workflow.task.dispatcher.definition;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.definition.BaseProperty;
import com.bytechef.platform.registry.util.SchemaUtils;
import com.bytechef.platform.registry.util.SchemaUtils.SchemaPropertyFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableArrayProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableObjectProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableValueProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public record PropertyFactory(Object value) implements SchemaPropertyFactory {

    @Override
    public BaseProperty create(String name, Class<? extends BaseProperty> baseValueProperty) {
        if (baseValueProperty == BaseProperty.BaseArrayProperty.class) {
            return getArrayProperty(name);
        } else if (baseValueProperty == BaseProperty.BaseBooleanProperty.class) {
            return TaskDispatcherDsl.bool(name);
        } else if (baseValueProperty == BaseProperty.BaseDateProperty.class) {
            return TaskDispatcherDsl.date(name);
        } else if (baseValueProperty == BaseProperty.BaseDateTimeProperty.class) {
            return TaskDispatcherDsl.dateTime(name);
        } else if (baseValueProperty == BaseProperty.BaseFileEntryProperty.class) {
            return TaskDispatcherDsl.fileEntry(name);
        } else if (baseValueProperty == BaseProperty.BaseIntegerProperty.class) {
            return TaskDispatcherDsl.integer(name);
        } else if (baseValueProperty == BaseProperty.BaseNumberProperty.class) {
            return TaskDispatcherDsl.number(name);
        } else if (baseValueProperty == BaseProperty.BaseNullProperty.class) {
            return TaskDispatcherDsl.nullable(name);
        } else if (baseValueProperty == BaseProperty.BaseObjectProperty.class) {
            return getObjectProperty();
        } else if (baseValueProperty == BaseProperty.BaseStringProperty.class) {
            return TaskDispatcherDsl.string(name);
        } else if (baseValueProperty == BaseProperty.BaseTimeProperty.class) {
            return TaskDispatcherDsl.time(name);
        } else {
            return TaskDispatcherDsl.object(name);
        }
    }

    private ModifiableArrayProperty getArrayProperty(String name) {
        ModifiableArrayProperty arrayProperty;
        Class<?> valueClass = value.getClass();

        if (valueClass.isArray()) {
            arrayProperty = TaskDispatcherDsl.array(name);
        } else {
            arrayProperty = TaskDispatcherDsl.array(name);

            List<?> list = (List<?>) value;

            if (!list.isEmpty()) {
                arrayProperty.items(
                    (ModifiableProperty<?>) SchemaUtils.getOutputSchema(
                        null, list.getFirst(), new PropertyFactory(list.getFirst())));
            }
        }

        return arrayProperty;
    }

    private ModifiableObjectProperty getObjectProperty() {
        ModifiableObjectProperty objectProperty = TaskDispatcherDsl.object();

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
