
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

package com.bytechef.hermes.component.registrar.oas;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.util.HttpClientUtils;
import com.bytechef.hermes.component.util.HttpClientUtils.BodyContentType;
import com.bytechef.hermes.component.util.HttpClientUtils.Payload;
import com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;
import com.bytechef.hermes.definition.Property;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.bytechef.hermes.component.util.HttpClientUtils.exchange;
import static com.bytechef.hermes.component.util.HttpClientUtils.RequestMethod;

/**
 * @author Ivica Cardic
 */
public class OpenApiClient {

    private static final String TYPE = "type";

    public HttpClientUtils.Response execute(
        ActionDefinition actionDefinition, Context context, TaskExecution taskExecution) {
        Map<String, Object> metadata = actionDefinition.getMetadata();

        return exchange(
            createUri(metadata, taskExecution.getParameters(), actionDefinition.getProperties()),
            MapValueUtils.get(metadata, "requestMethod", RequestMethod.class))
            .configuration(
                HttpClientUtils.responseFormat(getResponseFormat(actionDefinition)))
            .headers(
                getValuesMap(taskExecution.getParameters(), actionDefinition.getProperties(), PropertyType.HEADER))
            .payload(
                getPayload(
                    MapValueUtils.get(metadata, "bodyContentType", BodyContentType.class),
                    MapValueUtils.getString(metadata, "mimeType"),
                    taskExecution.getParameters(), actionDefinition.getProperties()))
            .queryParameters(
                getValuesMap(taskExecution.getParameters(), actionDefinition.getProperties(), PropertyType.QUERY))
            .execute();
    }

    private String createUri(
        Map<String, Object> metadata, Map<String, Object> parameters, List<Property<?>> properties) {

        String path = (String) metadata.get("path");

        for (Property<?> property : properties) {
            if (MapValueUtils.get(property.getMetadata(), TYPE, PropertyType.class) == PropertyType.PATH) {
                path = path.replace(
                    "{" + property.getName() + "}", MapValueUtils.getRequiredString(parameters, property.getName()));
            }
        }

        return path;
    }

    private Payload getPayload(
        BodyContentType bodyContentType, String mimeType, Map<String, Object> parameters,
        List<Property<?>> properties) {
        Payload payload = null;

        if (bodyContentType != null) {
            for (Property<?> property : properties) {
                if (Objects.equals(MapValueUtils.get(property.getMetadata(), TYPE, PropertyType.class),
                    PropertyType.BODY)) {
                    payload = switch (bodyContentType) {
                        case BINARY -> Payload.of(
                            MapValueUtils.getRequired(parameters, property.getName(), Context.FileEntry.class),
                            mimeType);
                        case FORM_DATA, FORM_URL_ENCODED -> Payload.of(
                            MapValueUtils.getRequiredMap(parameters, property.getName()));
                        case JSON, XML -> {
                            if (property.getType() == Property.Type.ARRAY) {
                                yield Payload.of(
                                    MapValueUtils.getRequiredList(parameters, property.getName(), Object.class));
                            } else if (property.getType() == Property.Type.OBJECT) {
                                yield Payload.of(
                                    MapValueUtils.getRequiredMap(parameters, property.getName()));
                            } else {
                                yield Payload.of(
                                    MapValueUtils.getRequiredString(parameters, property.getName()));
                            }
                        }
                        case RAW -> Payload.of(MapValueUtils.getRequiredString(parameters, property.getName()),
                            mimeType);
                    };

                    break;
                }
            }
        }

        return payload;
    }

    private ResponseFormat getResponseFormat(ActionDefinition actionDefinition) {
        ResponseFormat responseFormat = null;
        List<Property<?>> outputProperties = actionDefinition.getOutputSchema();

        if (outputProperties != null && !outputProperties.isEmpty()) {
            Property<?> property = outputProperties.get(0);

            responseFormat = MapValueUtils.get(property.getMetadata(), "responseFormat", ResponseFormat.class);
        }

        return responseFormat;
    }

    private Map<String, List<String>> getValuesMap(
        Map<String, Object> parameters, List<Property<?>> properties, PropertyType propertyType) {

        Map<String, List<String>> valuesMap = new HashMap<>();

        for (Property<?> property : properties) {
            if (Objects.equals(MapValueUtils.get(property.getMetadata(), TYPE, PropertyType.class), propertyType)) {
                String value = MapValueUtils.getString(parameters, property.getName());

                valuesMap.compute(property.getName(), (key, values) -> {
                    if (StringUtils.hasText(value)) {
                        if (values == null) {
                            values = new ArrayList<>();
                        }

                        if (!values.contains(value)) {
                            values.add(value);
                        }
                    }

                    return values;
                });
            }
        }

        return valuesMap;
    }
}
