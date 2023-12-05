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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.component.definition.EditorDescriptionDataSource.EditorDescriptionFunction;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;
import com.bytechef.hermes.component.definition.SampleOutputDataSource.SampleOutputFunction;
import com.bytechef.hermes.definition.DefinitionDSL;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableDynamicPropertiesProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableInputProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableObjectProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableOutputProperty;
import com.bytechef.hermes.definition.Help;
import com.bytechef.hermes.definition.Property.InputProperty;
import com.bytechef.hermes.definition.Property.OutputProperty;
import com.bytechef.hermes.definition.Property.ValueProperty;
import com.bytechef.hermes.definition.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public final class ComponentDSL extends DefinitionDSL {

    private static final int VERSION_1 = 1;

    public static ModifiableActionDefinition action(String name) {
        return new ModifiableActionDefinition(name);
    }

    public static ModifiableActionDefinition action(String name, ComponentDefinition componentDefinition) {
        return new ModifiableActionDefinition(name, componentDefinition);
    }

    public static ModifiableAuthorization authorization(String name, AuthorizationType authorizationType) {
        return new ModifiableAuthorization(name, authorizationType);
    }

    public static ModifiableComponentDefinition component(String name) {
        return new ModifiableComponentDefinition(name);
    }

    public static ModifiableConnectionDefinition connection() {
        return new ModifiableConnectionDefinition();
    }

    public static ModifiableDynamicPropertiesProperty dynamicProperties(String name) {
        return new ModifiableDynamicPropertiesProperty(name);
    }

    public static ModifiableObjectProperty fileEntry() {
        return fileEntry(null);
    }

    public static ModifiableObjectProperty fileEntry(String name) {
        return buildObject(
            name, null, "FILE_ENTRY", string("extension").required(true), string("mimeType").required(true),
            string("name").required(true), string("url").required(true));
    }

    public static ModifiableJdbcComponentDefinition jdbcComponent(String name) {
        return new ModifiableJdbcComponentDefinition(name);
    }

    public static ModifiableTriggerDefinition trigger(String name) {
        return new ModifiableTriggerDefinition(name);
    }

    public static final class ModifiableActionDefinition implements ActionDefinition {

        private Boolean batch;
        private String componentName;
        private String componentDescription;
        private String componentTitle;
        private int componentVersion;
        private Boolean deprecated;
        private String description;
        private EditorDescriptionFunction editorDescriptionFunction;
        private PerformFunction performFunction;
        private Help help;
        private Map<String, Object> metadata;
        private final String name;
        private ModifiableOutputProperty<?> outputSchemaProperty;
        private OutputSchemaFunction outputSchemaFunction;
        private List<? extends ModifiableInputProperty> properties;
        private Object sampleOutput;
        private SampleOutputFunction sampleOutputFunction;
        private String title;

        private ModifiableActionDefinition(String name) {
            this.name = Objects.requireNonNull(name);
        }

        private ModifiableActionDefinition(String name, ComponentDefinition componentDefinition) {
            this.name = Objects.requireNonNull(name);
            this.componentDescription = componentDefinition.getDescription()
                .orElse(null);
            this.componentName = componentDefinition.getName();
            this.componentTitle = componentDefinition.getTitle()
                .orElse(null);
            this.componentVersion = componentDefinition.getVersion();
        }

        public ModifiableActionDefinition batch(boolean batch) {
            this.batch = batch;

            return this;
        }

        public ModifiableActionDefinition deprecated(Boolean deprecated) {
            this.deprecated = deprecated;

            return this;
        }

        public ModifiableActionDefinition description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableActionDefinition editorDescription(EditorDescriptionFunction editorDescription) {
            this.editorDescriptionFunction = editorDescription;

            return this;
        }

        public ModifiableActionDefinition perform(PerformFunction perform) {
            this.performFunction = perform;

            return this;
        }

        public ModifiableActionDefinition help(String body) {
            this.help = new HelpImpl(body, null);

            return this;
        }

        public ModifiableActionDefinition help(String body, String learnMoreUrl) {
            this.help = new HelpImpl(body, learnMoreUrl);

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

        public <P extends ModifiableOutputProperty<?>> ModifiableActionDefinition outputSchema(P outputSchema) {
            this.outputSchemaProperty = outputSchema;

            return this;
        }

        public ModifiableActionDefinition outputSchema(OutputSchemaFunction outputSchema) {
            this.outputSchemaFunction = outputSchema;

            return this;
        }

        @SafeVarargs
        public final <P extends ModifiableInputProperty> ModifiableActionDefinition properties(P... properties) {
            if (properties != null) {
                this.properties = List.of(properties);
            }

            return this;
        }

        public ModifiableActionDefinition sampleOutput(boolean sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableActionDefinition sampleOutput(LocalDate sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableActionDefinition sampleOutput(List<Map<String, ?>> sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableActionDefinition sampleOutput(LocalDateTime sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableActionDefinition sampleOutput(Map<String, ?> sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableActionDefinition sampleOutput(Number sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableActionDefinition sampleOutput(String sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableActionDefinition sampleOutput(SampleOutputFunction sampleOutput) {
            this.sampleOutputFunction = sampleOutput;

            return this;
        }

        public ModifiableActionDefinition title(String title) {
            this.title = title;

            return this;
        }

        @Override
        public Optional<Boolean> getBatch() {
            return Optional.ofNullable(batch);
        }

        @Override
        public Optional<Boolean> getDeprecated() {
            return Optional.ofNullable(deprecated);
        }

        @Override
        public Optional<String> getComponentDescription() {
            return Optional.ofNullable(componentDescription);
        }

        @Override
        public String getComponentName() {
            return componentName;
        }

        @Override
        public Optional<String> getComponentTitle() {
            return Optional.ofNullable(componentTitle);
        }

        @Override
        public int getComponentVersion() {
            return componentVersion;
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<EditorDescriptionDataSource> getEditorDescriptionDataSource() {
            return Optional.ofNullable(
                editorDescriptionFunction == null
                    ? null
                    : new EditorDescriptionDataSourceImpl(editorDescriptionFunction));
        }

        @Override
        public Optional<PerformFunction> getPerform() {
            return Optional.ofNullable(performFunction);
        }

        @Override
        public Optional<Help> getHelp() {
            return Optional.ofNullable(help);
        }

        @Override
        public Optional<Map<String, Object>> getMetadata() {
            return Optional.ofNullable(metadata == null ? null : new HashMap<>(metadata));
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<OutputProperty<?>> getOutputSchema() {
            return Optional.ofNullable(outputSchemaProperty);
        }

        @Override
        public Optional<OutputSchemaDataSource> getOutputSchemaDataSource() {
            return Optional.ofNullable(
                outputSchemaFunction == null ? null : new OutputSchemaDataSourceImpl(outputSchemaFunction));
        }

        @Override
        public Optional<List<? extends InputProperty>> getProperties() {
            return Optional.ofNullable(properties);
        }

        @Override
        public Optional<Object> getSampleOutput() {
            return Optional.ofNullable(sampleOutput);
        }

        @Override
        public Optional<SampleOutputDataSource> getSampleOutputDataSource() {
            return Optional.ofNullable(
                sampleOutputFunction == null ? null : new SampleOutputDataSourceImpl(sampleOutputFunction));
        }

        @Override
        public Optional<String> getTitle() {
            return Optional.ofNullable(title);
        }
    }

    public static final class ModifiableAuthorization implements Authorization {

        private AcquireFunction acquireFunction;
        private ApplyFunction applyFunction;
        private AuthorizationCallbackFunction authorizationCallbackFunction;
        private AuthorizationUrlFunction authorizationUrlFunction;
        private ClientIdFunction clientIdFunction;
        private ClientSecretFunction clientSecretFunction;
        private List<Object> detectOn;
        private String description;
        private final String name;
        private List<? extends InputProperty> properties;
        private RefreshFunction refreshFunction;
        private List<Object> refreshOn;
        private RefreshUrlFunction refreshUrlFunction;
        private ScopesFunction scopesFunction;
        private PkceFunction pkceFunction;
        private String title;
        private TokenUrlFunction tokenUrlFunction;
        private final AuthorizationType type;

        private ModifiableAuthorization(String name, AuthorizationType type) {
            this.name = Objects.requireNonNull(name);
            this.type = Objects.requireNonNull(type);
        }

        public ModifiableAuthorization acquire(AcquireFunction acquire) {
            this.acquireFunction = Objects.requireNonNull(acquire);

            return this;
        }

        public ModifiableAuthorization apply(ApplyFunction apply) {
            this.applyFunction = Objects.requireNonNull(apply);

            return this;
        }

        public ModifiableAuthorization authorizationCallback(AuthorizationCallbackFunction authorizationCallback) {
            this.authorizationCallbackFunction = authorizationCallback;

            return this;
        }

        public ModifiableAuthorization authorizationUrl(AuthorizationUrlFunction authorizationUrl) {
            this.authorizationUrlFunction = Objects.requireNonNull(authorizationUrl);

            return this;
        }

        public ModifiableAuthorization clientId(ClientIdFunction clientId) {
            this.clientIdFunction = Objects.requireNonNull(clientId);

            return this;
        }

        public ModifiableAuthorization clientSecret(ClientSecretFunction clientSecret) {
            this.clientSecretFunction = Objects.requireNonNull(clientSecret);

            return this;
        }

        public ModifiableAuthorization detectOn(Object... detectOn) {
            if (detectOn != null) {
                this.detectOn = List.of(detectOn);
            }

            return this;
        }

        public ModifiableAuthorization description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableAuthorization refreshOn(Object... refreshOn) {
            if (refreshOn != null) {
                this.refreshOn = List.of(refreshOn);
            }

            return this;
        }

        public ModifiableAuthorization pkce(PkceFunction pkce) {
            this.pkceFunction = Objects.requireNonNull(pkce);

            return this;
        }

        @SafeVarargs
        public final <P extends InputProperty> ModifiableAuthorization properties(P... properties) {
            if (properties != null) {
                this.properties = List.of(properties);
            }

            return this;
        }

        public ModifiableAuthorization refresh(RefreshFunction refresh) {
            this.refreshFunction = refresh;

            return this;
        }

        public ModifiableAuthorization refreshUrl(RefreshUrlFunction refreshUrl) {
            this.refreshUrlFunction = Objects.requireNonNull(refreshUrl);

            return this;
        }

        public ModifiableAuthorization scopes(ScopesFunction scopes) {
            this.scopesFunction = Objects.requireNonNull(scopes);

            return this;
        }

        public ModifiableAuthorization title(String title) {
            this.title = title;

            return this;
        }

        public ModifiableAuthorization tokenUrl(TokenUrlFunction tokenUrl) {
            this.tokenUrlFunction = Objects.requireNonNull(tokenUrl);

            return this;
        }

        @Override
        public Optional<AcquireFunction> getAcquire() {
            return Optional.ofNullable(acquireFunction);
        }

        @Override
        public Optional<ApplyFunction> getApply() {
            return Optional.ofNullable(applyFunction);
        }

        @Override
        public Optional<AuthorizationCallbackFunction> getAuthorizationCallback() {
            return Optional.ofNullable(authorizationCallbackFunction);
        }

        @Override
        public Optional<AuthorizationUrlFunction> getAuthorizationUrl() {
            return Optional.ofNullable(authorizationUrlFunction);
        }

        @Override
        public Optional<ClientIdFunction> getClientId() {
            return Optional.ofNullable(clientIdFunction);
        }

        @Override
        public Optional<ClientSecretFunction> getClientSecret() {
            return Optional.ofNullable(clientSecretFunction);
        }

        @Override
        public Optional<List<Object>> getDetectOn() {
            return Optional.ofNullable(detectOn);
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<List<Object>> getRefreshOn() {
            return Optional.ofNullable(refreshOn);
        }

        @Override
        public Optional<PkceFunction> getPkce() {
            return Optional.ofNullable(pkceFunction);
        }

        @Override
        public Optional<List<? extends InputProperty>> getProperties() {
            return Optional.ofNullable(properties);
        }

        @Override
        public Optional<RefreshFunction> getRefresh() {
            return Optional.ofNullable(refreshFunction);
        }

        @Override
        public Optional<RefreshUrlFunction> getRefreshUrl() {
            return Optional.ofNullable(refreshUrlFunction);
        }

        @Override
        public Optional<ScopesFunction> getScopes() {
            return Optional.ofNullable(scopesFunction);
        }

        @Override
        public Optional<String> getTitle() {
            return Optional.ofNullable(title);
        }

        @Override
        public Optional<TokenUrlFunction> getTokenUrl() {
            return Optional.ofNullable(tokenUrlFunction);
        }

        @Override
        public AuthorizationType getType() {
            return type;
        }
    }

    public static final class ModifiableComponentDefinition implements ComponentDefinition {

        private List<? extends ModifiableActionDefinition> actions;
        private String category;
        private ModifiableConnectionDefinition connection;
        private Boolean customAction;
        private Help customActionHelp;
        private String description;
        private String icon;
        private List<String> tags;
        private AllowedConnectionDefinitionsFunction allowedConnectionDefinitionsFunction;
        private Map<String, Object> metadata;
        private final String name;
        private Resources resources;
        private int version = VERSION_1;
        private String title;
        private List<? extends ModifiableTriggerDefinition> triggers;

        private ModifiableComponentDefinition(String name) {
            this.name = Objects.requireNonNull(name);
        }

        @SafeVarargs
        public final <A extends ModifiableActionDefinition> ModifiableComponentDefinition actions(
            A... actionDefinitions) {

            if (actionDefinitions != null) {
                return actions(List.of(actionDefinitions));
            }

            return this;
        }

        public <A extends ModifiableActionDefinition> ModifiableComponentDefinition actions(List<A> actionDefinitions) {
            this.actions = Collections.unmodifiableList(Objects.requireNonNull(actionDefinitions));

            for (ModifiableActionDefinition actionDefinition : actions) {
                actionDefinition.componentDescription = this.getDescription()
                    .orElse(null);
                actionDefinition.componentName = this.getName();
                actionDefinition.componentTitle = this.getTitle()
                    .orElse(null);
                actionDefinition.componentVersion = this.getVersion();
            }

            return this;
        }

        public ModifiableComponentDefinition category(String category) {
            this.category = category;

            return this;
        }

        public ModifiableComponentDefinition connection(ModifiableConnectionDefinition connectionDefinition) {
            this.connection = connectionDefinition;

            this.connection.componentDescription = this.getDescription()
                .orElse(null);
            this.connection.componentName = this.getName();
            this.connection.componentTitle = this.getTitle()
                .orElse(null);

            return this;
        }

        public ModifiableComponentDefinition connections(AllowedConnectionDefinitionsFunction connectionDefinitions) {
            this.allowedConnectionDefinitionsFunction = connectionDefinitions;

            return this;
        }

        public ModifiableComponentDefinition customAction(boolean customAction) {
            this.customAction = customAction;

            return this;
        }

        public ModifiableComponentDefinition customAction(Help customActionHelp) {
            this.customActionHelp = customActionHelp;

            return this;
        }

        public ModifiableComponentDefinition description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableComponentDefinition icon(String icon) {
            this.icon = icon;

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

        public ModifiableComponentDefinition resources(String documentationUrl) {
            this.resources = new ResourcesImpl(documentationUrl, null, null);

            return this;
        }

        public ModifiableComponentDefinition resources(String documentationUrl, List<String> categories) {
            this.resources = new ResourcesImpl(documentationUrl, categories, null);

            return this;
        }

        public ModifiableComponentDefinition resources(
            String documentationUrl, List<String> categories, Map<String, String> additionalUrls) {

            this.resources = new ResourcesImpl(documentationUrl, categories, additionalUrls);

            return this;
        }

        public ModifiableComponentDefinition tags(String... tags) {
            if (tags != null) {
                this.tags = List.of(tags);
            }

            return this;
        }

        public ModifiableComponentDefinition title(String title) {
            this.title = title;

            return this;
        }

        @SafeVarargs
        public final <T extends ModifiableTriggerDefinition> ModifiableComponentDefinition triggers(
            T... triggerDefinitions) {

            if (triggerDefinitions != null) {
                return triggers(List.of(triggerDefinitions));
            }

            return this;
        }

        public <T extends ModifiableTriggerDefinition> ModifiableComponentDefinition triggers(
            List<T> triggerDefinitions) {

            this.triggers = Collections.unmodifiableList(Objects.requireNonNull(triggerDefinitions));

            for (ModifiableTriggerDefinition triggerDefinition : triggers) {
                triggerDefinition.componentDescription = this.getDescription()
                    .orElse(null);
                triggerDefinition.componentName = this.getName();
                triggerDefinition.componentTitle = this.getTitle()
                    .orElse(null);
                triggerDefinition.componentVersion = this.getVersion();
            }

            return this;
        }

        public ModifiableComponentDefinition version(int version) {
            this.version = version;

            return this;
        }

        @Override
        public Optional<AllowedConnectionDefinitionsFunction> getAllowedConnections() {
            return Optional.ofNullable(allowedConnectionDefinitionsFunction);
        }

        @Override
        public Optional<List<? extends ActionDefinition>> getActions() {
            return Optional.ofNullable(actions == null ? null : new ArrayList<>(actions));
        }

        @Override
        public Optional<String> getCategory() {
            return Optional.ofNullable(category);
        }

        @Override
        public Optional<ConnectionDefinition> getConnection() {
            return Optional.ofNullable(connection);
        }

        @Override
        public Optional<Boolean> getCustomAction() {
            return Optional.ofNullable(customAction);
        }

        @Override
        public Optional<Help> getCustomActionHelp() {
            return Optional.ofNullable(customActionHelp);
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<String> getIcon() {
            return Optional.ofNullable(icon);
        }

        @Override
        public Optional<Map<String, Object>> getMetadata() {
            return Optional.ofNullable(metadata == null ? null : new HashMap<>(metadata));
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        @SuppressFBWarnings("EI")
        public Optional<Resources> getResources() {
            return Optional.ofNullable(resources);
        }

        @Override
        public Optional<List<String>> getTags() {
            return Optional.ofNullable(tags == null ? null : Collections.unmodifiableList(tags));
        }

        @Override
        public Optional<String> getTitle() {
            return Optional.ofNullable(title);
        }

        @Override
        public Optional<List<? extends TriggerDefinition>> getTriggers() {
            return Optional.ofNullable(triggers == null ? null : new ArrayList<>(triggers));
        }

        @Override
        public int getVersion() {
            return version;
        }
    }

    public static final class ModifiableConnectionDefinition implements ConnectionDefinition {

        private List<? extends ModifiableAuthorization> authorizations;
        private BaseUriFunction baseUriFunction;
        private String componentName;
        private String componentDescription;
        private String componentTitle;
        private List<? extends ModifiableInputProperty> properties;
        private TestConsumer testConsumer;
        private int version = 1;
        private Boolean authorizationRequired;

        private ModifiableConnectionDefinition() {
        }

        @SuppressWarnings({
            "rawtypes", "unchecked"
        })
        public <P extends ValueProperty<?>> ModifiableConnectionDefinition append(P property) {
            if (this.properties == null) {
                this.properties = new ArrayList<>();
            }

            ((List) this.properties).add(property);

            return this;
        }

        public ModifiableConnectionDefinition authorizationRequired(boolean authorizationRequired) {
            this.authorizationRequired = authorizationRequired;

            return this;
        }

        public ModifiableConnectionDefinition authorizations(ModifiableAuthorization... authorizations) {
            if (authorizations != null) {
                this.authorizations = List.of(authorizations);
            }

            return this;
        }

        public ModifiableConnectionDefinition baseUri(BaseUriFunction baseUri) {
            this.baseUriFunction = Objects.requireNonNull(baseUri);

            return this;
        }

        public ModifiableConnectionDefinition version(int version) {
            this.version = version;

            return this;
        }

        @SafeVarargs
        public final <P extends ModifiableInputProperty> ModifiableConnectionDefinition properties(P... properties) {
            if (properties != null) {
                this.properties = List.of(properties);
            }

            return this;
        }

        public ModifiableConnectionDefinition test(TestConsumer test) {
            this.testConsumer = test;

            return this;
        }

        @Override
        public boolean containsAuthorizations() {
            return authorizations != null && !authorizations.isEmpty();
        }

        @Override
        public ModifiableAuthorization getAuthorization(String authorizationName) {
            Objects.requireNonNull(authorizations, "Authorization %s does not exist".formatted(authorizationName));

            return authorizations.stream()
                .filter(authorization -> Objects.equals(authorization.getName(), authorizationName))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        }

        @Override
        public Optional<Boolean> getAuthorizationRequired() {
            return Optional.ofNullable(authorizationRequired);
        }

        @Override
        public Optional<List<? extends Authorization>> getAuthorizations() {
            return Optional.ofNullable(authorizations == null ? null : new ArrayList<>(authorizations));
        }

        @Override
        public Optional<String> getComponentDescription() {
            return Optional.ofNullable(componentDescription);
        }

        @Override
        public String getComponentName() {
            return componentName;
        }

        @Override
        public Optional<String> getComponentTitle() {
            return Optional.ofNullable(componentTitle);
        }

        @Override
        public Optional<BaseUriFunction> getBaseUri() {
            return Optional.ofNullable(baseUriFunction);
        }

        @Override
        public Optional<List<? extends InputProperty>> getProperties() {
            return Optional.ofNullable(properties == null ? null : new ArrayList<>(properties));
        }

        @Override
        public Optional<TestConsumer> getTest() {
            return Optional.ofNullable(testConsumer);
        }

        @Override
        public int getVersion() {
            return version;
        }
    }

    public static final class ModifiableJdbcComponentDefinition implements JdbcComponentDefinition {

        private String databaseJdbcName;
        private String icon;
        private String jdbcDriverClassName;
        private String description;
        private final String name;
        private Resources resources;
        private String title;
        private int version = VERSION_1;

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

        public ModifiableJdbcComponentDefinition description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableJdbcComponentDefinition icon(String icon) {
            this.icon = icon;

            return this;
        }

        public ModifiableJdbcComponentDefinition resources(String documentationUrl) {
            this.resources = new ResourcesImpl(documentationUrl, null, null);

            return this;
        }

        public ModifiableJdbcComponentDefinition resources(String documentationUrl, List<String> categories) {
            this.resources = new ResourcesImpl(documentationUrl, categories, null);

            return this;
        }

        public ModifiableJdbcComponentDefinition resources(
            String documentationUrl, List<String> categories, Map<String, String> additionalUrls) {

            this.resources = new ResourcesImpl(documentationUrl, categories, additionalUrls);

            return this;
        }

        public ModifiableJdbcComponentDefinition title(String title) {
            this.title = title;

            return this;
        }

        public ModifiableJdbcComponentDefinition version(int version) {
            this.version = version;

            return this;
        }

        @Override
        public String getDatabaseJdbcName() {
            return databaseJdbcName;
        }

        @Override
        public String getJdbcDriverClassName() {
            return Objects.requireNonNull(jdbcDriverClassName);
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<String> getIcon() {
            return Optional.ofNullable(icon);
        }

        @Override
        @SuppressFBWarnings("EI")
        public Optional<Resources> getResources() {
            return Optional.ofNullable(resources);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<String> getTitle() {
            return Optional.ofNullable(title);
        }

        @Override
        public int getVersion() {
            return version;
        }
    }

    public static class ModifiableTriggerDefinition implements TriggerDefinition {

        private Boolean batch;
        private String componentName;
        private String componentDescription;
        private String componentTitle;
        private int componentVersion;
        private DeduplicateFunction deduplicateFunction;
        private Boolean deprecated;
        private String description;
        private DynamicWebhookDisableConsumer dynamicWebhookDisableConsumer;
        private DynamicWebhookEnableFunction dynamicWebhookEnableFunction;
        private DynamicWebhookRefreshFunction dynamicWebhookRefreshFunction;
        private DynamicWebhookRequestFunction dynamicWebhookRequestFunction;
        private EditorDescriptionFunction editorDescriptionFunction;
        private Help help;
        private ListenerDisableConsumer listenerDisableConsumer;
        private ListenerEnableConsumer listenerEnableConsumer;
        private String name;
        private ModifiableOutputProperty<?> outputSchemaProperty;
        private OutputSchemaFunction outputSchemaFunction;
        private PollFunction pollFunction;
        private List<? extends ModifiableInputProperty> properties;
        private Object sampleOutput;
        private SampleOutputFunction sampleOutputFunction;
        private StaticWebhookRequestFunction staticWebhookRequest;
        private String title;
        private TriggerType type;
        private Boolean webhookRawBody;
        private WebhookValidateFunction webhookValidateFunction;
        private Boolean workflowSyncExecution;
        private Boolean workflowSyncValidation;

        private ModifiableTriggerDefinition(String name) {
            this.name = Objects.requireNonNull(name);
        }

        public ModifiableTriggerDefinition batch(boolean batch) {
            this.batch = batch;

            return this;
        }

        public ModifiableTriggerDefinition deduplicate(DeduplicateFunction deduplicate) {
            this.deduplicateFunction = deduplicate;

            return this;
        }

        public ModifiableTriggerDefinition deprecated(Boolean deprecated) {
            this.deprecated = deprecated;

            return this;
        }

        public ModifiableTriggerDefinition description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableTriggerDefinition dynamicWebhookDisable(DynamicWebhookDisableConsumer dynamicWebhookDisable) {
            this.dynamicWebhookDisableConsumer = dynamicWebhookDisable;

            return this;
        }

        public ModifiableTriggerDefinition dynamicWebhookEnable(DynamicWebhookEnableFunction dynamicWebhookEnable) {
            this.dynamicWebhookEnableFunction = dynamicWebhookEnable;

            return this;
        }

        public ModifiableTriggerDefinition dynamicWebhookRefresh(DynamicWebhookRefreshFunction dynamicWebhookRefresh) {
            this.dynamicWebhookRefreshFunction = dynamicWebhookRefresh;

            return this;
        }

        public ModifiableTriggerDefinition dynamicWebhookRequest(DynamicWebhookRequestFunction dynamicWebhookRequest) {
            this.dynamicWebhookRequestFunction = dynamicWebhookRequest;

            return this;
        }

        public ModifiableTriggerDefinition editorDescription(EditorDescriptionFunction editorDescription) {
            this.editorDescriptionFunction = editorDescription;

            return this;
        }

        public ModifiableTriggerDefinition help(String body) {
            this.help = new HelpImpl(body, null);

            return this;
        }

        public ModifiableTriggerDefinition help(String body, String learnMoreUrl) {
            this.help = new HelpImpl(body, learnMoreUrl);

            return this;
        }

        public ModifiableTriggerDefinition listenerDisable(ListenerDisableConsumer listenerDisable) {
            this.listenerDisableConsumer = listenerDisable;

            return this;
        }

        public ModifiableTriggerDefinition listenerEnable(ListenerEnableConsumer listenerEnable) {
            this.listenerEnableConsumer = listenerEnable;

            return this;
        }

        public ModifiableTriggerDefinition name(String name) {
            this.name = name;

            return this;
        }

        public <P extends ModifiableOutputProperty<?>> ModifiableTriggerDefinition outputSchema(P outputSchema) {
            this.outputSchemaProperty = outputSchema;

            return this;
        }

        public ModifiableTriggerDefinition outputSchema(OutputSchemaFunction outputSchema) {
            this.outputSchemaFunction = outputSchema;

            return this;
        }

        public ModifiableTriggerDefinition poll(PollFunction poll) {
            this.pollFunction = poll;

            return this;
        }

        @SafeVarargs
        public final <P extends ModifiableInputProperty> ModifiableTriggerDefinition properties(P... properties) {
            if (properties != null) {
                this.properties = List.of(properties);
            }

            return this;
        }

        public ModifiableTriggerDefinition sampleOutput(boolean sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableTriggerDefinition sampleOutput(LocalDate sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableTriggerDefinition sampleOutput(List<Map<String, ?>> sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableTriggerDefinition sampleOutput(LocalDateTime sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableTriggerDefinition sampleOutput(Map<String, ?> sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableTriggerDefinition sampleOutput(Number sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableTriggerDefinition sampleOutput(String sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableTriggerDefinition sampleOutput(SampleOutputFunction sampleOutput) {
            this.sampleOutputFunction = sampleOutput;

            return this;
        }

        public ModifiableTriggerDefinition staticWebhookRequest(StaticWebhookRequestFunction staticWebhookRequest) {
            this.staticWebhookRequest = staticWebhookRequest;

            return this;
        }

        public ModifiableTriggerDefinition title(String title) {
            this.title = title;

            return this;
        }

        public ModifiableTriggerDefinition type(TriggerType type) {
            this.type = type;

            return this;
        }

        public ModifiableTriggerDefinition webhookRawBody(boolean webhookRawBody) {
            this.webhookRawBody = webhookRawBody;

            return this;
        }

        public ModifiableTriggerDefinition webhookValidate(WebhookValidateFunction webhookValidate) {
            this.webhookValidateFunction = webhookValidate;

            return this;
        }

        public ModifiableTriggerDefinition workflowSyncExecution(boolean workflowSyncExecution) {
            this.workflowSyncExecution = workflowSyncExecution;

            return this;
        }

        public ModifiableTriggerDefinition workflowSyncValidation(boolean workflowSyncValidation) {
            this.workflowSyncValidation = workflowSyncValidation;

            return this;
        }

        @Override
        public Optional<Boolean> getBatch() {
            return Optional.ofNullable(batch);
        }

        @Override
        public Optional<String> getComponentDescription() {
            return Optional.ofNullable(componentDescription);
        }

        @Override
        public String getComponentName() {
            return componentName;
        }

        @Override
        public Optional<String> getComponentTitle() {
            return Optional.ofNullable(componentTitle);
        }

        @Override
        public int getComponentVersion() {
            return componentVersion;
        }

        @Override
        public Optional<Boolean> getDeprecated() {
            return Optional.ofNullable(deprecated);
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<DeduplicateFunction> getDeduplicate() {
            return Optional.ofNullable(deduplicateFunction);
        }

        @Override
        public Optional<DynamicWebhookDisableConsumer> getDynamicWebhookDisable() {
            return Optional.ofNullable(dynamicWebhookDisableConsumer);
        }

        @Override
        public Optional<DynamicWebhookEnableFunction> getDynamicWebhookEnable() {
            return Optional.ofNullable(dynamicWebhookEnableFunction);
        }

        @Override
        public Optional<DynamicWebhookRefreshFunction> getDynamicWebhookRefresh() {
            return Optional.ofNullable(dynamicWebhookRefreshFunction);
        }

        @Override
        public Optional<DynamicWebhookRequestFunction> getDynamicWebhookRequest() {
            return Optional.ofNullable(dynamicWebhookRequestFunction);
        }

        @Override
        public Optional<EditorDescriptionDataSource> getEditorDescriptionDataSource() {
            return Optional.ofNullable(
                editorDescriptionFunction == null
                    ? null
                    : new EditorDescriptionDataSourceImpl(editorDescriptionFunction));
        }

        @Override
        public Optional<Help> getHelp() {
            return Optional.ofNullable(help);
        }

        @Override
        public Optional<ListenerDisableConsumer> getListenerDisable() {
            return Optional.ofNullable(listenerDisableConsumer);
        }

        @Override
        public Optional<ListenerEnableConsumer> getListenerEnable() {
            return Optional.ofNullable(listenerEnableConsumer);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<OutputProperty<?>> getOutputSchema() {
            return Optional.ofNullable(outputSchemaProperty);
        }

        @Override
        public Optional<OutputSchemaDataSource> getOutputSchemaDataSource() {
            return Optional.ofNullable(
                outputSchemaFunction == null ? null : new OutputSchemaDataSourceImpl(outputSchemaFunction));
        }

        @Override
        public Optional<PollFunction> getPoll() {
            return Optional.ofNullable(pollFunction);
        }

        @Override
        public Optional<List<? extends InputProperty>> getProperties() {
            return Optional.ofNullable(properties);
        }

        @Override
        public Optional<Object> getSampleOutput() {
            return Optional.ofNullable(sampleOutput);
        }

        @Override
        public Optional<SampleOutputDataSource> getSampleOutputDataSource() {
            return Optional.ofNullable(
                sampleOutputFunction == null
                    ? null
                    : new SampleOutputDataSourceImpl(sampleOutputFunction));
        }

        @Override
        public Optional<StaticWebhookRequestFunction> getStaticWebhookRequest() {
            return Optional.ofNullable(staticWebhookRequest);
        }

        @Override
        public Optional<String> getTitle() {
            return Optional.ofNullable(title);
        }

        @Override
        public TriggerType getType() {
            return type;
        }

        @Override
        public Optional<Boolean> getWebhookRawBody() {
            return Optional.ofNullable(webhookRawBody);
        }

        @Override
        public Optional<WebhookValidateFunction> getWebhookValidate() {
            return Optional.ofNullable(webhookValidateFunction);
        }

        @Override
        public Optional<Boolean> getWorkflowSyncExecution() {
            return Optional.ofNullable(workflowSyncExecution);
        }

        @Override
        public Optional<Boolean> getWorkflowSyncValidation() {
            return Optional.ofNullable(workflowSyncValidation);
        }
    }

    private record EditorDescriptionDataSourceImpl(
        EditorDescriptionFunction editorDescription) implements EditorDescriptionDataSource {

        @Override
        public EditorDescriptionFunction getEditorDescription() {
            return editorDescription;
        }
    }

    private record OutputSchemaDataSourceImpl(OutputSchemaFunction outputSchema) implements OutputSchemaDataSource {

        @Override
        public OutputSchemaFunction getOutputSchema() {
            return outputSchema;
        }
    }

    private record HelpImpl(String body, String learnMoreUrl) implements Help {

        @Override
        public String getBody() {
            return body;
        }

        @Override
        public Optional<String> getLearnMoreUrl() {
            return Optional.ofNullable(learnMoreUrl);
        }
    }

    private record SampleOutputDataSourceImpl(
        SampleOutputFunction sampleOutputFunction) implements SampleOutputDataSource {

        @Override
        public SampleOutputFunction getSampleOutput() {
            return sampleOutputFunction;
        }
    }
}
