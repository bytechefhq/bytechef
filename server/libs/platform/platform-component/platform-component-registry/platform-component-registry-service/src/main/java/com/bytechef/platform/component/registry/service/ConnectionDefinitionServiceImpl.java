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

package com.bytechef.platform.component.registry.service;

import static com.bytechef.component.definition.Authorization.CODE;
import static com.bytechef.component.definition.ConnectionDefinition.BaseUriFunction;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
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
import com.bytechef.component.definition.Authorization.PkceFunction;
import com.bytechef.component.definition.Authorization.RefreshUrlFunction;
import com.bytechef.component.definition.Authorization.ScopesFunction;
import com.bytechef.component.definition.Authorization.TokenUrlFunction;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ScriptComponentDefinition;
import com.bytechef.platform.component.exception.ComponentExecutionException;
import com.bytechef.platform.component.registry.ComponentDefinitionRegistry;
import com.bytechef.platform.component.registry.constant.ConnectionDefinitionErrorType;
import com.bytechef.platform.component.registry.definition.ParametersImpl;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.ConnectionDefinition;
import com.bytechef.platform.component.registry.domain.OAuth2AuthorizationParameters;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.Methanol;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service("connectionDefinitionService")
public class ConnectionDefinitionServiceImpl implements ConnectionDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;

    @SuppressFBWarnings("EI2")
    public ConnectionDefinitionServiceImpl(ComponentDefinitionRegistry componentDefinitionRegistry) {
        this.componentDefinitionRegistry = componentDefinitionRegistry;
    }

    @Override
    public ApplyResponse executeAuthorizationApply(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connection.authorizationName());

        ApplyFunction applyFunction = OptionalUtils.orElse(
            authorization.getApply(), getDefaultApply(authorization.getType()));

        try {
            return applyFunction.apply(new ParametersImpl(connection.parameters()), context);
        } catch (Exception e) {
            throw new ComponentExecutionException(e, ConnectionDefinitionErrorType.EXECUTE_AUTHORIZATION_APPLY);
        }
    }

    @Override
    public AuthorizationCallbackResponse executeAuthorizationCallback(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context,
        @NonNull String redirectUri) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connection.authorizationName());
        String verifier = null;

        if (authorization.getType() == AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE) {
            PkceFunction pkceFunction = OptionalUtils.orElse(
                authorization.getPkce(), getDefaultPkce());

            // TODO pkce
            Authorization.Pkce pkce;

            try {
                pkce = pkceFunction.apply(null, null, "SHA256", context);
            } catch (Exception e) {
                throw new ComponentExecutionException(e, ConnectionDefinitionErrorType.EXECUTE_AUTHORIZATION_CALLBACK);
            }

            verifier = pkce.verifier();
        }

        AuthorizationCallbackFunction authorizationCallbackFunction = OptionalUtils.orElse(
            authorization.getAuthorizationCallback(),
            getDefaultAuthorizationCallbackFunction(
                OptionalUtils.orElse(
                    authorization.getClientId(),
                    (connectionParameters, context1) -> getDefaultClientId(
                        connectionParameters)),
                OptionalUtils.orElse(
                    authorization.getClientSecret(),
                    (connectionParameters, context1) -> getDefaultClientSecret(
                        connectionParameters)),
                OptionalUtils.orElse(
                    authorization.getTokenUrl(),
                    (connectionParameters, context1) -> getDefaultTokenUrl(
                        connectionParameters))));

        try {
            return authorizationCallbackFunction.apply(
                new ParametersImpl(connection.parameters()), MapUtils.getString(connection.parameters(), CODE),
                redirectUri, verifier, context);
        } catch (Exception e) {
            throw new ComponentExecutionException(e, ConnectionDefinitionErrorType.EXECUTE_AUTHORIZATION_CALLBACK);
        }
    }

    @Override
    public Optional<String> executeBaseUri(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

        com.bytechef.component.definition.ConnectionDefinition connectionDefinition =
            componentDefinitionRegistry.getConnectionDefinition(componentName);

        BaseUriFunction baseUriFunction =
            OptionalUtils.orElse(
                connectionDefinition.getBaseUri(),
                (connectionParameters, context1) -> getDefaultBaseUri(connectionParameters));

        return Optional.ofNullable(baseUriFunction.apply(new ParametersImpl(connection.parameters()), context));
    }

    @Override
    public AuthorizationType getAuthorizationType(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, authorizationName);

        return authorization.getType();
    }

    @Override
    public ConnectionDefinition getConnectionDefinition(@NonNull String componentName, int componentVersion) {
        return toConnectionDefinition(
            componentDefinitionRegistry.getComponentDefinition(componentName, componentVersion));
    }

    @Override
    public List<ConnectionDefinition> getConnectionDefinitions() {
        return componentDefinitionRegistry.getComponentDefinitions()
            .stream()
            .filter(componentDefinition -> OptionalUtils.isPresent(componentDefinition.getConnection()))
            .map(ConnectionDefinitionServiceImpl::toConnectionDefinition)
            .toList();
    }

    @Override
    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connection.authorizationName());

        AuthorizationUrlFunction authorizationUrlFunction = OptionalUtils.orElse(
            authorization.getAuthorizationUrl(),
            (connectionParameters, context1) -> getDefaultAuthorizationUrl(
                connectionParameters));
        ClientIdFunction clientIdFunction = OptionalUtils.orElse(
            authorization.getClientId(),
            (connectionParameters, context1) -> getDefaultClientId(connectionParameters));
        ScopesFunction scopesFunction = OptionalUtils.orElse(
            authorization.getScopes(),
            (connectionParameters, context1) -> getDefaultScopes(connectionParameters));

        ParametersImpl connectionParameters = new ParametersImpl(connection.parameters());

        try {
            return new OAuth2AuthorizationParameters(
                authorizationUrlFunction.apply(connectionParameters, context),
                clientIdFunction.apply(connectionParameters, context),
                scopesFunction.apply(connectionParameters, context));
        } catch (Exception e) {
            throw new ComponentExecutionException(e, ConnectionDefinitionErrorType.GET_OAUTH2_AUTHORIZATION_PARAMETERS);
        }
    }

    @Override
    public List<ConnectionDefinition> getConnectionDefinitions(
        @NonNull String componentName, @NonNull Integer componentVersion) {

        return getConnectionComponentDefinitions(componentName, componentVersion)
            .stream()
            .map(ConnectionDefinitionServiceImpl::toConnectionDefinition)
            .toList();
    }

    private List<ComponentDefinition> getConnectionComponentDefinitions(
        String componentName, int componentVersion) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        if (componentDefinition instanceof ScriptComponentDefinition) {
            return componentDefinitionRegistry
                .getComponentDefinitions()
                .stream()
                .filter(curComponentDefinition -> curComponentDefinition
                    .getConnection()
                    .isPresent())
                .toList();
        } else {
            return List.of(componentDefinition);
        }
    }

    private static ApplyFunction getDefaultApply(AuthorizationType type) {
        return switch (type) {
            case API_KEY -> (Parameters connectionParameters, Context context) -> {
                String addTo = MapUtils.getString(
                    connectionParameters, Authorization.ADD_TO, ApiTokenLocation.HEADER.name());

                if (ApiTokenLocation.valueOf(addTo.toUpperCase()) == ApiTokenLocation.HEADER) {
                    return ApplyResponse.ofHeaders(
                        Map.of(
                            MapUtils.getString(
                                connectionParameters, Authorization.KEY, Authorization.API_TOKEN),
                            List.of(
                                MapUtils.getString(connectionParameters, Authorization.VALUE, ""))));
                } else {
                    return ApplyResponse.ofQueryParameters(
                        Map.of(
                            MapUtils.getString(
                                connectionParameters, Authorization.KEY, Authorization.API_TOKEN),
                            List.of(
                                MapUtils.getString(connectionParameters, Authorization.VALUE, ""))));
                }
            };
            case BASIC_AUTH, DIGEST_AUTH -> (Parameters connectionParameters, Context context) -> {
                String valueToEncode =
                    MapUtils.getString(connectionParameters, Authorization.USERNAME) +
                        ":" +
                        MapUtils.getString(connectionParameters, Authorization.PASSWORD);

                return ApplyResponse.ofHeaders(
                    Map.of(
                        "Authorization",
                        List.of(
                            "Basic " +
                                EncodingUtils.encodeBase64ToString(valueToEncode.getBytes(StandardCharsets.UTF_8)))));
            };
            case BEARER_TOKEN -> (Parameters connectionParameters, Context context) -> ApplyResponse.ofHeaders(
                Map.of(
                    Authorization.AUTHORIZATION,
                    List.of(
                        Authorization.BEARER + " " +
                            MapUtils.getString(connectionParameters, Authorization.TOKEN))));
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
                                    MapUtils.getString(
                                        connectionParameters, Authorization.ACCESS_TOKEN))));
        };
    }

    private static AuthorizationCallbackFunction getDefaultAuthorizationCallbackFunction(
        ClientIdFunction clientIdFunction, ClientSecretFunction clientSecretFunction,
        TokenUrlFunction tokenUrlFunction) {

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
                        .build(),
                    HttpResponse.BodyHandlers.ofString());

                if (httpResponse.statusCode() < 200 || httpResponse.statusCode() > 299) {
                    throw new ComponentExecutionException(
                        "Invalid claim", ConnectionDefinitionErrorType.GET_DEFAULT_AUTHORIZATION_CALLBACK_FUNCTION);
                }

                if (httpResponse.body() == null) {
                    throw new ComponentExecutionException(
                        "Invalid claim", ConnectionDefinitionErrorType.GET_DEFAULT_AUTHORIZATION_CALLBACK_FUNCTION);
                }

                return new AuthorizationCallbackResponse(JsonUtils.read(httpResponse.body(), new TypeReference<>() {}));
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
        return MapUtils.getString(connectionParameters, Authorization.CLIENT_ID);
    }

    private static String getDefaultClientSecret(Parameters connectionParameters) {
        return MapUtils.getString(connectionParameters, Authorization.CLIENT_SECRET);
    }

    private static PkceFunction getDefaultPkce() {
        return (verifier, challenge, challengeMethod, context) -> new Authorization.Pkce(verifier, challenge,
            challengeMethod);
    }

    private static String getDefaultRefreshUrl(
        Parameters connectionParameters, RefreshUrlFunction refreshUrlFunction, TokenUrlFunction tokenUrlFunction,
        Context context) {

        String refreshUrl = MapUtils.getString(connectionParameters, Authorization.REFRESH_URL);

        if (refreshUrl == null) {
            if (refreshUrlFunction == null) {

                try {
                    refreshUrl = tokenUrlFunction.apply(connectionParameters, context);
                } catch (Exception e) {
                    throw new ComponentExecutionException(e, ConnectionDefinitionErrorType.GET_DEFAULT_REFRESH_URL);
                }
            } else {
                try {
                    refreshUrl = refreshUrlFunction.apply(connectionParameters, context);
                } catch (Exception e) {
                    throw new ComponentExecutionException(e, ConnectionDefinitionErrorType.GET_DEFAULT_REFRESH_URL);
                }
            }
        }

        return refreshUrl;
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

    private static ConnectionDefinition toConnectionDefinition(
        ComponentDefinition componentDefinition) {

        Optional<String> descriptionOptional = componentDefinition.getDescription();
        Optional<String> titleOptional = componentDefinition.getTitle();

        return new ConnectionDefinition(
            OptionalUtils.get(componentDefinition.getConnection()), componentDefinition.getName(),
            titleOptional.orElse(componentDefinition.getName()), descriptionOptional.orElse(null));
    }
}
