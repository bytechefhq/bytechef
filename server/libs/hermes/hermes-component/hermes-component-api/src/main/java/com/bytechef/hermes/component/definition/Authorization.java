
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

    String ACCESS_TOKEN = "access_token";
    String ADD_TO = "addTo";
    String API_TOKEN = "api_token";
    String AUTHORIZATION = "Authorization";
    String AUTHORIZATION_URL = "authorizationUrl";
    String BEARER = "Bearer";
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

    /**
     *
     */
    enum AuthorizationType {
        API_KEY((
            InputParameters connectionInputParameters, AuthorizationContext authorizationContext, String url,
            HttpVerb httpVerb) -> {

            String addTo = connectionInputParameters.getString(ADD_TO, ApiTokenLocation.HEADER.name());

            if (ApiTokenLocation.valueOf(addTo.toUpperCase()) == ApiTokenLocation.HEADER) {
                authorizationContext.setHeaders(
                    Map.of(
                        connectionInputParameters.getString(KEY, API_TOKEN),
                        List.of(connectionInputParameters.getString(VALUE, ""))));
            } else {
                authorizationContext.setQueryParameters(
                    Map.of(
                        connectionInputParameters.getString(KEY, API_TOKEN),
                        List.of(connectionInputParameters.getString(VALUE, ""))));
            }
        }),
        BASIC_AUTH(
            (
                InputParameters connectionInputParameters, AuthorizationContext authorizationContext, String url,
                HttpVerb httpVerb) -> authorizationContext
                    .setUsernamePassword(
                        connectionInputParameters.getString(USERNAME),
                        connectionInputParameters.getString(PASSWORD))),
        BEARER_TOKEN(
            (
                InputParameters connectionInputParameters, AuthorizationContext authorizationContext, String url,
                HttpVerb httpVerb) -> authorizationContext
                    .setHeaders(
                        Map.of(
                            AUTHORIZATION,
                            List.of(BEARER + " " + connectionInputParameters.getString(TOKEN))))),
        CUSTOM(null),
        DIGEST_AUTH(
            (
                InputParameters connectionInputParameters, AuthorizationContext authorizationContext, String url,
                HttpVerb httpVerb) -> authorizationContext
                    .setUsernamePassword(
                        connectionInputParameters.getString(USERNAME),
                        connectionInputParameters.getString(PASSWORD))),
        OAUTH2_AUTHORIZATION_CODE(
            (
                InputParameters connectionInputParameters, AuthorizationContext authorizationContext, String url,
                HttpVerb httpVerb) -> authorizationContext
                    .setHeaders(getOAuth2Headers(connectionInputParameters))),
        OAUTH2_AUTHORIZATION_CODE_PKCE(
            (
                InputParameters connectionInputParameters, AuthorizationContext authorizationContext, String url,
                HttpVerb httpVerb) -> authorizationContext
                    .setHeaders(getOAuth2Headers(connectionInputParameters))),
        OAUTH2_CLIENT_CREDENTIALS(
            (
                InputParameters connectionInputParameters, AuthorizationContext authorizationContext, String url,
                HttpVerb httpVerb) -> authorizationContext
                    .setHeaders(getOAuth2Headers(connectionInputParameters))),
        OAUTH2_IMPLICIT_CODE(
            (
                InputParameters connectionInputParameters, AuthorizationContext authorizationContext, String url,
                HttpVerb httpVerb) -> authorizationContext
                    .setHeaders(getOAuth2Headers(connectionInputParameters))),
        OAUTH2_RESOURCE_OWNER_PASSWORD(
            (
                InputParameters connectionInputParameters, AuthorizationContext authorizationContext, String url,
                HttpVerb httpVerb) -> authorizationContext
                    .setHeaders(getOAuth2Headers(connectionInputParameters)));

        private final ApplyConsumer defaultApplyConsumer;

        AuthorizationType(ApplyConsumer defaultApplyConsumer) {
            this.defaultApplyConsumer = defaultApplyConsumer;
        }

        public ApplyConsumer getDefaultApply() {
            return defaultApplyConsumer;
        }

        public String toLowerCase() {
            return name().toLowerCase();
        }

        private static Map<String, List<String>> getOAuth2Headers(InputParameters connectionInputParameters) {
            return Map.of(
                AUTHORIZATION,
                List.of(
                    connectionInputParameters.getString(HEADER_PREFIX, BEARER) + " " +
                        connectionInputParameters.getString(ACCESS_TOKEN)));
        }
    }

    /**
     *
     */
    enum ApiTokenLocation {
        HEADER,
        QUERY_PARAMETERS,
    }

    /**
     *
     */
    enum HttpVerb {
        DELETE, GET, PATCH, POST, PUT
    }

    /**
     * TODO
     *
     * @return
     */
    Optional<AcquireFunction> getAcquire();

    /**
     *
     * @return
     */
    ApplyConsumer getApply();

    /**
     *
     * @return
     */
    AuthorizationCallbackFunction getAuthorizationCallback();

    /**
     *
     * @return
     */
    AuthorizationUrlFunction getAuthorizationUrl();

    /**
     *
     * @return
     */
    ClientIdFunction getClientId();

    /**
     *
     * @return
     */
    ClientSecretFunction getClientSecret();

    /**
     * TODO
     *
     * @return
     */
    Optional<List<Object>> getDetectOn();

    /**
     *
     * @return
     */
    Optional<String> getDescription();

    String getName();

    /**
     * TODO
     *
     * @return
     */
    PkceFunction getPkce();

    /**
     *
     * @return
     */
    List<? extends Property<?>> getProperties();

    /**
     * TODO
     *
     * @return
     */
    Optional<RefreshFunction> getRefresh();

    /**
     * TODO
     *
     * @return
     */
    Optional<List<Object>> getRefreshOn();

    /**
     * TODO
     *
     * @return
     */
    RefreshUrlFunction getRefreshUrl();

    /**
     *
     * @return
     */
    ScopesFunction getScopes();

    /**
     *
     * @return
     */
    String getTitle();

    /**
     *
     * @return
     */
    TokenUrlFunction getTokenUrl();

    /**
     *
     * @return
     */
    AuthorizationType getType();

    /**
     *
     */
    @FunctionalInterface
    interface AcquireFunction {

        /**
         *
         * @param connectionInputParameters
         * @return
         */
        String apply(InputParameters connectionInputParameters);
    }

    @FunctionalInterface
    interface ApplyConsumer {

        /**
         *
         * @param connectionInputParameters
         * @param authorizationContext
         */
        void accept(
            InputParameters connectionInputParameters, AuthorizationContext authorizationContext, String url,
            HttpVerb httpVerb);
    }

    /**
     *
     */
    @FunctionalInterface
    interface AuthorizationCallbackFunction {

        /**
         *
         * @param connectionInputParameters
         * @param code
         * @param redirectUri
         * @param codeVerifier
         * @return
         */
        AuthorizationCallbackResponse apply(
            InputParameters connectionInputParameters, String code, String redirectUri, String codeVerifier);
    }

    /**
     *
     */
    @FunctionalInterface
    interface AuthorizationUrlFunction {

        /**
         *
         * @param connectionInputParameters
         * @return
         */
        String apply(InputParameters connectionInputParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface ClientIdFunction {

        /**
         *
         * @param connectionInputParameters
         * @return
         */
        String apply(InputParameters connectionInputParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface ClientSecretFunction {

        /**
         *
         * @param connectionInputParameters
         * @return
         */
        String apply(InputParameters connectionInputParameters);
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
         * @param connectionInputParameters
         * @return
         */
        String apply(InputParameters connectionInputParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface RefreshUrlFunction {

        /**
         *
         * @param connectionInputParameters
         * @return
         */
        String apply(InputParameters connectionInputParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface ScopesFunction {

        /**
         *
         * @param connectionInputParameters
         * @return
         */
        List<String> apply(InputParameters connectionInputParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface TokenUrlFunction {

        /**
         *
         * @param connectionInputParameters
         * @return
         */
        String apply(InputParameters connectionInputParameters);
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

        AuthorizationCallbackResponse(String accessToken, String refreshToken) {
            this(accessToken, refreshToken, Map.of());
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();

            map.put(ACCESS_TOKEN, accessToken);
            map.put(REFRESH_TOKEN, refreshToken);

            map.putAll(additionalParameters);

            return map;
        }
    }

    @SuppressFBWarnings("EI")
    record AuthorizationContext(
        Map<String, List<String>> headers, Map<String, List<String>> queryParameters, Map<String, String> body) {

        private static final Base64.Encoder ENCODER = Base64.getEncoder();

        public void setHeaders(Map<String, List<String>> headers) {
            this.headers.putAll(headers);
        }

        public void setQueryParameters(Map<String, List<String>> queryParameters) {
            this.queryParameters.putAll(queryParameters);
        }

        public void setBody(Map<String, String> body) {
            this.body.putAll(body);
        }

        public void setUsernamePassword(String username, String password) {
            String valueToEncode = username + ":" + password;

            headers.put(
                "Authorization",
                List.of("Basic " + ENCODER.encodeToString(valueToEncode.getBytes(StandardCharsets.UTF_8))));
        }
    }

    record Pkce(String verifier, String challenge, String challengeMethod) {
    }
}
