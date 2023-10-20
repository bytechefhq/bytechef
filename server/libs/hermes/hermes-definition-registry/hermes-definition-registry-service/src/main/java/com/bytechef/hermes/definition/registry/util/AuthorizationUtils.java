
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

import com.bytechef.commons.util.MapValueUtils;
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
            case API_KEY -> (
                Map<String, Object> connectionInputParameters, AuthorizationContext authorizationContext) -> {

                String addTo = MapValueUtils.getString(
                    connectionInputParameters, Authorization.ADD_TO, Authorization.ApiTokenLocation.HEADER.name());

                if (Authorization.ApiTokenLocation
                    .valueOf(addTo.toUpperCase()) == Authorization.ApiTokenLocation.HEADER) {

                    authorizationContext.setHeaders(
                        Map.of(
                            MapValueUtils.getString(
                                connectionInputParameters, Authorization.KEY, Authorization.API_TOKEN),
                            List.of(
                                MapValueUtils.getString(connectionInputParameters, Authorization.VALUE, ""))));
                } else {
                    authorizationContext.setQueryParameters(
                        Map.of(
                            MapValueUtils.getString(
                                connectionInputParameters, Authorization.KEY, Authorization.API_TOKEN),
                            List.of(
                                MapValueUtils.getString(connectionInputParameters, Authorization.VALUE, ""))));
                }
            };
            case BASIC_AUTH, DIGEST_AUTH -> (
                Map<String, Object> connectionInputParameters,
                AuthorizationContext authorizationContext) -> authorizationContext
                    .setUsernamePassword(
                        MapValueUtils.getString(connectionInputParameters, Authorization.USERNAME),
                        MapValueUtils.getString(connectionInputParameters, Authorization.PASSWORD));
            case BEARER_TOKEN -> (
                Map<String, Object> connectionInputParameters,
                AuthorizationContext authorizationContext) -> authorizationContext
                    .setHeaders(
                        Map.of(
                            Authorization.AUTHORIZATION,
                            List.of(
                                Authorization.BEARER + " " +
                                    MapValueUtils.getString(connectionInputParameters, Authorization.TOKEN))));
            case CUSTOM -> (
                Map<String, Object> connectionInputParameters, AuthorizationContext authorizationContext) -> {};
            case OAUTH2_AUTHORIZATION_CODE, OAUTH2_AUTHORIZATION_CODE_PKCE, OAUTH2_CLIENT_CREDENTIALS,
                OAUTH2_IMPLICIT_CODE, OAUTH2_RESOURCE_OWNER_PASSWORD -> (
                    Map<String, Object> connectionInputParameters,
                    AuthorizationContext authorizationContext) -> authorizationContext
                        .setHeaders(
                            Map.of(
                                Authorization.AUTHORIZATION,
                                List.of(
                                    MapValueUtils.getString(
                                        connectionInputParameters, Authorization.HEADER_PREFIX, Authorization.BEARER) +
                                        " " +
                                        MapValueUtils.getString(
                                            connectionInputParameters, Authorization.ACCESS_TOKEN))));
        };
    }

    public static String getDefaultAuthorizationUrl(Map<String, Object> connectionInputParameters) {
        return MapValueUtils.getString(connectionInputParameters, Authorization.AUTHORIZATION_URL);
    }

    public static String getDefaultBaseUri(Map<String, Object> connectionInputParameters) {
        return MapValueUtils.getString(connectionInputParameters, ConnectionDefinition.BASE_URI);
    }

    public static String getDefaultClientId(Map<String, Object> connectionInputParameters) {
        return MapValueUtils.getString(connectionInputParameters, Authorization.CLIENT_ID);
    }

    public static String getDefaultClientSecret(Map<String, Object> connectionInputParameters) {
        return MapValueUtils.getString(connectionInputParameters, Authorization.CLIENT_SECRET);
    }

    public static Authorization.PkceFunction getDefaultPkce() {
        return Authorization.Pkce::new;
    }

    public static String
        getDefaultRefreshUrl(Map<String, Object> connectionInputParameters, TokenUrlFunction tokeUrlFunction) {
        String refreshUrl = MapValueUtils.getString(connectionInputParameters, Authorization.REFRESH_URL);

        if (refreshUrl == null) {
            refreshUrl = tokeUrlFunction.apply(connectionInputParameters);
        }

        return refreshUrl;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getDefaultScopes(Map<String, Object> connectionInputParameters) {
        Object scopes = MapValueUtils.get(connectionInputParameters, Authorization.SCOPES);

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

    public static String getDefaultTokenUrl(Map<String, Object> connectionInputParameters) {
        return MapValueUtils.getString(connectionInputParameters, Authorization.TOKEN_URL);
    }
}
