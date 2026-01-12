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

package com.bytechef.component.liferay.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ConnectionDefinition.BASE_URI;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.liferay.constant.LiferayConstants.APPLICATION;
import static com.bytechef.component.liferay.constant.LiferayConstants.BODY;
import static com.bytechef.component.liferay.constant.LiferayConstants.ENDPOINT;
import static com.bytechef.component.liferay.constant.LiferayConstants.HEADER;
import static com.bytechef.component.liferay.constant.LiferayConstants.HIDDEN_PROPERTIES;
import static com.bytechef.component.liferay.constant.LiferayConstants.PATH;
import static com.bytechef.component.liferay.constant.LiferayConstants.PROPERTIES;
import static com.bytechef.component.liferay.constant.LiferayConstants.QUERY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.liferay.util.LiferayOptionUtils;
import com.bytechef.component.liferay.util.LiferayPropertiesUtils;
import com.bytechef.component.liferay.util.PropertiesContainer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Igor Beslic
 */
public class LiferayHeadlessAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("headlessRequest")
        .title("Headless Request")
        .description("The Headless endpoint to use.")
        .properties(
            string(APPLICATION)
                .label("Application")
                .options((ActionDefinition.OptionsFunction<String>) LiferayOptionUtils::getApplicationsOptions)
                .required(true),
            string(ENDPOINT)
                .label("Endpoint")
                .options((ActionDefinition.OptionsFunction<String>) LiferayOptionUtils::getEndpointsOptions)
                .optionsLookupDependsOn(APPLICATION)
                .required(true),
            dynamicProperties(PROPERTIES)
                .properties(
                    (ActionDefinition.PropertiesFunction) (
                        inputParameters, connectionParameters, lookupDependsOnPaths, context) -> {

                        String endpoint = inputParameters.getRequiredString(ENDPOINT);

                        if (endpoint.contains("batch")) {
                            return List.of(array(BODY)
                                .label("Body")
                                .description("JSON structure of body")
                                .required(false));
                        }

                        PropertiesContainer propertiesContainer = LiferayPropertiesUtils.createPropertiesForParameters(
                            inputParameters.getRequiredString(APPLICATION), endpoint,
                            context);

                        return propertiesContainer.properties();
                    })
                .propertiesLookupDependsOn(APPLICATION, ENDPOINT)
                .required(false))
        .output()
        .perform(LiferayHeadlessAction::perform);

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Map<String, ?> properties = inputParameters.getMap(PROPERTIES);

        if (properties == null) {
            properties = Map.of();
        }

        Map<String, ?> hiddenProperties = (Map<String, ?>) properties.get(HIDDEN_PROPERTIES);

        if (hiddenProperties == null) {
            hiddenProperties = Map.of();
        }

        String endpoint = inputParameters.getRequiredString(ENDPOINT);

        String[] endpointParts = endpoint.split(" ");

        Executor executor = getExecutor(
            context, endpointParts[0],
            getEndpointUri(
                inputParameters, connectionParameters, endpointParts[1],
                getParameterValueMap((List<String>) hiddenProperties.get(PATH), properties)));

        Response response = executor.headers(
            getParameterValueMap((List<String>) hiddenProperties.get(HEADER), properties))
            .queryParameters(
                getParameterValueMap((List<String>) hiddenProperties.get(QUERY), properties))
            .configuration(
                Http.timeout(Duration.ofMillis(inputParameters.getInteger("timeout", 10000))))
            .configuration(
                responseType(ResponseType.JSON))
            .body(
                getBody((List<String>) hiddenProperties.get(BODY), properties))
            .execute();

        return response.getBody();
    }

    private static String getEndpointUri(
        Parameters inputParameters, Parameters connectionParameters, String applicationEndpoint,
        Map<String, ?> pathParameters) {

        String baseUri = connectionParameters.getRequiredString(BASE_URI);

        String endpointUri = baseUri + "/o/" + inputParameters.getRequiredString(APPLICATION) + applicationEndpoint;

        if (Objects.isNull(pathParameters) || pathParameters.isEmpty()) {
            return endpointUri;
        }

        for (Map.Entry<String, ?> entry : pathParameters.entrySet()) {
            String key = entry.getKey();
            Object rawValue = entry.getValue();

            String value;
            if (rawValue instanceof List<?> list && !list.isEmpty()) {
                value = String.valueOf(list.getFirst());
            } else {
                value = String.valueOf(rawValue);
            }

            endpointUri = endpointUri.replace("{" + key + "}", value);
        }

        return endpointUri;
    }

    private static Map<String, List<String>> getParameterValueMap(
        List<String> parameterNames, Map<String, ?> properties) {

        if (parameterNames == null) {
            return Map.of();
        }

        return parameterNames
            .stream()
            .filter(
                properties::containsKey)
            .collect(
                Collectors.toMap(
                    parameterName -> parameterName,
                    parameterName -> List.of(String.valueOf(properties.get(parameterName)))));
    }

    private static Body getBody(List<String> parameterNames, Map<String, ?> properties) {
        if (properties.containsKey(BODY)) {
            return Body.of((List<?>) properties.get(BODY));
        }

        return Body.of(parameterNames
            .stream()
            .filter(
                properties::containsKey)
            .collect(
                Collectors.toMap(
                    parameterName -> parameterName,
                    parameterName -> String.valueOf(properties.get(parameterName)))));

    }

    private static Executor getExecutor(Context context, String method, String finalEndpointUri) {
        return context.http(http -> switch (method) {
            case "GET" -> http.get(finalEndpointUri);
            case "POST" -> http.post(finalEndpointUri);
            case "PUT" -> http.put(finalEndpointUri);
            case "PATCH" -> http.patch(finalEndpointUri);
            case "DELETE" -> http.delete(finalEndpointUri);
            case "HEAD" -> http.head(finalEndpointUri);
            default -> throw new IllegalArgumentException("Unknown HTTP method: " + method);
        });
    }
}
