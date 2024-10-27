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

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.definition.BaseControlType;
import com.bytechef.definition.BaseFileEntry;
import com.bytechef.definition.BaseProperty;
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
import com.bytechef.platform.registry.domain.OutputResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public class SchemaUtils {

    private static final Logger logger = LoggerFactory.getLogger(SchemaUtils.class);

    public static BaseProperty getOutputSchema(
        @NonNull Object value, @NonNull SchemaPropertyFactory propertyFactoryFunction) {

        return getOutputSchema(null, value, propertyFactoryFunction);
    }

    public static BaseProperty getOutputSchema(
        String name, Object value, @NonNull SchemaPropertyFactory propertyFactory) {

        Validate.notNull(propertyFactory, "propertyFactory must not be null");

        BaseProperty outputProperty;

        if (value == null) {
            outputProperty = propertyFactory.create(name, null, BaseNullProperty.class);
        } else {
            Class<?> valueClass = value.getClass();

            if (value instanceof List<?> || valueClass.isArray()) {
                outputProperty = propertyFactory.create(name, value, BaseArrayProperty.class);
            } else if (value instanceof Boolean) {
                outputProperty = propertyFactory.create(name, value, BaseBooleanProperty.class);
            } else if (value instanceof Date || value instanceof LocalDate) {
                outputProperty = propertyFactory.create(name, value, BaseDateProperty.class);
            } else if (value instanceof LocalDateTime) {
                outputProperty = propertyFactory.create(name, value, BaseDateTimeProperty.class);
            } else if (value instanceof BaseFileEntry) {
                outputProperty = propertyFactory.create(name, value, BaseFileEntryProperty.class);
            } else if (value instanceof Integer) {
                outputProperty = propertyFactory.create(name, value, BaseIntegerProperty.class);
            } else if (value instanceof Number) {
                outputProperty = propertyFactory.create(name, value, BaseNumberProperty.class);
            } else if (value instanceof Map<?, ?>) {
                outputProperty = propertyFactory.create(name, value, BaseObjectProperty.class);
            } else if (value instanceof String) {
                outputProperty = propertyFactory.create(name, value, BaseStringProperty.class);
            } else if (value instanceof LocalTime) {
                outputProperty = propertyFactory.create(name, value, BaseTimeProperty.class);
            } else {
                if (ConvertUtils.canConvert(value, Map.class)) {
                    outputProperty = propertyFactory.create(name, value, BaseObjectProperty.class);
                } else {
                    outputProperty = propertyFactory.create(name, value, BaseProperty.class);
                }
            }
        }

        return outputProperty;
    }

    public static OutputResponse toOutput(
        @NonNull com.bytechef.definition.BaseOutputDefinition.OutputResponse outputResponse,
        @NonNull OutputFactoryFunction outputFactoryFunction,
        @NonNull SchemaPropertyFactory propertyFactoryFunction) {

        Object sampleOutput = outputResponse.sampleOutput();

        if (sampleOutput == null) {
            sampleOutput = getSampleOutput(outputResponse.outputSchema());
        } else if (sampleOutput instanceof String string) {
            try {
                sampleOutput = JsonUtils.readMap(string);
            } catch (Exception e) {
                if (logger.isTraceEnabled()) {
                    logger.trace(e.getMessage(), e);
                }
            }
        }

        BaseProperty.BaseValueProperty<?> outputSchema = outputResponse.outputSchema();

        if (outputSchema == null) {
            outputSchema = (BaseProperty.BaseValueProperty<?>) getOutputSchema(sampleOutput, propertyFactoryFunction);
        }

        return outputFactoryFunction.apply(outputSchema, sampleOutput);
    }

    @SuppressFBWarnings("DLS")
    private static Object getSampleOutput(BaseProperty definitionProperty) {
        return switch (definitionProperty) {
            case BaseArrayProperty<? extends BaseProperty> p -> {
                List<Object> items = new ArrayList<>();

                List<? extends BaseProperty> properties = OptionalUtils.orElse(
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
                    "extension", "txt", "mimeType", "text/plain", "name", "sampleName", "url",
                    "file:///tmp/sampleFileName.txt");
            case BaseIntegerProperty ignored -> 57;
            case BaseNullProperty ignored -> null;
            case BaseNumberProperty ignored -> 23.34;
            case BaseObjectProperty<? extends BaseProperty> p -> {
                Map<String, Object> map = new HashMap<>();

                for (BaseProperty property : OptionalUtils.orElse(
                    p.getProperties(), List.of())) {

                    map.put(property.getName(), getSampleOutput(property));
                }

                yield map;
            }
            case BaseStringProperty p -> {
                BaseControlType baseControlType = p.getControlType();

                if (Objects.equals(baseControlType.name(), "EMAIL")) {
                    yield "sample_email@" + p.getName();
                } else {
                    yield "sample " + p.getName();
                }
            }
            case BaseTimeProperty ignored -> LocalTime.now();
            default -> throw new IllegalArgumentException(
                "Definition %s is not allowed".formatted(definitionProperty.getName()));
        };
    }

    @FunctionalInterface
    public interface OutputFactoryFunction {

        OutputResponse apply(BaseProperty property, Object value);
    }

    @FunctionalInterface
    public interface SchemaPropertyFactory {

        BaseProperty create(String name, Object value, Class<? extends BaseProperty> basePropertyClass);
    }
}
