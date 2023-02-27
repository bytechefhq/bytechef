
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

package com.bytechef.hermes.component.definition;

import static com.bytechef.hermes.component.constant.ComponentConstants.ADD_TO;
import static com.bytechef.hermes.component.constant.ComponentConstants.HEADER_PREFIX;
import static com.bytechef.hermes.component.constant.ComponentConstants.KEY;
import static com.bytechef.hermes.component.constant.ComponentConstants.PASSWORD;
import static com.bytechef.hermes.component.constant.ComponentConstants.TOKEN;
import static com.bytechef.hermes.component.constant.ComponentConstants.USERNAME;
import static com.bytechef.hermes.component.constant.ComponentConstants.VALUE;

import com.bytechef.hermes.component.AuthorizationContext;
import com.bytechef.hermes.component.Connection;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Ivica Cardic
 */
@JsonDeserialize(as = ComponentDSL.ModifiableAuthorization.class)
public sealed interface Authorization permits ComponentDSL.ModifiableAuthorization {

    String API_TOKEN = "api_token";
    String ACCESS_TOKEN = "access_token";
    String REFRESH_TOKEN = "refresh_token";

    enum AuthorizationType {
        API_KEY((AuthorizationContext authorizationContext, Connection connection) -> {
            String addTo = connection.getParameter(ADD_TO, ApiTokenLocation.HEADER.name());

            if (ApiTokenLocation.valueOf(addTo.toUpperCase()) == ApiTokenLocation.HEADER) {
                authorizationContext.setHeaders(
                    Map.of(connection.getParameter(KEY, API_TOKEN), List.of(connection.getParameter(VALUE, ""))));
            } else {
                authorizationContext.setQueryParameters(
                    Map.of(connection.getParameter(KEY, API_TOKEN), List.of(connection.getParameter(VALUE, ""))));
            }
        }),
        BASIC_AUTH((AuthorizationContext authorizationContext, Connection connection) -> authorizationContext
            .setUsernamePassword(connection.getParameter(USERNAME), connection.getParameter(PASSWORD))),
        BEARER_TOKEN(
            (AuthorizationContext authorizationContext, Connection connection) -> authorizationContext.setHeaders(
                Map.of(Constants.AUTHORIZATION, List.of(Constants.BEARER + " " + connection.getParameter(TOKEN))))),
        CUSTOM(null),
        DIGEST_AUTH((AuthorizationContext authorizationContext, Connection connection) -> authorizationContext
            .setUsernamePassword(
                connection.getParameter(USERNAME), connection.getParameter(PASSWORD))),
        OAUTH2_AUTHORIZATION_CODE(
            (AuthorizationContext authorizationContext, Connection connection) -> authorizationContext
                .setHeaders(getOAuth2Headers(connection))),
        OAUTH2_AUTHORIZATION_CODE_PKCE(
            (AuthorizationContext authorizationContext, Connection connection) -> authorizationContext
                .setHeaders(getOAuth2Headers(connection))),
        OAUTH2_CLIENT_CREDENTIALS(
            (AuthorizationContext authorizationContext, Connection connection) -> authorizationContext
                .setHeaders(getOAuth2Headers(connection))),
        OAUTH2_IMPLICIT_CODE(
            (AuthorizationContext authorizationContext, Connection connection) -> authorizationContext
                .setHeaders(getOAuth2Headers(connection))),
        OAUTH2_RESOURCE_OWNER_PASSWORD(
            (AuthorizationContext authorizationContext, Connection connection) -> authorizationContext
                .setHeaders(getOAuth2Headers(connection)));

        private final BiConsumer<AuthorizationContext, Connection> defaultApplyConsumer;

        AuthorizationType(BiConsumer<AuthorizationContext, Connection> defaultApplyConsumer) {
            this.defaultApplyConsumer = defaultApplyConsumer;
        }

        public BiConsumer<AuthorizationContext, Connection> getDefaultApplyConsumer() {
            return defaultApplyConsumer;
        }

        private static class Constants {
            private static final String AUTHORIZATION = "Authorization";
            private static final String BEARER = "Bearer";
        }

        private static Map<String, List<String>> getOAuth2Headers(Connection connection) {
            return Map.of(
                Constants.AUTHORIZATION,
                List.of(
                    connection.getParameter(HEADER_PREFIX, Constants.BEARER) + " " +
                        connection.getParameter(ACCESS_TOKEN)));
        }
    }

    enum ApiTokenLocation {
        HEADER,
        QUERY_PARAMETERS,
    }

    Optional<Function<Connection, String>> getAcquireFunction();

    BiConsumer<AuthorizationContext, Connection> getApplyConsumer();

    BiFunction<Connection, String, AuthorizationCallbackResponse> getAuthorizationCallbackFunction();

    Function<Connection, String> getAuthorizationUrlFunction();

    Function<Connection, String> getCallbackUrlFunction();

    Function<Connection, String> getClientIdFunction();

    Function<Connection, String> getClientSecretFunction();

    List<Object> getDetectOn();

    Display getDisplay();

    String getName();

    List<Object> getRefreshOn();

    BiFunction<String, String, Pkce> getPkceFunction();

    List<Property<?>> getProperties();

    Optional<Function<Connection, String>> getRefreshFunction();

    Function<Connection, String> getRefreshUrlFunction();

    Function<Connection, List<String>> getScopesFunction();

    Function<Connection, String> getTokenUrlFunction();

    AuthorizationType getType();

    @SuppressFBWarnings("EI")
    record AuthorizationCallbackResponse(
        String accessToken, String refreshToken, Map<String, Object> additionalParameters) {

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();

            map.put(ACCESS_TOKEN, accessToken);
            map.put(REFRESH_TOKEN, refreshToken);

            map.putAll(additionalParameters);

            return map;
        }
    }

    record Pkce(String verifier, String challenge, String challengeMethod) {
    }
}
