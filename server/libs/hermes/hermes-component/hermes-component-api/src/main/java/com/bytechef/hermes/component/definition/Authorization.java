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

import static com.bytechef.hermes.component.constants.ComponentConstants.ACCESS_TOKEN;
import static com.bytechef.hermes.component.constants.ComponentConstants.ADD_TO;
import static com.bytechef.hermes.component.constants.ComponentConstants.API_TOKEN;
import static com.bytechef.hermes.component.constants.ComponentConstants.HEADER_PREFIX;
import static com.bytechef.hermes.component.constants.ComponentConstants.KEY;
import static com.bytechef.hermes.component.constants.ComponentConstants.PASSWORD;
import static com.bytechef.hermes.component.constants.ComponentConstants.TOKEN;
import static com.bytechef.hermes.component.constants.ComponentConstants.USERNAME;
import static com.bytechef.hermes.component.constants.ComponentConstants.VALUE;

import com.bytechef.hermes.component.AuthorizationContext;
import com.bytechef.hermes.component.ConnectionParameters;
import com.bytechef.hermes.component.constants.ComponentConstants;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Ivica Cardic
 */
@Schema(name = "Authorization", description = "Contains information required for a connection's authorization.")
public class Authorization {

    public enum AuthorizationType {
        API_KEY((AuthorizationContext authorizationContext, ConnectionParameters connectionParameters) -> {
            if (ApiTokenLocation.valueOf(StringUtils.upperCase(
                            connectionParameters.getParameter(ADD_TO, ApiTokenLocation.HEADER.name())))
                    == ApiTokenLocation.HEADER) {
                authorizationContext.setHeaders(Map.of(
                        connectionParameters.getParameter(KEY, API_TOKEN),
                        List.of(connectionParameters.getParameter(VALUE, ""))));
            } else {
                authorizationContext.setQueryParameters(Map.of(
                        connectionParameters.getParameter(KEY, API_TOKEN),
                        List.of(connectionParameters.getParameter(VALUE, ""))));
            }
        }),
        BASIC_AUTH((AuthorizationContext authorizationContext, ConnectionParameters connectionParameters) -> {
            authorizationContext.setUsernamePassword(
                    connectionParameters.getParameter(USERNAME), connectionParameters.getParameter(PASSWORD));
        }),
        BEARER_TOKEN((AuthorizationContext authorizationContext, ConnectionParameters connectionParameters) -> {
            authorizationContext.setHeaders(
                    Map.of("Authorization", List.of("Bearer " + connectionParameters.getParameter(TOKEN))));
        }),
        CUSTOM(null),
        DIGEST_AUTH((AuthorizationContext authorizationContext, ConnectionParameters connectionParameters) ->
                authorizationContext.setUsernamePassword(
                        connectionParameters.getParameter(USERNAME), connectionParameters.getParameter(PASSWORD))),
        OAUTH2_AUTHORIZATION_CODE(
                (AuthorizationContext authorizationContext, ConnectionParameters connectionParameters) ->
                        authorizationContext.setHeaders(Map.of(
                                "Authorization",
                                List.of(connectionParameters.getParameter(HEADER_PREFIX, "Bearer") + " "
                                        + connectionParameters.getParameter(ACCESS_TOKEN))))),
        OAUTH2_CLIENT_CREDENTIALS(
                (AuthorizationContext authorizationContext, ConnectionParameters connectionParameters) ->
                        authorizationContext.setHeaders(Map.of(
                                "Authorization",
                                List.of(connectionParameters.getParameter(HEADER_PREFIX, "Bearer") + " "
                                        + connectionParameters.getParameter(ACCESS_TOKEN)))));

        private final BiConsumer<AuthorizationContext, ConnectionParameters> defaultApplyConsumer;

        AuthorizationType(BiConsumer<AuthorizationContext, ConnectionParameters> defaultApplyConsumer) {
            this.defaultApplyConsumer = defaultApplyConsumer;
        }

        public BiConsumer<AuthorizationContext, ConnectionParameters> getDefaultApplyConsumer() {
            return defaultApplyConsumer;
        }
    }

    public enum ApiTokenLocation {
        HEADER,
        QUERY_PARAMS,
    }

    @JsonIgnore
    private BiConsumer<AuthorizationContext, ConnectionParameters> applyConsumer;

    @JsonIgnore
    private BiFunction<ConnectionParameters, String, String> authorizationCallbackFunction;

    @JsonIgnore
    private Function<ConnectionParameters, String> authorizationUrlFunction =
            connectionParameters -> connectionParameters.getParameter(ComponentConstants.AUTHORIZATION_URL);

