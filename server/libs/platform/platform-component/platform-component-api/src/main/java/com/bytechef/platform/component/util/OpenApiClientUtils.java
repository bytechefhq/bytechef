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

package com.bytechef.platform.component.util;

import static com.bytechef.component.definition.Context.Http.RequestMethod;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.OpenApiComponentHandler.PropertyType;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.ProcessErrorResponseFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.exception.AbstractErrorType;
import com.bytechef.exception.ExecutionException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public class OpenApiClientUtils {

    private static final String TYPE = "type";

    public static Object execute(
        Map<String, ?> inputParameters, List<? extends Property> properties,
        @Nullable OutputDefinition outputDefinition, Map<String, Object> metadata,
        @Nullable ProcessErrorResponseFunction processErrorResponseFunction, ActionContext context) {

        BaseValueProperty<?> outputSchema = outputDefinition == null ? null : outputDefinition.getOutputSchema();

        ResponseType responseType = outputSchema == null
            ? MapUtils.get(metadata, "responseType", ResponseType.class)
            : MapUtils.get(outputSchema.getMetadata(), "responseType", ResponseType.class);

        Response response = context
            .http(http -> http.exchange(
                createUrl(inputParameters, metadata, properties),
                MapUtils.get(metadata, "method", RequestMethod.class)))
            .configuration(Http.responseType(responseType))
            .headers(getValuesMap(inputParameters, properties, PropertyType.HEADER))
            .body(
                getBody(
                    MapUtils.get(metadata, "bodyContentType", BodyContentType.class),
                    MapUtils.getString(metadata, "mimeType"), inputParameters, properties))
            .queryParameters(getValuesMap(inputParameters, properties, PropertyType.QUERY))
            .execute();

        if (response.getStatusCode() < 200 || response.getStatusCode() > 299) {
            Object body = response.getBody();

            if (processErrorResponseFunction == null) {
                throw new ProviderException(response.getStatusCode(), body == null ? null : body.toString());
            } else {
                try {
                    throw processErrorResponseFunction.apply(response.getStatusCode(), body, context);
                } catch (Exception e) {
                    throw new ExecutionException(e, ActionDefinitionErrorType.ERROR_RESPONSE_NOT_PROCESSED);
                }
            }
        }

        return response.getBody();
    }

    private static String createUrl(
        Map<String, ?> inputParameters, Map<String, ?> metadata, List<? extends Property> properties) {

        String path = (String) metadata.get("path");

        for (Property property : properties) {
            if (MapUtils.get(property.getMetadata(), TYPE, PropertyType.class) == PropertyType.PATH) {
                path = path.replace(
                    "{" + property.getName() + "}",
                    URLEncoder.encode(
                        MapUtils.getRequiredString(inputParameters, property.getName()),
                        StandardCharsets.UTF_8));
            }
        }

        return path;
    }

    private static Body getBody(
        BodyContentType bodyContentType, String mimeType, Map<String, ?> inputParameters,
        List<? extends Property> properties) {

        if (bodyContentType == null) {
            return null;
        }

        List<? extends Property> bodyProperties = properties.stream()
            .filter(property -> Objects.equals(
                MapUtils.get(property.getMetadata(), TYPE, PropertyType.class), PropertyType.BODY))
            .toList();

        if (bodyProperties.size() == 1) {
            Property bodyProperty = bodyProperties.getFirst();

            String name = bodyProperty.getName();

            return switch (bodyContentType) {
                case BINARY -> Http.Body.of(
                    MapUtils.get(inputParameters, name, FileEntry.class), mimeType);
                case FORM_DATA -> Http.Body.of(
                    MapUtils.getMap(inputParameters, name, List.of(FileEntry.class), Map.of()), bodyContentType);
                case FORM_URL_ENCODED -> Http.Body.of(
                    MapUtils.getMap(inputParameters, name, Map.of()), bodyContentType);
                case JSON, XML -> {
                    Property.Type type = bodyProperty.getType();

                    if (type == Property.Type.ARRAY) {
                        yield Http.Body.of(
                            MapUtils.getList(inputParameters, name, Object.class, List.of()), bodyContentType);
                    } else if (type == Property.Type.DYNAMIC_PROPERTIES) {
                        yield Http.Body.of(MapUtils.getMap(inputParameters, name, Map.of()), bodyContentType);
                    } else {
                        yield Http.Body.of(Map.of(name, MapUtils.get(inputParameters, name)), bodyContentType);
                    }
                }
                case RAW -> Http.Body.of(MapUtils.getString(inputParameters, name), mimeType);
            };
        } else if (bodyProperties.size() > 1) {
            Map<String, Object> body = new HashMap<>();

            for (Property property : bodyProperties) {
                Property.Type type = property.getType();

                if (type.equals(Property.Type.DYNAMIC_PROPERTIES)) {
                    String name = property.getName();

                    Map<String, ?> map = MapUtils.getMap(inputParameters, name, Map.of());

                    body.put(name, map.get(name));
                } else {
                    Object value = MapUtils.get(inputParameters, property.getName());

                    if (value != null) {
                        body.put(property.getName(), value);
                    }
                }
            }

            return Http.Body.of(body, bodyContentType);
        }

        return null;
    }

    protected static Map<String, List<String>> getValuesMap(
        Map<String, ?> inputParameters, List<? extends Property> properties, PropertyType propertyType) {

        Map<String, List<String>> valuesMap = new HashMap<>();

        for (Property property : properties) {
            if (Objects.equals(MapUtils.get(property.getMetadata(), TYPE, PropertyType.class), propertyType)) {
                List<String> values;

                if (property.getType() == Property.Type.ARRAY) {
                    values = MapUtils.getList(inputParameters, property.getName(), String.class, List.of());
                } else {
                    String value = MapUtils.getString(inputParameters, property.getName());

                    values = value == null ? List.of() : List.of(value);
                }

                valuesMap.compute(property.getName(), (key, curValues) -> {
                    for (String value : values) {
                        if (StringUtils.isNotBlank(value)) {
                            if (curValues == null) {
                                curValues = new ArrayList<>();
                            }

                            if (!curValues.contains(value)) {
                                curValues.add(URLEncoder.encode(value, StandardCharsets.UTF_8));
                            }
                        }
                    }

                    return curValues;
                });
            }
        }

        return valuesMap;
    }

    private static class ActionDefinitionErrorType extends AbstractErrorType {

        private static final ActionDefinitionErrorType ERROR_RESPONSE_NOT_PROCESSED = new ActionDefinitionErrorType(
            105);

        ActionDefinitionErrorType(int errorKey) {
            super(ActionDefinition.class, errorKey);
        }
    }
}
