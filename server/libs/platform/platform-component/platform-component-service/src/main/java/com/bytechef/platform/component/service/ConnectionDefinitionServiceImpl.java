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

package com.bytechef.platform.component.service;

import static com.bytechef.component.definition.Authorization.CODE;
import static com.bytechef.component.definition.ConnectionDefinition.BaseUriFunction;
import static com.bytechef.platform.component.domain.Authorization.DEFAULT_REFRESH_ON;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.FormatUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApiTokenLocation;
import com.bytechef.component.definition.Authorization.ApplyFunction;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackFunction;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Authorization.AuthorizationUrlFunction;
import com.bytechef.component.definition.Authorization.ClientIdFunction;
import com.bytechef.component.definition.Authorization.ClientSecretFunction;
import com.bytechef.component.definition.Authorization.OAuth2AuthorizationExtraQueryParametersFunction;
import com.bytechef.component.definition.Authorization.PkceFunction;
import com.bytechef.component.definition.Authorization.RefreshFunction;
import com.bytechef.component.definition.Authorization.RefreshTokenFunction;
import com.bytechef.component.definition.Authorization.RefreshTokenResponse;
import com.bytechef.component.definition.Authorization.RefreshUrlFunction;
import com.bytechef.component.definition.Authorization.ScopesFunction;
import com.bytechef.component.definition.Authorization.TokenUrlFunction;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.annotation.WithTokenRefresh;
import com.bytechef.platform.component.annotation.WithTokenRefresh.ComponentNameParam;
import com.bytechef.platform.component.annotation.WithTokenRefresh.ConnectionParam;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ScriptComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.exception.ConnectionDefinitionErrorType;
import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.Methanol;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
@Service("connectionDefinitionService")
public class ConnectionDefinitionServiceImpl implements ConnectionDefinitionService {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionDefinitionServiceImpl.class);

    private final ComponentDefinitionRegistry componentDefinitionRegistry;
    private final ContextFactory contextFactory;

    public ConnectionDefinitionServiceImpl(
        @Lazy ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory) {

        this.componentDefinitionRegistry = componentDefinitionRegistry;
        this.contextFactory = contextFactory;
    }

    @Override
    public Map<String, ?> executeAcquire(
        String componentName, int connectionVersion, AuthorizationType authorizationType,
        Map<String, ?> connectionParameters, Context context) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationType);

        try {
            return authorization.getAcquire()
                .orElseThrow(() -> new IllegalStateException("Acquire function is not defined."))
                .apply(ParametersFactory.create(connectionParameters), context);
        } catch (Exception e) {
            throw new ConfigurationException(e, ConnectionDefinitionErrorType.ACQUIRE_FAILED);
        }
    }

    @Override
    public ApplyResponse executeAuthorizationApply(
        String componentName, int connectionVersion, AuthorizationType authorizationType,
        Map<String, ?> connectionParameters, Context context) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationType);

        ApplyFunction applyFunction = authorization.getApply()
            .orElse(getDefaultApplyFunction(authorization.getType()));

        try {
            return applyFunction.apply(ParametersFactory.create(connectionParameters), context);
        } catch (Exception e) {
            throw new ConfigurationException(e, ConnectionDefinitionErrorType.AUTHORIZATION_APPLY_FAILED);
        }
    }

    @Override
    public AuthorizationCallbackResponse executeAuthorizationCallback(
        String componentName, int connectionVersion, AuthorizationType authorizationType,
        Map<String, ?> connectionParameters, String redirectUri) {

        return executeAuthorizationCallback(
            componentName, connectionVersion, authorizationType, connectionParameters,
            contextFactory.createContext(componentName, null), redirectUri);
    }

    @Override
    public Optional<String> executeBaseUri(
        String componentName, ComponentConnection componentConnection, Context context) {

        return executeBaseUriInternal(componentName, componentConnection, context);
    }

    @Override
    @WithTokenRefresh(
        errorTypeClass = ConnectionDefinitionErrorType.class,
        errorTypeField = "INVALID_CLAIM")
    public Optional<String> executeBaseUri(
        @ComponentNameParam String componentName, @ConnectionParam ComponentConnection componentConnection) {
        Context context = contextFactory.createContext(componentName, componentConnection);

        return executeBaseUriInternal(componentName, componentConnection, context);
    }

    private Optional<String> executeBaseUriInternal(
        String componentName, ComponentConnection componentConnection, Context context) {

        com.bytechef.component.definition.ConnectionDefinition connectionDefinition =
            componentDefinitionRegistry.getConnectionDefinition(componentName, componentConnection.getVersion());

        BaseUriFunction baseUriFunction = connectionDefinition.getBaseUri()
            .orElse((connectionParameters, context1) -> getDefaultBaseUri(connectionParameters));

        return Optional.ofNullable(
            baseUriFunction.apply(ParametersFactory.create(componentConnection.parameters()), context));
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, int connectionVersion, @Nullable String componentOperationName,
        int statusCode, Object body) {

        com.bytechef.component.definition.ConnectionDefinition connectionDefinition =
            componentDefinitionRegistry.getConnectionDefinition(componentName, connectionVersion);

        Context context = contextFactory.createContext(componentName, null);

        try {
            return connectionDefinition.getProcessErrorResponse()
                .orElseGet(() -> (statusCode1, body1, context1) -> ProviderException.getProviderException(
                    statusCode1, body1))
                .apply(statusCode, body, context);
        } catch (Exception e) {
            throw new ExecutionException(e, ConnectionDefinitionErrorType.INVALID_CLAIM);
        }
    }

    @Override
    public RefreshTokenResponse executeRefresh(
        String componentName, int connectionVersion, AuthorizationType authorizationType,
        Map<String, ?> connectionParameters, Context context) {

        if (logger.isTraceEnabled()) {
            logger.trace("Executing with {} authorization type.", authorizationType);
        }

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationType);

        if (logger.isTraceEnabled()) {
            logger.trace("Executing with persisted refresh token {}", authorization.getRefreshToken());
        }

        RefreshFunction refreshFunction = authorization.getRefresh()
            .orElse(
                getDefaultRefreshFunction(
                    authorization.getClientId()
                        .orElse((connectionParameters1, context1) -> getDefaultClientId(connectionParameters1)),
                    authorization.getClientSecret()
                        .orElse((connectionParameters1, context1) -> getDefaultClientSecret(connectionParameters1)),
                    authorization.getRefreshToken()
                        .orElse((connectionParameters1, context1) -> getDefaultRefreshToken(connectionParameters1)),
                    authorization.getRefreshUrl()
                        .orElse(
                            (connectionParameters1, context1) -> getDefaultRefreshUrl(
                                connectionParameters1,
                                authorization.getTokenUrl()
                                    .orElse((connectionParameters2, context2) -> getDefaultTokenUrl(
                                        connectionParameters2)),
                                context1))));

        try {
            if (logger.isTraceEnabled()) {
                logger.trace("Refreshing using {}", FormatUtils.toString(connectionParameters));
            }

            return refreshFunction.apply(ParametersFactory.create(connectionParameters), context);

        } catch (Exception exception) {
            throw new ConfigurationException(
                "Unable to perform oauth token refresh", exception,
                ConnectionDefinitionErrorType.OAUTH_TOKEN_REFRESH_FAILED);
        }
    }

    @Override
    public List<String> getAuthorizationDetectOn(
        String componentName, int connectionVersion, AuthorizationType authorizationType) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationType);

        return authorization.getDetectOn()
            .orElse(List.of());
    }

    @Override
    public List<Object> getAuthorizationRefreshOn(
        String componentName, int connectionVersion, AuthorizationType authorizationType) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationType);

        return authorization.getRefreshOn()
            .orElse(DEFAULT_REFRESH_ON);
    }

    @Override
    public AuthorizationType getAuthorizationType(
        String componentName, int connectionVersion, AuthorizationType authorizationType) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationType);

        return authorization.getType();
    }

    @Override
    public ConnectionDefinition getConnectionConnectionDefinition(String componentName, int connectionVersion) {
        return toConnectionDefinition(
            componentDefinitionRegistry.getComponentDefinitions(componentName)
                .stream()
                .filter(componentDefinition -> componentDefinition.getConnection()
                    .isPresent())
                .filter(componentDefinition -> {
                    com.bytechef.component.definition.ConnectionDefinition connectionDefinition =
                        componentDefinition.getConnection()
                            .orElseThrow();

                    return connectionDefinition.getVersion() == connectionVersion;
                })
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format(
                        "Could not find connection definition for component=%s, version=%d",
                        componentName, connectionVersion))));
    }

    @Override
    public ConnectionDefinition getConnectionDefinition(String componentName, Integer componentVersion) {
        return toConnectionDefinition(
            componentDefinitionRegistry.getComponentDefinition(componentName, componentVersion));
    }

    @Override
    public List<ConnectionDefinition> getConnectionDefinitions() {
        return componentDefinitionRegistry.getComponentDefinitions()
            .stream()
            .filter(componentDefinition -> componentDefinition.getConnection()
                .isPresent())
            .map(ConnectionDefinitionServiceImpl::toConnectionDefinition)
            .toList();
    }

    @Override
    public List<ConnectionDefinition> getConnectionDefinitions(
        String componentName, Integer componentVersion) {

        return getConnectableComponentDefinitions(componentName, componentVersion)
            .stream()
            .map(ConnectionDefinitionServiceImpl::toConnectionDefinition)
            .toList();
    }

    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        String componentName, int connectionVersion, AuthorizationType authorizationType,
        Map<String, ?> connectionParameters) {

        return getOAuth2AuthorizationParameters(
            componentName, connectionVersion, authorizationType, connectionParameters,
            contextFactory.createContext(componentName, null));
    }

    private AuthorizationCallbackResponse executeAuthorizationCallback(
        String componentName, int connectionVersion, AuthorizationType authorizationType,
        Map<String, ?> connectionParameters, Context context, String redirectUri) {

        if (logger.isTraceEnabled()) {
            logger.trace("Executing with {} authorization type.", authorizationType);
        }

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationType);
        String verifier = null;

        if (authorization.getType() == AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE) {
            PkceFunction pkceFunction = authorization.getPkce()
                .orElse(getDefaultPkceFunction());

            // TODO pkce
            Authorization.Pkce pkce;

            try {
                pkce = pkceFunction.apply(null, null, "SHA256", context);
            } catch (Exception e) {
                throw new ConfigurationException(
                    e, ConnectionDefinitionErrorType.AUTHORIZATION_CALLBACK_FAILED);
            }

            verifier = pkce.verifier();
        }

        AuthorizationCallbackFunction authorizationCallbackFunction = authorization.getAuthorizationCallback()
            .orElse(
                getDefaultAuthorizationCallbackFunction(
                    authorization.getClientId()
                        .orElse((connectionParameters1, context1) -> getDefaultClientId(connectionParameters1)),
                    authorization.getClientSecret()
                        .orElse((connectionParameters1, context1) -> getDefaultClientSecret(connectionParameters1)),
                    authorization.getTokenUrl()
                        .orElse((connectionParameters1, context1) -> getDefaultTokenUrl(connectionParameters1))));

        try {
            return authorizationCallbackFunction.apply(
                ParametersFactory.create(connectionParameters),
                MapUtils.getString(connectionParameters, CODE), redirectUri, verifier, context);
        } catch (Exception e) {
            throw new ConfigurationException(
                e, ConnectionDefinitionErrorType.AUTHORIZATION_CALLBACK_FAILED);
        }
    }

    private List<ComponentDefinition> getConnectableComponentDefinitions(
        String componentName, Integer componentVersion) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        if (componentDefinition instanceof ScriptComponentDefinition) {
            return componentDefinitionRegistry.getComponentDefinitions()
                .stream()
                .filter(curComponentDefinition -> curComponentDefinition.getConnection()
                    .isPresent())
                .toList();
        } else {
            return List.of(componentDefinition);
        }
    }

    private static ApplyFunction getDefaultApplyFunction(AuthorizationType type) {
        if (logger.isTraceEnabled()) {
            logger.trace("Resolving applyFunction for {} authorization type.", type);
        }

        return switch (type) {
            case API_KEY -> (Parameters connectionParameters, Context context) -> {
                String addTo = MapUtils.getString(
                    connectionParameters, Authorization.ADD_TO, ApiTokenLocation.HEADER.name());

                if (ApiTokenLocation.valueOf(addTo.toUpperCase()) == ApiTokenLocation.HEADER) {
                    return ApplyResponse.ofHeaders(
                        Map.of(
                            MapUtils.getString(connectionParameters, Authorization.KEY, Authorization.API_TOKEN),
                            List.of(MapUtils.getString(connectionParameters, Authorization.VALUE, ""))));
                } else {
                    return ApplyResponse.ofQueryParameters(
                        Map.of(
                            MapUtils.getString(connectionParameters, Authorization.KEY, Authorization.API_TOKEN),
                            List.of(MapUtils.getString(connectionParameters, Authorization.VALUE, ""))));
                }
            };
            case BASIC_AUTH, DIGEST_AUTH -> (Parameters connectionParameters, Context context) -> {
                String valueToEncode =
                    MapUtils.getString(connectionParameters, Authorization.USERNAME) +
                        ":" + MapUtils.getString(connectionParameters, Authorization.PASSWORD);

                return ApplyResponse.ofHeaders(
                    Map.of(
                        "Authorization",
                        List.of(
                            "Basic " +
                                EncodingUtils.base64EncodeToString(valueToEncode.getBytes(StandardCharsets.UTF_8)))));
            };
            case BEARER_TOKEN -> (Parameters connectionParameters, Context context) -> ApplyResponse.ofHeaders(
                Map.of(
                    Authorization.AUTHORIZATION,
                    List.of(
                        Authorization.BEARER + " " + MapUtils.getString(connectionParameters, Authorization.TOKEN))));
            case CUSTOM -> (Parameters connectionParameters, Context context) -> null;
            case OAUTH2_AUTHORIZATION_CODE, OAUTH2_AUTHORIZATION_CODE_PKCE, OAUTH2_CLIENT_CREDENTIALS,
                OAUTH2_IMPLICIT_CODE, OAUTH2_RESOURCE_OWNER_PASSWORD -> (
                    Parameters connectionParameters, Context context) -> ApplyResponse.ofHeaders(
                        Map.of(
                            Authorization.AUTHORIZATION,
                            List.of(
                                MapUtils.getString(
                                    connectionParameters, Authorization.HEADER_PREFIX,
                                    Authorization.BEARER) +
                                    " " +
                                    MapUtils.getRequiredString(connectionParameters, Authorization.ACCESS_TOKEN))));
        };
    }

    private static AuthorizationCallbackFunction getDefaultAuthorizationCallbackFunction(
        ClientIdFunction clientIdFunction, ClientSecretFunction clientSecretFunction,
        TokenUrlFunction tokenUrlFunction) {

        if (logger.isTraceEnabled()) {
            logger.trace("Executing with clientId, clientSecret, tokenUrl");
        }

        return (connectionParameters, code, redirectUri, codeVerifier, context) -> {
            FormBodyPublisher.Builder builder = FormBodyPublisher.newBuilder();

            builder.query("client_id", clientIdFunction.apply(connectionParameters, context));
            builder.query("client_secret", clientSecretFunction.apply(connectionParameters, context));
            builder.query("code", code);
            builder.query("grant_type", "authorization_code");
            builder.query("redirect_uri", redirectUri);

            if (codeVerifier != null) {
                builder.query("code_verifier", codeVerifier);
            }

            try (HttpClient httpClient = Methanol.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build()) {

                HttpResponse<String> httpResponse = httpClient.send(
                    HttpRequest.newBuilder()
                        .POST(builder.build())
                        .uri(URI.create(tokenUrlFunction.apply(connectionParameters, context)))
                        .header("Accept", "application/json")
                        .build(),
                    HttpResponse.BodyHandlers.ofString());

                if (httpResponse.statusCode() < 200 || httpResponse.statusCode() > 299) {
                    throw new ConfigurationException(
                        httpResponse.body(), ConnectionDefinitionErrorType.INVALID_CLAIM);
                }

                if (httpResponse.body() == null) {
                    throw new ConfigurationException(
                        "Invalid claim", ConnectionDefinitionErrorType.INVALID_CLAIM);
                }

                return new AuthorizationCallbackResponse(JsonUtils.read(httpResponse.body(), new TypeReference<>() {}));
            }
        };
    }

    /**
     * Constructs the default refresh function for OAuth token refresh handling. This function generates a POST request
     * to the OAuth provider's token endpoint using the provided client ID, client secret, refresh token, and grant
     * type. The response is processed to retrieve the new access token, refresh token, and expiration time if
     * available. If the token refresh attempt fails, appropriate exceptions are thrown to handle errors.
     *
     * @param clientIdFunction     a function that retrieves the client ID using the connection parameters and context
     * @param clientSecretFunction a function that retrieves the client secret using the connection parameters and
     *                             context
     * @param refreshTokenFunction a function that retrieves the refresh token using the connection parameters and
     *                             context
     * @param refreshUrlFunction   a function that retrieves the refresh token URL using the connection parameters and
     *                             context
     * @return a {@code RefreshFunction} that executes the defined behavior for refreshing an OAuth token
     * @throws ConfigurationException if the token refresh attempt fails due to HTTP errors or unexpected response
     *                                content
     */
    private static RefreshFunction getDefaultRefreshFunction(
        ClientIdFunction clientIdFunction, ClientSecretFunction clientSecretFunction,
        RefreshTokenFunction refreshTokenFunction, RefreshUrlFunction refreshUrlFunction) {

        if (logger.isTraceEnabled()) {
            logger.trace("Default refresh function using refresh token function {}", refreshTokenFunction);
        }

        return (connectionParameters, context) -> {
            FormBodyPublisher.Builder builder = FormBodyPublisher.newBuilder();

            builder.query("client_id", clientIdFunction.apply(connectionParameters, context));
            builder.query("client_secret", clientSecretFunction.apply(connectionParameters, context));
            builder.query("refresh_token", refreshTokenFunction.apply(connectionParameters, context));
            builder.query("grant_type", "refresh_token");

            try (HttpClient httpClient = Methanol.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build()) {

                HttpResponse<String> httpResponse = httpClient.send(
                    HttpRequest.newBuilder()
                        .POST(builder.build())
                        .uri(URI.create(refreshUrlFunction.apply(connectionParameters, context)))
                        .header("Accept", "application/json")
                        .build(),
                    HttpResponse.BodyHandlers.ofString());

                if (httpResponse.statusCode() < 200 || httpResponse.statusCode() > 299) {
                    throw new ConfigurationException(
                        "OAuth provider rejected token refresh request",
                        ConnectionDefinitionErrorType.TOKEN_REFRESH_FAILED);
                }

                if (httpResponse.body() == null) {
                    throw new ConfigurationException(
                        "Unable to locate access_token, body content misses",
                        ConnectionDefinitionErrorType.INVALID_CLAIM);
                }

                Map<String, Object> responseMap = JsonUtils.read(httpResponse.body(), new TypeReference<>() {});

                return new RefreshTokenResponse(
                    (String) responseMap.get(Authorization.ACCESS_TOKEN),
                    (String) responseMap.get(Authorization.REFRESH_TOKEN),
                    responseMap.containsKey(Authorization.EXPIRES_IN)
                        ? Long.valueOf((Integer) responseMap.get(Authorization.EXPIRES_IN)) : null);
            }
        };
    }

    private static String getDefaultAuthorizationUrl(Parameters connectionParameters) {
        return MapUtils.getString(connectionParameters, Authorization.AUTHORIZATION_URL);
    }

    private static String getDefaultBaseUri(Parameters connectionParameters) {
        return MapUtils.getString(connectionParameters,
            com.bytechef.component.definition.ConnectionDefinition.BASE_URI);
    }

    private static String getDefaultClientId(Parameters connectionParameters) {
        return MapUtils.getRequiredString(connectionParameters, Authorization.CLIENT_ID);
    }

    private static String getDefaultClientSecret(Parameters connectionParameters) {
        return MapUtils.getRequiredString(connectionParameters, Authorization.CLIENT_SECRET);
    }

    private static String getDefaultRefreshToken(Parameters connectionParameters) {
        return MapUtils.getRequiredString(connectionParameters, Authorization.REFRESH_TOKEN);
    }

    private static PkceFunction getDefaultPkceFunction() {
        return (verifier, challenge, challengeMethod, context) -> new Authorization.Pkce(
            verifier, challenge, challengeMethod);
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static String getDefaultRefreshUrl(
        Parameters connectionParameters, TokenUrlFunction tokenUrlFunction, Context context) {

        String refreshUrl = MapUtils.getString(connectionParameters, Authorization.REFRESH_URL);

        if (refreshUrl != null) {
            return refreshUrl;
        }

        try {
            return tokenUrlFunction.apply(connectionParameters, context);
        } catch (Exception e) {
            throw new ConfigurationException(e, ConnectionDefinitionErrorType.TOKEN_REFRESH_FAILED);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> getDefaultScopes(Parameters connectionParameters) {
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

    private static String getDefaultTokenUrl(Parameters connectionParameters) {
        return MapUtils.getString(connectionParameters, Authorization.TOKEN_URL);
    }

    private OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        String componentName, int connectionVersion, AuthorizationType authorizationType,
        Map<String, ?> connectionParameters,
        Context context) {

        if (logger.isTraceEnabled()) {
            logger.trace("Executing with {} authorization type.", authorizationType);
        }

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationType);

        AuthorizationUrlFunction authorizationUrlFunction = authorization.getAuthorizationUrl()
            .orElse((curConnectionParameters, context1) -> getDefaultAuthorizationUrl(curConnectionParameters));
        ClientIdFunction clientIdFunction = authorization.getClientId()
            .orElse((curConnectionParameters, context1) -> getDefaultClientId(curConnectionParameters));
        ScopesFunction scopesFunction = authorization.getScopes()
            .orElse((curConnectionParameters, context1) -> getDefaultScopes(curConnectionParameters));
        OAuth2AuthorizationExtraQueryParametersFunction oAuth2AuthorizationExtraQueryParametersFunction =
            authorization.getOauth2AuthorizationExtraQueryParameters()
                .orElse((curConnectionParameters, context1) -> Map.of());

        Parameters parameters = ParametersFactory.create(connectionParameters);

        try {
            if (logger.isTraceEnabled()) {
                logger.trace("Executing with parameters {}", parameters);
            }

            return new OAuth2AuthorizationParameters(
                authorizationUrlFunction.apply(parameters, context),
                clientIdFunction.apply(parameters, context),
                oAuth2AuthorizationExtraQueryParametersFunction.apply(parameters, context),
                scopesFunction.apply(parameters, context));
        } catch (Exception e) {
            throw new ConfigurationException(
                e, ConnectionDefinitionErrorType.INVALID_OAUTH2_AUTHORIZATION_PARAMETERS);
        }
    }

    private static ConnectionDefinition toConnectionDefinition(
        ComponentDefinition componentDefinition) {

        Optional<String> descriptionOptional = componentDefinition.getDescription();
        Optional<String> titleOptional = componentDefinition.getTitle();

        return new ConnectionDefinition(
            componentDefinition.getConnection()
                .orElseThrow(),
            componentDefinition.getName(),
            titleOptional.orElse(componentDefinition.getName()), descriptionOptional.orElse(null));
    }
}
