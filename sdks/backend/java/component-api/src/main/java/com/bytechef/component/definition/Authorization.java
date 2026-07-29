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
 * Defines an authorization scheme used by a {@link ConnectionDefinition} to authenticate outgoing requests. It captures
 * the authorization type, the input properties collected from the user, and the functions that apply credentials,
 * acquire and refresh tokens, and drive OAuth2 flows. The constants declared here are the well-known parameter keys
 * used within connection and authorization parameter maps.
 *
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public interface Authorization {

    /**
     * Parameter key under which an OAuth2 access token is stored.
     */
    String ACCESS_TOKEN = "access_token";

    /**
     * Parameter key indicating where an API key should be added to the request (for example, as a header or query
     * parameter).
     */
    String ADD_TO = "addTo";

    /**
     * Parameter key under which an API token is stored.
     */
    String API_TOKEN = "api_token";

    /**
     * Name of the standard HTTP {@code Authorization} header.
     */
    String AUTHORIZATION = "Authorization";

    /**
     * Parameter key identifying the {@link AuthorizationType} of a connection.
     */
    String AUTHORIZATION_TYPE = "authorizationType";

    /**
     * Parameter key holding the OAuth2 authorization endpoint URL.
     */
    String AUTHORIZATION_URL = "authorizationUrl";

    /**
     * Parameter key holding the lifetime, in seconds, of an OAuth2 access token.
     */
    String EXPIRES_IN = "expires_in";

    /**
     * Value of the {@code Bearer} authorization scheme prefix.
     */
    String BEARER = "Bearer";

    /**
     * Parameter key holding the OAuth2 client identifier.
     */
    String CLIENT_ID = "clientId";

    /**
     * Parameter key holding the OAuth2 client secret.
     */
    String CLIENT_SECRET = "clientSecret";

    /**
     * Parameter key holding the authorization code returned by an OAuth2 authorization-code flow.
     */
    String CODE = "code";

    /**
     * Parameter key holding the prefix prepended to an API key value placed in a header.
     */
    String HEADER_PREFIX = "headerPrefix";

    /**
     * Parameter key holding the name of the header or query parameter under which an API key is sent.
     */
    String KEY = "key";

    /**
     * Parameter key holding a password used for basic or resource-owner-password authentication.
     */
    String PASSWORD = "password";

    /**
     * Parameter key under which an OAuth2 refresh token is stored.
     */
    String REFRESH_TOKEN = "refresh_token";

    /**
     * Parameter key holding the OAuth2 token-refresh endpoint URL.
     */
    String REFRESH_URL = "refreshUrl";

    /**
     * Parameter key holding the set of OAuth2 scopes requested for the connection.
     */
    String SCOPES = "scopes";

    /**
     * Parameter key holding a generic token value.
     */
    String TOKEN = "token";

    /**
     * Parameter key holding the OAuth2 token endpoint URL.
     */
    String TOKEN_URL = "tokenUrl";

    /**
     * Parameter key holding a username used for basic or resource-owner-password authentication.
     */
    String USERNAME = "username";

    /**
     * Parameter key holding the value of an API key.
     */
    String VALUE = "value";

    /**
     * Enumerates the supported authorization schemes, each flagged with whether it requires periodic token refresh.
     */
    enum AuthorizationType {
        /**
         * API key authentication, where a secret key is sent as a header or query parameter.
         */
        API_KEY(false),

        /**
         * HTTP Basic authentication using a username and password.
         */
        BASIC_AUTH(false),

        /**
         * Bearer token authentication, where a token is sent in the {@code Authorization} header.
         */
        BEARER_TOKEN(false),

        /**
         * Custom authorization type
         */
        CUSTOM(false),

        /**
         * HTTP Digest authentication using a challenge-response mechanism.
         */
        DIGEST_AUTH(false),

        /**
         * OAuth2 authorization-code grant, which exchanges an authorization code for tokens and supports token refresh.
         */
        OAUTH2_AUTHORIZATION_CODE(true),

        /**
         * OAuth2 authorization-code grant with Proof Key for Code Exchange (PKCE), which supports token refresh.
         */
        OAUTH2_AUTHORIZATION_CODE_PKCE(true),

        /**
         * OAuth2 client-credentials grant, which authenticates using client identifier and secret without a user.
         */
        OAUTH2_CLIENT_CREDENTIALS(false),

        /**
         * OAuth2 implicit grant, which returns an access token directly from the authorization endpoint.
         */
        OAUTH2_IMPLICIT_CODE(false),

        /**
         * OAuth2 resource-owner-password grant, which exchanges a username and password for tokens.
         */
        OAUTH2_RESOURCE_OWNER_PASSWORD(false);

        private final boolean requiresTokenRefresh;

        /**
         * Returns whether this authorization type requires periodic refreshing of an access token.
         *
         * @return {@code true} if the authorization type relies on refreshable tokens, {@code false} otherwise
         */
        public boolean requiresTokenRefresh() {
            return requiresTokenRefresh;
        }

        /**
         *
         * @return the lowercased name of enum
         */
        public String getName() {
            return name().toLowerCase();
        }

        AuthorizationType(boolean requiresTokenRefresh) {
            this.requiresTokenRefresh = requiresTokenRefresh;
        }

    }

    /**
     * Enumerates the possible locations to which an API key can be added when authenticating a request.
     */
    enum ApiTokenLocation {

        /**
         * The API key is sent as an HTTP request header.
         */
        HEADER,

        /**
         * The API key is sent as a URL query parameter.
         */
        QUERY_PARAMETERS,
    }

    /**
     * @return the implementation of {@link AcquireFunction} interface if defined
     */
    Optional<AcquireFunction> getAcquire();

    /**
     * Returns the optional function that applies the connection's credentials to an outgoing request by contributing
     * headers or query parameters.
     *
     * @return an {@code Optional} containing the {@link ApplyFunction} if defined, or an empty {@code Optional}
     *         otherwise
     */
    Optional<ApplyFunction> getApply();

    /**
     * Returns the optional function invoked when the OAuth2 authorization callback is received, used to exchange the
     * authorization code for tokens.
     *
     * @return an {@code Optional} containing the {@link AuthorizationCallbackFunction} if defined, or an empty
     *         {@code Optional} otherwise
     */
    Optional<AuthorizationCallbackFunction> getAuthorizationCallback();

    /**
     * Returns the optional function that resolves the OAuth2 authorization endpoint URL.
     *
     * @return an {@code Optional} containing the {@link AuthorizationUrlFunction} if defined, or an empty
     *         {@code Optional} otherwise
     */
    Optional<AuthorizationUrlFunction> getAuthorizationUrl();

    /**
     * Returns the optional function that resolves the OAuth2 client identifier.
     *
     * @return an {@code Optional} containing the {@link ClientIdFunction} if defined, or an empty {@code Optional}
     *         otherwise
     */
    Optional<ClientIdFunction> getClientId();

    /**
     * Returns the optional function that resolves the OAuth2 client secret.
     *
     * @return an {@code Optional} containing the {@link ClientSecretFunction} if defined, or an empty {@code Optional}
     *         otherwise
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
     * Returns the human-readable description of this authorization scheme.
     *
     * @return an {@code Optional} containing the description if defined, or an empty {@code Optional} otherwise
     */
    Optional<String> getDescription();

    /**
     * Returns the unique name that identifies this authorization within its connection.
     *
     * @return the authorization name
     */
    String getName();

    /**
     * Returns the optional function that supplies extra query parameters appended to the OAuth2 authorization request.
     *
     * @return an {@code Optional} containing the {@link OAuth2AuthorizationExtraQueryParametersFunction} if defined, or
     *         an empty {@code Optional} otherwise
     */
    Optional<OAuth2AuthorizationExtraQueryParametersFunction> getOauth2AuthorizationExtraQueryParameters();

    /**
     * TODO
     *
     * @return
     */
    Optional<PkceFunction> getPkce();

    /**
     * Returns the input properties that define the credentials and settings collected from the user for this
     * authorization.
     *
     * @return an {@code Optional} containing the list of properties if defined, or an empty {@code Optional} otherwise
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
     * Returns the optional function that resolves the OAuth2 token-refresh endpoint URL.
     *
     * @return an {@code Optional} containing the {@link RefreshUrlFunction} if defined, or an empty {@code Optional}
     *         otherwise
     */
    Optional<RefreshUrlFunction> getRefreshUrl();

    /**
     * Returns the optional function that extracts the refresh token value from the connection parameters.
     *
     * @return an {@code Optional} containing the {@link RefreshTokenFunction} if defined, or an empty {@code Optional}
     *         otherwise
     */
    Optional<RefreshTokenFunction> getRefreshToken();

    /**
     * Returns the optional function that resolves the OAuth2 scopes requested for the connection.
     *
     * @return an {@code Optional} containing the {@link ScopesFunction} if defined, or an empty {@code Optional}
     *         otherwise
     */
    Optional<ScopesFunction> getScopes();

    /**
     * Returns the human-readable title of this authorization displayed in the user interface.
     *
     * @return an {@code Optional} containing the title if defined, or an empty {@code Optional} otherwise
     */
    Optional<String> getTitle();

    /**
     * Returns the optional function that resolves the OAuth2 token endpoint URL.
     *
     * @return an {@code Optional} containing the {@link TokenUrlFunction} if defined, or an empty {@code Optional}
     *         otherwise
     */
    Optional<TokenUrlFunction> getTokenUrl();

    /**
     * Returns the {@link AuthorizationType} that identifies the authentication scheme of this authorization.
     *
     * @return the authorization type
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
     * Functional interface that applies a connection's credentials to an outgoing request, producing the headers and
     * query parameters that carry the authentication information.
     */
    @FunctionalInterface
    interface ApplyFunction {

        /**
         * Produces the authentication data to attach to a request based on the connection parameters.
         *
         * @param connectionParameters the parameters of the connection being authenticated
         * @param context              the context for the current call
         * @return the {@link ApplyResponse} describing the headers and query parameters to add to the request
         * @throws Exception if the credentials cannot be applied
         */
        ApplyResponse apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     * Functional interface invoked when an OAuth2 authorization callback is received, exchanging the returned
     * authorization code for access and refresh tokens.
     */
    @FunctionalInterface
    interface AuthorizationCallbackFunction {

        /**
         * Exchanges the authorization code obtained from the OAuth2 callback for tokens.
         *
         * @param connectionParameters the parameters of the connection being authorized
         * @param code                 the authorization code returned by the authorization server
         * @param redirectUri          the redirect URI registered for the OAuth2 flow
         * @param codeVerifier         the PKCE code verifier, or {@code null} if PKCE is not used
         * @param context              the context for the current call
         * @return the {@link AuthorizationCallbackResponse} containing the acquired tokens
         * @throws Exception if the token exchange fails
         */
        AuthorizationCallbackResponse apply(
            Parameters connectionParameters, String code, String redirectUri, String codeVerifier,
            Context context) throws Exception;
    }

    /**
     * Functional interface that resolves the OAuth2 authorization endpoint URL for a connection.
     */
    @FunctionalInterface
    interface AuthorizationUrlFunction {

        /**
         * Resolves the OAuth2 authorization endpoint URL.
         *
         * @param connectionParameters the parameters of the connection being authorized
         * @param context              the context for the current call
         * @return the authorization endpoint URL
         * @throws Exception if the URL cannot be resolved
         */
        String apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     * Functional interface that resolves the OAuth2 client identifier for a connection.
     */
    @FunctionalInterface
    interface ClientIdFunction {

        /**
         * Resolves the OAuth2 client identifier.
         *
         * @param connectionParameters the parameters of the connection being authorized
         * @param context              the context for the current call
         * @return the client identifier
         * @throws Exception if the client identifier cannot be resolved
         */
        String apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     * Functional interface that resolves the OAuth2 client secret for a connection.
     */
    @FunctionalInterface
    interface ClientSecretFunction {

        /**
         * Resolves the OAuth2 client secret.
         *
         * @param connectionParameters the parameters of the connection being authorized
         * @param context              the context for the current call
         * @return the client secret
         * @throws Exception if the client secret cannot be resolved
         */
        String apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     * Functional interface that supplies extra query parameters appended to the OAuth2 authorization request.
     */
    @FunctionalInterface
    interface OAuth2AuthorizationExtraQueryParametersFunction {

        /**
         * Produces the additional query parameters to include in the OAuth2 authorization request.
         *
         * @param connectionParameters the parameters of the connection being authorized
         * @param context              the context for the current call
         * @return a map of extra query parameter names to values
         * @throws Exception if the parameters cannot be produced
         */
        Map<String, String> apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     * Functional interface that produces the Proof Key for Code Exchange (PKCE) values used to secure an OAuth2
     * authorization-code flow.
     */
    @FunctionalInterface
    interface PkceFunction {

        /**
         * Produces the PKCE values for an OAuth2 authorization-code flow.
         *
         * @param verifier        the code verifier
         * @param challenge       the code challenge derived from the verifier
         * @param challengeMethod the method used to derive the challenge (for example, {@code S256})
         * @param context         the context for the current call
         * @return the {@link Pkce} record holding the verifier, challenge, and challenge method
         * @throws Exception if the PKCE values cannot be produced
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
         * Extracts the refresh token value from the connection parameters.
         *
         * @param connectionParameters the parameters of the connection being refreshed
         * @param context              the context for the current call
         * @return the refresh token value
         * @throws Exception if the refresh token cannot be resolved
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
     * Functional interface that resolves the OAuth2 token-refresh endpoint URL for a connection.
     */
    @FunctionalInterface
    interface RefreshUrlFunction {

        /**
         * Resolves the OAuth2 token-refresh endpoint URL.
         *
         * @param connectionParameters the parameters of the connection being refreshed
         * @param context              the context for the current call
         * @return the token-refresh endpoint URL
         * @throws Exception if the URL cannot be resolved
         */
        String apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     * Functional interface that resolves the OAuth2 scopes requested for a connection.
     */
    @FunctionalInterface
    interface ScopesFunction {

        /**
         * Resolves the OAuth2 scopes to request for a connection.
         *
         * @param connectionParameters the parameters of the connection being authorized
         * @param context              the context for the current call
         * @return a map of scope names to a flag indicating whether each scope is enabled
         * @throws Exception if the scopes cannot be resolved
         */
        Map<String, Boolean> apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     * Functional interface that resolves the OAuth2 token endpoint URL for a connection.
     */
    @FunctionalInterface
    interface TokenUrlFunction {

        /**
         * Resolves the OAuth2 token endpoint URL.
         *
         * @param connectionParameters the parameters of the connection being authorized
         * @param context              the context for the current call
         * @return the token endpoint URL
         * @throws Exception if the URL cannot be resolved
         */
        String apply(Parameters connectionParameters, Context context) throws Exception;
    }

    /**
     * Holds the authentication data — HTTP headers and query parameters — contributed by an {@link ApplyFunction} to an
     * outgoing request.
     */
    @SuppressFBWarnings("EI")
    class ApplyResponse {
        private final Map<String, List<String>> headers = new HashMap<>();
        private final Map<String, List<String>> queryParameters = new HashMap<>();

        /**
         * Creates an {@link ApplyResponse} that contributes the given HTTP headers to a request.
         *
         * @param headers the HTTP headers to add, keyed by header name, or {@code null} for none
         * @return a new {@link ApplyResponse} carrying the given headers
         */
        public static ApplyResponse ofHeaders(Map<String, List<String>> headers) {
            ApplyResponse applyResponse = new ApplyResponse();

            if (headers != null) {
                applyResponse.headers.putAll(headers);
            }

            return applyResponse;
        }

        /**
         * Creates an {@link ApplyResponse} that contributes the given query parameters to a request.
         *
         * @param queryParameters the query parameters to add, keyed by parameter name, or {@code null} for none
         * @return a new {@link ApplyResponse} carrying the given query parameters
         */
        public static ApplyResponse ofQueryParameters(Map<String, List<String>> queryParameters) {
            ApplyResponse applyResponse = new ApplyResponse();

            if (queryParameters != null) {
                applyResponse.queryParameters.putAll(queryParameters);
            }

            return applyResponse;
        }

        /**
         * Returns the HTTP headers this response contributes to a request.
         *
         * @return the map of header names to values
         */
        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        /**
         * Returns the query parameters this response contributes to a request.
         *
         * @return the map of query parameter names to values
         */
        public Map<String, List<String>> getQueryParameters() {
            return queryParameters;
        }
    }

    /**
     * Holds the tokens and additional data produced by exchanging an OAuth2 authorization code, stored as a single
     * result map.
     *
     * @param result the map of acquired token values keyed by their parameter names
     */
    @SuppressFBWarnings("EI")
    record AuthorizationCallbackResponse(Map<String, ?> result) {

        /**
         * Creates a callback response from the acquired tokens and any additional parameters.
         *
         * @param accessToken          the acquired OAuth2 access token
         * @param refreshToken         the acquired OAuth2 refresh token, or {@code null} if none was issued
         * @param expiresIn            the lifetime of the access token in seconds, or {@code null} if unknown
         * @param additionalParameters additional token-related values to include in the result
         */
        public AuthorizationCallbackResponse(
            String accessToken, String refreshToken, Long expiresIn, Map<String, Object> additionalParameters) {

            this(toMap(accessToken, refreshToken, expiresIn, additionalParameters));
        }

        /**
         * Creates a callback response from the acquired tokens.
         *
         * @param accessToken  the acquired OAuth2 access token
         * @param refreshToken the acquired OAuth2 refresh token, or {@code null} if none was issued
         * @param expiresIn    the lifetime of the access token in seconds, or {@code null} if unknown
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
     * Holds the refreshed OAuth2 tokens returned by a {@link RefreshFunction}.
     *
     * @param accessToken  the refreshed access token
     * @param refreshToken the refreshed refresh token, or {@code null} if unchanged
     * @param expiresIn    the lifetime of the refreshed access token in seconds, or {@code null} if unknown
     */
    record RefreshTokenResponse(String accessToken, String refreshToken, Integer expiresIn) {
    }

    /**
     * Holds the Proof Key for Code Exchange (PKCE) values used to secure an OAuth2 authorization-code flow.
     *
     * @param verifier        the code verifier
     * @param challenge       the code challenge derived from the verifier
     * @param challengeMethod the method used to derive the challenge (for example, {@code S256})
     */
    record Pkce(String verifier, String challenge, String challengeMethod) {
    }
}
