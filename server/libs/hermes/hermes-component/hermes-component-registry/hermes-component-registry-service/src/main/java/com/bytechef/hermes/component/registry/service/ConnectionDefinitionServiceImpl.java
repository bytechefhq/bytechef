
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

package com.bytechef.hermes.component.registry.service;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.ApiTokenLocation;
import com.bytechef.hermes.component.definition.Authorization.ApplyFunction;
import com.bytechef.hermes.component.definition.Authorization.ApplyResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackFunction;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationUrlFunction;
import com.bytechef.hermes.component.definition.Authorization.ClientIdFunction;
import com.bytechef.hermes.component.definition.Authorization.PkceFunction;
import com.bytechef.hermes.component.definition.Authorization.ScopesFunction;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.definition.constant.AuthorizationConstants;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.registry.ComponentDefinitionRegistry;
import com.bytechef.hermes.component.registry.domain.ConnectionDefinition;
import com.bytechef.hermes.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.hermes.component.registry.dto.ComponentConnection;
import com.bytechef.hermes.component.definition.ParameterMapImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.Methanol;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

import static com.bytechef.hermes.component.definition.ConnectionDefinition.BaseUriFunction;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.CODE;

/**
 * @author Ivica Cardic
 */
@Service("connectionDefinitionService")
public class ConnectionDefinitionServiceImpl implements ConnectionDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public ConnectionDefinitionServiceImpl(
        ComponentDefinitionRegistry componentDefinitionRegistry, ObjectMapper objectMapper) {

        this.componentDefinitionRegistry = componentDefinitionRegistry;
        this.objectMapper = objectMapper;
    }

    public static ApplyFunction getDefaultApply(AuthorizationType type) {
        return switch (type) {
            case API_KEY -> (ParameterMap connectionParameters, Context context) -> {
                String addTo = MapUtils.getString(
                    connectionParameters, AuthorizationConstants.ADD_TO, ApiTokenLocation.HEADER.name());

                if (ApiTokenLocation.valueOf(addTo.toUpperCase()) == ApiTokenLocation.HEADER) {
                    return ApplyResponse.ofHeaders(
                        Map.of(
                            MapUtils.getString(
                                connectionParameters, AuthorizationConstants.KEY, AuthorizationConstants.API_TOKEN),
                            List.of(
                                MapUtils.getString(connectionParameters, AuthorizationConstants.VALUE, ""))));
                } else {
                    return ApplyResponse.ofQueryParameters(
                        Map.of(
                            MapUtils.getString(
                                connectionParameters, AuthorizationConstants.KEY, AuthorizationConstants.API_TOKEN),
                            List.of(
                                MapUtils.getString(connectionParameters, AuthorizationConstants.VALUE, ""))));
                }
            };
            case BASIC_AUTH, DIGEST_AUTH -> (ParameterMap connectionParameters, Context context) -> {
                String valueToEncode =
                    MapUtils.getString(connectionParameters, AuthorizationConstants.USERNAME) +
                        ":" +
                        MapUtils.getString(connectionParameters, AuthorizationConstants.PASSWORD);

                return ApplyResponse.ofHeaders(
                    Map.of(
                        "Authorization",
                        List.of(
                            "Basic " +
                                EncodingUtils.encodeBase64ToString(valueToEncode.getBytes(StandardCharsets.UTF_8)))));
            };
            case BEARER_TOKEN -> (ParameterMap connectionParameters, Context context) -> ApplyResponse.ofHeaders(
                Map.of(
                    AuthorizationConstants.AUTHORIZATION,
                    List.of(
                        AuthorizationConstants.BEARER + " " +
                            MapUtils.getString(connectionParameters, AuthorizationConstants.TOKEN))));
            case CUSTOM -> (ParameterMap connectionParameters, Context context) -> null;
            case OAUTH2_AUTHORIZATION_CODE, OAUTH2_AUTHORIZATION_CODE_PKCE, OAUTH2_CLIENT_CREDENTIALS,
                OAUTH2_IMPLICIT_CODE, OAUTH2_RESOURCE_OWNER_PASSWORD -> (
                    ParameterMap connectionParameters, Context context) -> ApplyResponse.ofHeaders(
                        Map.of(
                            AuthorizationConstants.AUTHORIZATION,
                            List.of(
                                MapUtils.getString(
                                    connectionParameters, AuthorizationConstants.HEADER_PREFIX,
                                    AuthorizationConstants.BEARER) +
                                    " " +
                                    MapUtils.getString(
                                        connectionParameters, AuthorizationConstants.ACCESS_TOKEN))));
        };
    }

    public static AuthorizationCallbackFunction getDefaultAuthorizationCallbackFunction(
        ClientIdFunction clientIdFunction, Authorization.ClientSecretFunction clientSecretFunction,
        Authorization.TokenUrlFunction tokenUrlFunction, ObjectMapper objectMapper) {

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

            HttpClient httpClient = Methanol.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
            HttpResponse<String> httpResponse;

            try {
                httpResponse = httpClient.send(
                    HttpRequest.newBuilder()
                        .POST(builder.build())
                        .uri(URI.create(tokenUrlFunction.apply(connectionParameters, context)))
                        .build(),
                    HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (httpResponse.statusCode() != 200) {
                throw new ComponentExecutionException("Invalid claim");
            }

            if (httpResponse.body() == null) {
                throw new ComponentExecutionException("Invalid claim");
            }

            Map<?, ?> body = JsonUtils.read(httpResponse.body(), Map.class, objectMapper);

            return new AuthorizationCallbackResponse(
                (String) body.get(AuthorizationConstants.ACCESS_TOKEN),
                (String) body.get(AuthorizationConstants.REFRESH_TOKEN));
        };
    }

    public static String getDefaultAuthorizationUrl(ParameterMap connectionParameters) {
        return MapUtils.getString(connectionParameters, AuthorizationConstants.AUTHORIZATION_URL);
    }

    public static String getDefaultBaseUri(ParameterMap connectionParameters) {
        return MapUtils.getString(connectionParameters,
            com.bytechef.hermes.component.definition.ConnectionDefinition.BASE_URI);
    }

    public static String getDefaultClientId(ParameterMap connectionParameters) {
        return MapUtils.getString(connectionParameters, AuthorizationConstants.CLIENT_ID);
    }

    public static String getDefaultClientSecret(ParameterMap connectionParameters) {
        return MapUtils.getString(connectionParameters, AuthorizationConstants.CLIENT_SECRET);
    }

    public static PkceFunction getDefaultPkce() {
        return (verifier, challenge, challengeMethod, context) -> new Authorization.Pkce(verifier, challenge,
            challengeMethod);
    }

    public static String getDefaultRefreshUrl(
        ParameterMap connectionParameters, Authorization.TokenUrlFunction tokenUrlFunction, Context context) {

        String refreshUrl = MapUtils.getString(connectionParameters, AuthorizationConstants.REFRESH_URL);

        if (refreshUrl == null) {
            refreshUrl = tokenUrlFunction.apply(connectionParameters, context);
        }

        return refreshUrl;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getDefaultScopes(ParameterMap connectionParameters) {
        Object scopes = MapUtils.get(connectionParameters, AuthorizationConstants.SCOPES);

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

    public static String getDefaultTokenUrl(ParameterMap connectionParameters) {
        return MapUtils.getString(connectionParameters, AuthorizationConstants.TOKEN_URL);
    }

    @Override
    public boolean connectionExists(String componentName, int connectionVersion) {
        return componentDefinitionRegistry.getComponentDefinitions()
            .stream()
            .map(ComponentDefinition::getConnection)
            .flatMap(Optional::stream)
            .anyMatch(connectionDefinition -> componentName.equalsIgnoreCase(connectionDefinition.getComponentName()) &&
                connectionDefinition.getVersion() == connectionVersion);
    }

    @Override
    public ApplyResponse executeAuthorizationApply(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connection.version(), connection.authorizationName());

        ApplyFunction applyFunction = OptionalUtils.orElse(
            authorization.getApply(), getDefaultApply(authorization.getType()));

        return applyFunction.apply(new ParameterMapImpl(connection.parameters()), context);
    }

    @Override
    public AuthorizationCallbackResponse executeAuthorizationCallback(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context,
        @NonNull String redirectUri) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connection.version(), connection.authorizationName());
        String verifier = null;

        if (authorization.getType() == AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE) {
            PkceFunction pkceFunction = OptionalUtils.orElse(
                authorization.getPkce(), getDefaultPkce());

            // TODO pkce
            Authorization.Pkce pkce = pkceFunction.apply(null, null, "SHA256", context);

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
                        connectionParameters)),
                objectMapper));

        return authorizationCallbackFunction.apply(
            new ParameterMapImpl(connection.parameters()), MapUtils.getString(connection.parameters(), CODE),
            redirectUri, verifier, context);
    }

    @Override
    public Optional<String> executeBaseUri(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

        com.bytechef.hermes.component.definition.ConnectionDefinition connectionDefinition =
            componentDefinitionRegistry.getConnectionDefinition(componentName, connection.version());

        BaseUriFunction baseUriFunction =
            OptionalUtils.orElse(
                connectionDefinition.getBaseUri(),
                (connectionParameters, context1) -> getDefaultBaseUri(connectionParameters));

        return Optional.ofNullable(
            baseUriFunction.apply(
                new ParameterMapImpl(
                    connection.parameters()),
                context));
    }

    @Override
    public AuthorizationType getAuthorizationType(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationName);

        return authorization.getType();
    }

    @Override
    public ConnectionDefinition getConnectionDefinition(
        @NonNull String componentName, int componentVersion) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        return new ConnectionDefinition(OptionalUtils.get(componentDefinition.getConnection()));
    }

    @Override
    public List<ConnectionDefinition> getConnectionDefinitions() {
        return componentDefinitionRegistry.getComponentDefinitions()
            .stream()
            .filter(componentDefinition -> OptionalUtils.isPresent(componentDefinition.getConnection()))
            .map(componentDefinition -> new ConnectionDefinition(
                OptionalUtils.get(componentDefinition.getConnection())))
            .toList();
    }

    @Override
    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connection.version(), connection.authorizationName());

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

        ParameterMapImpl connectionParameters = new ParameterMapImpl(connection.parameters());

        return new OAuth2AuthorizationParameters(
            authorizationUrlFunction.apply(connectionParameters, context),
            clientIdFunction.apply(connectionParameters, context), scopesFunction.apply(connectionParameters, context));
    }

    @Override
    public List<ConnectionDefinition>
        getConnectionDefinitions(@NonNull String componentName, @NonNull Integer componentVersion) {
        return componentDefinitionRegistry.getConnectionDefinitions(componentName, componentVersion)
            .stream()
            .map(ConnectionDefinition::new)
            .toList();
    }
}
