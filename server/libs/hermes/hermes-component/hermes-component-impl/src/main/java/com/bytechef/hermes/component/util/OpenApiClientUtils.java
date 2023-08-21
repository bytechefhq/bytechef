
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.util;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.hermes.component.Context.FileEntry;
import com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import com.bytechef.hermes.component.util.HttpClientUtils.Body;
import com.bytechef.hermes.component.util.HttpClientUtils.BodyContentType;
import com.bytechef.hermes.component.util.HttpClientUtils.Response;
import com.bytechef.hermes.component.util.HttpClientUtils.ResponseType;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Property.InputProperty;
import com.bytechef.hermes.definition.Property.OutputProperty;
import com.bytechef.hermes.definition.Property.Type;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.bytechef.hermes.component.util.HttpClientUtils.RequestMethod;

/**
 * @author Ivica Cardic
 */
public class OpenApiClientUtils {

    private static final String TYPE = "type";

    public static Response execute(
        Map<String, ?> inputParameters, List<? extends InputProperty> properties,
        @Nullable OutputProperty<?> outputSchema, Map<String, Object> metadata) {

        return HttpClientUtils.exchange(
            createUrl(inputParameters, metadata, properties),
            MapUtils.get(metadata, "method", RequestMethod.class))
            .configuration(HttpClientUtils.responseType(
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
                    case BINARY -> Body.of(
                        MapUtils.get(inputParameters, property.getName(), FileEntry.class), mimeType);
                    case FORM_DATA -> Body.of(
                        MapUtils.getMap(inputParameters, property.getName(), List.of(FileEntry.class), Map.of()),
                        bodyContentType);
                    case FORM_URL_ENCODED -> Body.of(
                        MapUtils.getMap(inputParameters, property.getName(), Map.of()), bodyContentType);
                    case JSON, XML -> {
                        if (property.getType() == Type.ARRAY) {
                            yield Body.of(
                                MapUtils.getList(inputParameters, property.getName(), Object.class, List.of()),
                                bodyContentType);
                        } else if (property.getType() == Type.OBJECT) {
                            yield Body.of(
                                MapUtils.getMap(inputParameters, property.getName(), Map.of()), bodyContentType);
                        } else {
                            yield Body.of(
                                MapUtils.getRequiredString(inputParameters, property.getName()), bodyContentType);
                        }
                    }
                    case RAW -> Body.of(MapUtils.getString(inputParameters, property.getName()), mimeType);
                };
            }
        }

        return null;
    }

    private static Map<String, List<String>> getValuesMap(
        Map<String, ?> inputParameters, List<? extends Property> properties, PropertyType propertyType) {

        Map<String, List<String>> valuesMap = new HashMap<>();

        for (Property property : properties) {
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
                        if (StringUtils.hasText(value)) {
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
