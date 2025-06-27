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

package com.bytechef.component.coda.util;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Marija Horvat
 */
public class CodaPropertiesUtils {

    private CodaPropertiesUtils() {
    }

    public static List<Property.ValueProperty<?>> createPropertiesForRowValues(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ActionContext context) {

        Map<String, Object> body = context
            .http(http -> http.get("/docs/" + inputParameters.getRequiredString("docId") + "/tables/"
                + inputParameters.getRequiredString("tableId") + "/columns"))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (!(body.get("items") instanceof List<?> items)) {
            return List.of();
        }

        return new ArrayList<>(items.stream()
            .filter(o -> o instanceof Map<?, ?>)
            .map(o -> createProperty((Map<?, ?>) o))
            .filter(Objects::nonNull)
            .toList());
    }

    private static ModifiableValueProperty<?, ?> createProperty(Map<?, ?> columnMap) {
        String type = null;
        String name = (String) columnMap.get("name");
        Object format = columnMap.get("format");

        if (format instanceof Map<?, ?> formatMap) {
            type = (String) formatMap.get("type");
        }

        if (type != null) {
            return switch (type) {
                case "duration", "slider", "scale" -> integer(name).label(name)
                    .required(false);
                case "lookup", "select" -> string(name).label(name)
                    .required(false);
                case "link", "image" -> string(name).label(name)
                    .required(false)
                    .controlType(Property.ControlType.URL);
                case "checkbox" -> bool(name).label(name)
                    .required(false);
                case "email" -> string(name).label(name)
                    .required(false)
                    .controlType(Property.ControlType.EMAIL);
                case "text", "canvas" -> string(name).label(name)
                    .required(false)
                    .controlType(Property.ControlType.TEXT);
                case "number", "percent", "currency" -> number(name).label(name)
                    .required(false);
                case "date" -> date(name).label(name)
                    .required(false);
                case "dateTime" -> dateTime(name).label(name)
                    .required(false);
                case "time" -> time(name).label(name)
                    .required(false);
                case "person" -> string(name).label(name)
                    .description("Use email address to insert person.")
                    .required(false);
                default -> throw new IllegalArgumentException(
                    "Unknown Coda field type='%s'".formatted(type));
            };
        }
        return null;
    }

    public static Map<String, Object> convertPropertyToCodaRowValue(
        Map<String, ?> rowValuesInput) {

        Map<String, Object> rowValues = new HashMap<>();
        for (Map.Entry<String, ?> entry : rowValuesInput.entrySet()) {
            rowValues.put(entry.getKey(), entry.getValue());
        }

        List<Map<String, Object>> cells = rowValues.entrySet()
            .stream()
            .map(entry -> Map.of("column", entry.getKey(), "value", entry.getValue()))
            .toList();

        return Map.of("rows", List.of(Map.of("cells", cells)));
    }
}
