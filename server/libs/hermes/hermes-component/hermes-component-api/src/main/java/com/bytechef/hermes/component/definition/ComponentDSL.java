
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

import com.bytechef.hermes.component.AuthorizationContext;
import com.bytechef.hermes.component.Connection;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.constants.ComponentConstants;
import com.bytechef.hermes.definition.DefinitionDSL;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Resources;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.bytechef.hermes.component.constants.ComponentConstants.BASE_URI;

/**
 * @author Ivica Cardic
 */
public final class ComponentDSL extends DefinitionDSL {

    public static ModifiableActionDefinition action(String name) {
        return new ModifiableActionDefinition(name);
    }

    public static ModifiableAuthorization authorization(
        String name, Authorization.AuthorizationType authorizationType) {
        return new ModifiableAuthorization(name, authorizationType);
    }

    public static ModifiableComponentDefinition component(String name) {
        return new ModifiableComponentDefinition(name);
    }

    public static ModifiableConnectionDefinition connection() {
        return new ModifiableConnectionDefinition();
    }

    public static ModifiableProperty.ModifiableObjectProperty fileEntry() {
        return fileEntry(null);
    }

    public static ModifiableProperty.ModifiableObjectProperty fileEntry(String name) {
        return buildObject(
            name,
            null,
            "FILE_ENTRY",
            string("extension").required(true),
            string("mimeType").required(true),
            string("name").required(true),
            string("url").required(true));
    }

    public static ModifiableJdbcComponentDefinition jdbcComponent(String name) {
        return new ModifiableJdbcComponentDefinition(name);
    }

    public static final class ModifiableActionDefinition implements ActionDefinition {

        private Display display;
        private Object exampleOutput;
        private Map<String, Object> metadata;
        private String name;
        private List<Property<? extends Property<?>>> output;
        private List<Property<?>> properties;

        @JsonIgnore
        private BiFunction<Context, ExecutionParameters, Object> performFunction;

        private ModifiableActionDefinition() {
        }

        private ModifiableActionDefinition(String name) {
            this.name = Objects.requireNonNull(name);
        }

        public ModifiableActionDefinition display(ModifiableDisplay display) {
            this.display = display;

            return this;
        }

        public ModifiableActionDefinition exampleOutput(Object exampleOutput) {
            this.exampleOutput = exampleOutput;

            return this;
        }

        public ModifiableActionDefinition metadata(String key, String value) {
            if (metadata == null) {
                metadata = new HashMap<>();
            }

            this.metadata.put(key, value);

            return this;
        }

        @SuppressFBWarnings("EI2")
        public ModifiableActionDefinition metadata(Map<String, Object> metadata) {
            this.metadata = metadata;

            return this;
        }

        public <P extends Property<?>> ModifiableActionDefinition output(P... output) {
            if (output != null) {
                this.output = List.of(output);
            }

            return this;
        }

        public ModifiableActionDefinition perform(BiFunction<Context, ExecutionParameters, Object> performFunction) {
            this.performFunction = performFunction;

            return this;
        }

        public <P extends Property<?>> ModifiableActionDefinition properties(P... properties) {
            this.properties = List.of(properties);

            return this;
        }

        public Display getDisplay() {
            return display;
        }

        @Override
        public Object getExampleOutput() {
            return exampleOutput;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return metadata == null ? null : new HashMap<>(metadata);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<Property<? extends Property<?>>> getOutput() {
            return output;
        }

        @Override
        public List<Property<?>> getProperties() {
            return properties;
        }

        @Override
        public Optional<BiFunction<Context, ExecutionParameters, Object>> getPerformFunction() {
            return Optional.ofNullable(performFunction);
        }
    }

    public static final class ModifiableAuthorization implements Authorization {

        @JsonIgnore
        private BiConsumer<AuthorizationContext, Connection> applyConsumer;

        @JsonIgnore
        private BiFunction<Connection, String, String> authorizationCallbackFunction;

