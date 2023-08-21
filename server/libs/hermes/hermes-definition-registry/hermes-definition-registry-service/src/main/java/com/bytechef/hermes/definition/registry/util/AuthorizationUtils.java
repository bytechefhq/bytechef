
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

import com.bytechef.commons.util.MapUtils;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.ApiTokenLocation;
import com.bytechef.hermes.component.definition.Authorization.ApplyResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackFunction;
import com.bytechef.hermes.component.definition.Authorization.ClientIdFunction;
import com.bytechef.hermes.component.definition.Authorization.ClientSecretFunction;
import com.bytechef.hermes.component.definition.Authorization.TokenUrlFunction;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.util.HttpClientUtils;
import com.bytechef.hermes.component.util.HttpClientUtils.Response;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.bytechef.hermes.component.util.HttpClientUtils.responseType;

/**
 * @author Ivica Cardic
 */
public class AuthorizationUtils {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    public static Authorization.ApplyFunction getDefaultApply(Authorization.AuthorizationType type) {
        return switch (type) {
            case API_KEY -> (Map<String, ?> connectionParameters) -> {
                String addTo = MapUtils.getString(
                    connectionParameters, Authorization.ADD_TO, ApiTokenLocation.HEADER.name());

                if (ApiTokenLocation.valueOf(addTo.toUpperCase()) == ApiTokenLocation.HEADER) {
                    return ApplyResponse.ofHeaders(
                        Map.of(
                            MapUtils.getString(
                                connectionParameters, Authorization.KEY, Authorization.API_TOKEN),
                            List.of(
                                MapUtils.getString(connectionParameters, Authorization.VALUE, ""))));
                } else {
                    return ApplyResponse.ofQueryParameters(
                        Map.of(
                            MapUtils.getString(
                                connectionParameters, Authorization.KEY, Authorization.API_TOKEN),
                            List.of(
                                MapUtils.getString(connectionParameters, Authorization.VALUE, ""))));
                }
            };
            case BASIC_AUTH, DIGEST_AUTH -> (Map<String, ?> connectionParameters) -> {
                String valueToEncode =
                    MapUtils.getString(connectionParameters, Authorization.USERNAME) +
                        ":" +
                        MapUtils.getString(connectionParameters, Authorization.PASSWORD);

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
                            MapUtils.getString(connectionParameters, Authorization.TOKEN))));
            case CUSTOM -> (Map<String, ?> connectionParameters) -> null;
            case OAUTH2_AUTHORIZATION_CODE, OAUTH2_AUTHORIZATION_CODE_PKCE, OAUTH2_CLIENT_CREDENTIALS,
                OAUTH2_IMPLICIT_CODE, OAUTH2_RESOURCE_OWNER_PASSWORD -> (
                    Map<String, ?> connectionParameters) -> ApplyResponse.ofHeaders(
                        Map.of(
                            Authorization.AUTHORIZATION,
                            List.of(
                                MapUtils.getString(
                                    connectionParameters, Authorization.HEADER_PREFIX, Authorization.BEARER) +
                                    " " +
                                    MapUtils.getString(
                                        connectionParameters, Authorization.ACCESS_TOKEN))));
        };
    }

    public static AuthorizationCallbackFunction getDefaultAuthorizationCallbackFunction(
        ClientIdFunction clientIdFunction, ClientSecretFunction clientSecretFunction,
        TokenUrlFunction tokenUrlFunction) {

        return (connectionParameters, code, redirectUri, codeVerifier) -> {
            Map<String, Object> payload = new HashMap<>() {
                {
                    put("client_id", clientIdFunction.apply(connectionParameters));
                    put("client_secret", clientSecretFunction.apply(connectionParameters));
                    put("code", code);
                    put("grant_type", "authorization_code");
                    put("redirect_uri", redirectUri);
                }
            };

            if (codeVerifier != null) {
                payload.put("code_verifier", codeVerifier);
            }

            Response response = HttpClientUtils.post(tokenUrlFunction.apply(connectionParameters))
                .body(HttpClientUtils.Body.of(payload, HttpClientUtils.BodyContentType.FORM_URL_ENCODED))
                .configuration(responseType(HttpClientUtils.ResponseType.JSON))
                .execute();

            if (response.statusCode() != 200) {
                throw new ComponentExecutionException("Invalid claim");
            }

            if (response.body() == null) {
                throw new ComponentExecutionException("Invalid claim");
            }

            Map<?, ?> body = (Map<?, ?>) response.body();

            return new Authorization.AuthorizationCallbackResponse(
                (String) body.get(Authorization.ACCESS_TOKEN), (String) body.get(Authorization.REFRESH_TOKEN));
        };
    }

    public static String getDefaultAuthorizationUrl(Map<String, ?> connectionParameters) {
        return MapUtils.getString(connectionParameters, Authorization.AUTHORIZATION_URL);
    }

    public static String getDefaultBaseUri(Map<String, ?> connectionParameters) {
        return MapUtils.getString(connectionParameters, ConnectionDefinition.BASE_URI);
    }

    public static String getDefaultClientId(Map<String, ?> connectionParameters) {
        return MapUtils.getString(connectionParameters, Authorization.CLIENT_ID);
    }

    public static String getDefaultClientSecret(Map<String, ?> connectionParameters) {
        return MapUtils.getString(connectionParameters, Authorization.CLIENT_SECRET);
    }

    public static Authorization.PkceFunction getDefaultPkce() {
        return Authorization.Pkce::new;
    }

    public static String getDefaultRefreshUrl(
        Map<String, Object> connectionParameters, TokenUrlFunction tokenUrlFunction) {

        String refreshUrl = MapUtils.getString(connectionParameters, Authorization.REFRESH_URL);

        if (refreshUrl == null) {
            refreshUrl = tokenUrlFunction.apply(connectionParameters);
        }

        return refreshUrl;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getDefaultScopes(Map<String, ?> connectionParameters) {
        Object scopes = MapUtils.get(connectionParameters, Authorization.SCOPES);

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
        return MapUtils.getString(connectionParameters, Authorization.TOKEN_URL);
    }
}
