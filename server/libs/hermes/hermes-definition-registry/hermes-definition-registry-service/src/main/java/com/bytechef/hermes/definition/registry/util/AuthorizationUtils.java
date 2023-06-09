
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
import com.bytechef.hermes.component.definition.Authorization.ApiTokenLocation;
import com.bytechef.hermes.component.definition.Authorization.ApplyResponse;
import com.bytechef.hermes.component.definition.Authorization.TokenUrlFunction;
import com.bytechef.hermes.component.definition.ConnectionDefinition;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class AuthorizationUtils {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    public static Authorization.ApplyFunction getDefaultApply(Authorization.AuthorizationType type) {
        return switch (type) {
            case API_KEY -> (Map<String, ?> connectionParameters) -> {
                String addTo = MapValueUtils.getString(
                    connectionParameters, Authorization.ADD_TO, ApiTokenLocation.HEADER.name());

                if (ApiTokenLocation.valueOf(addTo.toUpperCase()) == ApiTokenLocation.HEADER) {
                    return ApplyResponse.ofHeaders(
                        Map.of(
                            MapValueUtils.getString(
                                connectionParameters, Authorization.KEY, Authorization.API_TOKEN),
                            List.of(
                                MapValueUtils.getString(connectionParameters, Authorization.VALUE, ""))));
                } else {
                    return ApplyResponse.ofQueryParameters(
                        Map.of(
                            MapValueUtils.getString(
                                connectionParameters, Authorization.KEY, Authorization.API_TOKEN),
                            List.of(
                                MapValueUtils.getString(connectionParameters, Authorization.VALUE, ""))));
                }
            };
            case BASIC_AUTH, DIGEST_AUTH -> (Map<String, ?> connectionParameters) -> {
                String valueToEncode =
                    MapValueUtils.getString(connectionParameters, Authorization.USERNAME) +
                        ":" +
                        MapValueUtils.getString(connectionParameters, Authorization.PASSWORD);

                return ApplyResponse.ofHeaders(
                    Map.of(
                        "Authorization",
                        List.of("Basic " + ENCODER.encodeToString(valueToEncode.getBytes(StandardCharsets.UTF_8)))));
            };
            case BEARER_TOKEN -> (Map<String, ?> connectionParameters) -> ApplyResponse.ofHeaders(
                Map.of(
                    Authorization.AUTHORIZATION,
                    List.of(
                        Authorization.BEARER + " " +
                            MapValueUtils.getString(connectionParameters, Authorization.TOKEN))));
            case CUSTOM -> (Map<String, ?> connectionParameters) -> null;
            case OAUTH2_AUTHORIZATION_CODE, OAUTH2_AUTHORIZATION_CODE_PKCE, OAUTH2_CLIENT_CREDENTIALS,
                OAUTH2_IMPLICIT_CODE, OAUTH2_RESOURCE_OWNER_PASSWORD -> (
                    Map<String, ?> connectionParameters) -> ApplyResponse.ofHeaders(
                        Map.of(
                            Authorization.AUTHORIZATION,
                            List.of(
                                MapValueUtils.getString(
                                    connectionParameters, Authorization.HEADER_PREFIX, Authorization.BEARER) +
                                    " " +
                                    MapValueUtils.getString(
                                        connectionParameters, Authorization.ACCESS_TOKEN))));
        };
    }

    public static String getDefaultAuthorizationUrl(Map<String, ?> connectionParameters) {
        return MapValueUtils.getString(connectionParameters, Authorization.AUTHORIZATION_URL);
    }

    public static String getDefaultBaseUri(Map<String, ?> connectionParameters) {
        return MapValueUtils.getString(connectionParameters, ConnectionDefinition.BASE_URI);
    }

    public static String getDefaultClientId(Map<String, ?> connectionParameters) {
        return MapValueUtils.getString(connectionParameters, Authorization.CLIENT_ID);
    }

    public static String getDefaultClientSecret(Map<String, ?> connectionParameters) {
        return MapValueUtils.getString(connectionParameters, Authorization.CLIENT_SECRET);
    }

    public static Authorization.PkceFunction getDefaultPkce() {
        return Authorization.Pkce::new;
    }

    public static String
        getDefaultRefreshUrl(Map<String, Object> connectionParameters, TokenUrlFunction tokeUrlFunction) {
        String refreshUrl = MapValueUtils.getString(connectionParameters, Authorization.REFRESH_URL);

        if (refreshUrl == null) {
            refreshUrl = tokeUrlFunction.apply(connectionParameters);
        }

        return refreshUrl;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getDefaultScopes(Map<String, ?> connectionParameters) {
        Object scopes = MapValueUtils.get(connectionParameters, Authorization.SCOPES);

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

    public static String getDefaultTokenUrl(Map<String, ?> connectionParameters) {
        return MapValueUtils.getString(connectionParameters, Authorization.TOKEN_URL);
    }
}
