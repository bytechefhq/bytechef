
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
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Ivica Cardic
 */
@Schema(name = "Authorization", description = "Contains information required for a connection's authorization.")
public sealed interface Authorization permits ComponentDSL.ModifiableAuthorization {

    enum AuthorizationType {
        API_KEY((AuthorizationContext authorizationContext, Connection connection) -> {
            String addTo = connection.getParameter(ADD_TO, ApiTokenLocation.HEADER.name());

            if (ApiTokenLocation.valueOf(addTo.toUpperCase()) == ApiTokenLocation.HEADER) {
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

    enum ApiTokenLocation {
        HEADER,
        QUERY_PARAMETERS,
    }

    BiConsumer<AuthorizationContext, Connection> getApplyConsumer();

    Optional<BiFunction<Connection, String, String>> getAuthorizationCallbackFunction();

    Function<Connection, String> getAuthorizationUrlFunction();

    Function<Connection, String> getClientIdFunction();

    Function<Connection, String> getClientSecretFunction();

    Display getDisplay();

    @Schema(name = "name", description = "The authorization name.")
    String getName();

    List<Object> getOnRefresh();

    @Schema(name = "properties", description = "Properties of the connection.")
    List<Property<?>> getProperties();

    Optional<Function<Connection, String>> getRefreshFunction();

    Function<Connection, String> getRefreshUrlFunction();

    Function<Connection, List<String>> getScopesFunction();

    Function<Connection, String> getTokenUrlFunction();

    @Schema(name = "type", description = "Authorization type.")
    AuthorizationType getType();
}
