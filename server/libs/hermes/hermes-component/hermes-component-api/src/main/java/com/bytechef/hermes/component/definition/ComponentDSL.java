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
import com.bytechef.hermes.component.ConnectionParameters;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.definition.DefinitionDSL;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

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
                "FILE_ENTRY",
                null,
                string("extension").required(true),
                string("mimeType").required(true),
                string("name").required(true),
                string("url").required(true));
    }

    public static ModifiableJdbcComponentDefinition jdbcComponent(String name) {
        return new ModifiableJdbcComponentDefinition(name);
    }

    public static final class ModifiableActionDefinition extends ActionDefinition {

        private ModifiableActionDefinition(String name) {
            super(name);
        }

        public ModifiableActionDefinition display(Display display) {
            this.display = display;

            return this;
        }

        public ModifiableActionDefinition exampleOutput(Object exampleOutput) {
            this.exampleOutput = exampleOutput;

            return this;
        }

        @SuppressWarnings("unchecked")
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

        public ModifiableActionDefinition output(Property... output) {
            this.output = List.of(output);

            return this;
        }

        public ModifiableActionDefinition perform(BiFunction<Context, ExecutionParameters, Object> performFunction) {
            this.performFunction = performFunction;

            return this;
        }

        public ModifiableActionDefinition properties(Property... properties) {
            this.properties = List.of(properties);

            return this;
        }
    }

    public static final class ModifiableAuthorization extends Authorization {

        private ModifiableAuthorization(String name, AuthorizationType type) {
            super(name, type);
        }

        public ModifiableAuthorization apply(BiConsumer<AuthorizationContext, ConnectionParameters> applyConsumer) {
            this.applyConsumer = applyConsumer;

            return this;
        }

        public ModifiableAuthorization authorizationCallback(
                BiFunction<ConnectionParameters, String, String> authorizationCallbackFunction) {
            this.authorizationCallbackFunction = authorizationCallbackFunction;

            return this;
        }

        public ModifiableAuthorization authorizationUrl(
                Function<ConnectionParameters, String> authorizationUrlFunction) {
            this.authorizationUrlFunction = authorizationUrlFunction;

            return this;
        }

        public ModifiableAuthorization clientId(Function<ConnectionParameters, String> clientIdFunction) {
            this.clientIdFunction = clientIdFunction;

            return this;
        }

        public ModifiableAuthorization clientSecret(Function<ConnectionParameters, String> clientSecretFunction) {
            this.clientSecretFunction = clientSecretFunction;

            return this;
        }

        public ModifiableAuthorization display(Display display) {
            this.display = display;

            return this;
        }

        public ModifiableAuthorization onRefresh(List<Object> onRefresh) {
            this.onRefresh = onRefresh;

            return this;
        }

        public ModifiableAuthorization properties(Property... properties) {
            this.properties = List.of(properties);

            return this;
        }

        public ModifiableAuthorization refresh(Function<ConnectionParameters, String> refreshFunction) {
            this.refreshFunction = refreshFunction;

            return this;
        }

        public ModifiableAuthorization refreshUrl(Function<ConnectionParameters, String> refreshUrlFunction) {
            this.refreshUrlFunction = refreshUrlFunction;

            return this;
        }

        public ModifiableAuthorization scopes(Function<ConnectionParameters, List<String>> scopes) {
            this.scopes = scopes;

            return this;
        }

        public ModifiableAuthorization tokenUrl(Function<ConnectionParameters, String> tokenUrlFunction) {
            this.tokenUrlFunction = tokenUrlFunction;

            return this;
        }
    }

    public static final class ModifiableComponentDefinition extends ComponentDefinition {

        private ModifiableComponentDefinition(String name) {
            super(name);
        }

        public ModifiableComponentDefinition actions(ActionDefinition... actionDefinitions) {
            this.actionDefinitions = List.of(actionDefinitions);

            return this;
        }

        public ModifiableComponentDefinition actions(List<ActionDefinition>... actionsList) {
            this.actionDefinitions =
                    Stream.of(actionsList).flatMap(Collection::stream).toList();

            return this;
        }

        public ModifiableComponentDefinition connection(ConnectionDefinition connectionDefinition) {
            this.connectionDefinition = new ModifiableConnectionDefinition()
                    .baseUri(connectionDefinition.getBaseUriFunction())
                    .authorizations(connectionDefinition.getAuthorizations())
                    .componentName(name)
                    .display(display)
                    .properties(connectionDefinition.getProperties())
                    .subtitle(connectionDefinition.getSubtitle())
                    .resources(connectionDefinition.getResources())
                    .subtitle(connectionDefinition.getSubtitle())
                    .testConsumer(connectionDefinition.getTestConsumer());

            return this;
        }

        public ModifiableComponentDefinition display(Display display) {
            this.display = display;

            return this;
        }

        @SuppressWarnings("unchecked")
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

        public ModifiableComponentDefinition resources(Resources resources) {
            this.resources = resources;

            return this;
        }

        public ModifiableComponentDefinition version(int version) {
            this.version = version;

            return this;
        }
    }

    public static final class ModifiableConnectionDefinition extends ConnectionDefinition {

        private ModifiableConnectionDefinition() {}

        public ModifiableConnectionDefinition authorizations(Authorization... authorizations) {
            if (authorizations != null) {
                this.authorizations = List.of(authorizations);
            }

            return this;
        }

        public ModifiableConnectionDefinition baseUri(Function<ConnectionParameters, String> baseUriFunction) {
            this.baseUriFunction = baseUriFunction;

            return this;
        }

        public ModifiableConnectionDefinition properties(Property<?>... properties) {
            this.properties = List.of(properties);

            return this;
        }

        public ModifiableConnectionDefinition resources(Resources resources) {
            this.resources = resources;

            return this;
        }

        public ModifiableConnectionDefinition subtitle(String subtitle) {
            this.subtitle = subtitle;

            return this;
        }

        public ModifiableConnectionDefinition testConsumer(Consumer<ConnectionParameters> testConsumer) {
            this.testConsumer = testConsumer;

            return this;
        }

        private ModifiableConnectionDefinition authorizations(List<Authorization> authorizations) {
            this.authorizations = authorizations;

            return this;
        }

        private ModifiableConnectionDefinition componentName(String componentName) {
            this.componentName = componentName;

            return this;
        }

        private ModifiableConnectionDefinition display(Display componentName) {
            this.display = display;

            return this;
        }

        private ModifiableConnectionDefinition properties(List<Property<?>> properties) {
            this.properties = properties;

            return this;
        }
    }

    public static final class ModifiableJdbcComponentDefinition extends JdbcComponentDefinition {

        private ModifiableJdbcComponentDefinition(String name) {
            super(name);
        }

        public ModifiableJdbcComponentDefinition databaseJdbcName(String databaseJdbcName) {
            this.databaseJdbcName = databaseJdbcName;

            return this;
        }

        public ModifiableJdbcComponentDefinition jdbcDriverClassName(String jdbcDriverClassName) {
            this.jdbcDriverClassName = jdbcDriverClassName;

            return this;
        }

        public ModifiableJdbcComponentDefinition display(Display display) {
            this.display = display;

            return this;
        }

        public ModifiableJdbcComponentDefinition resources(Resources resources) {
            this.resources = resources;

            return this;
        }

        public ModifiableJdbcComponentDefinition version(double version) {
            this.version = version;

            return this;
        }
    }
}
