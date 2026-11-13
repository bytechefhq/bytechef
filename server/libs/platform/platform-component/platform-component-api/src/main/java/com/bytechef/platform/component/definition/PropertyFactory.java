/*
 * Copyright 2025 ByteChef
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
import com.bytechef.component.definition.ComponentDsl.ModifiableBooleanProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableNumberProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.exception.ComponentErrorType;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.util.SchemaUtils;
import com.bytechef.platform.util.SchemaUtils.JsonSchemaPropertyFactory;
import com.bytechef.platform.util.SchemaUtils.SchemaPropertyFactory;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public record PropertyFactory() implements SchemaPropertyFactory {

    @SuppressWarnings("unchecked")
    public static final JsonSchemaPropertyFactory JSON_SCHEMA_PROPERTY_FACTORY = new JsonSchemaPropertyFactory() {

        @Override
        public void addChildren(
            BaseProperty.BaseValueProperty<?> property,
            List<BaseProperty.BaseValueProperty<?>> children) {

            if (property instanceof ModifiableArrayProperty modifiableArrayProperty) {
                modifiableArrayProperty.items(
                    children.stream()
                        .map(child -> (ModifiableValueProperty<?, ?>) child)
                        .toList());
            } else {
                ((ModifiableObjectProperty) property).properties(
                    children.stream()
                        .map(child -> (ModifiableValueProperty<?, ?>) child)
                        .toList());
            }
        }

        @Override
        public BaseValueProperty<?> create(String name, String type) {
            return switch (type) {
                case "array" -> ComponentDsl.array(name);
                case "boolean" -> ComponentDsl.bool(name);
                case "integer" -> ComponentDsl.integer(name);
                case "number" -> ComponentDsl.number(name);
                case "object" -> object(name);
                case "string" -> ComponentDsl.string(name);
                default -> throw new ConfigurationException(
                    "Unsupported JSON schema type: " + type, ComponentErrorType.UNSUPPORTED_PROPERTY_TYPE);
            };
        }

        @Override
        public List<BaseProperty.BaseValueProperty<?>> getChildren(BaseProperty.BaseValueProperty<?> property) {
            if (property instanceof ModifiableArrayProperty modifiableArrayProperty) {
                return (List<BaseProperty.BaseValueProperty<?>>) (List<?>) new ArrayList<>(
                    modifiableArrayProperty.getItems());
            } else {
                return (List<BaseProperty.BaseValueProperty<?>>) (List<?>) new ArrayList<>(
                    ((ModifiableObjectProperty) property).getProperties());
            }
        }

        @Override
        public void setDefaultValue(BaseProperty.BaseValueProperty<?> property, Object defaultValue) {
            if (property instanceof ModifiableArrayProperty arrayProperty && defaultValue instanceof List<?> list) {
                setArrayDefaultValue(arrayProperty, list);
            } else if (property instanceof ModifiableBooleanProperty booleanProperty &&
                defaultValue instanceof Boolean booleanValue) {

                booleanProperty.defaultValue(booleanValue);
            } else if (property instanceof ModifiableIntegerProperty integerProperty &&
                defaultValue instanceof Long longValue) {

                integerProperty.defaultValue(longValue);
            } else if (property instanceof ModifiableNumberProperty numberProperty &&
                defaultValue instanceof Double doubleValue) {

                numberProperty.defaultValue(doubleValue);
            } else if (property instanceof ModifiableObjectProperty objectProperty &&
                defaultValue instanceof Map<?, ?> map) {

                objectProperty.defaultValue((Map<String, Object>) map);
            } else if (property instanceof ModifiableStringProperty stringProperty &&
                defaultValue instanceof String string) {

                stringProperty.defaultValue(string);
            }
        }
    };

    public static final SchemaUtils.OutputFactoryFunction OUTPUT_FACTORY_FUNCTION =
        (outputSchema, sampleOutput, placeholder, sampleOutputGenerated) -> new OutputResponse(
            Property.toProperty((com.bytechef.component.definition.Property) outputSchema), sampleOutput, placeholder,
            sampleOutputGenerated);

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
        ModifiableArrayProperty arrayProperty = ComponentDsl.array(name);

        Class<?> valueClass = value.getClass();
        Object firstItem = null;

        if (valueClass.isArray()) {
            if (Array.getLength(value) > 0) {
                firstItem = Array.get(value, 0);
            }
        } else {
            List<?> list = (List<?>) value;

            if (!list.isEmpty()) {
                firstItem = list.getFirst();
            }
        }

        if (firstItem != null) {
            ModifiableValueProperty<?, ?> itemProperty =
                (ModifiableValueProperty<?, ?>) SchemaUtils.getOutputSchema(null, firstItem, PROPERTY_FACTORY);

            arrayProperty.items(itemProperty);
        }

        return arrayProperty;
    }

    private static boolean isListOf(List<?> list, Class<?> itemClass) {
        return list.stream()
            .allMatch(itemClass::isInstance);
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private static void setArrayDefaultValue(ModifiableArrayProperty arrayProperty, List<?> list) {
        if (isListOf(list, String.class)) {
            arrayProperty.defaultValue(list.toArray(String[]::new));
        } else if (isListOf(list, Boolean.class)) {
            arrayProperty.defaultValue(list.toArray(Boolean[]::new));
        } else if (isListOf(list, Integer.class)) {
            arrayProperty.defaultValue(list.toArray(Integer[]::new));
        } else if (isListOf(list, Long.class)) {
            arrayProperty.defaultValue(list.toArray(Long[]::new));
        } else if (isListOf(list, Double.class)) {
            arrayProperty.defaultValue(list.toArray(Double[]::new));
        } else if (isListOf(list, Map.class)) {
            arrayProperty.defaultValue((Map<String, ?>[]) list.toArray(Map[]::new));
        }
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
