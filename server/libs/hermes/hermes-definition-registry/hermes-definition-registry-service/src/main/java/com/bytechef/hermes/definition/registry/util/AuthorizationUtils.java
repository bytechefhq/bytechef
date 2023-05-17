
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

package com.bytechef.hermes.definition.registry.util;

import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationContext;
import com.bytechef.hermes.component.definition.Authorization.TokenUrlFunction;
import com.bytechef.hermes.component.definition.ConnectionDefinition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class AuthorizationUtils {

    public static Authorization.ApplyConsumer getDefaultApply(Authorization.AuthorizationType type) {
        return switch (type) {
            case API_KEY -> ((
                InputParameters connectionInputParameters, AuthorizationContext authorizationContext) -> {

                String addTo = connectionInputParameters.getString(
                    Authorization.ADD_TO, Authorization.ApiTokenLocation.HEADER.name());

                if (Authorization.ApiTokenLocation
                    .valueOf(addTo.toUpperCase()) == Authorization.ApiTokenLocation.HEADER) {

                    authorizationContext.setHeaders(
                        Map.of(
                            connectionInputParameters.getString(Authorization.KEY, Authorization.API_TOKEN),
                            List.of(connectionInputParameters.getString(Authorization.VALUE, ""))));
                } else {
                    authorizationContext.setQueryParameters(
                        Map.of(
                            connectionInputParameters.getString(Authorization.KEY, Authorization.API_TOKEN),
                            List.of(connectionInputParameters.getString(Authorization.VALUE, ""))));
                }
            });
            case BASIC_AUTH, DIGEST_AUTH -> ((
                InputParameters connectionInputParameters,
                AuthorizationContext authorizationContext) -> authorizationContext
                    .setUsernamePassword(
                        connectionInputParameters.getString(Authorization.USERNAME),
                        connectionInputParameters.getString(Authorization.PASSWORD)));
            case BEARER_TOKEN -> ((
                InputParameters connectionInputParameters,
                AuthorizationContext authorizationContext) -> authorizationContext
                    .setHeaders(
                        Map.of(
                            Authorization.AUTHORIZATION,
                            List.of(Authorization.BEARER + " " + connectionInputParameters.getString(
                                Authorization.TOKEN)))));
            case CUSTOM -> ((
                InputParameters connectionInputParameters, AuthorizationContext authorizationContext) -> {});
            case OAUTH2_AUTHORIZATION_CODE, OAUTH2_AUTHORIZATION_CODE_PKCE, OAUTH2_CLIENT_CREDENTIALS,
                OAUTH2_IMPLICIT_CODE, OAUTH2_RESOURCE_OWNER_PASSWORD -> ((
                    InputParameters connectionInputParameters,
                    AuthorizationContext authorizationContext) -> authorizationContext
                        .setHeaders(
                            Map.of(
                                Authorization.AUTHORIZATION,
                                List.of(
                                    connectionInputParameters.getString(
                                        Authorization.HEADER_PREFIX, Authorization.BEARER) + " " +
                                        connectionInputParameters.getString(Authorization.ACCESS_TOKEN)))));
        };
    }

    public static String getDefaultAuthorizationUrl(InputParameters connectionInputParameters) {
        return connectionInputParameters.getString(Authorization.AUTHORIZATION_URL);
    }

    public static String getDefaultBaseUri(InputParameters connectionInputParameters) {
        return connectionInputParameters.containsKey(ConnectionDefinition.BASE_URI)
            ? connectionInputParameters.getString(ConnectionDefinition.BASE_URI)
            : null;
    }

    public static String getDefaultClientId(InputParameters connectionInputParameters) {
        return connectionInputParameters.getString(Authorization.CLIENT_ID);
    }

    public static String getDefaultClientSecret(InputParameters connectionInputParameters) {
        return connectionInputParameters.getString(Authorization.CLIENT_SECRET);
    }

    public static Authorization.PkceFunction getDefaultPkce() {
        return Authorization.Pkce::new;
    }

    public static String
        getDefaultRefreshUrl(InputParameters connectionInputParameters, TokenUrlFunction tokeUrlFunction) {
        String refreshUrl = connectionInputParameters.getString(Authorization.REFRESH_URL);

        if (refreshUrl == null) {
            refreshUrl = tokeUrlFunction.apply(connectionInputParameters);
        }

        return refreshUrl;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getDefaultScopes(InputParameters connectionInputParameters) {
        Object scopes = connectionInputParameters.getString(Authorization.SCOPES);

        if (scopes == null) {
            return Collections.emptyList();
        } else if (scopes instanceof List<?>) {
            return (List<String>) scopes;
        } else {
            return Arrays.stream(((String) scopes).split(","))
                .filter(Objects::nonNull)
                .filter(scope -> !scope.isBlank())
                .map(String::trim)
                .toList();
        }
    }

    public static String getDefaultTokenUrl(InputParameters connectionInputParameters) {
        return connectionInputParameters.getString(Authorization.TOKEN_URL);
    }
}
