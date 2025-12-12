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
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ConnectionDefinition.BASE_URI;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.liferay.constant.LiferayConstants.APPLICATION;
import static com.bytechef.component.liferay.constant.LiferayConstants.BODY;
import static com.bytechef.component.liferay.constant.LiferayConstants.ENDPOINT;
import static com.bytechef.component.liferay.constant.LiferayConstants.PROPERTIES;

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
                            return List.of(string(BODY)
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
        PropertiesContainer propertiesContainer = LiferayPropertiesUtils.createPropertiesForParameters(
            inputParameters.getRequiredString(APPLICATION), inputParameters.getRequiredString(ENDPOINT),
            context);
        Map<String, ?> properties = inputParameters.getMap(PROPERTIES);

        Map<String, ?> pathParameters = getParameterValueMap(propertiesContainer.pathParameters(), properties);

        String endpoint = inputParameters.getRequiredString(ENDPOINT);

        String[] endpointParts = endpoint.split(" ");

        String baseUri = connectionParameters.getRequiredString(BASE_URI);

        String endpointUri = baseUri + "/o/" + inputParameters.getRequiredString(APPLICATION) + endpointParts[1];

        for (Map.Entry<String, ?> entry : pathParameters.entrySet()) {
            String key = entry.getKey();
            String value = String.valueOf(entry.getValue());

            endpointUri = endpointUri.replace("{" + key + "}", value);
        }

        String method = endpointParts[0];

        Executor executor = getExecutor(context, method, endpointUri);

        Response response = executor.headers(
                getParameterValueMap(propertiesContainer.headerParameters(), properties))
            .queryParameters(
                getParameterValueMap(propertiesContainer.queryParameters(), properties))
            .configuration(
                Http.timeout(Duration.ofMillis(inputParameters.getInteger("timeout", 10000))))
            .configuration(
                responseType(ResponseType.JSON))
            .body(
                getBody(propertiesContainer.bodyParameters(), properties, context))
            .execute();

        return response.getBody();
    }

    private static Map<String, List<String>> getParameterValueMap(
        List<String> parameterNames, Map<String, ?> properties) {

        return parameterNames
            .stream()
            .filter(
                properties::containsKey
            ).collect(
                Collectors.toMap(
            parameterName -> parameterName,
            parameterName -> List.of(String.valueOf(properties.get(parameterName))))
            );
    }

    private static Body getBody(List<String> parameterNames, Map<String, ?> properties, Context context) {
        if (properties.containsKey("body")) {
            return Body.of((List<?>) context.json(json -> json.read((String)properties.get("body"))));
        }

        return Body.of(parameterNames
            .stream()
            .filter(
                properties::containsKey)
            .collect(
                Collectors.toMap(
                parameterName -> parameterName,
                parameterName -> String.valueOf(properties.get(parameterName)))
            )
        );

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
