
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

package com.bytechef.hermes.component.oas;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.Context.FileEntry;
import com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.util.HttpClientUtils;
import com.bytechef.hermes.component.util.HttpClientUtils.BodyContentType;
import com.bytechef.hermes.component.util.HttpClientUtils.Body;
import com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Property.Type;
import com.bytechef.hermes.definition.Property.ValueProperty;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.bytechef.hermes.component.util.HttpClientUtils.RequestMethod;

/**
 * @author Ivica Cardic
 */
public class OpenApiClient {

    private static final String TYPE = "type";

    public HttpClientUtils.Response execute(ActionDefinition actionDefinition, TaskExecution taskExecution) {
        Map<String, Object> metadata = actionDefinition.getMetadata();

        return HttpClientUtils.exchange(
            createUrl(
                metadata, taskExecution.getParameters(),
                OptionalUtils.orElse(actionDefinition.getProperties(), Collections.emptyList())),
            MapValueUtils.get(metadata, "method", RequestMethod.class))
            .configuration(
                HttpClientUtils.responseFormat(getResponseFormat(actionDefinition)))
            .headers(
                getValuesMap(
                    taskExecution.getParameters(),
                    OptionalUtils.orElse(actionDefinition.getProperties(), Collections.emptyList()),
                    PropertyType.HEADER))
            .body(
                getBody(
                    MapValueUtils.get(metadata, "bodyContentType", BodyContentType.class),
                    MapValueUtils.getString(metadata, "mimeType"),
                    taskExecution.getParameters(),
                    OptionalUtils.orElse(actionDefinition.getProperties(), Collections.emptyList())))
            .queryParameters(
                getValuesMap(
                    taskExecution.getParameters(),
                    OptionalUtils.orElse(actionDefinition.getProperties(), Collections.emptyList()),
                    PropertyType.QUERY))
            .execute();
    }

    private String createUrl(Map<String, ?> metadata, Map<String, ?> parameters, List<? extends Property> properties) {
        String path = (String) metadata.get("path");

        for (Property property : properties) {
            if (MapValueUtils.get(property.getMetadata(), TYPE, PropertyType.class) == PropertyType.PATH) {
                path = path.replace(
                    "{" + property.getName() + "}", MapValueUtils.getRequiredString(parameters, property.getName()));
            }
        }

        return path;
    }

    private Body getBody(
        BodyContentType bodyContentType, String mimeType, Map<String, ?> parameters,
        List<? extends Property> properties) {

        if (bodyContentType == null) {
            return null;
        }

        for (Property property : properties) {
            if (Objects.equals(
                MapValueUtils.get(property.getMetadata(), TYPE, PropertyType.class), PropertyType.BODY)) {

                return switch (bodyContentType) {
                    case BINARY -> Body.of(
                        MapValueUtils.get(parameters, property.getName(), FileEntry.class), mimeType);
                    case FORM_DATA -> Body.of(
                        MapValueUtils.getMap(parameters, property.getName(), List.of(FileEntry.class), Map.of()),
                        bodyContentType);
                    case FORM_URL_ENCODED -> Body.of(
                        MapValueUtils.getMap(parameters, property.getName(), Map.of()), bodyContentType);
                    case JSON, XML -> {
                        if (property.getType() == Type.ARRAY) {
                            yield Body.of(
                                MapValueUtils.getList(parameters, property.getName(), Object.class, List.of()),
                                bodyContentType);
                        } else if (property.getType() == Type.OBJECT) {
                            yield Body.of(
                                MapValueUtils.getMap(parameters, property.getName(), Map.of()), bodyContentType);
                        } else {
                            yield Body.of(
                                MapValueUtils.getRequiredString(parameters, property.getName()), bodyContentType);
                        }
                    }
                    case RAW -> Body.of(MapValueUtils.getString(parameters, property.getName()), mimeType);
                };

                break;
            }
        }

        return null;
    }

    private ResponseFormat getResponseFormat(ActionDefinition actionDefinition) {
        ResponseFormat responseFormat = null;
        List<? extends ValueProperty<?>> outputProperties = OptionalUtils.orElse(
            actionDefinition.getOutputSchema(), Collections.emptyList());

        if (!outputProperties.isEmpty()) {
            ValueProperty<?> outputProperty = outputProperties.get(0);

            responseFormat = MapValueUtils.get(outputProperty.getMetadata(), "responseFormat", ResponseFormat.class);
        }

        return responseFormat;
    }

    private Map<String, List<String>> getValuesMap(
        Map<String, ?> parameters, List<? extends Property> properties, PropertyType propertyType) {

        Map<String, List<String>> valuesMap = new HashMap<>();

        for (Property property : properties) {
            if (Objects.equals(MapValueUtils.get(property.getMetadata(), TYPE, PropertyType.class), propertyType)) {
                List<String> values;

                if (property.getType() == Type.ARRAY) {
                    values = MapValueUtils.getList(parameters, property.getName(), String.class, List.of());
                } else {
                    String value = MapValueUtils.getString(parameters, property.getName());

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
