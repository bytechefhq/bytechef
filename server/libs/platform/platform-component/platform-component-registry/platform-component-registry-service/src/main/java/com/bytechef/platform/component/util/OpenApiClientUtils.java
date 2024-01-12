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

import static com.bytechef.hermes.component.definition.Context.Http.RequestMethod;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import com.bytechef.hermes.component.definition.ActionContext.FileEntry;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.Context.Http;
import com.bytechef.hermes.component.definition.Context.Http.Body;
import com.bytechef.hermes.component.definition.Context.Http.BodyContentType;
import com.bytechef.hermes.component.definition.Context.Http.Response;
import com.bytechef.hermes.component.definition.Context.Http.ResponseType;
import com.bytechef.hermes.definition.BaseProperty;
import com.bytechef.hermes.definition.BaseProperty.InputProperty;
import com.bytechef.hermes.definition.BaseProperty.OutputProperty;
import com.bytechef.hermes.definition.BaseProperty.Type;
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

    public static Response execute(
        Map<String, ?> inputParameters, List<? extends InputProperty> properties,
        @Nullable OutputProperty<?> outputSchema, Map<String, Object> metadata, Context context) {

        return context.http(http -> http.exchange(
            createUrl(inputParameters, metadata, properties), MapUtils.get(metadata, "method", RequestMethod.class)))
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
    }

    private static String createUrl(
        Map<String, ?> inputParameters, Map<String, ?> metadata, List<? extends BaseProperty> properties) {

        String path = (String) metadata.get("path");

        for (BaseProperty property : properties) {
            if (MapUtils.get(property.getMetadata(), TYPE, PropertyType.class) == PropertyType.PATH) {
                path = path.replace(
                    "{" + property.getName() + "}", MapUtils.getRequiredString(inputParameters, property.getName()));
            }
        }

        return path;
    }

    private static Body getBody(
        BodyContentType bodyContentType, String mimeType, Map<String, ?> inputParameters,
        List<? extends BaseProperty> properties) {

        if (bodyContentType == null) {
            return null;
        }

        for (BaseProperty property : properties) {
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
                        if (property.getType() == Type.ARRAY) {
                            yield Http.Body.of(
                                MapUtils.getList(inputParameters, property.getName(), Object.class, List.of()),
                                bodyContentType);
                        } else if (property.getType() == Type.OBJECT) {
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

    private static Map<String, List<String>> getValuesMap(
        Map<String, ?> inputParameters, List<? extends BaseProperty> properties, PropertyType propertyType) {

        Map<String, List<String>> valuesMap = new HashMap<>();

        for (BaseProperty property : properties) {
            if (Objects.equals(MapUtils.get(property.getMetadata(), TYPE, PropertyType.class), propertyType)) {
                List<String> values;

                if (property.getType() == Type.ARRAY) {
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
                                curValues.add(value);
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
