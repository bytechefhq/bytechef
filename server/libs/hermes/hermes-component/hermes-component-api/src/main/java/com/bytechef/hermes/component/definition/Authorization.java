
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
import com.bytechef.hermes.component.Connection;
import com.bytechef.hermes.component.constants.ComponentConstants;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Ivica Cardic
 */
@Schema(name = "Authorization", description = "Contains information required for a connection's authorization.")
public sealed class Authorization permits ComponentDSL.ModifiableAuthorization {

    public enum AuthorizationType {
        API_KEY((AuthorizationContext authorizationContext, Connection connection) -> {
            if (ApiTokenLocation.valueOf(
                StringUtils.upperCase(
                    connection.getParameter(ADD_TO, ApiTokenLocation.HEADER.name()))) == ApiTokenLocation.HEADER) {
                authorizationContext.setHeaders(
                    Map.of(connection.getParameter(KEY, API_TOKEN), List.of(connection.getParameter(VALUE, ""))));
            } else {
                authorizationContext.setQueryParameters(
                    Map.of(connection.getParameter(KEY, API_TOKEN), List.of(connection.getParameter(VALUE, ""))));
            }
        }),
        BASIC_AUTH((AuthorizationContext authorizationContext, Connection connection) -> authorizationContext
            .setUsernamePassword(
                connection.getParameter(USERNAME), connection.getParameter(PASSWORD))),
        BEARER_TOKEN(
            (AuthorizationContext authorizationContext, Connection connection) -> authorizationContext.setHeaders(
                Map.of("Authorization", List.of("Bearer " + connection.getParameter(TOKEN))))),
        CUSTOM(null),
        DIGEST_AUTH((AuthorizationContext authorizationContext, Connection connection) -> authorizationContext
            .setUsernamePassword(
                connection.getParameter(USERNAME), connection.getParameter(PASSWORD))),
        OAUTH2_AUTHORIZATION_CODE(
            (
                AuthorizationContext authorizationContext,
                Connection connection) -> authorizationContext.setHeaders(Map.of(
                    "Authorization",
                    List.of(connection.getParameter(HEADER_PREFIX, "Bearer") + " "
                        + connection.getParameter(ACCESS_TOKEN))))),
        OAUTH2_CLIENT_CREDENTIALS(
            (
                AuthorizationContext authorizationContext,
                Connection connection) -> authorizationContext.setHeaders(Map.of(
                    "Authorization",
                    List.of(connection.getParameter(HEADER_PREFIX, "Bearer") + " "
                        + connection.getParameter(ACCESS_TOKEN)))));

        private final BiConsumer<AuthorizationContext, Connection> defaultApplyConsumer;

        AuthorizationType(BiConsumer<AuthorizationContext, Connection> defaultApplyConsumer) {
            this.defaultApplyConsumer = defaultApplyConsumer;
        }

        public BiConsumer<AuthorizationContext, Connection> getDefaultApplyConsumer() {
            return defaultApplyConsumer;
        }
    }

    public enum ApiTokenLocation {
        HEADER,
        QUERY_PARAMETERS,
    }

    @JsonIgnore
    protected BiConsumer<AuthorizationContext, Connection> applyConsumer;

    @JsonIgnore
    protected BiFunction<Connection, String, String> authorizationCallbackFunction;

    @JsonIgnore
    protected Function<Connection, String> authorizationUrlFunction = connectionParameters -> connectionParameters
        .getParameter(ComponentConstants.AUTHORIZATION_URL);

    @JsonIgnore
    protected Function<Connection, String> clientIdFunction = connectionParameters -> connectionParameters
        .getParameter(ComponentConstants.CLIENT_ID);

    @JsonIgnore
    protected Function<Connection, String> clientSecretFunction = connectionParameters -> connectionParameters
        .getParameter(ComponentConstants.CLIENT_SECRET);

    protected Display display;

    @JsonIgnore
    protected List<Object> onRefresh;

    protected List<Property<?>> properties;

    @JsonIgnore
    protected Function<Connection, String> refreshFunction;

    @JsonIgnore
    protected Function<Connection, String> refreshUrlFunction = connectionParameters -> connectionParameters
        .getParameter(ComponentConstants.REFRESH_URL);

    @JsonIgnore
    protected Function<Connection, List<String>> scopesFunction = connectionParameters -> connectionParameters
        .getParameter(ComponentConstants.SCOPES);

    @JsonIgnore
    protected Function<Connection, String> tokenUrlFunction = connectionParameters -> connectionParameters
        .getParameter(ComponentConstants.TOKEN_URL);

    private final String name;
    private final AuthorizationType type;

    protected Authorization(String name, AuthorizationType type) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.applyConsumer = type.getDefaultApplyConsumer();
    }

    public BiConsumer<AuthorizationContext, Connection> getApplyConsumer() {
        return applyConsumer;
    }

    public Optional<BiFunction<Connection, String, String>> getAuthorizationCallbackFunction() {
        return Optional.ofNullable(authorizationCallbackFunction);
    }

    public Function<Connection, String> getAuthorizationUrlFunction() {
        return authorizationUrlFunction;
    }

    public Function<Connection, String> getClientIdFunction() {
        return clientIdFunction;
    }

    public Function<Connection, String> getClientSecretFunction() {
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

    public Optional<Function<Connection, String>> getRefreshFunction() {
        return Optional.ofNullable(refreshFunction);
    }

    public Function<Connection, String> getRefreshUrlFunction() {
        return refreshUrlFunction;
    }

    public Function<Connection, List<String>> getScopesFunction() {
        return scopesFunction;
    }

    public Function<Connection, String> getTokenUrlFunction() {
        return tokenUrlFunction;
    }

    @Schema(name = "type", description = "Authorization type.")
    public AuthorizationType getType() {
        return type;
    }
}
