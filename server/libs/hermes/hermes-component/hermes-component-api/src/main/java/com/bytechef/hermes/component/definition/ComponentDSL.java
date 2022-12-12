
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
import com.bytechef.hermes.definition.DefinitionDSL;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
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

    public static final class ModifiableActionDefinition extends ActionDefinition {

        private ModifiableActionDefinition(String name) {
            super(name);
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
    }

    public static final class ModifiableAuthorization extends Authorization {

        private ModifiableAuthorization(String name, AuthorizationType type) {
            super(name, type);
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
    }

    public static final class ModifiableComponentDefinition extends ComponentDefinition {

        private ModifiableComponentDefinition(String name) {
            super(name);
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
    }

    public static final class ModifiableConnectionDefinition extends ConnectionDefinition {

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

        protected ModifiableConnectionDefinition componentName(String componentName) {
            this.componentName = componentName;

            return this;
        }

        protected ModifiableConnectionDefinition componentVersion(int componentVersion) {
            this.componentVersion = componentVersion;

            return this;
        }

        protected ModifiableConnectionDefinition display(Display display) {
            this.display = display;

            return this;
        }

        protected ModifiableConnectionDefinition subtitle(String subtitle) {
            this.subtitle = subtitle;

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
    }
}
