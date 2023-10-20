
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

import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableAuthorization;
import com.bytechef.hermes.definition.Property;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public sealed interface Authorization permits ModifiableAuthorization {

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
    Optional<ApplyConsumer> getApply();

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
    Optional<List<? extends Property<?>>> getProperties();

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
         *
         * @param connectionInputParameters
         * @return
         */
        String apply(Map<String, Object> connectionInputParameters);
    }

    @FunctionalInterface
    interface ApplyConsumer {

        /**
         *
         * @param connectionInputParameters
         * @param authorizationContext
         */
        void accept(
            Map<String, Object> connectionInputParameters, AuthorizationContext authorizationContext);
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
            Map<String, Object> connectionInputParameters, String code, String redirectUri, String codeVerifier);
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
        String apply(Map<String, Object> connectionInputParameters);
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
        String apply(Map<String, Object> connectionInputParameters);
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
        String apply(Map<String, Object> connectionInputParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface PkceFunction {

        Pkce apply(String verifier, String challenge, String challengeMethod);
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
        String apply(Map<String, Object> connectionInputParameters);
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
        String apply(Map<String, Object> connectionInputParameters);
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
        List<String> apply(Map<String, Object> connectionInputParameters);
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
        String apply(Map<String, Object> connectionInputParameters);
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

            map.put(ACCESS_TOKEN, accessToken);
            map.put(REFRESH_TOKEN, refreshToken);

            map.putAll(additionalParameters);

            return map;
        }
    }

    interface AuthorizationContext {
        void setHeaders(Map<String, List<String>> headers);

        void setQueryParameters(Map<String, List<String>> queryParameters);

        void setBody(Map<String, String> body);

        void setUsernamePassword(String username, String password);
    }

    record Pkce(String verifier, String challenge, String challengeMethod) {
    }
}
