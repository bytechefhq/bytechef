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

package com.bytechef.platform.registry.util;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.definition.BaseFileEntry;
import com.bytechef.definition.BaseProperty.BaseArrayProperty;
import com.bytechef.definition.BaseProperty.BaseBooleanProperty;
import com.bytechef.definition.BaseProperty.BaseDateProperty;
import com.bytechef.definition.BaseProperty.BaseDateTimeProperty;
import com.bytechef.definition.BaseProperty.BaseFileEntryProperty;
import com.bytechef.definition.BaseProperty.BaseIntegerProperty;
import com.bytechef.definition.BaseProperty.BaseNumberProperty;
import com.bytechef.definition.BaseProperty.BaseObjectProperty;
import com.bytechef.definition.BaseProperty.BaseStringProperty;
import com.bytechef.definition.BaseProperty.BaseTimeProperty;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.platform.registry.domain.BaseOutputSchema;
import com.bytechef.platform.registry.domain.BaseProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class OutputSchemaUtils {

    private static final Logger logger = LoggerFactory.getLogger(OutputSchemaUtils.class);

    public static BaseValueProperty<?> getOutputSchemaDefinition(
        Object value, OutputSchemaValuePropertyFactory propertyFactoryFunction) {

        return getOutputSchemaDefinition(null, value, propertyFactoryFunction);
    }

    public static BaseValueProperty<?> getOutputSchemaDefinition(
        String name, Object value, OutputSchemaValuePropertyFactory propertyFactoryFunction) {

        BaseValueProperty<?> outputProperty;
        Class<?> valueClass = value.getClass();

        if (value instanceof List<?> || valueClass.isArray()) {
            outputProperty = propertyFactoryFunction.create(name, BaseArrayProperty.class);
        } else if (value instanceof Boolean) {
            outputProperty = propertyFactoryFunction.create(name, BaseBooleanProperty.class);
        } else if (value instanceof Date || value instanceof LocalDate) {
            outputProperty = propertyFactoryFunction.create(name, BaseDateProperty.class);
        } else if (value instanceof LocalDateTime) {
            outputProperty = propertyFactoryFunction.create(name, BaseDateTimeProperty.class);
        } else if (value instanceof BaseFileEntry) {
            outputProperty = propertyFactoryFunction.create(name, BaseFileEntryProperty.class);
        } else if (value instanceof Integer) {
            outputProperty = propertyFactoryFunction.create(name, BaseIntegerProperty.class);
        } else if (value instanceof Number) {
            outputProperty = propertyFactoryFunction.create(name, BaseNumberProperty.class);
        } else if (value instanceof Map<?, ?>) {
            outputProperty = propertyFactoryFunction.create(name, BaseObjectProperty.class);
        } else if (value instanceof String) {
            outputProperty = propertyFactoryFunction.create(name, BaseStringProperty.class);
        } else if (value instanceof LocalTime) {
            outputProperty = propertyFactoryFunction.create(name, BaseTimeProperty.class);
        } else {
            try {
                outputProperty = getOutputSchemaDefinition(
                    JsonUtils.convertValue(value, Map.class), propertyFactoryFunction);
            } catch (IllegalArgumentException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(e.getMessage(), e);
                }

                outputProperty = propertyFactoryFunction.create(name, BaseValueProperty.class);
            }
        }

        return outputProperty;
    }

    public static <P extends BaseProperty, O extends BaseOutputSchema<P>> O toOutputSchema(
        com.bytechef.definition.BaseOutputSchema<? extends BaseValueProperty<?>> outputSchema,
        BiFunction<BaseValueProperty<?>, Object, O> outputSchemaFactoryFunction) {

        Object sampleOutput = outputSchema.getSampleOutput();

        if (sampleOutput == null) {
            sampleOutput = getSampleOutput(outputSchema.getDefinition());
        }

        if (sampleOutput instanceof String string) {
            try {
                sampleOutput = JsonUtils.read(string);
            } catch (Exception e) {
                //
            }
        }

        return outputSchemaFactoryFunction.apply(outputSchema.getDefinition(), sampleOutput);
    }

    private static Object getSampleOutput(BaseValueProperty<?> definitionProperty) {
        return switch (definitionProperty) {
            case BaseArrayProperty<? extends BaseValueProperty<?>> p -> {
                List<Object> items = new ArrayList<>();

                List<? extends BaseValueProperty<?>> properties = OptionalUtils.orElse(p.getItems(), List.of());

                if (!properties.isEmpty()) {
                    items.add(getSampleOutput(properties.getFirst()));
                }

                yield items;
            }
            case BaseBooleanProperty p -> true;
            case BaseDateProperty p -> LocalDate.now();
            case BaseDateTimeProperty p -> LocalDateTime.now();
            case BaseFileEntryProperty<?> p ->
                Map.of(
                    "extension", "sampleExtension", "mimeType", "sampleMimeType", "name", "sampleName", "url",
                    "file:///tmp/fileName.txt");
            case BaseIntegerProperty p -> 57;
            case com.bytechef.definition.BaseProperty.BaseNullProperty p -> null;
            case BaseNumberProperty p -> 23.34;
            case BaseObjectProperty<?> p -> {
                Map<String, Object> map = new HashMap<>();

                for (BaseValueProperty<?> property : OptionalUtils.orElse(p.getProperties(), List.of())) {
                    map.put(property.getName(), getSampleOutput(property));
                }

                yield map;
            }
            case BaseStringProperty p -> "sample " + p.getName();
            case BaseTimeProperty p -> LocalTime.now();
            default -> throw new IllegalArgumentException(
                "Definition %s is not allowed".formatted(definitionProperty.getName()));
        };
    }

    public interface OutputSchemaValuePropertyFactory {

        @SuppressWarnings("rawtypes")
        BaseValueProperty<?> create(String name, Class<? extends BaseValueProperty> baseValuePropertyClass);
    }
}
