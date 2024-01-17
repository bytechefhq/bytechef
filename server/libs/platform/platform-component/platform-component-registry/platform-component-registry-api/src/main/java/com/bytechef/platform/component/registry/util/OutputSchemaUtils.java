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

package com.bytechef.platform.component.registry.util;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableValueProperty;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.platform.component.registry.domain.OutputSchema;
import com.bytechef.platform.component.registry.domain.Property;
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
public class OutputSchemaUtils {

    private static final Logger logger = LoggerFactory.getLogger(OutputSchemaUtils.class);

    public static ValueProperty<?> getOutputSchemaDefinition(Object value) {
        return getOutputSchemaDefinition(null, value);
    }

    private static ValueProperty<?> getOutputSchemaDefinition(String name, Object value) {
        ValueProperty<?> outputProperty;

        Class<?> valueClass = value.getClass();

        if (value instanceof Boolean) {
            outputProperty = bool(name);
        } else if (value instanceof Date || value instanceof LocalDate) {
            outputProperty = date(name);
        } else if (value instanceof LocalDateTime) {
            outputProperty = dateTime(name);
        } else if (value instanceof LocalTime) {
            outputProperty = time(name);
        } else if (value instanceof Integer) {
            outputProperty = integer(name);
        } else if (value instanceof Number) {
            outputProperty = number(name);
        } else if (value instanceof String) {
            outputProperty = string(name);
        } else if (value instanceof ActionContext.FileEntry) {
            outputProperty = fileEntry(name);
        } else if (valueClass.isArray()) {
            outputProperty = array(name);
        } else if (value instanceof List<?> list) {
            ModifiableArrayProperty arrayProperty = array(name);

            if (!list.isEmpty()) {
                arrayProperty.items((ModifiableValueProperty<?, ?>) getOutputSchemaDefinition(null, list.getFirst()));
            }

            outputProperty = arrayProperty;
        } else if (value instanceof Map<?, ?> map) {
            ModifiableObjectProperty objectProperty = object();

            List<ValueProperty<?>> properties = new ArrayList<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                properties.add(getOutputSchemaDefinition((String) entry.getKey(), entry.getValue()));
            }

            outputProperty = objectProperty.properties(
                CollectionUtils.map(properties, property -> (ModifiableValueProperty<?, ?>) property));
        } else {
            try {
                outputProperty = getOutputSchemaDefinition(JsonUtils.convertValue(value, Map.class));
            } catch (IllegalArgumentException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(e.getMessage(), e);
                }

                outputProperty = object(name);
            }
        }

        return outputProperty;
    }

    public static OutputSchema toOutputSchema(com.bytechef.component.definition.OutputSchema outputSchema) {
        Object sampleOutput = outputSchema.sampleOutput();

        if (sampleOutput == null) {
            sampleOutput = getSampleOutput(outputSchema.definition());
        }

        if (sampleOutput instanceof String string) {
            try {
                sampleOutput = JsonUtils.read(string);
            } catch (Exception e) {
                //
            }
        }

        return new OutputSchema(Property.toProperty(outputSchema.definition()), sampleOutput);
    }

    private static Object getSampleOutput(ValueProperty<?> definitionProperty) {
        return switch (definitionProperty) {
            case com.bytechef.component.definition.Property.ArrayProperty p -> {
                List<Object> items = new ArrayList<>();

                List<? extends ValueProperty<?>> properties = OptionalUtils.orElse(p.getItems(), List.of());

                if (!properties.isEmpty()) {
                    items.add(getSampleOutput(properties.getFirst()));
                }

                yield items;
            }
            case com.bytechef.component.definition.Property.BooleanProperty p -> true;
            case com.bytechef.component.definition.Property.DateProperty p -> LocalDate.now();
            case com.bytechef.component.definition.Property.DateTimeProperty p -> LocalDateTime.now();
            case com.bytechef.component.definition.Property.FileEntryProperty p ->
                Map.of(
                    "extension", "sampleExtension", "mimeType", "sampleMimeType", "name", "sampleName", "url",
                    "file:///tmp/fileName.txt");
            case com.bytechef.component.definition.Property.IntegerProperty p -> 57;
            case com.bytechef.component.definition.Property.NullProperty p -> null;
            case com.bytechef.component.definition.Property.NumberProperty p -> 23.34;
            case com.bytechef.component.definition.Property.ObjectProperty p -> {
                Map<String, Object> map = new HashMap<>();

                for (ValueProperty<?> property : OptionalUtils.orElse(p.getProperties(), List.of())) {
                    map.put(property.getName(), getSampleOutput(property));
                }

                yield map;
            }
            case com.bytechef.component.definition.Property.StringProperty p -> "sample " + p.getName();
            case com.bytechef.component.definition.Property.TimeProperty p -> LocalTime.now();
            default -> throw new IllegalArgumentException(
                "Definition %s is not allowed".formatted(definitionProperty.getName()));
        };
    }
}
