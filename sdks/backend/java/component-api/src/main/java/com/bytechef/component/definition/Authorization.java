/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.definition;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface Authorization {

    String ACCESS_TOKEN = "access_token";
    String ADD_TO = "addTo";
    String API_TOKEN = "api_token";
    String AUTHORIZATION = "Authorization";
    String AUTHORIZATION_TYPE = "authorizationName";
    String AUTHORIZATION_URL = "authorizationUrl";
    String EXPIRES_IN = "expires_in";
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
        API_KEY, BASIC_AUTH, BEARER_TOKEN, CUSTOM, DIGEST_AUTH, OAUTH2_AUTHORIZATION_CODE,
        OAUTH2_AUTHORIZATION_CODE_PKCE, OAUTH2_CLIENT_CREDENTIALS, OAUTH2_IMPLICIT_CODE,
        OAUTH2_RESOURCE_OWNER_PASSWORD;

        public String toLowerCase() {
            return name().toLowerCase();
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
     * TODO
     *
     * @return
     */
    Optional<AcquireFunction> getAcquire();

    /**
     *
     * @return
     */
    Optional<ApplyFunction> getApply();

    /**
     *
     * @return
     */
    Optional<AuthorizationCallbackFunction> getAuthorizationCallback();

    /**
     *
     * @return
     */
    Optional<AuthorizationUrlFunction> getAuthorizationUrl();

    /**
     *
     * @return
     */
    Optional<ClientIdFunction> getClientId();

    /**
     *
     * @return
     */
    Optional<ClientSecretFunction> getClientSecret();

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
    Optional<PkceFunction> getPkce();

    /**
     *
     * @return
     */
    Optional<List<? extends Property>> getProperties();

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
     *
     * @return
     */
    Optional<RefreshUrlFunction> getRefreshUrl();

    /**
     *
     * @return
     */
    Optional<RefreshTokenFunction> getRefreshToken();

    /**
     *
     * @return
     */
    Optional<ScopesFunction> getScopes();

    /**
     *
     * @return
     */
    Optional<String> getTitle();

    /**
     *
     * @return
     */
    Optional<TokenUrlFunction> getTokenUrl();

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
         * @param connectionParameters
         * @param context
         * @return
         */
        Map<String, ?> apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface ApplyFunction {

        /**
         * @param connectionParameters
         * @param context
         * @return
         */
        ApplyResponse apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface AuthorizationCallbackFunction {

        /**
         * @param connectionParameters
         * @param code
         * @param redirectUri
         * @param codeVerifier
         * @param context
         * @return
         */
        AuthorizationCallbackResponse apply(
            Parameters connectionParameters, String code, String redirectUri, String codeVerifier, Context context)
            throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface AuthorizationUrlFunction {

        /**
         * @param connectionParameters
         * @param context
         * @return
         */
        String apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface ClientIdFunction {

        /**
         * @param connectionParameters
         * @param context
         * @return
         */
        String apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface ClientSecretFunction {

        /**
         * @param connectionParameters
         * @param context
         * @return
         */
        String apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     * adds oauth refresh token value to provided connectionParameters
     */
    @FunctionalInterface
    interface RefreshTokenFunction {

        /**
         * @param connectionParameters
         * @param context
         * @return
         */
        String apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface PkceFunction {

        Pkce apply(String verifier, String challenge, String challengeMethod, Context context)
            throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface RefreshFunction {

        /**
         * @param connectionParameters
         * @param context
         * @return
         */
        RefreshTokenResponse apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface RefreshUrlFunction {

        /**
         * @param connectionParameters
         * @param context
         * @return
         */
        String apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface ScopesFunction {

        /**
         * @param connectionParameters
         * @param context
         * @return
         */
        List<String> apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface TokenUrlFunction {

        /**
         * @param connectionParameters
         * @param context
         * @return
         */
        String apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     *
     */
    @SuppressFBWarnings("EI")
    class ApplyResponse {
        private final Map<String, List<String>> headers = new HashMap<>();
        private final Map<String, List<String>> queryParameters = new HashMap<>();

        /**
         *
         * @param headers
         * @return
         */
        public static ApplyResponse ofHeaders(Map<String, List<String>> headers) {
            ApplyResponse applyResponse = new ApplyResponse();

            if (headers != null) {
                applyResponse.headers.putAll(headers);
            }

            return applyResponse;
        }

        /**
         *
         * @param queryParameters
         * @return
         */
        public static ApplyResponse ofQueryParameters(Map<String, List<String>> queryParameters) {
            ApplyResponse applyResponse = new ApplyResponse();

            if (queryParameters != null) {
                applyResponse.queryParameters.putAll(queryParameters);
            }

            return applyResponse;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public Map<String, List<String>> getQueryParameters() {
            return queryParameters;
        }
    }

    /**
     * @param result
     */
    @SuppressFBWarnings("EI")
    record AuthorizationCallbackResponse(Map<String, ?> result) {

        public AuthorizationCallbackResponse(
            String accessToken, String refreshToken, Long expiresIn, Map<String, Object> additionalParameters) {

            this(toMap(accessToken, refreshToken, expiresIn, additionalParameters));
        }

        public AuthorizationCallbackResponse(String accessToken, String refreshToken, Long expiresIn) {
            this(accessToken, refreshToken, expiresIn, Map.of());
        }

        private static Map<String, Object> toMap(
            String accessToken, String refreshToken, Long expiresIn, Map<String, Object> additionalParameters) {

            Map<String, Object> map = new HashMap<>();

            map.put(ACCESS_TOKEN, accessToken);
            map.put(REFRESH_TOKEN, refreshToken);
            map.put(EXPIRES_IN, expiresIn);

            map.putAll(additionalParameters);

            return map;
        }
    }

    record RefreshTokenResponse(String accessToken, Long expiresIn) {
    }

    record Pkce(String verifier, String challenge, String challengeMethod) {
    }
}
