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

package com.bytechef.component.retable.util;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.retable.constant.RetableConstants.RETABLE_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Marija Horvat
 */
public class RetablePropertiesUtils {

    private RetablePropertiesUtils() {
    }

    public static List<Property.ValueProperty<?>> createPropertiesForRowValues(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ActionContext actionContext) {

        Map<String, Map<String, Object>> body = actionContext
            .http(http -> http.get("/retable/" + inputParameters.getRequiredString(RETABLE_ID)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (!(body.get("data")
            .get("columns") instanceof List<?> items)) {
            return List.of();
        }

        return new ArrayList<>(items.stream()
            .filter(o -> o instanceof Map<?, ?>)
            .map(o -> createProperty((Map<?, ?>) o))
            .filter(Objects::nonNull)
            .toList());
    }

    private static ModifiableValueProperty<?, ?> createProperty(Map<?, ?> columnMap) {
        String name = (String) columnMap.get("title");
        String type = (String) columnMap.get("type");

        if (type != null) {
            return switch (type) {
                case "text", "select", "color", "phone_number" -> string(name)
                    .label(name)
                    .required(false);
                case "email" -> string(name)
                    .label(name)
                    .required(false)
                    .controlType(Property.ControlType.EMAIL);
                case "checkbox" -> bool(name)
                    .label(name)
                    .required(false);
                case "number", "percent", "currency" -> number(name)
                    .label(name)
                    .required(false);
                case "rating", "duration" -> integer(name)
                    .label(name)
                    .required(false);
                case "calendar" -> dateTime(name)
                    .label(name)
                    .required(false);
                case "attachment", "image" -> fileEntry(name)
                    .label(name)
                    .required(false);
                default -> null;
            };
        }

        return null;
    }
}
