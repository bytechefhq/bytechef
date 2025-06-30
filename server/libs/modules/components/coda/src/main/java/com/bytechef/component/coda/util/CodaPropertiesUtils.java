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

import static com.bytechef.component.coda.constant.CodaConstants.DOC_ID;
import static com.bytechef.component.coda.constant.CodaConstants.TABLE_ID;
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
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Marija Horvat
 */
public class CodaPropertiesUtils {

    private CodaPropertiesUtils() {
    }

    public static List<ValueProperty<?>> createPropertiesForRowValues(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ActionContext actionContext) {

        Map<String, Object> body = actionContext
            .http(http -> http.get("/docs/%s/tables/%s/columns".formatted(
                inputParameters.getRequiredString(DOC_ID), inputParameters.getRequiredString(TABLE_ID))))
            .configuration(responseType(Http.ResponseType.JSON))
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
                case "duration", "slider", "scale" -> integer(name)
                    .label(name)
                    .required(false);
                case "lookup", "select", "text", "canvas" -> string(name)
                    .label(name)
                    .required(false);
                case "link", "image" -> string(name)
                    .label(name)
                    .required(false)
                    .controlType(ControlType.URL);
                case "checkbox" -> bool(name)
                    .label(name)
                    .required(false);
                case "email" -> string(name)
                    .label(name)
                    .required(false)
                    .controlType(ControlType.EMAIL);
                case "number", "percent", "currency" -> number(name)
                    .label(name)
                    .required(false);
                case "date" -> date(name)
                    .label(name)
                    .required(false);
                case "dateTime" -> dateTime(name)
                    .label(name)
                    .required(false);
                case "time" -> time(name)
                    .label(name)
                    .required(false);
                case "person" -> string(name)
                    .label(name)
                    .description("Use email address to insert person.")
                    .required(false);
                default -> null;
            };
        }

        return null;
    }
}
