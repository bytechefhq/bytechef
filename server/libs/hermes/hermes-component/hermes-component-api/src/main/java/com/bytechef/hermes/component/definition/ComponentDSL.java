
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

import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.util.HttpClientUtils;
import com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;
import com.bytechef.hermes.definition.DefinitionDSL;
import com.bytechef.hermes.definition.Display;
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

    public static final int VERSION_1 = 1;

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

    public static ModifiableExampleOutputDataSource exampleOutputDataSource(
        ExampleOutputDataSource.ExampleOutputFunction exampleOutputFunction) {

        return new ModifiableExampleOutputDataSource(exampleOutputFunction);
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

    public static ModifiableComponentOptionsDataSource optionsDataSource(
        ComponentOptionsDataSource.OptionsFunction optionsFunction, String... loadOptionsDependOnPropertyNames) {

        return new ModifiableComponentOptionsDataSource(optionsFunction, List.of(loadOptionsDependOnPropertyNames));
    }

    public static ModifiableOutputSchemaDataSource outputSchemaDataSource(
        OutputSchemaDataSource.OutputSchemaFunction outputSchemaFunction) {

        return new ModifiableOutputSchemaDataSource(outputSchemaFunction);
    }

    public static ModifiableComponentPropertiesDataSource propertiesDataSource(
        ComponentPropertiesDataSource.PropertiesFunction propertiesFunction,
        String... propertiesDependOnPropertyNames) {

        return new ModifiableComponentPropertiesDataSource(propertiesFunction,
            List.of(propertiesDependOnPropertyNames));
    }

    public static ModifiableTriggerDefinition trigger(String name) {
        return new ModifiableTriggerDefinition(name);
    }

    public static final class ModifiableActionDefinition implements ActionDefinition {

        private String componentName;
        private Display display;
        private Object exampleOutput;

        @JsonIgnore
        private ExecuteFunction execute;
        private Map<String, Object> metadata;
        private String name;
        private List<? extends Property<?>> outputSchema;
        private List<? extends Property<?>> properties;
        private ExampleOutputDataSource exampleOutputDataSource;
        private OutputSchemaDataSource outputSchemaDataSource;

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

        public ModifiableActionDefinition execute(ExecuteFunction execute) {
            this.execute = execute;

            return this;
        }

        public ModifiableActionDefinition exampleOutputDataSource(ExampleOutputDataSource exampleOutputDataSource) {
            this.exampleOutputDataSource = exampleOutputDataSource;

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

        public ModifiableActionDefinition outputSchemaDataSource(OutputSchemaDataSource outputSchemaDataSource) {
            this.outputSchemaDataSource = outputSchemaDataSource;

            return this;
        }

        public <P extends Property<?>> ModifiableActionDefinition properties(P... properties) {
            this.properties = List.of(properties);

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
        @SuppressFBWarnings("NP")
        public Boolean getBatch() {
            return null;
        }

        @Override
        @JsonIgnore
        public String getComponentName() {
            return componentName;
        }

        public Display getDisplay() {
            return display;
        }

        @Override
        public Object getExampleOutput() {
            return exampleOutput;
        }

        @Override
        public Optional<ExecuteFunction> getExecute() {
            return Optional.ofNullable(execute);
        }

        @Override
        public ExampleOutputDataSource getExampleOutputDataSource() {
            return exampleOutputDataSource;
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
        public List<? extends Property<?>> getOutputSchema() {
            return outputSchema;
        }

        @Override
        public OutputSchemaDataSource getOutputSchemaDataSource() {
            return outputSchemaDataSource;
        }

        @Override
        public List<? extends Property<?>> getProperties() {
            return properties;
        }

        @Override
        public Resources getResources() {
            return null;
        }

        private ModifiableActionDefinition componentName(String componentName) {
            this.componentName = componentName;

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

            HttpClientUtils.Response response = HttpClientUtils.post(tokenUrlFunction.apply(connectionParameters))
                .payload(
                    HttpClientUtils.Payload.of(payload, HttpClientUtils.BodyContentType.FORM_URL_ENCODED))
                .configuration(responseFormat(ResponseFormat.JSON))
                .execute();

            if (response.statusCode() != 200) {
                throw new ComponentExecutionException("Invalid claim");
            }

            if (response.body() == null) {
                throw new ComponentExecutionException("Invalid claim");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.body();

            return new AuthorizationCallbackResponse(
                (String) body.get(Authorization.ACCESS_TOKEN),
                (String) body.get(Authorization.REFRESH_TOKEN), Map.of());
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

        private Display display;

        @JsonIgnore
        private List<Object> refreshOn;

        private List<? extends Property<?>> properties;

        @JsonIgnore
        private RefreshFunction refresh;

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

            if (scopes instanceof List<?>) {
                return (List<String>) scopes;
            } else if (scopes == null) {
                return Collections.emptyList();
            } else {
                return Arrays.stream(((String) scopes).split(","))
                    .filter(Objects::nonNull)
                    .filter(scope -> !scope.isBlank())
                    .map(String::trim)
                    .toList();
            }
        };

        @JsonIgnore
        private TokenUrlFunction tokenUrl = connectionParameters -> connectionParameters
            .getString(Authorization.TOKEN_URL);

        private String name;

        @JsonIgnore
        private PkceFunction pkce = (verifier, challenge) -> new Pkce(verifier, challenge, "SHA256");
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

        public ModifiableAuthorization display(ModifiableDisplay display) {
            this.display = display;

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
        public List<Object> getDetectOn() {
            return detectOn;
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
        public List<Object> getRefreshOn() {
            return refreshOn;
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
        private ConnectionDefinition connection;
        private Display display;

        @JsonIgnore
        private FilterCompatibleConnectionDefinitionsFunction filterCompatibleConnectionDefinitions;

        private Map<String, Object> metadata;
        private String name;
        private Resources resources;
        private int version = VERSION_1;
        private List<? extends TriggerDefinition> triggers;

        private ModifiableComponentDefinition() {
        }

        private ModifiableComponentDefinition(String name) {
            this.name = Objects.requireNonNull(name);
        }

        public ModifiableComponentDefinition actions(ActionDefinition... actionDefinitions) {
            if (actionDefinitions != null) {
                this.actions = Stream.of(actionDefinitions)
                    .map(actionDefinition -> ((ModifiableActionDefinition) actionDefinition).componentName(this.name))
                    .toList();
            }

            return this;
        }

        public ModifiableComponentDefinition actions(ModifiableActionDefinition... actionDefinitions) {
            if (actionDefinitions != null) {
                this.actions = Stream.of(actionDefinitions)
                    .map(actionDefinition -> actionDefinition.componentName(this.name))
                    .toList();
            }

            return this;
        }

        public ModifiableComponentDefinition connection(ConnectionDefinition connectionDefinition) {
            this.connection = ((ModifiableConnectionDefinition) connectionDefinition)
                .componentName(this.name)
                .display(this.display)
                .name(this.name);

            return this;
        }

        public ModifiableComponentDefinition connection(ModifiableConnectionDefinition connectionDefinition) {
            this.connection = connectionDefinition
                .componentName(this.name)
                .display(this.display)
                .name(this.name);

            return this;
        }

        public ModifiableComponentDefinition display(ModifiableDisplay display) {
            this.display = display;

            return this;
        }

        public ModifiableComponentDefinition filterCompatibleConnectionDefinitions(
            FilterCompatibleConnectionDefinitionsFunction filterCompatibleConnectionDefinitions) {

            this.filterCompatibleConnectionDefinitions = filterCompatibleConnectionDefinitions;

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

        public ModifiableComponentDefinition triggers(TriggerDefinition... triggerDefinitions) {
            if (triggerDefinitions != null) {
                this.triggers = Stream.of(triggerDefinitions)
                    .map(
                        triggerDefinition -> ((ModifiableTriggerDefinition) triggerDefinition)
                            .componentName(this.name))
                    .toList();
            }

            return this;
        }

        public ModifiableComponentDefinition triggers(ModifiableTriggerDefinition... triggerDefinitions) {
            if (triggerDefinitions != null) {
                this.triggers = Stream.of(triggerDefinitions)
                    .map(triggerDefinition -> triggerDefinition.componentName(this.name))
                    .toList();
            }

            return this;
        }

        public ModifiableComponentDefinition version(int version) {
            this.version = version;

            return this;
        }

        @Override
        public List<ConnectionDefinition> applyFilterCompatibleConnectionDefinitions(
            ComponentDefinition componentDefinition, List<ConnectionDefinition> connectionDefinitions) {

            List<ConnectionDefinition> filteredConnectionDefinitions = Collections.emptyList();

            if (filterCompatibleConnectionDefinitions != null) {
                filteredConnectionDefinitions = filterCompatibleConnectionDefinitions.apply(
                    componentDefinition, connectionDefinitions);
            }

            return filteredConnectionDefinitions;
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
        public FilterCompatibleConnectionDefinitionsFunction getFilterCompatibleConnectionDefinitions() {
            return filterCompatibleConnectionDefinitions;
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
        public List<? extends TriggerDefinition> getTriggers() {
            return triggers == null ? null : new ArrayList<>(triggers);
        }

        @Override
        public int getVersion() {
            return version;
        }
    }

    public static final class ModifiableComponentOptionsDataSource extends DefinitionDSL.ModifiableOptionsDataSource
        implements ComponentOptionsDataSource {

        @JsonIgnore
        private OptionsFunction options;

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

    public static final class ModifiableComponentPropertiesDataSource
        extends DefinitionDSL.ModifiablePropertiesDataSource implements ComponentPropertiesDataSource {

        @JsonIgnore
        private PropertiesFunction properties;

        private ModifiableComponentPropertiesDataSource(
            PropertiesFunction properties, List<String> loadPropertiesDependOnPropertyNames) {

            super(loadPropertiesDependOnPropertyNames);

            this.properties = properties;
        }

        @Override
        public PropertiesFunction getProperties() {
            return properties;
        }
    }

    public static final class ModifiableConnectionDefinition implements ConnectionDefinition {

        private Boolean authorizationRequired;
        private List<? extends Authorization> authorizations;

        @JsonIgnore
        private BaseUriFunction baseUri = (connectionParameters) -> connectionParameters.containsKey(BASE_URI)
            ? connectionParameters.getString(BASE_URI)
            : null;
        private String componentName;
        private Display display;
        private String name;
        private List<? extends Property<?>> properties;
        private Resources resources;

        @JsonIgnore
        private TestConsumer test;
        private int version = 1;

        private ModifiableConnectionDefinition() {
        }

        public ModifiableConnectionDefinition authorizationRequired(Boolean authorizationRequired) {
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
                this.properties = List.of(properties);
            }

            return this;
        }

        public ModifiableConnectionDefinition resources(ModifiableResources resources) {
            this.resources = resources;

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

            return componentName.equals(that.componentName) && version == that.version;
        }

        @Override
        public boolean isAuthorizationRequired() {
            return (authorizationRequired == null || authorizationRequired) && containsAuthorizations();
        }

        @Override
        public int hashCode() {
            return Objects.hash(componentName, version);
        }

        @Override
        public Authorization getAuthorization(String authorizationName) {
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
        public List<? extends Authorization> getAuthorizations() {
            return authorizations == null ? null : new ArrayList<>(authorizations);
        }

        @Override
        public BaseUriFunction getBaseUri() {
            return baseUri;
        }

        @Override
        @JsonIgnore
        public String getComponentName() {
            return componentName;
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
        public int getVersion() {
            return version;
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
        public Optional<TestConsumer> getTest() {
            return Optional.ofNullable(test);
        }

        private ModifiableConnectionDefinition componentName(String componentName) {
            this.componentName = componentName;

            return this;
        }

        @SuppressWarnings("PMD")
        private ModifiableConnectionDefinition display(Display display) {
            this.display = display;

            return this;
        }

        private ModifiableConnectionDefinition name(String name) {
            this.name = name;

            return this;
        }
    }

    public static final class ModifiableExampleOutputDataSource implements ExampleOutputDataSource {

        private final ExampleOutputFunction exampleOutput;

        public ModifiableExampleOutputDataSource(ExampleOutputFunction exampleOutput) {
            this.exampleOutput = exampleOutput;
        }

        @Override
        public ExampleOutputFunction getExampleOutput() {
            return exampleOutput;
        }
    }

    public static final class ModifiableJdbcComponentDefinition implements JdbcComponentDefinition {

        private String databaseJdbcName;
        private String jdbcDriverClassName;
        private Display display;
        private Resources resources;
        private double version = VERSION_1;
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

    public static final class ModifiableOutputSchemaDataSource implements OutputSchemaDataSource {

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

        private String componentName;
        private Display display;
        private Object exampleOutput;
        private ExampleOutputDataSource exampleOutputDataSource;
        private String name;
        private List<? extends Property<?>> outputSchema;
        private OutputSchemaDataSource outputSchemaDataSource;
        private List<? extends Property<?>> properties;
        private Resources resources;
        private TriggerType type;
        private ManualEnableConsumer manualEnable;
        private ManualDisableConsumer manualDisable;
        private PollDisableConsumer pollDisable;
        private PollEnableConsumer pollEnable;
        private PollFunction poll;
        private WebhookDisableConsumer webhookDisable;
        private WebhookEnableFunction webhookEnable;
        private WebhookRefreshFunction webhookRefresh;
        private WebhookRequestFunction webhookRequest;

        private ModifiableTriggerDefinition() {
        }

        private ModifiableTriggerDefinition(String name) {
            this.name = Objects.requireNonNull(name);
        }

        public ModifiableTriggerDefinition display(Display display) {
            this.display = display;

            return this;
        }

        public ModifiableTriggerDefinition exampleOutput(Object exampleOutput) {
            this.exampleOutput = exampleOutput;

            return this;
        }

        public ModifiableTriggerDefinition exampleOutputDataSource(ExampleOutputDataSource exampleOutputDataSource) {
            this.exampleOutputDataSource = exampleOutputDataSource;

            return this;
        }

        public ModifiableTriggerDefinition name(String name) {
            this.name = name;

            return this;
        }

        public <P extends Property<?>> ModifiableTriggerDefinition outputSchema(P... outputSchema) {
            if (outputSchema != null) {
                this.outputSchema = List.of(outputSchema);
            }

            return this;
        }

        public ModifiableTriggerDefinition outputSchemaDataSource(OutputSchemaDataSource outputSchemaDataSource) {
            this.outputSchemaDataSource = outputSchemaDataSource;

            return this;
        }

        public <P extends Property<?>> ModifiableTriggerDefinition properties(P... properties) {
            this.properties = List.of(properties);

            return this;
        }

        public ModifiableTriggerDefinition resources(Resources resources) {
            this.resources = resources;

            return this;
        }

        public ModifiableTriggerDefinition type(TriggerType type) {
            this.type = type;

            return this;
        }

        public ModifiableTriggerDefinition manualEnable(ManualEnableConsumer manualEnable) {
            this.manualEnable = manualEnable;

            return this;
        }

        public ModifiableTriggerDefinition manualDisable(ManualDisableConsumer manualDisable) {
            this.manualDisable = manualDisable;

            return this;
        }

        public ModifiableTriggerDefinition pollDisable(PollDisableConsumer pollDisable) {
            this.pollDisable = pollDisable;

            return this;
        }

        public ModifiableTriggerDefinition pollEnable(PollEnableConsumer pollEnable) {
            this.pollEnable = pollEnable;

            return this;
        }

        public ModifiableTriggerDefinition poll(PollFunction poll) {
            this.poll = poll;

            return this;
        }

        public ModifiableTriggerDefinition webhookDisable(WebhookDisableConsumer webhookDisable) {
            this.webhookDisable = webhookDisable;

            return this;
        }

        public ModifiableTriggerDefinition webhookEnable(WebhookEnableFunction webhookEnable) {
            this.webhookEnable = webhookEnable;

            return this;
        }

        public ModifiableTriggerDefinition webhookRefresh(WebhookRefreshFunction webhookRefresh) {
            this.webhookRefresh = webhookRefresh;

            return this;
        }

        public ModifiableTriggerDefinition webhookRequest(WebhookRequestFunction webhookRequest) {
            this.webhookRequest = webhookRequest;

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
        @SuppressFBWarnings("NP")
        public Boolean getBatch() {
            return null;
        }

        @Override
        @JsonIgnore
        public String getComponentName() {
            return componentName;
        }

        @Override
        public Display getDisplay() {
            return display;
        }

        @Override
        public Object getExampleOutput() {
            return exampleOutput;
        }

        @Override
        public ExampleOutputDataSource getExampleOutputDataSource() {
            return exampleOutputDataSource;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<? extends Property<?>> getOutputSchema() {
            return outputSchema;
        }

        @Override
        public OutputSchemaDataSource getOutputSchemaDataSource() {
            return outputSchemaDataSource;
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
        public TriggerType getType() {
            return type;
        }

        @Override
        public Optional<ManualEnableConsumer> getManualEnable() {
            return Optional.ofNullable(manualEnable);
        }

        @Override
        public Optional<ManualDisableConsumer> getManualDisable() {
            return Optional.ofNullable(manualDisable);
        }

        @Override
        public Optional<PollDisableConsumer> getPollDisable() {
            return Optional.ofNullable(pollDisable);
        }

        @Override
        public Optional<PollEnableConsumer> getPollEnable() {
            return Optional.ofNullable(pollEnable);
        }

        @Override
        public Optional<PollFunction> getPoll() {
            return Optional.ofNullable(poll);
        }

        @Override
        public Optional<WebhookDisableConsumer> getWebhookDisable() {
            return Optional.ofNullable(webhookDisable);
        }

        @Override
        public Optional<WebhookEnableFunction> getWebhookEnable() {
            return Optional.ofNullable(webhookEnable);
        }

        @Override
        public Optional<WebhookRefreshFunction> getWebhookRefresh() {
            return Optional.ofNullable(webhookRefresh);
        }

        @Override
        public Optional<WebhookRequestFunction> getWebhookRequest() {
            return Optional.ofNullable(webhookRequest);
        }

        private ModifiableTriggerDefinition componentName(String componentName) {
            this.componentName = componentName;

            return this;
        }
    }
}