    @JsonIgnore
    private Function<ConnectionParameters, String> clientIdFunction =
            connectionParameters -> connectionParameters.getParameter(ComponentConstants.CLIENT_ID);

    @JsonIgnore
    private Function<ConnectionParameters, String> clientSecretFunction =
            connectionParameters -> connectionParameters.getParameter(ComponentConstants.CLIENT_SECRET);

    private Display display;
    private String name;

    @JsonIgnore
    private List<Object> onRefresh;

    private List<Property<?>> properties;

    @JsonIgnore
    private Function<ConnectionParameters, String> refreshFunction;

    @JsonIgnore
    private Function<ConnectionParameters, String> refreshUrlFunction =
            connectionParameters -> connectionParameters.getParameter(ComponentConstants.REFRESH_URL);

    @JsonIgnore
    private Function<ConnectionParameters, List<String>> scopes =
            connectionParameters -> connectionParameters.getParameter(ComponentConstants.SCOPES);

    @JsonIgnore
    private Function<ConnectionParameters, String> tokenUrlFunction =
            connectionParameters -> connectionParameters.getParameter(ComponentConstants.TOKEN_URL);

    private final AuthorizationType type;

    public Authorization(String name, AuthorizationType type) {
        this.name = name;
        this.type = type;
        this.applyConsumer = type.getDefaultApplyConsumer();
    }

    public Authorization apply(BiConsumer<AuthorizationContext, ConnectionParameters> applyConsumer) {
        this.applyConsumer = applyConsumer;

        return this;
    }

    public Authorization authorizationCallback(
            BiFunction<ConnectionParameters, String, String> authorizationCallbackFunction) {
        this.authorizationCallbackFunction = authorizationCallbackFunction;

        return this;
    }

    public Authorization authorizationUrl(Function<ConnectionParameters, String> authorizationUrlFunction) {
        this.authorizationUrlFunction = authorizationUrlFunction;

        return this;
    }

    public Authorization clientId(Function<ConnectionParameters, String> clientIdFunction) {
        this.clientIdFunction = clientIdFunction;

        return this;
    }

    public Authorization clientSecret(Function<ConnectionParameters, String> clientSecretFunction) {
        this.clientSecretFunction = clientSecretFunction;

        return this;
    }

    public Authorization display(Display display) {
        this.display = display;

        return this;
    }

    public Authorization onRefresh(List<Object> onRefresh) {
        this.onRefresh = onRefresh;

        return this;
    }

    public Authorization properties(Property... properties) {
        this.properties = List.of(properties);

        return this;
    }

    public Authorization refresh(Function<ConnectionParameters, String> refreshFunction) {
        this.refreshFunction = refreshFunction;

        return this;
    }

    public Authorization refreshUrl(Function<ConnectionParameters, String> refreshUrlFunction) {
        this.refreshUrlFunction = refreshUrlFunction;

        return this;
    }

    public Authorization scopes(Function<ConnectionParameters, List<String>> scopes) {
        this.scopes = scopes;

        return this;
    }

    public Authorization tokenUrl(Function<ConnectionParameters, String> tokenUrlFunction) {
        this.tokenUrlFunction = tokenUrlFunction;

        return this;
    }

    public BiConsumer<AuthorizationContext, ConnectionParameters> getApplyConsumer() {
        return applyConsumer;
    }

    public BiFunction<ConnectionParameters, String, String> getAuthorizationCallbackFunction() {
        return authorizationCallbackFunction;
    }

    public Function<ConnectionParameters, String> getAuthorizationUrlFunction() {
        return authorizationUrlFunction;
    }

    public Function<ConnectionParameters, String> getClientIdFunction() {
        return clientIdFunction;
    }

    public Function<ConnectionParameters, String> getClientSecretFunction() {
        return clientSecretFunction;
    }

    public Display getDisplay() {
        return display;
    }

    @Schema(name = "name", description = "The authorization name.")
    public String getName() {
        return name;
    }

    public List<Object> getOnRefresh() {
        return onRefresh;
    }

    @Schema(name = "properties", description = "Properties of the connection.")
    public List<Property<?>> getProperties() {
        return properties;
    }

    public Function<ConnectionParameters, String> getRefreshFunction() {
        return refreshFunction;
    }

    public Function<ConnectionParameters, String> getRefreshUrlFunction() {
        return refreshUrlFunction;
    }

    public Function<ConnectionParameters, List<String>> getScopes() {
        return scopes;
    }

    public Function<ConnectionParameters, String> getTokenUrlFunction() {
        return tokenUrlFunction;
    }

    @Schema(name = "type", description = "Authorization type.")
    public AuthorizationType getType() {
        return type;
    }
}
