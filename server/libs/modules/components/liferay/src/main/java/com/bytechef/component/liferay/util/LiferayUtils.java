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

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.liferay.constant.LiferayConstants.CONTEXT_NAME;
import static com.bytechef.component.liferay.constant.LiferayConstants.DISCOVER;
import static com.bytechef.component.liferay.constant.LiferayConstants.ENDPOINT;
import static com.bytechef.component.liferay.constant.LiferayConstants.METHOD;
import static com.bytechef.component.liferay.constant.LiferayConstants.NAME;
import static com.bytechef.component.liferay.constant.LiferayConstants.PARAMETERS;
import static com.bytechef.component.liferay.constant.LiferayConstants.SERVICE;
import static com.bytechef.component.liferay.constant.LiferayConstants.SERVICES;
import static com.bytechef.component.liferay.constant.LiferayConstants.TYPE;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.liferay.constant.LiferayContextName;
import com.bytechef.component.liferay.constant.LiferayFieldType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Nikolina Spehar
 */
public class LiferayUtils {

    public static List<Option<String>> getContextNameOptions() {
        List<Option<String>> contextNameOptions = new ArrayList<>();

        for (LiferayContextName contextName : LiferayContextName.values()) {
            contextNameOptions.add(option(contextName.name(), contextName.getContextName()));
        }

        return contextNameOptions;
    }

    public static List<ValueProperty<?>> createParameters(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        Context context) {

        List<Map<String, String>> parameters = getParameters(
            context, inputParameters.getRequiredString(CONTEXT_NAME), inputParameters.getRequiredLong(SERVICE));

        return List.copyOf(
            parameters.stream()
                .map(LiferayUtils::createProperty)
                .filter(Objects::nonNull)
                .toList());
    }

    private static List<Map<String, String>> getParameters(Context context, String contextName, long serviceId) {
        List<Map<String, String>> parameters = new ArrayList<>();
        List<Object> services = getServices(context, contextName);

        if (services.get((int) serviceId - 1) instanceof Map<?, ?> serviceMap &&
            serviceMap.get(PARAMETERS) instanceof List<?> curParameters) {

            for (Object parameter : curParameters) {
                if (parameter instanceof Map<?, ?> parameterMap) {
                    parameters.add(Map.of(
                        NAME, (String) parameterMap.get(NAME), TYPE, (String) parameterMap.get(TYPE)));
                }
            }
        }

        return parameters;
    }

    public static Map<String, String> getServiceHttpData(Context context, String contextName, long serviceId) {
        String endpoint = "";
        String method = "";
        List<Object> services = getServices(context, contextName);

        if (services.get((int) serviceId - 1) instanceof Map<?, ?> serviceMap) {
            endpoint = (String) serviceMap.get("path");
            method = (String) serviceMap.get("method");
        }

        return Map.of(METHOD, method, ENDPOINT, endpoint);
    }

    public static List<Option<Long>> getServiceOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<Long>> serviceOptions = new ArrayList<>();
        List<Object> services = getServices(context, inputParameters.getRequiredString(CONTEXT_NAME));

        for (int index = 0; index < services.size(); index++) {
            if (services.get(index) instanceof Map<?, ?> servicesMap) {
                serviceOptions.add(option((String) servicesMap.get("name"), index + 1));
            }
        }

        return serviceOptions;
    }

    public static List<Object> getServices(Context context, String contextName) {
        contextName = contextName.equals("portal") ? "" : contextName;

        Map<String, Object> response = context.http(http -> http.get("/api/jsonws"))
            .queryParameters(CONTEXT_NAME, contextName, DISCOVER, "")
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Object> services = new ArrayList<>();

        if (response.get(SERVICES) instanceof List<?> curServices) {
            services.addAll(curServices);
        }

        return services;
    }

    private static ModifiableValueProperty<?, ?> createProperty(Map<String, String> map) {
        String name = map.get(NAME);
        String type = map.get(TYPE);

        LiferayFieldType liferayFieldType = LiferayFieldType.getLiferayFieldType(type);

        return switch (liferayFieldType) {
            case BOOLEAN -> bool(name)
                .label(name)
                .required(false);
            case LONG -> number(name)
                .label(name)
                .required(false);
            case STRING -> string(name)
                .label(name)
                .required(false);
            case INT -> integer(name)
                .label(name)
                .required(false);
            case MAP -> object(name)
                .label(name)
                .required(false);
            case IGNORE -> null;
        };
    }
}
