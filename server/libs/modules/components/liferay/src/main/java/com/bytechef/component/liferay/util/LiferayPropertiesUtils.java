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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Marija Horvat
 */
public class LiferayPropertiesUtils {

    private static final Cache<String, PropertiesContainer> PROPERTIES_CONTAINER_CACHE =
        Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    private LiferayPropertiesUtils() {
    }

    public static PropertiesContainer createPropertiesForParameters(
        String application, String endpoint, ActionContext context) {

        String url = "/o/" + application + "/openapi.json";
        String cacheKey = "/o/" + application + "/" + endpoint;

        return PROPERTIES_CONTAINER_CACHE.get(cacheKey, key -> getPropertiesContainer(url, endpoint, context));
    }

    private static List<Map<String, Object>> extractPropertiesFromSchema(
        Map<?, ?> schema, Map<String, ?> specification) {

        List<Map<String, Object>> result = new ArrayList<>();

        if (schema.containsKey("$ref")) {
            String ref = (String) schema.get("$ref");

            String component = ref.replace("#/components/schemas/", "");

            if (specification.get("components") instanceof Map<?, ?> components &&
                components.get("schemas") instanceof Map<?, ?> schemas &&
                schemas.get(component) instanceof Map<?, ?> referencedSchema) {

                return extractPropertiesFromSchema(referencedSchema, specification);
            }
        }

        if (!(schema.get("properties") instanceof Map<?, ?> props)) {
            return result;
        }

        for (Map.Entry<?, ?> entry : props.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            if (!(value instanceof Map<?, ?> propertySchema)) {
                continue;
            }

            Object readOnly = propertySchema.get("readOnly");

            if (Boolean.TRUE.equals(readOnly)) {
                continue;
            }

            String type = (String) propertySchema.get("type");

            if ("object".equals(type) && propertySchema.containsKey("properties")) {
                result.addAll(extractPropertiesFromSchema(propertySchema, specification));

                continue;
            }

            if (propertySchema.containsKey("$ref")) {
                String ref = (String) propertySchema.get("$ref");

                String component = ref.replace("#/components/schemas/", "");

                if (specification.get("components") instanceof Map<?, ?> components &&
                    components.get("schemas") instanceof Map<?, ?> schemas &&
                    schemas.get(component) instanceof Map<?, ?> nestedSchema) {

                    result.addAll(extractPropertiesFromSchema(nestedSchema, specification));

                    continue;
                }
            }

            Map<String, Object> field = new HashMap<>();

            field.put("name", key);
            field.put("schema", Map.of("type", type != null ? type : "string"));
            field.put("in", "body");
            field.put("required", true);

            result.add(field);
        }

        return result;
    }

    private static ModifiableValueProperty<?, ?> createProperty(
        Map<String, ?> parameterMap, List<String> bodyParameters, List<String> headerParameters,
        List<String> pathParameters, List<String> queryParameters) {

        String in = (String) parameterMap.get("in");

        if (in == null) {
            return null;
        }

        String name = (String) parameterMap.get("name");

        if (name == null) {
            return null;
        }

        switch (in) {
            case "body" -> bodyParameters.add(name);
            case "header" -> headerParameters.add(name);
            case "path" -> pathParameters.add(name);
            case "query" -> queryParameters.add(name);
            default -> throw new IllegalArgumentException("Unknown parameter type: " + in);
        }

        Object schemaObj = parameterMap.get("schema");

        if (!(schemaObj instanceof Map<?, ?> schema)) {
            return null;
        }

        String type = (String) schema.get("type");

        if (type == null) {
            return null;
        }

        boolean required = parameterMap.get("required") != null;

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

    private static PropertiesContainer getPropertiesContainer(String url, String endpoint, ActionContext context) {
        Map<String, ?> body = context.http(http -> http.get(url))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        String[] endpointParts = endpoint.split(" ");

        String method = endpointParts[0].toLowerCase();
        String endpointUrl = endpointParts[1];

        List<Map<String, Object>> parameters = new ArrayList<>();

        String version = "";

        if (body.get("info") instanceof Map<?, ?> info) {
            version = String.valueOf(info.get("version"));
        }

        if (body.get("paths") instanceof Map<?, ?> paths) {
            for (Map.Entry<?, ?> pathEntry : paths.entrySet()) {
                String pathKey = String.valueOf(pathEntry.getKey());

                String replaceFirst = pathKey.replaceFirst("/" + version, "");

                if (replaceFirst.equals(endpointUrl)) {
                    Object path = pathEntry.getValue();

                    if (path instanceof Map<?, ?> pathObj) {
                        Object methodObj = pathObj.get(method);

                        if (methodObj instanceof Map<?, ?> methods) {
                            if (methods.get("parameters") instanceof List<?> parametersList) {
                                for (Object parametersObj : parametersList) {
                                    if (parametersObj instanceof Map<?, ?> parametersMap) {
                                        Map<String, Object> parametersTypedMap = new HashMap<>();

                                        parametersMap.forEach(
                                            (key, value) -> parametersTypedMap.put(String.valueOf(key), value));

                                        parameters.add(parametersTypedMap);
                                    }
                                }
                            }

                            if (methods.get("requestBody") instanceof Map<?, ?> requestBody &&
                                requestBody.get("content") instanceof Map<?, ?> content) {

                                Object jsonContent = content.get("application/json");

                                if (jsonContent instanceof Map<?, ?> jsonContentMap &&
                                    jsonContentMap.get("schema") instanceof Map<?, ?> schema) {

                                    parameters.addAll(extractPropertiesFromSchema(schema, body));
                                }
                            }
                        }
                    }
                }
            }
        }

        List<String> bodyParameters = new ArrayList<>();
        List<String> headerParameters = new ArrayList<>();
        List<String> pathParameters = new ArrayList<>();
        List<String> queryParameters = new ArrayList<>();

        return new PropertiesContainer(
            new ArrayList<>(
                parameters.stream()
                    .map(parameterMap -> createProperty(
                        parameterMap, bodyParameters, headerParameters, pathParameters, queryParameters))
                    .filter(Objects::nonNull)
                    .toList()),
            bodyParameters, headerParameters, pathParameters, queryParameters);
    }
}
