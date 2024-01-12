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

package com.bytechef.platform.component.util;

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
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.Property;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class OutputSchemaUtils {

    private static final Logger logger = LoggerFactory.getLogger(OutputSchemaUtils.class);

    public static Property.OutputProperty<?> getOutputSchema(Object value) {
        return getOutputSchema(null, value);
    }

    private static Property.OutputProperty<?> getOutputSchema(String name, Object value) {
        Property.OutputProperty<?> outputProperty;

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
            outputProperty = fileEntry();
        } else if (valueClass.isArray()) {
            outputProperty = array(name);
        } else if (value instanceof List<?> list) {
            ComponentDSL.ModifiableArrayProperty arrayProperty = array(name);

            Set<Property.OutputProperty<?>> itemProperties = new HashSet<>();

            for (Object item : list) {
                itemProperties.add(getOutputSchema(null, item));
            }

            outputProperty = arrayProperty.items(
                CollectionUtils.map(
                    new ArrayList<>(itemProperties),
                    property -> (ComponentDSL.ModifiableValueProperty<?, ?>) property));
        } else if (value instanceof Map<?, ?> map) {
            ComponentDSL.ModifiableObjectProperty objectProperty = object();

            List<Property.OutputProperty<?>> properties = new ArrayList<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                properties.add(getOutputSchema((String) entry.getKey(), entry.getValue()));
            }

            outputProperty = objectProperty.properties(
                CollectionUtils.map(properties, property -> (ComponentDSL.ModifiableValueProperty<?, ?>) property));
        } else {
            try {
                outputProperty = getOutputSchema(JsonUtils.convertValue(value, Map.class));
            } catch (IllegalArgumentException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(e.getMessage(), e);
                }

                outputProperty = object(name);
            }
        }

        return outputProperty;
    }
}
