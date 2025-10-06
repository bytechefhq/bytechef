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
import static com.bytechef.component.liferay.constant.LiferayConstants.BODY_PARAMETERS;
import static com.bytechef.component.liferay.constant.LiferayConstants.ENDPOINT;
import static com.bytechef.component.liferay.constant.LiferayConstants.HEADER_PARAMETERS;
import static com.bytechef.component.liferay.constant.LiferayConstants.PATH_PARAMETERS;
import static com.bytechef.component.liferay.constant.LiferayConstants.PROPERTIES;
import static com.bytechef.component.liferay.constant.LiferayConstants.QUERY_PARAMETERS;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.liferay.util.LiferayOptionUtils;
import com.bytechef.component.liferay.util.LiferayPropertiesUtils;
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
                .options((ActionOptionsFunction<String>) LiferayOptionUtils::getApplicationsOptions)
                .required(true),
            string(ENDPOINT)
                .label("Endpoint")
                .options((ActionOptionsFunction<String>) LiferayOptionUtils::getEndpointsOptions)
                .optionsLookupDependsOn(APPLICATION)
                .required(true),
            dynamicProperties(PROPERTIES)
                .properties(LiferayPropertiesUtils::createPropertiesForParameters)
                .propertiesLookupDependsOn(APPLICATION, ENDPOINT)
                .required(false))
        .output()
        .perform(LiferayHeadlessAction::perform);

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String baseUri = connectionParameters.getRequiredString(BASE_URI);

        String[] endpointParts = inputParameters.getRequiredString(ENDPOINT)
            .split(" ");
        String method = endpointParts[0];
        String endpoint = endpointParts[1];

        String endpointUri = baseUri + "/o/" + inputParameters.getRequiredString(APPLICATION) + endpoint;

        Map<String, ?> properties = inputParameters.getMap(PROPERTIES);

        Map<String, ?> body = Map.of();
        Map<String, List<String>> headers = Map.of();
        Map<String, List<String>> queryParams = Map.of();
        Map<String, ?> pathParams = Map.of();

        if (properties != null) {
            body = BODY_PARAMETERS.stream()
                .filter(properties::containsKey)
                .collect(Collectors.toMap(p -> p, p -> properties.get(p)
                    .toString()));

            BODY_PARAMETERS.clear();

            headers = HEADER_PARAMETERS.stream()
                .filter(properties::containsKey)
                .collect(Collectors.toMap(p -> p, p -> List.of(properties.get(p)
                    .toString())));

            HEADER_PARAMETERS.clear();

            queryParams = QUERY_PARAMETERS.stream()
                .filter(properties::containsKey)
                .collect(Collectors.toMap(p -> p, p -> List.of(properties.get(p)
                    .toString())));

            QUERY_PARAMETERS.clear();

            pathParams = PATH_PARAMETERS.stream()
                .filter(properties::containsKey)
                .collect(Collectors.toMap(p -> p, p -> properties.get(p)
                    .toString()));

            PATH_PARAMETERS.clear();
        }

        for (Map.Entry<String, ?> entry : pathParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue()
                .toString();
            endpointUri = endpointUri.replace("{" + key + "}", value);
        }

        final String finalEndpointUri = endpointUri;

        Executor request = context.http(http -> {
            Executor req;

            switch (method) {
                case "GET" -> req = http.get(finalEndpointUri);
                case "POST" -> req = http.post(finalEndpointUri);
                case "PUT" -> req = http.put(finalEndpointUri);
                case "PATCH" -> req = http.patch(finalEndpointUri);
                case "DELETE" -> req = http.delete(finalEndpointUri);
                case "HEAD" -> req = http.head(finalEndpointUri);
                default ->
                    throw new IllegalArgumentException("Unknown HTTP method: " + method);
            }

            return req;
        });

        Response response = request
            .headers(headers)
            .queryParameters(queryParams)
            .configuration(Http.timeout(Duration.ofMillis(inputParameters.getInteger("timeout", 10000))))
            .configuration(responseType(ResponseType.JSON))
            .body(Body.of(body))
            .execute();

        int statusCode = response.getStatusCode();

        if ((statusCode >= 200) && (statusCode < 300)) {
            return response.getBody();
        }

        context.log(log -> log.warn("Received response code {}, from endpoint {}", statusCode, finalEndpointUri));

        return endpoint;
    }
}
