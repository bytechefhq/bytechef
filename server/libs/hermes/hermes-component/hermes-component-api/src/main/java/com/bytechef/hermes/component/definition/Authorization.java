
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

import com.bytechef.hermes.component.InputParameters;
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

    enum HttpVerb {
        DELETE, GET, PATCH, POST, PUT
    }

    enum AuthorizationType {
        API_KEY((
            InputParameters connectionParameters, AuthorizationContext authorizationContext, String url,
            HttpVerb httpVerb) -> {

            String addTo = connectionParameters.getString(ADD_TO, ApiTokenLocation.HEADER.name());

            if (ApiTokenLocation.valueOf(addTo.toUpperCase()) == ApiTokenLocation.HEADER) {
                authorizationContext.setHeaders(
                    Map.of(
                        connectionParameters.getString(KEY, API_TOKEN),
                        List.of(connectionParameters.getString(VALUE, ""))));
            } else {
                authorizationContext.setQueryParameters(
                    Map.of(
                        connectionParameters.getString(KEY, API_TOKEN),
                        List.of(connectionParameters.getString(VALUE, ""))));
            }
        }),
        BASIC_AUTH(
            (
                InputParameters connectionParameters, AuthorizationContext authorizationContext, String url,
                HttpVerb httpVerb) -> authorizationContext
                    .setUsernamePassword(
                        connectionParameters.getString(USERNAME),
                        connectionParameters.getString(PASSWORD))),
        BEARER_TOKEN(
            (
                InputParameters connectionParameters, AuthorizationContext authorizationContext, String url,
                HttpVerb httpVerb) -> authorizationContext
                    .setHeaders(
                        Map.of(
                            Constants.AUTHORIZATION,
                            List.of(Constants.BEARER + " " + connectionParameters.getString(TOKEN))))),
        CUSTOM(null),
        DIGEST_AUTH(
            (
                InputParameters connectionParameters, AuthorizationContext authorizationContext, String url,
                HttpVerb httpVerb) -> authorizationContext
                    .setUsernamePassword(
                        connectionParameters.getString(USERNAME),
                        connectionParameters.getString(PASSWORD))),
        OAUTH2_AUTHORIZATION_CODE(
            (
                InputParameters connectionParameters, AuthorizationContext authorizationContext, String url,
                HttpVerb httpVerb) -> authorizationContext
                    .setHeaders(getOAuth2Headers(connectionParameters))),
        OAUTH2_AUTHORIZATION_CODE_PKCE(
            (
                InputParameters connectionParameters, AuthorizationContext authorizationContext, String url,
                HttpVerb httpVerb) -> authorizationContext
                    .setHeaders(getOAuth2Headers(connectionParameters))),
        OAUTH2_CLIENT_CREDENTIALS(
            (
                InputParameters connectionParameters, AuthorizationContext authorizationContext, String url,
                HttpVerb httpVerb) -> authorizationContext
                    .setHeaders(getOAuth2Headers(connectionParameters))),
        OAUTH2_IMPLICIT_CODE(
            (
                InputParameters connectionParameters, AuthorizationContext authorizationContext, String url,
                HttpVerb httpVerb) -> authorizationContext
                    .setHeaders(getOAuth2Headers(connectionParameters))),
        OAUTH2_RESOURCE_OWNER_PASSWORD(
            (
                InputParameters connectionParameters, AuthorizationContext authorizationContext, String url,
                HttpVerb httpVerb) -> authorizationContext
                    .setHeaders(getOAuth2Headers(connectionParameters)));

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

        private static Map<String, List<String>> getOAuth2Headers(InputParameters connectionParameters) {
            return Map.of(
                Constants.AUTHORIZATION,
                List.of(
                    connectionParameters.getString(HEADER_PREFIX, Constants.BEARER) + " " +
                        connectionParameters.getString(ACCESS_TOKEN)));
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
         * @param connectionParameters
         * @return
         */
        String apply(InputParameters connectionParameters);
    }

    @FunctionalInterface
    interface ApplyConsumer {

        /**
         *
         * @param connectionParameters
         * @param authorizationContext
         */
        void accept(
            InputParameters connectionParameters, AuthorizationContext authorizationContext, String url,
            HttpVerb httpVerb);
    }

    /**
     *
     */
    @FunctionalInterface
    interface AuthorizationCallbackFunction {

        /**
         *
         * @param connectionParameters
         * @param code
         * @param redirectUri
         * @param codeVerifier
         * @return
         */
        AuthorizationCallbackResponse apply(
            InputParameters connectionParameters, String code, String redirectUri, String codeVerifier);
    }

    @SuppressFBWarnings("EI")
    record AuthorizationContext(
        Map<String, List<String>> headers, Map<String, List<String>> queryParameters, Map<String, String> payload) {

        private static final Base64.Encoder ENCODER = Base64.getEncoder();

        public void setHeaders(Map<String, List<String>> headers) {
            this.headers.putAll(headers);
        }

        public void setQueryParameters(Map<String, List<String>> queryParameters) {
            this.queryParameters.putAll(queryParameters);
        }

        public void setPayload(Map<String, String> payload) {
            this.payload.putAll(payload);
        }

        public void setUsernamePassword(String username, String password) {
            String valueToEncode = username + ":" + password;

            headers.put(
                "Authorization",
                List.of("Basic " + ENCODER.encodeToString(valueToEncode.getBytes(StandardCharsets.UTF_8))));
        }
    }

    /**
     *
     */
    @FunctionalInterface
    interface AuthorizationUrlFunction {

        /**
         *
         * @param connectionParameters
         * @return
         */
        String apply(InputParameters connectionParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface ClientIdFunction {

        /**
         *
         * @param connectionParameters
         * @return
         */
        String apply(InputParameters connectionParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface ClientSecretFunction {

        /**
         *
         * @param connectionParameters
         * @return
         */
        String apply(InputParameters connectionParameters);
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
         * @param connectionParameters
         * @return
         */
        String apply(InputParameters connectionParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface RefreshUrlFunction {

        /**
         *
         * @param connectionParameters
         * @return
         */
        String apply(InputParameters connectionParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface ScopesFunction {

        /**
         *
         * @param connectionParameters
         * @return
         */
        List<String> apply(InputParameters connectionParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface TokenUrlFunction {

        /**
         *
         * @param connectionParameters
         * @return
         */
        String apply(InputParameters connectionParameters);
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
