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
import com.bytechef.definition.BaseOutput;
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

    public static com.bytechef.definition.BaseProperty getOutputSchema(
        Object value, SchemaPropertyFactory propertyFactoryFunction) {

        return getOutputSchema(null, value, propertyFactoryFunction);
    }

    public static com.bytechef.definition.BaseProperty getOutputSchema(
        String name, Object value, SchemaPropertyFactory propertyFactory) {

        com.bytechef.definition.BaseProperty outputProperty;
        Class<?> valueClass = value.getClass();

        if (value instanceof List<?> || valueClass.isArray()) {
            outputProperty = propertyFactory.create(name, BaseArrayProperty.class);
        } else if (value instanceof Boolean) {
            outputProperty = propertyFactory.create(name, BaseBooleanProperty.class);
        } else if (value instanceof Date || value instanceof LocalDate) {
            outputProperty = propertyFactory.create(name, BaseDateProperty.class);
        } else if (value instanceof LocalDateTime) {
            outputProperty = propertyFactory.create(name, BaseDateTimeProperty.class);
        } else if (value instanceof BaseFileEntry) {
            outputProperty = propertyFactory.create(name, BaseFileEntryProperty.class);
        } else if (value instanceof Integer) {
            outputProperty = propertyFactory.create(name, BaseIntegerProperty.class);
        } else if (value instanceof Number) {
            outputProperty = propertyFactory.create(name, BaseNumberProperty.class);
        } else if (value instanceof Map<?, ?>) {
            outputProperty = propertyFactory.create(name, BaseObjectProperty.class);
        } else if (value instanceof String) {
            outputProperty = propertyFactory.create(name, BaseStringProperty.class);
        } else if (value instanceof LocalTime) {
            outputProperty = propertyFactory.create(name, BaseTimeProperty.class);
        } else {
            try {
                outputProperty = getOutputSchema(
                    "value", JsonUtils.convertValue(value, Map.class), propertyFactory);
            } catch (IllegalArgumentException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(e.getMessage(), e);
                }

                outputProperty = propertyFactory.create(name, com.bytechef.definition.BaseProperty.class);
            }
        }

        return outputProperty;
    }

    @SuppressWarnings("unchecked")
    public static <P extends BaseProperty, O extends com.bytechef.platform.registry.domain.BaseOutput<P>> O toOutput(
        BaseOutput<? extends com.bytechef.definition.BaseProperty> output,
        OutputFactoryFunction<P, O> outputFactoryFunction) {

        Map<String, ?> sampleOutput;

        if (output.getSampleOutput() == null) {
            sampleOutput = (Map<String, ?>) getSampleOutput(output.getOutputSchema());
        } else if (output.getSampleOutput() instanceof Map map) {
            sampleOutput = map;
        } else if (output.getSampleOutput() instanceof String string) {
            try {
                sampleOutput = JsonUtils.readMap(string);
            } catch (Exception e) {
                //
                sampleOutput = Map.of("result", output.getSampleOutput());
            }
        } else {
            sampleOutput = Map.of("result", output.getSampleOutput());
        }

        return outputFactoryFunction.apply(output.getOutputSchema(), sampleOutput);
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
    public interface OutputFactoryFunction<P extends BaseProperty, S extends com.bytechef.platform.registry.domain.BaseOutput<P>> {

        S apply(com.bytechef.definition.BaseProperty property, Map<String, ?> value);
    }

    @FunctionalInterface
    public interface SchemaPropertyFactory {

        com.bytechef.definition.BaseProperty create(
            String name, Class<? extends com.bytechef.definition.BaseProperty> basePropertyClass);
    }
}