        @JsonIgnore
        private Function<Connection, String> authorizationUrlFunction = connectionParameters -> connectionParameters
            .getParameter(ComponentConstants.AUTHORIZATION_URL);

        @JsonIgnore
        private Function<Connection, String> clientIdFunction = connectionParameters -> connectionParameters
            .getParameter(ComponentConstants.CLIENT_ID);

        @JsonIgnore
        private Function<Connection, String> clientSecretFunction = connectionParameters -> connectionParameters
            .getParameter(ComponentConstants.CLIENT_SECRET);

        private Display display;

        @JsonIgnore
        private List<Object> onRefresh;

        private List<Property<?>> properties;

        @JsonIgnore
        private Function<Connection, String> refreshFunction;

        @JsonIgnore
        private Function<Connection, String> refreshUrlFunction = connectionParameters -> connectionParameters
            .getParameter(ComponentConstants.REFRESH_URL);

        @JsonIgnore
        private Function<Connection, List<String>> scopesFunction = connectionParameters -> connectionParameters
            .getParameter(ComponentConstants.SCOPES);

        @JsonIgnore
        private Function<Connection, String> tokenUrlFunction = connectionParameters -> connectionParameters
            .getParameter(ComponentConstants.TOKEN_URL);

        private String name;
        private AuthorizationType type;

        private ModifiableAuthorization() {
        }

        private ModifiableAuthorization(String name, AuthorizationType type) {
            this.name = Objects.requireNonNull(name);
            this.type = Objects.requireNonNull(type);
            this.applyConsumer = type.getDefaultApplyConsumer();
        }

        public ModifiableAuthorization apply(BiConsumer<AuthorizationContext, Connection> applyConsumer) {
            if (applyConsumer != null) {
                this.applyConsumer = applyConsumer;
            }

            return this;
        }

        public ModifiableAuthorization authorizationCallback(
            BiFunction<Connection, String, String> authorizationCallbackFunction) {
            this.authorizationCallbackFunction = authorizationCallbackFunction;

            return this;
        }

        public ModifiableAuthorization authorizationUrl(Function<Connection, String> authorizationUrlFunction) {
            if (authorizationUrlFunction != null) {
                this.authorizationUrlFunction = authorizationUrlFunction;
            }

            return this;
        }

        public ModifiableAuthorization clientId(Function<Connection, String> clientIdFunction) {
            if (clientIdFunction != null) {
                this.clientIdFunction = clientIdFunction;
            }

            return this;
        }

        public ModifiableAuthorization clientSecret(Function<Connection, String> clientSecretFunction) {
            if (clientSecretFunction != null) {
                this.clientSecretFunction = clientSecretFunction;
            }

            return this;
        }

        public ModifiableAuthorization display(ModifiableDisplay display) {
            this.display = display;

            return this;
        }

        public ModifiableAuthorization onRefresh(Object... onRefresh) {
            if (onRefresh != null) {
                this.onRefresh = List.of(onRefresh);
            }

            return this;
        }

        public <P extends Property<?>> ModifiableAuthorization properties(P... properties) {
            if (properties != null) {
                this.properties = List.of(properties);
            }

            return this;
        }

        public ModifiableAuthorization refresh(Function<Connection, String> refreshFunction) {
            this.refreshFunction = refreshFunction;

            return this;
        }

        public ModifiableAuthorization refreshUrl(Function<Connection, String> refreshUrlFunction) {
            if (refreshUrlFunction != null) {
                this.refreshUrlFunction = refreshUrlFunction;
            }

            return this;
        }

        public ModifiableAuthorization scopes(Function<Connection, List<String>> scopesFunction) {
            if (scopesFunction != null) {
                this.scopesFunction = scopesFunction;
            }

            return this;
        }

        public ModifiableAuthorization tokenUrl(Function<Connection, String> tokenUrlFunction) {
            if (tokenUrlFunction != null) {
                this.tokenUrlFunction = tokenUrlFunction;
            }

            return this;
        }

