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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.definition.BaseProperty;
import com.bytechef.platform.registry.util.SchemaUtils;
import com.bytechef.platform.registry.util.SchemaUtils.SchemaPropertyFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.ModifiableArrayProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.ModifiableObjectProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.ModifiableProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.ModifiableValueProperty;
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
            return TaskDispatcherDSL.bool(name);
        } else if (baseValueProperty == BaseProperty.BaseDateProperty.class) {
            return TaskDispatcherDSL.date(name);
        } else if (baseValueProperty == BaseProperty.BaseDateTimeProperty.class) {
            return TaskDispatcherDSL.dateTime(name);
        } else if (baseValueProperty == BaseProperty.BaseFileEntryProperty.class) {
            return TaskDispatcherDSL.fileEntry(name);
        } else if (baseValueProperty == BaseProperty.BaseIntegerProperty.class) {
            return TaskDispatcherDSL.integer(name);
        } else if (baseValueProperty == BaseProperty.BaseNumberProperty.class) {
            return TaskDispatcherDSL.number(name);
        } else if (baseValueProperty == BaseProperty.BaseObjectProperty.class) {
            return getObjectProperty();
        } else if (baseValueProperty == BaseProperty.BaseStringProperty.class) {
            return TaskDispatcherDSL.string(name);
        } else if (baseValueProperty == BaseProperty.BaseTimeProperty.class) {
            return TaskDispatcherDSL.time(name);
        } else {
            return TaskDispatcherDSL.object(name);
        }
    }

    private ModifiableArrayProperty getArrayProperty(String name) {
        ModifiableArrayProperty arrayProperty;
        Class<?> valueClass = value.getClass();

        if (valueClass.isArray()) {
            arrayProperty = TaskDispatcherDSL.array(name);
        } else {
            arrayProperty = TaskDispatcherDSL.array(name);

            List<?> list = (List<?>) value;

            if (!list.isEmpty()) {
                arrayProperty.items(
                    (ModifiableProperty<?>) SchemaUtils.getOutputSchema(
                        list.getFirst(), null, new PropertyFactory(list.getFirst())));
            }
        }

        return arrayProperty;
    }

    private ModifiableObjectProperty getObjectProperty() {
        ModifiableObjectProperty objectProperty = TaskDispatcherDSL.object();

        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        Map<?, ?> map;

        if (value instanceof Map<?, ?>) {
            map = (Map<?, ?>) value;
        } else {
            map = JsonUtils.convertValue(value, Map.class);
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            properties.add((ModifiableValueProperty<?, ?>) SchemaUtils.getOutputSchema(
                entry.getValue(), (String) entry.getKey(), new PropertyFactory(entry.getValue())));
        }

        return objectProperty.properties(properties);
    }
}
