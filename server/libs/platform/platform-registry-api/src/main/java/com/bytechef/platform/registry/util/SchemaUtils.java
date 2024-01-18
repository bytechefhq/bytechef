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
import com.bytechef.definition.BaseProperty.BaseNullProperty;
import com.bytechef.definition.BaseProperty.BaseNumberProperty;
import com.bytechef.definition.BaseProperty.BaseObjectProperty;
import com.bytechef.definition.BaseProperty.BaseStringProperty;
import com.bytechef.definition.BaseProperty.BaseTimeProperty;
import com.bytechef.platform.registry.domain.BaseOutputSchema;
import com.bytechef.platform.registry.domain.BaseProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class SchemaUtils {

    private static final Logger logger = LoggerFactory.getLogger(SchemaUtils.class);

    public static com.bytechef.definition.BaseProperty getSchemaDefinition(
        Object value, SchemaPropertyFactoryFunction propertyFactoryFunction) {

        return getSchemaDefinition(null, value, propertyFactoryFunction);
    }

    public static com.bytechef.definition.BaseProperty getSchemaDefinition(
        String name, Object value, SchemaPropertyFactoryFunction propertyFactoryFunction) {

        com.bytechef.definition.BaseProperty outputProperty;
        Class<?> valueClass = value.getClass();

        if (value instanceof List<?> || valueClass.isArray()) {
            outputProperty = propertyFactoryFunction.apply(name, BaseArrayProperty.class);
        } else if (value instanceof Boolean) {
            outputProperty = propertyFactoryFunction.apply(name, BaseBooleanProperty.class);
        } else if (value instanceof Date || value instanceof LocalDate) {
            outputProperty = propertyFactoryFunction.apply(name, BaseDateProperty.class);
        } else if (value instanceof LocalDateTime) {
            outputProperty = propertyFactoryFunction.apply(name, BaseDateTimeProperty.class);
        } else if (value instanceof BaseFileEntry) {
            outputProperty = propertyFactoryFunction.apply(name, BaseFileEntryProperty.class);
        } else if (value instanceof Integer) {
            outputProperty = propertyFactoryFunction.apply(name, BaseIntegerProperty.class);
        } else if (value instanceof Number) {
            outputProperty = propertyFactoryFunction.apply(name, BaseNumberProperty.class);
        } else if (value instanceof Map<?, ?>) {
            outputProperty = propertyFactoryFunction.apply(name, BaseObjectProperty.class);
        } else if (value instanceof String) {
            outputProperty = propertyFactoryFunction.apply(name, BaseStringProperty.class);
        } else if (value instanceof LocalTime) {
            outputProperty = propertyFactoryFunction.apply(name, BaseTimeProperty.class);
        } else {
            try {
                outputProperty = getSchemaDefinition(
                    JsonUtils.convertValue(value, Map.class), propertyFactoryFunction);
            } catch (IllegalArgumentException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(e.getMessage(), e);
                }

                outputProperty = propertyFactoryFunction.apply(name, com.bytechef.definition.BaseProperty.class);
            }
        }

        return outputProperty;
    }

    public static <P extends BaseProperty, O extends BaseOutputSchema<P>> O toOutputSchema(
        com.bytechef.definition.BaseOutputSchema<? extends com.bytechef.definition.BaseProperty> outputSchema,
        OutputSchemaFactoryFunction<P, O> outputSchemaFactoryFunction) {

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

    @SuppressFBWarnings("DLS")
    private static Object getSampleOutput(com.bytechef.definition.BaseProperty definitionProperty) {
        return switch (definitionProperty) {
            case BaseArrayProperty<? extends com.bytechef.definition.BaseProperty> p -> {
                List<Object> items = new ArrayList<>();

                List<? extends com.bytechef.definition.BaseProperty> properties = OptionalUtils.orElse(
                    p.getItems(), List.of());

                if (!properties.isEmpty()) {
                    items.add(getSampleOutput(properties.getFirst()));
                }

                yield items;
            }
            case BaseBooleanProperty ignored -> true;
            case BaseDateProperty ignored -> LocalDate.now();
            case BaseDateTimeProperty ignored -> LocalDateTime.now();
            case BaseFileEntryProperty<?> ignored ->
                Map.of(
                    "extension", "sampleExtension", "mimeType", "sampleMimeType", "name", "sampleName", "url",
                    "file:///tmp/fileName.txt");
            case BaseIntegerProperty ignored -> 57;
            case BaseNullProperty ignored -> null;
            case BaseNumberProperty ignored -> 23.34;
            case BaseObjectProperty<? extends com.bytechef.definition.BaseProperty> p -> {
                Map<String, Object> map = new HashMap<>();

                for (com.bytechef.definition.BaseProperty property : OptionalUtils.orElse(
                    p.getProperties(), List.of())) {

                    map.put(property.getName(), getSampleOutput(property));
                }

                yield map;
            }
            case BaseStringProperty p -> "sample " + p.getName();
            case BaseTimeProperty ignored -> LocalTime.now();
            default -> throw new IllegalArgumentException(
                "Definition %s is not allowed".formatted(definitionProperty.getName()));
        };
    }

    @FunctionalInterface
    public interface OutputSchemaFactoryFunction<P extends BaseProperty, S extends BaseOutputSchema<P>> {

        S apply(com.bytechef.definition.BaseProperty property, Object value);
    }

    @FunctionalInterface
    public interface SchemaPropertyFactoryFunction {

        com.bytechef.definition.BaseProperty apply(
            String name, Class<? extends com.bytechef.definition.BaseProperty> basePropertyClass);
    }
}
