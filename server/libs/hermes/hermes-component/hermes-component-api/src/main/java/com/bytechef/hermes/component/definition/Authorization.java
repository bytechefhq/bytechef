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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.component.definition.constant.AuthorizationConstants;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.definition.Property.InputProperty;
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
    Optional<List<? extends InputProperty>> getProperties();

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
    Optional<RefreshUrlFunction> getRefreshUrl();

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
        String apply(ParameterMap connectionParameters, Context context) throws ComponentExecutionException;
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
        ApplyResponse apply(ParameterMap connectionParameters, Context context) throws ComponentExecutionException;
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
            ParameterMap connectionParameters, String code, String redirectUri, String codeVerifier, Context context)
            throws ComponentExecutionException;
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
        String apply(ParameterMap connectionParameters, Context context) throws ComponentExecutionException;
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
        String apply(ParameterMap connectionParameters, Context context) throws ComponentExecutionException;
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
        String apply(ParameterMap connectionParameters, Context context) throws ComponentExecutionException;
    }

    /**
     *
     */
    @FunctionalInterface
    interface PkceFunction {

        Pkce apply(String verifier, String challenge, String challengeMethod, Context context)
            throws ComponentExecutionException;
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
        String apply(ParameterMap connectionParameters, Context context) throws ComponentExecutionException;
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
        String apply(ParameterMap connectionParameters, Context context) throws ComponentExecutionException;
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
        List<String> apply(ParameterMap connectionParameters, Context context) throws ComponentExecutionException;
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
        String apply(ParameterMap connectionParameters, Context context) throws ComponentExecutionException;
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
     *
     * @param accessToken
     * @param refreshToken
     * @param additionalParameters
     */
    @SuppressFBWarnings("EI")
    record AuthorizationCallbackResponse(
        String accessToken, String refreshToken, Map<String, Object> additionalParameters) {

        public AuthorizationCallbackResponse(String accessToken, String refreshToken) {
            this(accessToken, refreshToken, Map.of());
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();

            map.put(AuthorizationConstants.ACCESS_TOKEN, accessToken);
            map.put(AuthorizationConstants.REFRESH_TOKEN, refreshToken);

            map.putAll(additionalParameters);

            return map;
        }
    }

    record Pkce(String verifier, String challenge, String challengeMethod) {
    }
}
