
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

import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.component.definition.EditorDescriptionDataSource.EditorDescriptionFunction;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;
import com.bytechef.hermes.component.definition.SampleOutputDataSource.SampleOutputFunction;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.definition.DefinitionDSL;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableDynamicPropertiesProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableObjectProperty;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
        private Boolean deprecated;
        private String description;
        private Object sampleOutput;
        private ExecuteFunction execute;
        private Help help;
        private Map<String, Object> metadata;
        private final String name;
        private List<? extends Property<?>> outputSchemaProperties;
        private List<? extends Property<?>> properties;
        private OutputSchemaFunction outputSchemaFunction;
        private SampleOutputFunction sampleOutputFunction;
        private String title;
        private EditorDescriptionFunction editorDescriptionFunction;

        private ModifiableActionDefinition(String name) {
            this.name = Objects.requireNonNull(name);
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

        public ModifiableActionDefinition editorDescription(EditorDescriptionFunction editorDescriptionFunction) {
            this.editorDescriptionFunction = editorDescriptionFunction;

            return this;
        }

        public ModifiableActionDefinition execute(ExecuteFunction execute) {
            this.execute = execute;

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

        public <P extends Property<?>> ModifiableActionDefinition outputSchema(P... properties) {
            if (properties != null) {
                this.outputSchemaProperties = List.of(properties);
            }

            return this;
        }

        public ModifiableActionDefinition outputSchema(OutputSchemaFunction outputSchema) {
            this.outputSchemaFunction = outputSchema;

            return this;
        }

        public <P extends Property<?>> ModifiableActionDefinition properties(P... properties) {
            if (properties != null) {
                for (Property<?> property : properties) {
                    String name = property.getName();

                    if (name == null || name.isEmpty()) {
                        throw new IllegalArgumentException("Defined properties cannot to have empty names.");
                    }
                }

                this.properties = List.of(properties);
            }

            return this;
        }

        public ModifiableActionDefinition sampleOutput(Object sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableActionDefinition sampleOutput(SampleOutputFunction sampleOutputFunction) {
            this.sampleOutputFunction = sampleOutputFunction;

            return this;
        }

        public ModifiableActionDefinition title(String title) {
            this.title = title;

            return this;
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
        public Optional<ExecuteFunction> getExecute() {
            return Optional.ofNullable(execute);
        }

        @Override
        public Optional<Help> getHelp() {
            return Optional.ofNullable(help);
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
        public Optional<List<? extends Property<?>>> getOutputSchema() {
            return Optional.ofNullable(outputSchemaProperties);
        }

        @Override
        public Optional<OutputSchemaDataSource> getOutputSchemaDataSource() {
            return Optional.ofNullable(
                outputSchemaFunction == null
                    ? null : new OutputSchemaDataSourceImpl(outputSchemaFunction));
        }

        @Override
        public Optional<List<? extends Property<?>>> getProperties() {
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
        public Optional<String> getTitle() {
            return Optional.ofNullable(title);
        }

        @Override
        public Optional<Boolean> getBatch() {
            return Optional.ofNullable(batch);
        }

        @Override
        public Optional<Boolean> getDeprecated() {
            return Optional.ofNullable(deprecated);
        }
    }

    public static final class ModifiableAuthorization implements Authorization {

        private AcquireFunction acquireFunction;
        private ApplyConsumer applyConsumer;
        private AuthorizationCallbackFunction authorizationCallbackFunction;
        private AuthorizationUrlFunction authorizationUrlFunction;
        private ClientIdFunction clientIdFunction;
        private ClientSecretFunction clientSecretFunction;
        private List<Object> detectOn;
        private String description;
        private String name;
        private List<? extends Property<?>> properties;
        private RefreshFunction refreshFunction;
        private List<Object> refreshOn;
        private RefreshUrlFunction refreshUrlFunction;
        private ScopesFunction scopesFunction;
        private PkceFunction pkceFunction;
        private String title;
        private TokenUrlFunction tokenUrlFunction;
        private AuthorizationType type;

        private ModifiableAuthorization() {
        }

        private ModifiableAuthorization(String name, AuthorizationType type) {
            this.name = Objects.requireNonNull(name);
            this.type = Objects.requireNonNull(type);
        }

        public ModifiableAuthorization acquire(AcquireFunction acquireFunction) {
            if (acquireFunction != null) {
                this.acquireFunction = acquireFunction;
            }

            return this;
        }

        public ModifiableAuthorization apply(ApplyConsumer applyConsumer) {
            if (applyConsumer != null) {
                this.applyConsumer = applyConsumer;
            }

            return this;
        }

        public ModifiableAuthorization
            authorizationCallback(AuthorizationCallbackFunction authorizationCallbackFunction) {
            this.authorizationCallbackFunction = authorizationCallbackFunction;

            return this;
        }

        public ModifiableAuthorization authorizationUrl(AuthorizationUrlFunction authorizationUrlFunction) {
            if (authorizationUrlFunction != null) {
                this.authorizationUrlFunction = authorizationUrlFunction;
            }

            return this;
        }

        public ModifiableAuthorization clientId(ClientIdFunction clientIdFunction) {
            if (clientIdFunction != null) {
                this.clientIdFunction = clientIdFunction;
            }

            return this;
        }

        public ModifiableAuthorization clientSecret(ClientSecretFunction clientSecretFunction) {
            if (clientSecretFunction != null) {
                this.clientSecretFunction = clientSecretFunction;
            }

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

        public ModifiableAuthorization pkce(PkceFunction pkceFunction) {
            if (pkceFunction != null) {
                this.pkceFunction = pkceFunction;
            }

            return this;
        }

        public <P extends Property<?>> ModifiableAuthorization properties(P... properties) {
            if (properties != null) {
                this.properties = List.of(properties);
            }

            return this;
        }

        public ModifiableAuthorization refresh(RefreshFunction refreshFunction) {
            this.refreshFunction = refreshFunction;

            return this;
        }

        public ModifiableAuthorization refreshUrl(RefreshUrlFunction refreshUrlFunction) {
            if (refreshUrlFunction != null) {
                this.refreshUrlFunction = refreshUrlFunction;
            }

            return this;
        }

        public ModifiableAuthorization scopes(ScopesFunction scopesFunction) {
            if (scopesFunction != null) {
                this.scopesFunction = scopesFunction;
            }

            return this;
        }

        public ModifiableAuthorization title(String title) {
            this.title = title;

            return this;
        }

        public ModifiableAuthorization tokenUrl(TokenUrlFunction tokenUrlFunction) {
            if (tokenUrlFunction != null) {
                this.tokenUrlFunction = tokenUrlFunction;
            }

            return this;
        }

        @Override
        public Optional<AcquireFunction> getAcquire() {
            return Optional.ofNullable(acquireFunction);
        }

        @Override
        public Optional<ApplyConsumer> getApply() {
            return Optional.ofNullable(applyConsumer);
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
        public Optional<List<? extends Property<?>>> getProperties() {
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

        private List<? extends ActionDefinition> actions;
        private String category;
        private ModifiableConnectionDefinition connection;
        private Boolean customAction;
        private Help customActionHelp;
        private String description;
        private String icon;
        private List<String> tags;
        private FilterCompatibleConnectionDefinitionsFunction filterCompatibleConnectionDefinitions;
        private Map<String, Object> metadata;
        private String name;
        private Resources resources;
        private int version = VERSION_1;
        private String title;
        private List<? extends TriggerDefinition> triggers;

        private ModifiableComponentDefinition() {
        }

        private ModifiableComponentDefinition(String name) {
            this.name = Objects.requireNonNull(name);
        }

        public ModifiableComponentDefinition actions(ActionDefinition... actionDefinitions) {
            if (actionDefinitions != null) {
                this.actions = List.of(actionDefinitions);
            }

            return this;
        }

        public ModifiableComponentDefinition actions(ModifiableActionDefinition... actionDefinitions) {
            if (actionDefinitions != null) {
                this.actions = List.of(actionDefinitions);
            }

            return this;
        }

        public ModifiableComponentDefinition actions(List<ActionDefinition> actionDefinitions) {
            if (actionDefinitions != null) {
                this.actions = Collections.unmodifiableList(actionDefinitions);
            }

            return this;
        }

        public ModifiableComponentDefinition category(String category) {
            this.category = category;

            return this;
        }

        public ModifiableComponentDefinition connection(ConnectionDefinition connectionDefinition) {
            this.connection = ((ModifiableConnectionDefinition) connectionDefinition);

            return this;
        }

        public ModifiableComponentDefinition connection(ModifiableConnectionDefinition connectionDefinition) {
            this.connection = connectionDefinition;

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

        public ModifiableComponentDefinition filterCompatibleConnectionDefinitions(
            FilterCompatibleConnectionDefinitionsFunction filterCompatibleConnectionDefinitions) {

            this.filterCompatibleConnectionDefinitions = filterCompatibleConnectionDefinitions;

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
            this.resources = new ResourcesImpl(null, null, documentationUrl);

            return this;
        }

        public ModifiableComponentDefinition resources(String documentationUrl, List<String> categories) {
            this.resources = new ResourcesImpl(null, null, documentationUrl);

            return this;
        }

        public ModifiableComponentDefinition resources(
            String documentationUrl, List<String> categories, Map<String, String> additionalUrls) {

            this.resources = new ResourcesImpl(null, null, documentationUrl);

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

        public ModifiableComponentDefinition triggers(TriggerDefinition... triggerDefinitions) {
            if (triggerDefinitions != null) {
                this.triggers = List.of(triggerDefinitions);
            }

            return this;
        }

        public ModifiableComponentDefinition triggers(ModifiableTriggerDefinition... triggerDefinitions) {
            if (triggerDefinitions != null) {
                this.triggers = List.of(triggerDefinitions);
            }

            return this;
        }

        public ModifiableComponentDefinition triggers(List<TriggerDefinition> triggerDefinitions) {
            if (triggerDefinitions != null) {
                this.triggers = Collections.unmodifiableList(triggerDefinitions);
            }

            return this;
        }

        public ModifiableComponentDefinition version(int version) {
            this.version = version;

            return this;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, version);
        }

        @Override
        public Optional<FilterCompatibleConnectionDefinitionsFunction> getFilterCompatibleConnectionDefinitions() {
            return Optional.ofNullable(filterCompatibleConnectionDefinitions);
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
        public Map<String, Object> getMetadata() {
            return metadata == null ? null : new HashMap<>(metadata);
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

        private boolean authorizationRequired = true;
        private List<? extends ModifiableAuthorization> authorizations;
        private BaseUriFunction baseUri;
        private List<? extends Property<?>> properties;
        private TestConsumer testConsumer;
        private int version = 1;

        private ModifiableConnectionDefinition() {
        }

        @SuppressWarnings("unchecked")
        public <P extends Property<?>> ModifiableConnectionDefinition append(P property) {
            if (this.properties == null) {
                this.properties = new ArrayList<>();
            }

            ((List<Property<?>>) this.properties).add(property);

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
            if (baseUri != null) {
                this.baseUri = baseUri;
            }

            return this;
        }

        public ModifiableConnectionDefinition version(int version) {
            this.version = version;

            return this;
        }

        public <P extends Property<?>> ModifiableConnectionDefinition properties(P... properties) {
            if (properties != null) {
                for (Property<?> property : properties) {
                    String name = property.getName();

                    if (name == null || name.isEmpty()) {
                        throw new IllegalArgumentException("Defined properties cannot to have empty names.");
                    }
                }

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
        public boolean isAuthorizationRequired() {
            return authorizationRequired && containsAuthorizations();
        }

        @Override
        public ModifiableAuthorization getAuthorization(String authorizationName) {
            if (authorizations == null) {
                throw new ComponentExecutionException("Authorization %s does not exist".formatted(authorizationName));
            }

            return authorizations.stream()
                .filter(
                    authorization -> Objects.equals(authorization.getName(), authorizationName))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        }

        @Override
        public Optional<List<? extends Authorization>> getAuthorizations() {
            return Optional.ofNullable(authorizations == null ? null : new ArrayList<>(authorizations));
        }

        @Override
        public Optional<BaseUriFunction> getBaseUri() {
            return Optional.ofNullable(baseUri);
        }

        @Override
        public int getVersion() {
            return version;
        }

        @Override
        public Optional<List<? extends Property<?>>> getProperties() {
            return Optional.ofNullable(properties == null ? null : new ArrayList<>(properties));
        }

        @Override
        public Optional<TestConsumer> getTest() {
            return Optional.ofNullable(testConsumer);
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
            this.resources = new ResourcesImpl(null, null, documentationUrl);

            return this;
        }

        public ModifiableJdbcComponentDefinition resources(String documentationUrl, List<String> categories) {
            this.resources = new ResourcesImpl(null, null, documentationUrl);

            return this;
        }

        public ModifiableJdbcComponentDefinition resources(
            String documentationUrl, List<String> categories, Map<String, String> additionalUrls) {

            this.resources = new ResourcesImpl(null, null, documentationUrl);

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
            return jdbcDriverClassName;
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public String getIcon() {
            return icon;
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
        public String getTitle() {
            return title;
        }

        @Override
        public int getVersion() {
            return version;
        }
    }

    public static class ModifiableTriggerDefinition implements TriggerDefinition {

        private Boolean batch;
        private DeduplicateFunction deduplicate;
        private Boolean deprecated;
        private String description;
        private DynamicWebhookDisableConsumer dynamicWebhookDisable;
        private DynamicWebhookEnableFunction dynamicWebhookEnable;
        private DynamicWebhookRefreshFunction dynamicWebhookRefresh;
        private DynamicWebhookRequestFunction dynamicWebhookRequest;
        private Object sampleOutput;
        private Help help;
        private ListenerEnableConsumer listenerEnable;
        private ListenerDisableConsumer listenerDisable;
        private String name;
        private List<? extends Property<?>> outputSchemaProperties;
        private OutputSchemaFunction outputSchemaFunction;
        private SampleOutputFunction sampleOutputFunction;
        private PollFunction poll;
        private List<? extends Property<?>> properties;
        private StaticWebhookRequestFunction staticWebhookRequest;
        private String title;
        private TriggerType type;
        private Boolean webhookBodyRaw;
        private WebhookValidateFunction webhookValidate;
        private Boolean workflowSyncExecution;
        private EditorDescriptionFunction editorDescriptionFunction;

        private ModifiableTriggerDefinition(String name) {
            this.name = Objects.requireNonNull(name);
        }

        public ModifiableTriggerDefinition batch(boolean batch) {
            this.batch = batch;

            return this;
        }

        public ModifiableTriggerDefinition deduplicate(DeduplicateFunction deduplicate) {
            this.deduplicate = deduplicate;

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

        public ModifiableTriggerDefinition sampleOutput(Object sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableTriggerDefinition dynamicWebhookDisable(DynamicWebhookDisableConsumer dynamicWebhookDisable) {
            this.dynamicWebhookDisable = dynamicWebhookDisable;

            return this;
        }

        public ModifiableTriggerDefinition dynamicWebhookEnable(DynamicWebhookEnableFunction dynamicWebhookEnable) {
            this.dynamicWebhookEnable = dynamicWebhookEnable;

            return this;
        }

        public ModifiableTriggerDefinition dynamicWebhookRefresh(DynamicWebhookRefreshFunction webhookRefresh) {
            this.dynamicWebhookRefresh = webhookRefresh;

            return this;
        }

        public ModifiableTriggerDefinition dynamicWebhookRequest(DynamicWebhookRequestFunction dynamicWebhookRequest) {
            this.dynamicWebhookRequest = dynamicWebhookRequest;

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

        public ModifiableTriggerDefinition listenerEnable(ListenerEnableConsumer listenerEnable) {
            this.listenerEnable = listenerEnable;

            return this;
        }

        public ModifiableTriggerDefinition listenerDisable(ListenerDisableConsumer listenerDisable) {
            this.listenerDisable = listenerDisable;

            return this;
        }

        public ModifiableTriggerDefinition name(String name) {
            this.name = name;

            return this;
        }

        public ModifiableTriggerDefinition editorDescription(EditorDescriptionFunction editorDescriptionFunction) {
            this.editorDescriptionFunction = editorDescriptionFunction;

            return this;
        }

        public <P extends Property<?>> ModifiableTriggerDefinition outputSchema(P... properties) {
            if (properties != null) {
                this.outputSchemaProperties = List.of(properties);
            }

            return this;
        }

        public ModifiableTriggerDefinition outputSchema(OutputSchemaFunction outputSchema) {
            this.outputSchemaFunction = outputSchema;

            return this;
        }

        public ModifiableTriggerDefinition poll(PollFunction poll) {
            this.poll = poll;

            return this;
        }

        public <P extends Property<?>> ModifiableTriggerDefinition properties(P... properties) {
            if (properties != null) {
                for (Property<?> property : properties) {
                    String name = property.getName();

                    if (name == null || name.isEmpty()) {
                        throw new IllegalArgumentException("Defined properties cannot to have empty names.");
                    }
                }

                this.properties = List.of(properties);
            }

            return this;
        }

        public ModifiableTriggerDefinition sampleOutput(SampleOutputFunction sampleOutputFunction) {
            this.sampleOutputFunction = sampleOutputFunction;

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

        public ModifiableTriggerDefinition webhookBodyRaw(boolean webhookBodyRaw) {
            this.webhookBodyRaw = webhookBodyRaw;

            return this;
        }

        public ModifiableTriggerDefinition webhookValidate(WebhookValidateFunction webhookValidate) {
            this.webhookValidate = webhookValidate;

            return this;
        }

        public ModifiableTriggerDefinition workflowSyncExecution(boolean workflowSyncExecution) {
            this.workflowSyncExecution = workflowSyncExecution;

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
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<DeduplicateFunction> getDeduplicate() {
            return Optional.ofNullable(deduplicate);
        }

        @Override
        public Optional<DynamicWebhookDisableConsumer> getDynamicWebhookDisable() {
            return Optional.ofNullable(dynamicWebhookDisable);
        }

        @Override
        public Optional<DynamicWebhookEnableFunction> getDynamicWebhookEnable() {
            return Optional.ofNullable(dynamicWebhookEnable);
        }

        @Override
        public Optional<DynamicWebhookRefreshFunction> getDynamicWebhookRefresh() {
            return Optional.ofNullable(dynamicWebhookRefresh);
        }

        @Override
        public Optional<DynamicWebhookRequestFunction> getDynamicWebhookRequest() {
            return Optional.ofNullable(dynamicWebhookRequest);
        }

        @Override
        public Optional<Help> getHelp() {
            return Optional.ofNullable(help);
        }

        @Override
        public Optional<ListenerEnableConsumer> getListenerEnable() {
            return Optional.ofNullable(listenerEnable);
        }

        @Override
        public Optional<ListenerDisableConsumer> getListenerDisable() {
            return Optional.ofNullable(listenerDisable);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<EditorDescriptionDataSource> getEditorDescriptionDataSource() {
            return Optional.ofNullable(
                editorDescriptionFunction == null
                    ? null
                    : new EditorDescriptionDataSourceImpl(editorDescriptionFunction));
        }

        @Override
        public Optional<List<? extends Property<?>>> getOutputSchema() {
            return Optional.ofNullable(outputSchemaProperties);
        }

        @Override
        public Optional<OutputSchemaDataSource> getOutputSchemaDataSource() {
            return Optional.ofNullable(
                outputSchemaFunction == null
                    ? null : new OutputSchemaDataSourceImpl(outputSchemaFunction));
        }

        @Override
        public Optional<PollFunction> getPoll() {
            return Optional.ofNullable(poll);
        }

        @Override
        public Optional<List<? extends Property<?>>> getProperties() {
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
        public Optional<WebhookValidateFunction> getWebhookValidate() {
            return Optional.ofNullable(webhookValidate);
        }

        @Override
        public Optional<Boolean> getWorkflowSyncExecution() {
            return Optional.ofNullable(workflowSyncExecution);
        }

        @Override
        public Optional<Boolean> getWebhookBodyRaw() {
            return Optional.ofNullable(webhookBodyRaw);
        }
    }

    private static final class EditorDescriptionDataSourceImpl implements EditorDescriptionDataSource {

        private final EditorDescriptionFunction editorDescription;

        private EditorDescriptionDataSourceImpl(EditorDescriptionFunction editorDescription) {
            this.editorDescription = editorDescription;
        }

        @Override
        public EditorDescriptionFunction getEditorDescription() {
            return editorDescription;
        }
    }

    private static final class OutputSchemaDataSourceImpl implements OutputSchemaDataSource {

        private final OutputSchemaFunction outputSchema;

        private OutputSchemaDataSourceImpl(OutputSchemaFunction outputSchema) {
            this.outputSchema = outputSchema;
        }

        @Override
        public OutputSchemaFunction getOutputSchema() {
            return outputSchema;
        }
    }

    public record HelpImpl(String body, String learnMoreUrl) implements Help {

        @Override
        public String getBody() {
            return body;
        }

        @Override
        public String getLearnMoreUrl() {
            return learnMoreUrl;
        }
    }

    private static class SampleOutputDataSourceImpl implements SampleOutputDataSource {

        private final SampleOutputFunction sampleOutputFunction;

        private SampleOutputDataSourceImpl(SampleOutputFunction sampleOutputFunction) {
            this.sampleOutputFunction = sampleOutputFunction;
        }

        @Override
        public SampleOutputFunction getSampleOutput() {
            return sampleOutputFunction;
        }
    }
}
