
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

import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.util.HttpClientUtils;
import com.bytechef.hermes.component.util.HttpClientUtils.Response;
import com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;
import com.bytechef.hermes.definition.DefinitionDSL;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Resources;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.bytechef.hermes.component.util.HttpClientUtils.responseFormat;

/**
 * @author Ivica Cardic
 */
public final class ComponentDSL extends DefinitionDSL {

    private static final int VERSION_1 = 1;

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

    public static ModifiableComponentDynamicPropertiesDataSource dynamicProperties(
        ComponentDynamicPropertiesDataSource.DynamicPropertiesFunction dynamicPropertiesFunction,
        String... propertiesDependOnPropertyNames) {

        return new ModifiableComponentDynamicPropertiesDataSource(dynamicPropertiesFunction,
            List.of(propertiesDependOnPropertyNames));
    }

    public static ModifiableProperty.ModifiableObjectProperty fileEntry() {
        return fileEntry(null);
    }

    public static ModifiableProperty.ModifiableObjectProperty fileEntry(String name) {
        return buildObject(
            name, null, "FILE_ENTRY", string("extension").required(true), string("mimeType").required(true),
            string("name").required(true), string("url").required(true));
    }

    public static Help help(String body) {
        return new Help(body, null);
    }

    public static Help help(String body, String learnMoreUrl) {
        return new Help(body, learnMoreUrl);
    }

    public static ModifiableJdbcComponentDefinition jdbcComponent(String name) {
        return new ModifiableJdbcComponentDefinition(name);
    }

    public static ModifiableComponentOptionsDataSource options(
        ComponentOptionsDataSource.OptionsFunction optionsFunction, String... loadOptionsDependOnPropertyNames) {

        return new ModifiableComponentOptionsDataSource(optionsFunction, List.of(loadOptionsDependOnPropertyNames));
    }

    public static ModifiableOutputSchemaDataSource outputSchema(
        OutputSchemaDataSource.OutputSchemaFunction outputSchemaFunction) {

        return new ModifiableOutputSchemaDataSource(outputSchemaFunction);
    }

    public static ModifiableSampleOutputDataSource
        sampleOutput(SampleOutputDataSource.SampleOutputFunction sampleOutputFunction) {
        return new ModifiableSampleOutputDataSource(sampleOutputFunction);
    }

    public static ModifiableTriggerDefinition trigger(String name) {
        return new ModifiableTriggerDefinition(name);
    }

    public static final class ModifiableActionDefinition implements ActionDefinition {

        private Boolean batch;

        @JsonIgnore
        private ComponentDefinition component;

        private Boolean deprecated;
        private String description;
        private Object sampleOutput;

        @JsonIgnore
        private ExecuteFunction execute;

        private Help help;

        @JsonIgnore
        private Map<String, Object> metadata;
        private String name;
        private List<? extends Property<?>> outputSchema;
        private List<? extends Property<?>> properties;
        private SampleOutputDataSource sampleOutputDataSource;
        private OutputSchemaDataSource outputSchemaDataSource;

        private String title;

        @JsonIgnore
        private EditorDescriptionFunction editorDescription =
            (Connection connection, InputParameters inputParameters) -> component.getTitle() + ": " + title;

        private ModifiableActionDefinition() {
        }

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

        public ModifiableActionDefinition editorDescription(EditorDescriptionFunction editorDescription) {
            this.editorDescription = editorDescription;

            return this;
        }

        public ModifiableActionDefinition execute(ExecuteFunction execute) {
            this.execute = execute;

            return this;
        }

