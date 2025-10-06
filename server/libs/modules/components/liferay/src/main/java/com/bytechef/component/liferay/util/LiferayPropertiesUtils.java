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

package com.bytechef.component.liferay.util;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.liferay.constant.LiferayConstants.APPLICATION;
import static com.bytechef.component.liferay.constant.LiferayConstants.BODY_PARAMETERS;
import static com.bytechef.component.liferay.constant.LiferayConstants.ENDPOINT;
import static com.bytechef.component.liferay.constant.LiferayConstants.HEADER_PARAMETERS;
import static com.bytechef.component.liferay.constant.LiferayConstants.PATH_PARAMETERS;
import static com.bytechef.component.liferay.constant.LiferayConstants.QUERY_PARAMETERS;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Context.Http.ResponseType;
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
public class LiferayPropertiesUtils {

    private LiferayPropertiesUtils() {
    }

    public static List<Property.ValueProperty<?>> createPropertiesForParameters(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ActionContext context) {

        Map<String, ?> body = context
            .http(http -> http.get("/o/" + inputParameters.getRequiredString(APPLICATION) + "/openapi.json"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        String[] endpointParts = inputParameters.getRequiredString(ENDPOINT)
            .split(" ");
        String method = endpointParts[0].toLowerCase();
        String endpoint = endpointParts[1];

        List<Map<String, Object>> parameters = new ArrayList<>();

        String version = "";
        if (body.get("info") instanceof Map<?, ?> info) {
            version = info.get("version")
                .toString();
        }

        if (body.get("paths") instanceof Map<?, ?> paths) {
            for (Map.Entry<?, ?> pathEntry : paths.entrySet()) {
                String pathKey = pathEntry.getKey()
                    .toString();

                if (pathKey
                    .replaceFirst("/" + version, "")
                    .equals(endpoint)) {
                    Object path = pathEntry.getValue();
                    if (path instanceof Map<?, ?> pathObj) {
                        Object methodObj = pathObj.get(method);
                        if (methodObj instanceof Map<?, ?> methods) {

                            if (methods.get("parameters") instanceof List<?> parametersList) {
                                for (Object param : parametersList) {
                                    if (param instanceof Map<?, ?> paramMap) {
                                        parameters.add((Map<String, Object>) paramMap);
                                    }
                                }
                            }

                            if (methods.get("requestBody") instanceof Map<?, ?> requestBody &&
                                requestBody.get("content") instanceof Map<?, ?> content) {

                                Object jsonContent = content.get("application/json");
                                if (jsonContent instanceof Map<?, ?> jsonContentMap
                                    && jsonContentMap.get("schema") instanceof Map<?, ?> schema) {
                                    parameters.addAll(extractPropertiesFromSchema(schema, body));
                                }
                            }
                        }
                    }
                }
            }
        }

        return new ArrayList<>(parameters.stream()
            .map(p -> createProperty((Map<String, ?>) p))
            .filter(Objects::nonNull)
            .toList());
    }

    private static List<Map<String, Object>> extractPropertiesFromSchema(
        Map<?, ?> schema, Map<String, ?> fullSpec) {

        List<Map<String, Object>> result = new ArrayList<>();

        if (schema.containsKey("$ref")) {
            String ref = (String) schema.get("$ref");
            String component = ref.replace("#/components/schemas/", "");

            if (fullSpec.get("components") instanceof Map<?, ?> components &&
                components.get("schemas") instanceof Map<?, ?> schemas &&
                schemas.get(component) instanceof Map<?, ?> referencedSchema) {

                return extractPropertiesFromSchema(referencedSchema, fullSpec);
            }
        }

        if (!(schema.get("properties") instanceof Map<?, ?> props)) {
            return result;
        }

        for (Map.Entry<?, ?> entry : props.entrySet()) {
            String propName = entry.getKey()
                .toString();
            Object propValue = entry.getValue();

            if (!(propValue instanceof Map<?, ?> propSchema)) {
                continue;
            }

            Object readOnly = propSchema.get("readOnly");
            if (Boolean.TRUE.equals(readOnly)) {
                continue;
            }

            String type = (String) propSchema.get("type");

            if ("object".equals(type) && propSchema.containsKey("properties")) {
                result.addAll(extractPropertiesFromSchema(propSchema, fullSpec));
                continue;
            }

            if (propSchema.containsKey("$ref")) {
                String ref = (String) propSchema.get("$ref");
                String component = ref.replace("#/components/schemas/", "");

                if (fullSpec.get("components") instanceof Map<?, ?> components &&
                    components.get("schemas") instanceof Map<?, ?> schemas &&
                    schemas.get(component) instanceof Map<?, ?> nestedSchema) {

                    result.addAll(extractPropertiesFromSchema(nestedSchema, fullSpec));
                    continue;
                }
            }

            Map<String, Object> field = new HashMap<>();
            field.put("name", propName);
            field.put("schema", Map.of("type", type != null ? type : "string"));
            field.put("in", "body");
            field.put("required", true);
            result.add(field);
        }

        return result;
    }

    private static ModifiableValueProperty<?, ?> createProperty(Map<String, ?> parameters) {

        String in = (String) parameters.get("in");
        if (in == null) {
            return null;
        }

        String name = (String) parameters.get("name");
        if (name == null) {
            return null;
        }

        switch (in) {
            case "body" -> BODY_PARAMETERS.add(name);
            case "header" -> HEADER_PARAMETERS.add(name);
            case "path" -> PATH_PARAMETERS.add(name);
            case "query" -> QUERY_PARAMETERS.add(name);
            default -> throw new IllegalArgumentException("Unknown parameter type: " + in);
        }

        Object schemaObj = parameters.get("schema");
        if (!(schemaObj instanceof Map<?, ?> schema)) {
            return null;
        }

        String type = (String) schema.get("type");
        if (type == null) {
            return null;
        }

        boolean required = parameters.get("required") != null;

        return switch (type) {
            case "boolean" -> bool(name)
                .label(name)
                .required(required);
            case "string" -> string(name)
                .label(name)
                .required(required);
            case "number" -> number(name)
                .label(name)
                .required(required);
            case "integer" -> integer(name)
                .label(name)
                .required(required);
            case "object" -> object(name)
                .label(name)
                .required(required);
            case "array" -> array(name)
                .label(name)
                .items(
                    string()
                        .options())
                .required(required);

            default -> null;
        };
    }
}
