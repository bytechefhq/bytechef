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

    /**
     *
     */
    String ACCESS_TOKEN = "access_token";

    /**
     *
     */
    String ADD_TO = "addTo";

    /**
     *
     */
    String API_TOKEN = "api_token";

    /**
     *
     */
    String AUTHORIZATION = "Authorization";

    /**
     *
     */
    String AUTHORIZATION_TYPE = "authorizationType";

    /**
     *
     */
    String AUTHORIZATION_URL = "authorizationUrl";

    /**
     *
     */
    String EXPIRES_IN = "expires_in";

    /**
     *
     */
    String BEARER = "Bearer";

    /**
     *
     */
    String CLIENT_ID = "clientId";

    /**
     *
     */
    String CLIENT_SECRET = "clientSecret";

    /**
     *
     */
    String CODE = "code";

    /**
     *
     */
    String HEADER_PREFIX = "headerPrefix";

    /**
     *
     */
    String KEY = "key";

    /**
     *
     */
    String PASSWORD = "password";

    /**
     *
     */
    String REFRESH_TOKEN = "refresh_token";

    /**
     *
     */
    String REFRESH_URL = "refreshUrl";

    /**
     *
     */
    String SCOPES = "scopes";

    /**
     *
     */
    String TOKEN = "token";

    /**
     *
     */
    String TOKEN_URL = "tokenUrl";

    /**
     *
     */
    String USERNAME = "username";

    /**
     *
     */
    String VALUE = "value";

    /**
     *
     */
    enum AuthorizationType {
        /**
         *
         */
        API_KEY,

        /**
         *
         */
        BASIC_AUTH,

        /**
         *
         */
        BEARER_TOKEN,

        /**
         * Custom authorization type
         */
        CUSTOM,

        /**
         *
         */
        DIGEST_AUTH,

        /**
         *
         */
        OAUTH2_AUTHORIZATION_CODE,

        /**
         *
         */
        OAUTH2_AUTHORIZATION_CODE_PKCE,

        /**
         *
         */
        OAUTH2_CLIENT_CREDENTIALS,

        /**
         *
         */
        OAUTH2_IMPLICIT_CODE,

        /**
         *
         */
        OAUTH2_RESOURCE_OWNER_PASSWORD;

        /**
         *
         * @return
         */
        public String getName() {
            return name().toLowerCase();
        }

    }

    /**
     *
     */
    enum ApiTokenLocation {

        /**
         *
         */
        HEADER,

        /**
         *
         */
        QUERY_PARAMETERS,
    }

    /**
     * @return the implementation of {@link AcquireFunction} interface if defined
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
     * This is an optional list of signals that identify when the system must raise the excpetion even if the response
     * has 200 status code . Some APIs don't signal errors with explicit response status code, such as 401. Instead,
     * they return a 200 (pseudo-successful response) with a payload that signals the error. For such APIs, ByteChef
     * does not pick up an error (expired credentials, bad requests, and so on). It interprets it as a successful
     * request because of the 200 response code. When it finds a match with signals
     * {@link com.bytechef.component.exception.ProviderException} is thrown, and two things can happen:
     *
     * 1. There can also be a match with <b>refreshOn</b> signals. This triggers a re-authorization where the
     * {@link RefreshFunction} or {@link AcquireFunction} function runs instead of the system raising an exception.
     *
     * 2. If there is no match with signals that are defined in <b>refreshOn</b>, the system raises an exception.
     *
     * Regex expression samples: "Error message", "^\{"response":\{"error".+$", "^.*(4\d\d)(\s(Unauthorized)?.*)?$"
     *
     * @return the list of Regex expressions which are matched on the response body
     */
    Optional<List<String>> getDetectOn();

    /**
     *
     * @return
     */
    Optional<String> getDescription();

    String getName();

    Optional<OAuth2AuthorizationExtraQueryParametersFunction> getOauth2AuthorizationExtraQueryParameters();

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
     * @return the implementation of {@link RefreshFunction} interface if defined
     */
    Optional<RefreshFunction> getRefresh();

    /**
     * This is an optional array of signals that identify when the system must re-acquire credentials. When it receives
     * an error response (400, 401, 500...), the list of signals is checked. If there is a match, re-authorization is
     * triggered by running either the {@link RefreshFunction}} function for <b>OAUTH2_AUTHORIZATION_CODE</b> or
     * <b>OAUTH2_AUTHORIZATION_CODE</b> {@link AuthorizationType}s, or the {@link AcquireFunction} function for
     * <b>CUSTOM</b> {@link AuthorizationType}.
     *
     * Regex expression samples: 401, "Unauthorized", "^.*Unauthorized.*$",
     *
     * @return the list of integers which are matched to HTTP response codes or Regex expressions which are matched on
     *         the response body
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
     * If {@link AuthorizationType} is <b>CUSTOM</b>, the acquire lambda function is only run if <b>refreshOn</b> or
     * <b>detectOn</b> is triggered.
     */
    @FunctionalInterface
    interface AcquireFunction {

        /**
         * Applies this function to the given arguments.
         *
         * @param connectionParameters the connection parameters
         * @param context              the context
         * @return the refreshed connection authorization parameters
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
            Parameters connectionParameters, String code, String redirectUri, String codeVerifier,
            Context context) throws Exception;
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
     *
     */
    @FunctionalInterface
    interface OAuth2AuthorizationExtraQueryParametersFunction {

        /**
         * @param connectionParameters
         * @param context
         * @return
         */
        Map<String, String> apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface PkceFunction {

        /**
         * @param verifier
         * @param challenge
         * @param challengeMethod
         * @param context
         * @return
         * @throws Exception
         */
        Pkce apply(String verifier, String challenge, String challengeMethod, Context context)
            throws Exception;
    }

    /**
     * Adds oauth refresh token value to provided connectionParameters
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
     * This function applies only to <b>OAUTH2_AUTHORIZATION_CODE</b> or <b>OAUTH2_AUTHORIZATION_CODE</b>
     * {@link AuthorizationType}s. In many situations, the API expires the access token after a prescribed amount of
     * time. The system then uses a refresh token to obtain a new access token. Refresh tokens usually do not expire.
     * Not all APIs issue refresh token credentials. You need to heck with your provider about this requirement.
     */
    @FunctionalInterface
    interface RefreshFunction {

        /**
         * Applies this function to the given arguments.
         *
         * @param connectionParameters the connection parameters
         * @param context              the context
         * @return the refreshed connection authorization parameters
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

        /**
         *
         * @return
         */
        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        /**
         *
         * @return
         */
        public Map<String, List<String>> getQueryParameters() {
            return queryParameters;
        }
    }

    /**
     * @param result
     */
    @SuppressFBWarnings("EI")
    record AuthorizationCallbackResponse(Map<String, ?> result) {

        /**
         *
         * @param accessToken
         * @param refreshToken
         * @param expiresIn
         * @param additionalParameters
         */
        public AuthorizationCallbackResponse(
            String accessToken, String refreshToken, Long expiresIn, Map<String, Object> additionalParameters) {

            this(toMap(accessToken, refreshToken, expiresIn, additionalParameters));
        }

        /**
         *
         * @param accessToken
         * @param refreshToken
         * @param expiresIn
         */
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

    /**
     *
     * @param accessToken
     * @param expiresIn
     */
    record RefreshTokenResponse(String accessToken, String refreshToken, Long expiresIn) {
    }

    /**
     *
     * @param verifier
     * @param challenge
     * @param challengeMethod
     */
    record Pkce(String verifier, String challenge, String challengeMethod) {
    }
}