        public ModifiableActionDefinition help(Help help) {
            this.help = help;

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

        public <P extends Property<?>> ModifiableActionDefinition outputSchema(P... outputSchema) {
            if (outputSchema != null) {
                this.outputSchema = List.of(outputSchema);
            }

            return this;
        }

        public ModifiableActionDefinition outputSchema(OutputSchemaDataSource outputSchemaDataSource) {
            this.outputSchemaDataSource = outputSchemaDataSource;

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

        public ModifiableActionDefinition sampleOutput(SampleOutputDataSource sampleOutputDataSource) {
            this.sampleOutputDataSource = sampleOutputDataSource;

            return this;
        }

        public ModifiableActionDefinition title(String title) {
            this.title = title;

            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ModifiableActionDefinition that = (ModifiableActionDefinition) o;

            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String getDescription() {
            return Objects.requireNonNullElseGet(description, () -> component.getTitle() + ": " + title);
        }

        @Override
        public EditorDescriptionFunction getEditorDescription() {
            return editorDescription;
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
            return Optional.ofNullable(outputSchema);
        }

        @Override
        public Optional<OutputSchemaDataSource> getOutputSchemaDataSource() {
            return Optional.ofNullable(outputSchemaDataSource);
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
            return Optional.ofNullable(sampleOutputDataSource);
        }

        @Override
        public String getTitle() {
            return Objects.requireNonNullElseGet(title, () -> name);
        }

        @Override
        public Optional<Boolean> getBatch() {
            return Optional.ofNullable(batch);
        }

        @Override
        public Optional<Boolean> getDeprecated() {
            return Optional.ofNullable(deprecated);
        }

        @SuppressWarnings("PMD")
        private ModifiableActionDefinition component(ComponentDefinition component) {
            this.component = component;

            return this;
        }
    }

    public static final class ModifiableAuthorization implements Authorization {

        @JsonIgnore
        private Optional<AcquireFunction> acquire;

        @JsonIgnore
        private ApplyConsumer apply;

        @JsonIgnore
        private AuthorizationCallbackFunction authorizationCallback = (
            connectionParameters, code, redirectUri, codeVerifier) -> {

            ClientIdFunction clientIdFunction = getClientId();
            ClientSecretFunction clientSecretFunction = getClientSecret();
            TokenUrlFunction tokenUrlFunction = getTokenUrl();

            Map<String, Object> payload = new HashMap<>() {
                {
                    put("client_id", clientIdFunction.apply(connectionParameters));
                    put("client_secret", clientSecretFunction.apply(connectionParameters));
                    put("code", code);
                    put("grant_type", "authorization_code");
                    put("redirect_uri", redirectUri);
                }
            };

            if (codeVerifier != null) {
                payload.put("code_verifier", codeVerifier);
            }

            Response response = HttpClientUtils.post(tokenUrlFunction.apply(connectionParameters))
                .body(
                    HttpClientUtils.Body.of(payload, HttpClientUtils.BodyContentType.FORM_URL_ENCODED))
                .configuration(responseFormat(ResponseFormat.JSON))
                .execute();

            if (response.getStatusCode() != 200) {
                throw new ComponentExecutionException("Invalid claim");
            }

            if (response.getBody() == null) {
                throw new ComponentExecutionException("Invalid claim");
            }

            Map<?, ?> body = (Map<?, ?>) response.getBody();

            return new AuthorizationCallbackResponse(
                (String) body.get(Authorization.ACCESS_TOKEN), (String) body.get(Authorization.REFRESH_TOKEN));
        };

        @JsonIgnore
        private AuthorizationUrlFunction authorizationUrl = connectionParameters -> connectionParameters
            .getString(Authorization.AUTHORIZATION_URL);

        @JsonIgnore
        private ClientIdFunction clientId = connectionParameters -> connectionParameters
            .getString(Authorization.CLIENT_ID);

        @JsonIgnore
        private ClientSecretFunction clientSecret = connectionParameters -> connectionParameters.getString(
            Authorization.CLIENT_SECRET);

        @JsonIgnore
        private List<Object> detectOn;

        private String description;

        private String name;

        private List<? extends Property<?>> properties;

        @JsonIgnore
        private RefreshFunction refresh;

        @JsonIgnore
        private List<Object> refreshOn;

        @JsonIgnore
        private RefreshUrlFunction refreshUrl = connectionParameters -> {
            String refreshUrl = connectionParameters.getString(Authorization.REFRESH_URL);

            if (refreshUrl == null) {
                TokenUrlFunction tokeUrlFunction = getTokenUrl();

                refreshUrl = tokeUrlFunction.apply(connectionParameters);
            }

            return refreshUrl;
        };

        @JsonIgnore
        @SuppressWarnings("unchecked")
        private ScopesFunction scopes = connectionParameters -> {
            Object scopes = connectionParameters.getString(Authorization.SCOPES);

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
        };

        @JsonIgnore
        private PkceFunction pkce = (verifier, challenge) -> new Pkce(verifier, challenge, "SHA256");
        private String title;

        @JsonIgnore
        private TokenUrlFunction tokenUrl =
            connectionParameters -> connectionParameters.getString(Authorization.TOKEN_URL);

        private AuthorizationType type;

        private ModifiableAuthorization() {
        }

        private ModifiableAuthorization(String name, AuthorizationType type) {
            this.name = Objects.requireNonNull(name);
            this.type = Objects.requireNonNull(type);
            this.apply = type.getDefaultApply();
        }

        public ModifiableAuthorization acquire(AcquireFunction acquire) {
            if (acquire != null) {
                this.acquire = Optional.of(acquire);
            }

            return this;
        }

        public ModifiableAuthorization apply(ApplyConsumer apply) {
            if (apply != null) {
                this.apply = apply;
            }

            return this;
        }

        public ModifiableAuthorization authorizationCallback(AuthorizationCallbackFunction authorizationCallback) {
            this.authorizationCallback = authorizationCallback;

            return this;
        }

        public ModifiableAuthorization authorizationUrl(AuthorizationUrlFunction authorizationUrl) {
            if (authorizationUrl != null) {
                this.authorizationUrl = authorizationUrl;
            }

            return this;
        }

        public ModifiableAuthorization clientId(ClientIdFunction clientId) {
            if (clientId != null) {
                this.clientId = clientId;
            }

            return this;
        }

        public ModifiableAuthorization clientSecret(ClientSecretFunction clientSecret) {
            if (clientSecret != null) {
                this.clientSecret = clientSecret;
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

        public ModifiableAuthorization pkce(PkceFunction pkce) {
            if (pkce != null) {
                this.pkce = pkce;
            }

            return this;
        }

        public <P extends Property<?>> ModifiableAuthorization properties(P... properties) {
            if (properties != null) {
                this.properties = List.of(properties);
            }

            return this;
        }

        public ModifiableAuthorization refresh(RefreshFunction refresh) {
            this.refresh = refresh;

            return this;
        }

        public ModifiableAuthorization refreshUrl(RefreshUrlFunction refreshUrl) {
            if (refreshUrl != null) {
                this.refreshUrl = refreshUrl;
            }

            return this;
        }

        public ModifiableAuthorization scopes(ScopesFunction scopes) {
            if (scopes != null) {
                this.scopes = scopes;
            }

            return this;
        }

        public ModifiableAuthorization title(String title) {
            this.title = title;

            return this;
        }

        public ModifiableAuthorization tokenUrl(TokenUrlFunction tokenUrl) {
            if (tokenUrl != null) {
                this.tokenUrl = tokenUrl;
            }

            return this;
        }

        @Override
        public Optional<AcquireFunction> getAcquire() {
            return acquire;
        }

        @Override
        public ApplyConsumer getApply() {
            return apply;
        }

        @Override
        public AuthorizationCallbackFunction getAuthorizationCallback() {
            return authorizationCallback;
        }

        @Override
        public AuthorizationUrlFunction getAuthorizationUrl() {
            return authorizationUrl;
        }

        @Override
        public ClientIdFunction getClientId() {
            return clientId;
        }

        @Override
        public ClientSecretFunction getClientSecret() {
            return clientSecret;
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
        public PkceFunction getPkce() {
            return pkce;
        }

        @Override
        public List<? extends Property<?>> getProperties() {
            return properties;
        }

        @Override
        public Optional<RefreshFunction> getRefresh() {
            return Optional.ofNullable(refresh);
        }

        @Override
        public RefreshUrlFunction getRefreshUrl() {
            return refreshUrl;
        }

        @Override
        public ScopesFunction getScopes() {
            return scopes;
        }

        @Override
        public String getTitle() {
            return Objects.requireNonNullElseGet(title, () -> name);
        }

        @Override
        public TokenUrlFunction getTokenUrl() {
            return tokenUrl;
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
        private String[] tags;

        @JsonIgnore
        private FilterCompatibleConnectionDefinitionsFunction filterCompatibleConnectionDefinitions;

        @JsonIgnore
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
                this.actions = Stream.of(actionDefinitions)
                    .map(actionDefinition -> ((ModifiableActionDefinition) actionDefinition).component(this))
                    .toList();
            }

            return this;
        }

        public ModifiableComponentDefinition actions(ModifiableActionDefinition... actionDefinitions) {
            if (actionDefinitions != null) {
                this.actions = Stream.of(actionDefinitions)
                    .map(actionDefinition -> actionDefinition.component(this))
                    .toList();
            }

            return this;
        }

        public ModifiableComponentDefinition category(String category) {
            this.category = category;

            return this;
        }

        public ModifiableComponentDefinition connection(ConnectionDefinition connectionDefinition) {
            this.connection = ((ModifiableConnectionDefinition) connectionDefinition)
                .description(this.description)
                .name(this.name)
                .title(this.getTitle());

            return this;
        }

        public ModifiableComponentDefinition connection(ModifiableConnectionDefinition connectionDefinition) {
            this.connection = connectionDefinition
                .description(this.description)
                .name(this.name)
                .title(this.getTitle());

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

        public ModifiableComponentDefinition resources(ModifiableResources resources) {
            this.resources = resources;

            return this;
        }

        public ModifiableComponentDefinition tags(String... tags) {
            this.tags = tags;

            return this;
        }

        public ModifiableComponentDefinition title(String title) {
            this.title = title;

            return this;
        }

        public ModifiableComponentDefinition triggers(TriggerDefinition... triggerDefinitions) {
            if (triggerDefinitions != null) {
                this.triggers = Stream.of(triggerDefinitions)
                    .map(
                        triggerDefinition -> ((ModifiableTriggerDefinition) triggerDefinition)
                            .component(this))
                    .toList();
            }

            return this;
        }

        public ModifiableComponentDefinition triggers(ModifiableTriggerDefinition... triggerDefinitions) {
            if (triggerDefinitions != null) {
                this.triggers = Stream.of(triggerDefinitions)
                    .map(triggerDefinition -> triggerDefinition.component(this))
                    .toList();
            }

            return this;
        }

        public ModifiableComponentDefinition triggers(List<TriggerDefinition> triggerDefinitions) {
            if (triggerDefinitions != null) {
                this.triggers = triggerDefinitions.stream()
                    .map(
                        triggerDefinition -> ((ModifiableTriggerDefinition) triggerDefinition).component(this))
                    .toList();
            }

            return this;
        }

        public ModifiableComponentDefinition version(int version) {
            this.version = version;

            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ModifiableComponentDefinition that = (ModifiableComponentDefinition) o;

            return version == that.version && name.equals(that.name);
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
        public String getIcon() {
            return icon;
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
        public Optional<String[]> getTags() {
            return Optional.ofNullable(tags == null ? null : tags.clone());
        }

        @Override
        public String getTitle() {
            return Objects.requireNonNull(title, () -> name);
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

    public static final class ModifiableComponentOptionsDataSource extends DefinitionDSL.ModifiableOptionsDataSource
        implements ComponentOptionsDataSource {

        @JsonIgnore
        private final OptionsFunction options;

        private ModifiableComponentOptionsDataSource(
            OptionsFunction options, List<String> loadOptionsDependOnPropertyNames) {

            super(loadOptionsDependOnPropertyNames);

            this.options = options;
        }

        @Override
        public OptionsFunction getOptions() {
            return options;
        }
    }

    public static final class ModifiableComponentDynamicPropertiesDataSource
        extends ModifiableDynamicPropertiesDataSource implements ComponentDynamicPropertiesDataSource {

        @JsonIgnore
        private DynamicPropertiesFunction dynamicProperties;

        private ModifiableComponentDynamicPropertiesDataSource(
            DynamicPropertiesFunction dynamicProperties, List<String> loadPropertiesDependOnPropertyNames) {

            super(loadPropertiesDependOnPropertyNames);

            this.dynamicProperties = dynamicProperties;
        }

        @Override
        public DynamicPropertiesFunction getDynamicProperties() {
            return dynamicProperties;
        }
    }

    public static final class ModifiableConnectionDefinition implements ConnectionDefinition {

        private boolean authorizationRequired = true;
        private List<? extends ModifiableAuthorization> authorizations;

        @JsonIgnore
        private BaseUriFunction baseUri = (connectionParameters) -> connectionParameters.containsKey(BASE_URI)
            ? connectionParameters.getString(BASE_URI)
            : null;
        private String description;
        private String name;
        private List<? extends Property<?>> properties;

        @JsonIgnore
        private TestConsumer test;
        private String title;
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
            this.test = test;

            return this;
        }

        @Override
        public boolean containsAuthorizations() {
            return authorizations != null && !authorizations.isEmpty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ModifiableConnectionDefinition that = (ModifiableConnectionDefinition) o;

            return name.equals(that.name) && version == that.version;
        }

        @Override
        public boolean isAuthorizationRequired() {
            return authorizationRequired && containsAuthorizations();
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, version);
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
        public BaseUriFunction getBaseUri() {
            return baseUri;
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
        public int getVersion() {
            return version;
        }

        @Override
        public Optional<List<? extends Property<?>>> getProperties() {
            return Optional.ofNullable(properties == null ? null : new ArrayList<>(properties));
        }

        @Override
        public Optional<TestConsumer> getTest() {
            return Optional.ofNullable(test);
        }

        @Override
        public String getTitle() {
            return title;
        }

        @SuppressWarnings("PMD")
        private ModifiableConnectionDefinition description(String description) {
            this.description = description;

            return this;
        }

        private ModifiableConnectionDefinition name(String name) {
            this.name = name;

            return this;
        }

        @SuppressWarnings("PMD")
        private ModifiableConnectionDefinition title(String title) {
            this.title = title;

            return this;

        }
    }

    public static final class ModifiableSampleOutputDataSource implements SampleOutputDataSource {

        @JsonIgnore
        private final SampleOutputFunction sampleOutput;

        public ModifiableSampleOutputDataSource(SampleOutputFunction sampleOutput) {
            this.sampleOutput = sampleOutput;
        }

        @Override
        public SampleOutputFunction getSampleOutput() {
            return sampleOutput;
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

        public ModifiableJdbcComponentDefinition resources(ModifiableResources resources) {
            this.resources = resources;

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

    public static final class ModifiableOutputSchemaDataSource implements OutputSchemaDataSource {

        @JsonIgnore
        private final OutputSchemaFunction outputSchema;

        public ModifiableOutputSchemaDataSource(OutputSchemaFunction outputSchema) {
            this.outputSchema = outputSchema;
        }

        @Override
        public OutputSchemaFunction getOutputSchema() {
            return outputSchema;
        }
    }

    public static class ModifiableTriggerDefinition implements TriggerDefinition {

        private Boolean batch;

        @JsonIgnore
        private ComponentDefinition component;

        private DeduplicateFunction deduplicate;
        private Boolean deprecated;
        private String description;

        @JsonIgnore
        private DynamicWebhookDisableConsumer dynamicWebhookDisable;

        @JsonIgnore
        private DynamicWebhookEnableFunction dynamicWebhookEnable;

        @JsonIgnore
        private DynamicWebhookRefreshFunction dynamicWebhookRefresh;

        @JsonIgnore
        private DynamicWebhookRequestFunction dynamicWebhookRequest;

        private Object sampleOutput;
        private Help help;

        @JsonIgnore
        private ListenerEnableConsumer listenerEnable;

        @JsonIgnore
        private ListenerDisableConsumer listenerDisable;

        private String name;

        private List<? extends Property<?>> outputSchema;
        private OutputSchemaDataSource outputSchemaDataSource;

        @JsonIgnore
        private PollFunction poll;

        private List<? extends Property<?>> properties;
        private SampleOutputDataSource sampleOutputDataSource;

        @JsonIgnore
        private StaticWebhookRequestFunction staticWebhookRequest;

        private String title;
        private TriggerType type;
        private Boolean webhookBodyRaw;

        @JsonIgnore
        private WebhookValidateFunction webhookValidate;

        private Boolean workflowSyncExecution;

        @JsonIgnore
        private EditorDescriptionFunction editorDescription =
            (Connection connection, InputParameters inputParameters) -> component.getTitle() + ": " + title;

        private ModifiableTriggerDefinition() {
        }

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

        public ModifiableTriggerDefinition help(Help help) {
            this.help = help;

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

        public ModifiableTriggerDefinition editorDescription(EditorDescriptionFunction editorDescription) {
            this.editorDescription = editorDescription;

            return this;
        }

        public <P extends Property<?>> ModifiableTriggerDefinition outputSchema(P... outputSchema) {
            if (outputSchema != null) {
                this.outputSchema = List.of(outputSchema);
            }

            return this;
        }

        public ModifiableTriggerDefinition outputSchema(OutputSchemaDataSource outputSchemaDataSource) {
            this.outputSchemaDataSource = outputSchemaDataSource;

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

        public ModifiableTriggerDefinition sampleOutput(SampleOutputDataSource sampleOutputDataSource) {
            this.sampleOutputDataSource = sampleOutputDataSource;

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
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ModifiableTriggerDefinition that = (ModifiableTriggerDefinition) o;

            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
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
        public String getDescription() {
            return Objects.requireNonNullElseGet(description, () -> component + ": " + name);
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
        public EditorDescriptionFunction getEditorDescription() {
            return editorDescription;
        }

        @Override
        public Optional<List<? extends Property<?>>> getOutputSchema() {
            return Optional.ofNullable(outputSchema);
        }

        @Override
        public Optional<OutputSchemaDataSource> getOutputSchemaDataSource() {
            return Optional.ofNullable(outputSchemaDataSource);
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
        public Object getSampleOutput() {
            return sampleOutput;
        }

        @Override
        public Optional<SampleOutputDataSource> getSampleOutputDataSource() {
            return Optional.ofNullable(sampleOutputDataSource);
        }

        @Override
        public Optional<StaticWebhookRequestFunction> getStaticWebhookRequest() {
            return Optional.ofNullable(staticWebhookRequest);
        }

        @Override
        public String getTitle() {
            return title;
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

        @SuppressWarnings("PMD")
        private ModifiableTriggerDefinition component(ComponentDefinition component) {
            this.component = component;

            return this;
        }
    }
}