        @Override
        public BiConsumer<AuthorizationContext, Connection> getApplyConsumer() {
            return applyConsumer;
        }

        @Override
        public Optional<BiFunction<Connection, String, String>> getAuthorizationCallbackFunction() {
            return Optional.ofNullable(authorizationCallbackFunction);
        }

        @Override
        public Function<Connection, String> getAuthorizationUrlFunction() {
            return authorizationUrlFunction;
        }

        @Override
        public Function<Connection, String> getClientIdFunction() {
            return clientIdFunction;
        }

        @Override
        public Function<Connection, String> getClientSecretFunction() {
            return clientSecretFunction;
        }

        @Override
        public Display getDisplay() {
            return display;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<Object> getOnRefresh() {
            return onRefresh;
        }

        @Override
        public List<Property<?>> getProperties() {
            return properties;
        }

        @Override
        public Optional<Function<Connection, String>> getRefreshFunction() {
            return Optional.ofNullable(refreshFunction);
        }

        @Override
        public Function<Connection, String> getRefreshUrlFunction() {
            return refreshUrlFunction;
        }

        @Override
        public Function<Connection, List<String>> getScopesFunction() {
            return scopesFunction;
        }

        @Override
        public Function<Connection, String> getTokenUrlFunction() {
            return tokenUrlFunction;
        }

        @Override
        public AuthorizationType getType() {
            return type;
        }
    }

    public static final class ModifiableComponentDefinition implements ComponentDefinition {

        private List<? extends ActionDefinition> actions;
        private ConnectionDefinition connection;
        private Display display;
        private Map<String, Object> metadata;
        private String name;
        private Resources resources;
        private int version = ComponentConstants.Versions.VERSION_1;

        private ModifiableComponentDefinition() {
        }

        private ModifiableComponentDefinition(String name) {
            this.name = Objects.requireNonNull(name);
        }

        public ModifiableComponentDefinition actions(ModifiableActionDefinition... actionDefinitions) {
            if (actionDefinitions != null) {
                this.actions = List.of(actionDefinitions);
            }

            return this;
        }

        @SafeVarargs
        public final ModifiableComponentDefinition actions(List<ModifiableActionDefinition>... actionsList) {
            if (actionsList != null) {
                this.actions = Stream.of(actionsList)
                    .flatMap(Collection::stream)
                    .toList();
            }

            return this;
        }

        public ModifiableComponentDefinition connection(ModifiableConnectionDefinition connectionDefinition) {
            this.connection = connectionDefinition
                .componentName(name)
                .componentVersion(version)
                .display(display);

            return this;
        }

        public ModifiableComponentDefinition display(ModifiableDisplay display) {
            this.display = display;

            return this;
        }

        public ModifiableComponentDefinition metadata(String key, String value) {
            if (metadata == null) {
                metadata = new HashMap<>();
            }

            this.metadata.put(key, value);

            return this;
        }

        @SuppressFBWarnings("EI2")
        public ModifiableComponentDefinition metadata(Map<String, Object> metadata) {
            this.metadata = metadata;

            return this;
        }

        public ModifiableComponentDefinition resources(ModifiableResources resources) {
            this.resources = resources;

            return this;
        }

