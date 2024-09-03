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

import static com.bytechef.component.definition.Context.Http.RequestMethod;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.OpenAPIComponentHandler.PropertyType;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.ProcessErrorResponseFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.OutputResponse;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.exception.ComponentExecutionException;
import com.bytechef.platform.exception.ErrorType;
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
public class OpenAPIClientUtils {

    public enum ActionDefinitionErrorType implements ErrorType {

        EXECUTE_PROCESS_ERROR_RESPONSE(105);

        private final int errorKey;

        ActionDefinitionErrorType(int errorKey) {
            this.errorKey = errorKey;
        }

        @Override
        public Class<?> getErrorClass() {
            return ActionDefinition.class;
        }

        @Override
        public int getErrorKey() {
            return errorKey;
        }
    }

    private static final String TYPE = "type";

    public static Response execute(
        Map<String, ?> inputParameters, List<? extends Property> properties,
        @Nullable OutputResponse outputResponse, Map<String, Object> metadata,
        @Nullable ProcessErrorResponseFunction processErrorResponseFunction, ActionContext context) {

        ValueProperty<?> outputSchema = outputResponse == null ? null : outputResponse.getOutputSchema();

        Response response = context
            .http(http -> http.exchange(
                createUrl(inputParameters, metadata, properties),
                MapUtils.get(metadata, "method", RequestMethod.class)))
            .configuration(Http.responseType(
                outputSchema == null
                    ? null
                    : MapUtils.get(outputSchema.getMetadata(), "responseType", ResponseType.class)))
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
                    throw new ComponentExecutionException(e, ActionDefinitionErrorType.EXECUTE_PROCESS_ERROR_RESPONSE);
                }
            }
        }

        return response;
    }

    private static String createUrl(
        Map<String, ?> inputParameters, Map<String, ?> metadata, List<? extends Property> properties) {

        String path = (String) metadata.get("path");

        for (Property property : properties) {
            if (MapUtils.get(property.getMetadata(), TYPE, PropertyType.class) == PropertyType.PATH) {
                path = path.replace(
                    "{" + property.getName() + "}", MapUtils.getRequiredString(inputParameters, property.getName()));
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

        for (Property property : properties) {
            if (Objects.equals(
                MapUtils.get(property.getMetadata(), TYPE, PropertyType.class), PropertyType.BODY)) {

                return switch (bodyContentType) {
                    case BINARY -> Http.Body.of(
                        MapUtils.get(inputParameters, property.getName(), FileEntry.class), mimeType);
                    case FORM_DATA -> Http.Body.of(
                        MapUtils.getMap(inputParameters, property.getName(), List.of(FileEntry.class), Map.of()),
                        bodyContentType);
                    case FORM_URL_ENCODED -> Http.Body.of(
                        MapUtils.getMap(inputParameters, property.getName(), Map.of()), bodyContentType);
                    case JSON, XML -> {
                        if (property.getType() == Property.Type.ARRAY) {
                            yield Http.Body.of(
                                MapUtils.getList(inputParameters, property.getName(), Object.class, List.of()),
                                bodyContentType);
                        } else if (property.getType() == Property.Type.DYNAMIC_PROPERTIES) {
                            yield Http.Body.of(
                                MapUtils.getMap(inputParameters, property.getName(), Map.of()), bodyContentType);
                        } else if (property.getType() == Property.Type.OBJECT) {
                            yield Http.Body.of(
                                MapUtils.getMap(inputParameters, property.getName(), Map.of()), bodyContentType);
                        } else {
                            yield Http.Body.of(
                                MapUtils.getRequiredString(inputParameters, property.getName()), bodyContentType);
                        }
                    }
                    case RAW -> Http.Body.of(MapUtils.getString(inputParameters, property.getName()), mimeType);
                };
            }
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
}
