
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

import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@JsonDeserialize(as = ComponentDSL.ModifiableAuthorization.class)
public sealed interface Authorization permits ComponentDSL.ModifiableAuthorization {

    String ADD_TO = "addTo";
    String API_TOKEN = "api_token";
    String ACCESS_TOKEN = "access_token";
    String AUTHORIZATION_URL = "authorizationUrl";
    String CLIENT_ID = "clientId";
    String CLIENT_SECRET = "clientSecret";
    String CODE = "code";
    String HEADER_PREFIX = "headerPrefix";
    String KEY = "key";
    String PASSWORD = "password";
    String REFRESH_TOKEN = "refresh_token";
    String REFRESH_URL = "refreshUrl";
    String SCOPES = "scopes";
    String TOKEN = "token";
    String TOKEN_URL = "tokenUrl";
    String USERNAME = "username";
    String VALUE = "value";

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
            .setHeaders(
                getBasicAuthorizationHeader(connection.getParameter(USERNAME), connection.getParameter(PASSWORD)))),
        BEARER_TOKEN(
            (AuthorizationContext authorizationContext, Connection connection) -> authorizationContext
                .setHeaders(
                    Map.of(Constants.AUTHORIZATION, List.of(Constants.BEARER + " " + connection.getParameter(TOKEN))))),
        CUSTOM(null),
        DIGEST_AUTH((AuthorizationContext authorizationContext, Connection connection) -> authorizationContext
            .setHeaders(
                getBasicAuthorizationHeader(connection.getParameter(USERNAME), connection.getParameter(PASSWORD)))),
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

        private static final Base64.Encoder ENCODER = Base64.getEncoder();

        private final ApplyConsumer defaultApplyConsumer;

        AuthorizationType(ApplyConsumer defaultApplyConsumer) {
            this.defaultApplyConsumer = defaultApplyConsumer;
        }

        public ApplyConsumer getDefaultApply() {
            return defaultApplyConsumer;
        }

        private static class Constants {
            private static final String AUTHORIZATION = "Authorization";
            private static final String BEARER = "Bearer";
        }

        private static Map<String, List<String>> getBasicAuthorizationHeader(Object username, Object password) {
            String valueToEncode = username + ":" + password;

            return Map.of(
                "Authorization",
                List.of("Basic " + ENCODER.encodeToString(valueToEncode.getBytes(StandardCharsets.UTF_8))));
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

    Optional<AcquireFunction> getAcquire();

    ApplyConsumer getApply();

    AuthorizationCallbackFunction getAuthorizationCallback();

    AuthorizationUrlFunction getAuthorizationUrl();

    ClientIdFunction getClientId();

    ClientSecretFunction getClientSecret();

    List<Object> getDetectOn();

    Display getDisplay();

    String getName();

    PkceFunction getPkce();

    List<? extends Property<?>> getProperties();

    Optional<RefreshFunction> getRefresh();

    List<Object> getRefreshOn();

    RefreshUrlFunction getRefreshUrl();

    ScopesFunction getScopes();

    TokenUrlFunction getTokenUrl();

    AuthorizationType getType();

    /**
     *
     */
    @FunctionalInterface
    interface AcquireFunction {

        /**
         *
         * @param connection
         * @return
         */
        String apply(Connection connection);
    }

    @FunctionalInterface
    interface ApplyConsumer {

        /**
         *
         * @param authorizationContext
         * @param connection
         */
        void accept(AuthorizationContext authorizationContext, Connection connection);
    }

    /**
     *
     */
    @FunctionalInterface
    interface AuthorizationCallbackFunction {

        /**
         *
         * @param connection
         * @param code
         * @param redirectUri
         * @param codeVerifier
         * @return
         */
        AuthorizationCallbackResponse apply(
            Connection connection, String code, String redirectUri, String codeVerifier);
    }

    interface AuthorizationContext {

        void setHeaders(Map<String, List<String>> headers);

        void setQueryParameters(Map<String, List<String>> queryParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface AuthorizationUrlFunction {

        /**
         *
         * @param connection
         * @return
         */
        String apply(Connection connection);
    }

    /**
     *
     */
    @FunctionalInterface
    interface ClientIdFunction {

        /**
         *
         * @param connection
         * @return
         */
        String apply(Connection connection);
    }

    /**
     *
     */
    @FunctionalInterface
    interface ClientSecretFunction {

        /**
         *
         * @param connection
         * @return
         */
        String apply(Connection connection);
    }

    /**
     *
     */
    @FunctionalInterface
    interface PkceFunction {

        Pkce apply(String verifier, String challenge);
    }

    /**
     *
     */
    @FunctionalInterface
    interface RefreshFunction {

        /**
         *
         * @param connection
         * @return
         */
        String apply(Connection connection);
    }

    /**
     *
     */
    @FunctionalInterface
    interface RefreshUrlFunction {

        /**
         *
         * @param connection
         * @return
         */
        String apply(Connection connection);
    }

    /**
     *
     */
    @FunctionalInterface
    interface ScopesFunction {

        /**
         *
         * @param connection
         * @return
         */
        List<String> apply(Connection connection);
    }

    /**
     *
     */
    @FunctionalInterface
    interface TokenUrlFunction {

        /**
         *
         * @param connection
         * @return
         */
        String apply(Connection connection);
    }

    /**
     *
     * @param accessToken
     * @param refreshToken
     * @param additionalParameters
     */
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