        public ModifiableComponentDefinition version(int version) {
            this.version = version;

            return this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<? extends ActionDefinition> getActions() {
            return actions == null ? null : new ArrayList<>(actions);
        }

        @SuppressWarnings("unchecked")
        @Override
        public ConnectionDefinition getConnection() {
            return connection;
        }

        @Override
        public Display getDisplay() {
            return display;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return metadata == null ? null : new HashMap<>(metadata);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Resources getResources() {
            return resources;
        }

        @Override
        public int getVersion() {
            return version;
        }
    }

    public static final class ModifiableConnectionDefinition implements ConnectionDefinition {

        private String componentName;
        private int componentVersion;
        private List<? extends Authorization> authorizations = Collections.emptyList();

        @JsonIgnore
        private Function<Connection, String> baseUriFunction = (
            connectionParameters) -> connectionParameters.containsKey(BASE_URI)
                ? connectionParameters.getParameter(BASE_URI) : null;

        private Display display;
        private List<? extends Property<?>> properties;
        private Resources resources;
        private String subtitle;

        @JsonIgnore
        private Consumer<Connection> testConsumer;

        private ModifiableConnectionDefinition() {
        }

        public ModifiableConnectionDefinition authorizations(ModifiableAuthorization... authorizations) {
            if (authorizations != null) {
                this.authorizations = List.of(authorizations);
            }

            return this;
        }

        public ModifiableConnectionDefinition baseUri(Function<Connection, String> baseUriFunction) {
            if (baseUriFunction != null) {
                this.baseUriFunction = baseUriFunction;
            }

            return this;
        }

        public <P extends Property<?>> ModifiableConnectionDefinition properties(P... properties) {
            if (properties != null) {
                this.properties = List.of(properties);
            }

            return this;
        }

        public ModifiableConnectionDefinition resources(ModifiableResources resources) {
            this.resources = resources;

            return this;
        }

        public ModifiableConnectionDefinition testConsumer(Consumer<Connection> testConsumer) {
            this.testConsumer = testConsumer;

            return this;
        }

        public ModifiableConnectionDefinition subtitle(String subtitle) {
            this.subtitle = subtitle;

            return this;
        }

        private ModifiableConnectionDefinition componentName(String componentName) {
            this.componentName = componentName;

            return this;
        }

        private ModifiableConnectionDefinition componentVersion(int componentVersion) {
            this.componentVersion = componentVersion;

            return this;
        }

        private ModifiableConnectionDefinition display(Display display) {
            this.display = display;

            return this;
        }

        @Override
        public List<? extends Authorization> getAuthorizations() {
            return authorizations == null ? null : new ArrayList<>(authorizations);
        }

        @Override
        public Function<Connection, String> getBaseUriFunction() {
            return baseUriFunction;
        }

        @Override
        public String getComponentName() {
            return componentName;
        }

        @Override
        public int getComponentVersion() {
            return componentVersion;
        }

        @Override
        public Display getDisplay() {
            return display;
        }

        @Override
        public List<? extends Property<?>> getProperties() {
            return properties;
        }

        @Override
        public Resources getResources() {
            return resources;
        }

        @Override
        public String getSubtitle() {
            return subtitle;
        }

        @Override
        public Optional<Consumer<Connection>> getTestConsumer() {
            return Optional.ofNullable(testConsumer);
        }
    }

    public static final class ModifiableJdbcComponentDefinition implements JdbcComponentDefinition {

        private String databaseJdbcName;
        private String jdbcDriverClassName;
        private Display display;
        private Resources resources;
        private double version = ComponentConstants.Versions.VERSION_1;
        private final String name;

        private ModifiableJdbcComponentDefinition(String name) {
            this.name = Objects.requireNonNull(name);
        }

        public ModifiableJdbcComponentDefinition databaseJdbcName(String databaseJdbcName) {
            this.databaseJdbcName = databaseJdbcName;

            return this;
        }

        public ModifiableJdbcComponentDefinition jdbcDriverClassName(String jdbcDriverClassName) {
            this.jdbcDriverClassName = jdbcDriverClassName;

            return this;
        }

        public ModifiableJdbcComponentDefinition display(ModifiableDisplay display) {
            this.display = display;

            return this;
        }

        public ModifiableJdbcComponentDefinition resources(ModifiableResources resources) {
            this.resources = resources;

            return this;
        }

        public ModifiableJdbcComponentDefinition version(double version) {
            this.version = version;

            return this;
        }

        @Override
        public String getDatabaseJdbcName() {
            return databaseJdbcName;
        }

        @Override
        public String getJdbcDriverClassName() {
            return jdbcDriverClassName;
        }

        @Override
        public Display getDisplay() {
            return display;
        }

        @Override
        public Resources getResources() {
            return resources;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public double getVersion() {
            return version;
        }
    }
}
