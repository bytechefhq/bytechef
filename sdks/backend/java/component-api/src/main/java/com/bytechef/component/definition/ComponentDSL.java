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

package com.bytechef.component.definition;

import com.bytechef.component.definition.OptionsDataSource.OptionsFunction;
import com.bytechef.component.definition.PropertiesDataSource.ActionPropertiesFunction;
import com.bytechef.component.definition.PropertiesDataSource.TriggerPropertiesFunction;
import com.bytechef.component.definition.Property.ObjectProperty;
import com.bytechef.component.definition.Property.ValueProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
public final class ComponentDSL {

    private static final int VERSION_1 = 1;

    public static ModifiableActionDefinition action(String name) {
        return new ModifiableActionDefinition(name);
    }

    public static ModifiableArrayProperty array() {
        return new ModifiableArrayProperty();
    }

    public static ModifiableArrayProperty array(String name) {
        return new ModifiableArrayProperty(name);
    }

    public static ModifiableAuthorization
        authorization(String name, Authorization.AuthorizationType authorizationType) {
        return new ModifiableAuthorization(name, authorizationType);
    }

    public static ModifiableBooleanProperty bool() {
        return new ModifiableBooleanProperty();
    }

    public static ModifiableBooleanProperty bool(String name) {
        return new ModifiableBooleanProperty(name);
    }

    public static ModifiableComponentDefinition component(String name) {
        return new ModifiableComponentDefinition(name);
    }

    public static ModifiableConnectionDefinition connection() {
        return new ModifiableConnectionDefinition();
    }

    public static ModifiableDateProperty date() {
        return new ModifiableDateProperty();
    }

    public static ModifiableDateProperty date(String name) {
        return new ModifiableDateProperty(name);
    }

    public static ModifiableDateTimeProperty dateTime() {
        return new ModifiableDateTimeProperty();
    }

    public static ModifiableDateTimeProperty dateTime(String name) {
        return new ModifiableDateTimeProperty(name);
    }

    public static ModifiableDynamicPropertiesProperty dynamicProperties(String name) {
        return new ModifiableDynamicPropertiesProperty(name);
    }

    public static ModifiableFileEntryProperty fileEntry() {
        return fileEntry(null);
    }

    public static ModifiableFileEntryProperty fileEntry(String name) {
        return new ModifiableFileEntryProperty(name);
    }

    public static ModifiableIntegerProperty integer() {
        return new ModifiableIntegerProperty();
    }

    public static ModifiableIntegerProperty integer(String name) {
        return new ModifiableIntegerProperty(name);
    }

    public static ModifiableJdbcComponentDefinition jdbcComponent(String name) {
        return new ModifiableJdbcComponentDefinition(name);
    }

    public static ModifiableNullProperty nullable() {
        return new ModifiableNullProperty();
    }

    public static ModifiableNullProperty nullable(String name) {
        return new ModifiableNullProperty(name);
    }

    public static ModifiableNumberProperty number() {
        return new ModifiableNumberProperty();
    }

    public static ModifiableNumberProperty number(String name) {
        return new ModifiableNumberProperty(name);
    }

    public static ModifiableObjectProperty object() {
        return new ModifiableObjectProperty();
    }

    public static ModifiableObjectProperty object(String name) {
        return new ModifiableObjectProperty(name);
    }

    public static ModifiableOption<Boolean> option(String label, boolean value) {
        return new ModifiableOption<>(label, value);
    }

    public static ModifiableOption<Boolean> option(String label, boolean value, String description) {
        return new ModifiableOption<>(label, value, description);
    }

    public static ModifiableOption<Double> option(String label, double value) {
        return new ModifiableOption<>(label, value);
    }

    public static ModifiableOption<Double> option(String label, double value, String description) {
        return new ModifiableOption<>(label, value, description);
    }

    public static ModifiableOption<Long> option(String label, int value) {
        return new ModifiableOption<>(label, (long) value);
    }

    public static ModifiableOption<Long> option(String label, int value, String description) {
        return new ModifiableOption<>(label, (long) value, description);
    }

    public static ModifiableOption<Long> option(String label, long value) {
        return new ModifiableOption<>(label, value);
    }

    public static ModifiableOption<Long> option(String label, long value, String description) {
        return new ModifiableOption<>(label, value, description);
    }

    public static ModifiableOption<LocalDate> option(String label, LocalDate value) {
        return new ModifiableOption<>(label, value);
    }

    public static ModifiableOption<LocalDate> option(String label, LocalDate value, String description) {
        return new ModifiableOption<>(label, value, description);
    }

    public static ModifiableOption<LocalDateTime> option(String label, LocalDateTime value) {
        return new ModifiableOption<>(label, value);
    }

    public static ModifiableOption<LocalDateTime> option(String label, LocalDateTime value, String description) {
        return new ModifiableOption<>(label, value, description);
    }

    public static ModifiableOption<Object> option(String label, Object value) {
        return new ModifiableOption<>(label, value);
    }

    public static ModifiableOption<Object> option(String label, Object value, String description) {
        return new ModifiableOption<>(label, value, description);
    }

    public static ModifiableOption<String> option(String label, String value) {
        return new ModifiableOption<>(label, value);
    }

    public static ModifiableOption<String> option(String label, String value, String description) {
        return new ModifiableOption<>(label, value, description);
    }

    public static ModifiableStringProperty string() {
        return new ModifiableStringProperty();
    }

    public static ModifiableStringProperty string(String name) {
        return new ModifiableStringProperty(name);
    }

    public static ModifiableTimeProperty time() {
        return new ModifiableTimeProperty();
    }

    public static ModifiableTimeProperty time(String name) {
        return new ModifiableTimeProperty(name);
    }

    public static ModifiableTriggerDefinition trigger(String name) {
        return new ModifiableTriggerDefinition(name);
    }

    public static final class ModifiableActionDefinition implements ActionDefinition {

        private Boolean batch;
        private boolean defaultOutputFunction;
        private Boolean deprecated;
        private String description;
        private PerformFunction performFunction;
        private Help help;
        private Map<String, Object> metadata;
        private final String name;
        private ModifiableValueProperty<?, ?> outputSchema;
        private OutputFunction outputFunction;
        private List<? extends Property> properties;
        private Object sampleOutput;
        private String title;
        private ActionWorkflowNodeDescriptionFunction workflowNodeDescriptionFunction;

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

        public ModifiableActionDefinition perform(PerformFunction perform) {
            this.performFunction = perform;

            return this;
        }

        public ModifiableActionDefinition perform(SingleConnectionPerformFunction perform) {
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

        public ModifiableActionDefinition output() {
            this.defaultOutputFunction = true;

            return this;
        }

        public ModifiableActionDefinition output(OutputFunction output) {
            this.outputFunction = output;
            this.defaultOutputFunction = false;

            return this;
        }

        public ModifiableActionDefinition output(SingleConnectionOutputFunction output) {
            this.outputFunction = output;
            this.defaultOutputFunction = false;

            return this;
        }

        public <P extends ModifiableValueProperty<?, ?>> ModifiableActionDefinition outputSchema(P outputSchema) {
            this.outputSchema = outputSchema;
            this.defaultOutputFunction = true;

            return this;
        }

        @SafeVarargs
        public final <P extends Property> ModifiableActionDefinition properties(P... properties) {
            if (properties != null) {
                this.properties = List.of(properties);
            }

            return this;
        }

        public <P extends Property> ModifiableActionDefinition properties(List<P> properties) {
            if (properties != null) {
                this.properties = Collections.unmodifiableList(properties);
            }

            return this;
        }

        public ModifiableActionDefinition sampleOutput(Object sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableActionDefinition title(String title) {
            this.title = title;

            return this;
        }

        public ModifiableActionDefinition workflowNodeDescription(
            ActionWorkflowNodeDescriptionFunction workflowNodeDescriptionFunction) {

            this.workflowNodeDescriptionFunction = workflowNodeDescriptionFunction;

            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableActionDefinition that)) {
                return false;
            }

            return Objects.equals(description, that.description)
                && Objects.equals(performFunction, that.performFunction) && Objects.equals(help, that.help)
                && Objects.equals(metadata, that.metadata) && Objects.equals(name, that.name)
                && Objects.equals(outputSchema, that.outputSchema)
                && Objects.equals(defaultOutputFunction, that.defaultOutputFunction)
                && Objects.equals(outputFunction, that.outputFunction)
                && Objects.equals(sampleOutput, that.sampleOutput)
                && Objects.equals(properties, that.properties) && Objects.equals(title, that.title)
                && Objects.equals(workflowNodeDescriptionFunction, that.workflowNodeDescriptionFunction);
        }

        @Override
        public int hashCode() {
            return Objects.hash(batch, deprecated, description, performFunction, help, metadata, name,
                outputSchema, defaultOutputFunction, outputFunction, sampleOutput, properties, title,
                workflowNodeDescriptionFunction);
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
        public Optional<Output> getOutput() {
            return Optional.ofNullable(outputSchema == null ? null : new Output(outputSchema, sampleOutput));
        }

        @Override
        public Optional<OutputFunction> getOutputFunction() {
            return Optional.ofNullable(outputFunction);
        }

        @Override
        public Optional<List<? extends Property>> getProperties() {
            return Optional.ofNullable(properties);
        }

        @Override
        public Optional<String> getTitle() {
            return Optional.ofNullable(title);
        }

        @Override
        public Optional<ActionWorkflowNodeDescriptionFunction> getWorkflowNodeDescriptionFunction() {
            return Optional.ofNullable(workflowNodeDescriptionFunction);
        }

        @Override
        public boolean isDefaultOutputFunction() {
            return defaultOutputFunction;
        }

        @Override
        public String toString() {
            return "ModifiableActionDefinition{" +
                "name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", batch=" + batch +
                ", deprecated=" + deprecated +
                ", help=" + help +
                ", outputSchema=" + outputSchema +
                ", properties=" + properties +
                ", sampleOutput=" + sampleOutput +
                ", metadata=" + metadata +
                '}';
        }
    }

    public static final class ModifiableArrayProperty
        extends ModifiableValueProperty<List<?>, ModifiableArrayProperty> implements Property.ArrayProperty {

        private List<? extends ModifiableValueProperty<?, ?>> items;
        private List<String> optionsLookupDependsOn;
        private Long maxItems;
        private Long minItems;
        private Boolean multipleValues;
        private List<? extends Option<Object>> options;
        private OptionsFunction optionsFunction;

        private ModifiableArrayProperty() {
            this(null);
        }

        private ModifiableArrayProperty(String name) {
            super(name, Type.ARRAY);
        }

        public ModifiableArrayProperty defaultValue(Boolean... defaultValue) {
            this.defaultValue = defaultValue == null ? List.of() : List.of((Object[]) defaultValue);

            return this;
        }

        public ModifiableArrayProperty defaultValue(Integer... defaultValue) {
            this.defaultValue = defaultValue == null ? List.of() : List.of((Object[]) defaultValue);

            return this;
        }

        public ModifiableArrayProperty defaultValue(Long... defaultValue) {
            this.defaultValue = defaultValue == null ? List.of() : List.of((Object[]) defaultValue);

            return this;
        }

        public ModifiableArrayProperty defaultValue(Float... defaultValue) {
            this.defaultValue = defaultValue == null ? List.of() : List.of((Object[]) defaultValue);

            return this;
        }

        public ModifiableArrayProperty defaultValue(Double... defaultValue) {
            this.defaultValue = defaultValue == null ? List.of() : List.of((Object[]) defaultValue);

            return this;
        }

        public ModifiableArrayProperty defaultValue(String... defaultValue) {
            this.defaultValue = defaultValue == null ? List.of() : List.of((Object[]) defaultValue);

            return this;
        }

        @SafeVarargs
        public final ModifiableArrayProperty defaultValue(Map<String, ?>... defaultValue) {
            this.defaultValue = defaultValue == null ? List.of() : List.of((Object[]) defaultValue);

            return this;
        }

        public ModifiableArrayProperty exampleValue(Boolean... exampleValue) {
            this.exampleValue = exampleValue == null ? List.of() : List.of((Object[]) exampleValue);

            return this;
        }

        public ModifiableArrayProperty exampleValue(Integer... exampleValue) {
            this.exampleValue = exampleValue == null ? List.of() : List.of((Object[]) exampleValue);

            return this;
        }

        public ModifiableArrayProperty exampleValue(Long... exampleValue) {
            this.exampleValue = exampleValue == null ? List.of() : List.of((Object[]) exampleValue);

            return this;
        }

        public ModifiableArrayProperty exampleValue(Float... exampleValue) {
            this.exampleValue = exampleValue == null ? List.of() : List.of((Object[]) exampleValue);

            return this;
        }

        public ModifiableArrayProperty exampleValue(Double... exampleValue) {
            this.exampleValue = exampleValue == null ? List.of() : List.of((Object[]) exampleValue);

            return this;
        }

        public ModifiableArrayProperty exampleValue(String... exampleValue) {
            this.exampleValue = exampleValue == null ? List.of() : List.of((Object[]) exampleValue);

            return this;
        }

        @SafeVarargs
        public final ModifiableArrayProperty exampleValue(Map<String, ?>... exampleValue) {
            this.exampleValue = exampleValue == null ? List.of() : List.of((Object[]) exampleValue);

            return this;
        }

        @SafeVarargs
        public final <P extends ModifiableValueProperty<?, ?>> ModifiableArrayProperty items(P... properties) {
            return items(properties == null ? List.of() : List.of(properties));
        }

        public <P extends ModifiableValueProperty<?, ?>> ModifiableArrayProperty items(List<P> properties) {
            if (properties != null) {
                this.items = new ArrayList<>(properties);
            }

            return this;
        }

        public ModifiableArrayProperty optionsLookupDependsOn(String... optionsLookupDependsOn) {
            if (optionsLookupDependsOn != null) {
                this.optionsLookupDependsOn = List.of(optionsLookupDependsOn);
            }

            return this;
        }

        public ModifiableArrayProperty maxItems(long maxItems) {
            this.maxItems = maxItems;

            return this;
        }

        public ModifiableArrayProperty minItems(long minItems) {
            this.minItems = minItems;

            return this;
        }

        public ModifiableArrayProperty multipleValues(boolean multipleValues) {
            this.multipleValues = multipleValues;

            return this;
        }

        @SafeVarargs
        public final ModifiableArrayProperty options(Option<Object>... options) {
            if (options != null) {
                this.options = List.of(options);
            }

            return this;
        }

        @SuppressWarnings("unchecked")
        public ModifiableArrayProperty options(List<? extends Option<?>> options) {
            this.options = new ArrayList<>((List<? extends Option<Object>>) options);

            return this;
        }

        public ModifiableArrayProperty options(OptionsFunction optionsFunction) {
            this.optionsFunction = optionsFunction;

            return this;
        }

        @Override
        public ControlType getControlType() {
            if (options == null && optionsFunction == null) {
                return ControlType.ARRAY_BUILDER;
            } else {
                return ControlType.MULTI_SELECT;
            }
        }

        @Override
        public Optional<List<? extends ValueProperty<?>>> getItems() {
            return Optional.ofNullable(items);
        }

        @Override
        public Optional<Long> getMaxItems() {
            return Optional.ofNullable(maxItems);
        }

        @Override
        public Optional<Long> getMinItems() {
            return Optional.ofNullable(minItems);
        }

        @Override
        public Optional<Boolean> getMultipleValues() {
            return Optional.ofNullable(multipleValues);
        }

        @Override
        public Optional<List<? extends Option<Object>>> getOptions() {
            return Optional.ofNullable(options);
        }

        @Override
        public Optional<OptionsDataSource> getOptionsDataSource() {
            return Optional.ofNullable(
                optionsFunction == null
                    ? null
                    : new OptionsDataSourceImpl(optionsLookupDependsOn, optionsFunction));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableArrayProperty that)) {
                return false;
            }

            if (!super.equals(o)) {
                return false;
            }

            return Objects.equals(items, that.items)
                && Objects.equals(optionsLookupDependsOn, that.optionsLookupDependsOn)
                && Objects.equals(maxItems, that.maxItems) && Objects.equals(minItems, that.minItems)
                && Objects.equals(multipleValues, that.multipleValues) && Objects.equals(options, that.options)
                && Objects.equals(optionsFunction, that.optionsFunction);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                super.hashCode(), items, optionsLookupDependsOn, multipleValues, options, optionsFunction);
        }
    }

    public static final class ModifiableAuthorization implements Authorization {

        private AcquireFunction acquireFunction;
        private ApplyFunction applyFunction;
        private AuthorizationCallbackFunction authorizationCallbackFunction;
        private AuthorizationUrlFunction authorizationUrlFunction;
        private ClientIdFunction clientIdFunction;
        private ClientSecretFunction clientSecretFunction;
        private RefreshTokenFunction refreshTokenFunction;
        private List<Object> detectOn;
        private String description;
        private final String name;
        private List<? extends Property> properties;
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

        public ModifiableAuthorization refreshTokenFunction(RefreshTokenFunction refreshTokenFunction) {
            if (refreshTokenFunction != null) {
                this.refreshTokenFunction = refreshTokenFunction;
            }

            return this;
        }

        public ModifiableAuthorization pkce(PkceFunction pkce) {
            this.pkceFunction = Objects.requireNonNull(pkce);

            return this;
        }

        @SafeVarargs
        public final <P extends Property> ModifiableAuthorization properties(P... properties) {
            if (properties != null) {
                this.properties = List.of(properties);
            }

            return this;
        }

        public ModifiableAuthorization refresh(RefreshFunction refresh) {
            this.refreshFunction = refresh;

            return this;
        }

        public ModifiableAuthorization refreshToken(RefreshTokenFunction refreshTokenFunction) {
            this.refreshTokenFunction = Objects.requireNonNull(refreshTokenFunction);

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
        public Optional<List<? extends Property>> getProperties() {
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
        public Optional<RefreshTokenFunction> getRefreshToken() {
            return Optional.ofNullable(refreshTokenFunction);
        }

        @Override
        public Optional<TokenUrlFunction> getTokenUrl() {
            return Optional.ofNullable(tokenUrlFunction);
        }

        @Override
        public AuthorizationType getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableAuthorization that)) {
                return false;
            }

            return Objects.equals(acquireFunction, that.acquireFunction)
                && Objects.equals(applyFunction, that.applyFunction)
                && Objects.equals(authorizationCallbackFunction, that.authorizationCallbackFunction)
                && Objects.equals(authorizationUrlFunction, that.authorizationUrlFunction)
                && Objects.equals(clientIdFunction, that.clientIdFunction)
                && Objects.equals(clientSecretFunction, that.clientSecretFunction)
                && Objects.equals(detectOn, that.detectOn) && Objects.equals(description, that.description)
                && Objects.equals(name, that.name) && Objects.equals(properties, that.properties)
                && Objects.equals(refreshFunction, that.refreshFunction) && Objects.equals(refreshOn, that.refreshOn)
                && Objects.equals(refreshUrlFunction, that.refreshUrlFunction)
                && Objects.equals(scopesFunction, that.scopesFunction)
                && Objects.equals(pkceFunction, that.pkceFunction) && Objects.equals(title, that.title)
                && Objects.equals(tokenUrlFunction, that.tokenUrlFunction) && type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(acquireFunction, applyFunction, authorizationCallbackFunction, authorizationUrlFunction,
                clientIdFunction, clientSecretFunction, detectOn, description, name, properties, refreshFunction,
                refreshOn, refreshUrlFunction, scopesFunction, pkceFunction, title, tokenUrlFunction, type);
        }
    }

    public static final class ModifiableBooleanProperty
        extends ModifiableValueProperty<Boolean, ModifiableBooleanProperty>
        implements Property.BooleanProperty {

        private final List<Option<Boolean>> options = List.of(
            option("True", true),
            option("False", false));

        private ModifiableBooleanProperty() {
            this(null);
        }

        private ModifiableBooleanProperty(String name) {
            super(name, Type.BOOLEAN);
        }

        public ModifiableBooleanProperty defaultValue(boolean defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ModifiableBooleanProperty exampleValue(boolean exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        @Override
        public ControlType getControlType() {
            return ControlType.SELECT;
        }

        @Override
        public Optional<List<? extends Option<Boolean>>> getOptions() {
            return Optional.of(options);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableBooleanProperty that)) {
                return false;
            }

            if (!super.equals(o)) {
                return false;
            }

            return Objects.equals(options, that.options);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), options);
        }
    }

    public static final class ModifiableComponentDefinition implements ComponentDefinition {

        private List<? extends ActionDefinition> actionDefinitions;
        private Boolean additionalConnections;
        private String category;
        private ConnectionDefinition connectionDefinition;
        private Boolean connectionRequired;
        private Boolean customAction;
        private Help customActionHelp;
        private DataStreamItemReader dataStreamItemReader;
        private DataStreamItemWriter dataStreamItemWriter;
        private String description;
        private String icon;
        private List<String> tags;
        private Map<String, Object> metadata;
        private final String name;
        private Resources resources;
        private int version = VERSION_1;
        private String title;
        private List<? extends TriggerDefinition> triggerDefinitions;
        private List<String> workflowConnectionKeys;

        private ModifiableComponentDefinition(String name) {
            this.name = Objects.requireNonNull(name);
        }

        @SafeVarargs
        public final <A extends ActionDefinition> ModifiableComponentDefinition actions(
            A... actionDefinitions) {

            if (actionDefinitions != null) {
                return actions(List.of(actionDefinitions));
            }

            return this;
        }

        public <A extends ActionDefinition> ModifiableComponentDefinition actions(List<A> actionDefinitions) {
            this.actionDefinitions = Collections.unmodifiableList(Objects.requireNonNull(actionDefinitions));

            return this;
        }

        public ModifiableComponentDefinition additionalConnections(boolean additionalConnections) {
            this.additionalConnections = additionalConnections;

            return this;
        }

        public ModifiableComponentDefinition category(String category) {
            this.category = category;

            return this;
        }

        public ModifiableComponentDefinition connection(ModifiableConnectionDefinition connectionDefinition) {
            this.connectionDefinition = connectionDefinition;

            return this;
        }

        public ModifiableComponentDefinition connectionRequired(boolean connectionRequired) {
            this.connectionRequired = connectionRequired;

            return this;
        }

        public ModifiableComponentDefinition customAction(boolean customAction) {
            this.customAction = customAction;

            return this;
        }

        public ModifiableComponentDefinition customActionHelp(Help customActionHelp) {
            this.customActionHelp = customActionHelp;

            return this;
        }

        public ModifiableComponentDefinition dataStreamItemReader(DataStreamItemReader dataStreamItemReader) {
            this.dataStreamItemReader = dataStreamItemReader;

            return this;
        }

        public ModifiableComponentDefinition dataStreamItemWriter(DataStreamItemWriter dataStreamItemWriter) {
            this.dataStreamItemWriter = dataStreamItemWriter;

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
        public final <T extends TriggerDefinition> ModifiableComponentDefinition triggers(
            T... triggerDefinitions) {

            if (triggerDefinitions != null) {
                return triggers(List.of(triggerDefinitions));
            }

            return this;
        }

        public <T extends TriggerDefinition> ModifiableComponentDefinition triggers(
            List<T> triggerDefinitions) {

            this.triggerDefinitions = Collections.unmodifiableList(Objects.requireNonNull(triggerDefinitions));

            return this;
        }

        public ModifiableComponentDefinition version(int version) {
            this.version = version;

            return this;
        }

        public ModifiableComponentDefinition workflowConnectionKeys(String... workflowConnectionKeys) {
            this.workflowConnectionKeys = List.of(workflowConnectionKeys);

            return this;
        }

        @Override
        public Optional<List<? extends ActionDefinition>> getActions() {
            return Optional.ofNullable(actionDefinitions == null ? null : new ArrayList<>(actionDefinitions));
        }

        @Override
        public Optional<String> getCategory() {
            return Optional.ofNullable(category);
        }

        @Override
        public Optional<ConnectionDefinition> getConnection() {
            return Optional.ofNullable(connectionDefinition);
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
        public Optional<DataStreamItemReader> getDataStreamItemReader() {
            return Optional.ofNullable(dataStreamItemReader);
        }

        @Override
        public Optional<DataStreamItemWriter> getDataStreamItemWriter() {
            return Optional.ofNullable(dataStreamItemWriter);
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
            return Optional.ofNullable(triggerDefinitions == null ? null : new ArrayList<>(triggerDefinitions));
        }

        @Override
        public int getVersion() {
            return version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableComponentDefinition that)) {
                return false;
            }

            return Objects.equals(actionDefinitions, that.actionDefinitions)
                && Objects.equals(additionalConnections, that.additionalConnections)
                && Objects.equals(category, that.category)
                && Objects.equals(connectionDefinition, that.connectionDefinition)
                && Objects.equals(connectionRequired, that.connectionRequired)
                && Objects.equals(customAction, that.customAction)
                && Objects.equals(customActionHelp, that.customActionHelp)
                && Objects.equals(description, that.description) && Objects.equals(icon, that.icon)
                && Objects.equals(tags, that.tags)
                && Objects.equals(metadata, that.metadata) && Objects.equals(name, that.name)
                && Objects.equals(resources, that.resources) && Objects.equals(title, that.title)
                && Objects.equals(triggerDefinitions, that.triggerDefinitions)
                && version == that.version && workflowConnectionKeys == that.workflowConnectionKeys;
        }

        @Override
        public int hashCode() {
            return Objects.hash(actionDefinitions, category, connectionDefinition, customAction, customActionHelp,
                description, icon, tags, metadata, name, resources, version, title, triggerDefinitions,
                workflowConnectionKeys);
        }

        @Override
        public String toString() {
            return "ModifiableComponentDefinition{" +
                "name='" + name + '\'' +
                ", version=" + version +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", connectionDefinition=" + connectionDefinition +
                ", additionalConnections=" + additionalConnections +
                ", category='" + category + '\'' +
                ", connectionRequired=" + connectionRequired +
                ", customAction=" + customAction +
                ", customActionHelp=" + customActionHelp +
                ", dataStreamItemReader=" + dataStreamItemReader +
                ", dataStreamItemWriter=" + dataStreamItemWriter +
                ", metadata=" + metadata +
                ", resources=" + resources +
                ", tags=" + tags +
                ", actionDefinitions=" + actionDefinitions +
                ", triggerDefinitions=" + triggerDefinitions +
                ", workflowConnectionKeys=" + workflowConnectionKeys +
                ", icon='" + icon + '\'' +
                '}';
        }
    }

    public static final class ModifiableConnectionDefinition implements ConnectionDefinition {

        private List<? extends ModifiableAuthorization> authorizations;
        private BaseUriFunction baseUriFunction;
        private List<? extends Property> properties;
        private TestConsumer testConsumer;
        private int version = 1;
        private Boolean authorizationRequired;

        private ModifiableConnectionDefinition() {
        }

        @SuppressWarnings({
            "rawtypes", "unchecked"
        })
        public <P extends Property> ModifiableConnectionDefinition append(P property) {
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
        public final <P extends Property> ModifiableConnectionDefinition properties(P... properties) {
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
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableConnectionDefinition that)) {
                return false;
            }

            return Objects.equals(authorizationRequired, that.authorizationRequired)
                && Objects.equals(authorizations, that.authorizations)
                && Objects.equals(baseUriFunction, that.baseUriFunction) && Objects.equals(properties, that.properties)
                && Objects.equals(testConsumer, that.testConsumer) && version == that.version;
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                authorizationRequired, authorizations, baseUriFunction, properties, testConsumer, version);
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
        public Optional<BaseUriFunction> getBaseUri() {
            return Optional.ofNullable(baseUriFunction);
        }

        @Override
        public Optional<List<? extends Property>> getProperties() {
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

        @Override
        public String toString() {
            return "ModifiableConnectionDefinition{" +
                ", version=" + version +
                ", authorizationRequired=" + authorizationRequired +
                ", authorizations=" + authorizations +
                ", properties=" + properties +
                '}';
        }
    }

    public static final class ModifiableDateProperty
        extends ModifiableValueProperty<LocalDate, ModifiableDateProperty>
        implements Property.DateProperty {

        private List<String> optionsLookupDependsOn;
        private List<Option<LocalDate>> options;
        private OptionsFunction optionsFunction;

        private ModifiableDateProperty() {
            this(null);
        }

        private ModifiableDateProperty(String name) {
            super(name, Type.DATE);
        }

        public ModifiableDateProperty defaultValue(LocalDate defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ModifiableDateProperty exampleValue(LocalDate exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ModifiableDateProperty optionsLookupDependsOn(String... optionsLookupDependsOn) {
            if (optionsLookupDependsOn != null) {
                this.optionsLookupDependsOn = List.of(optionsLookupDependsOn);
            }

            return this;
        }

        @SafeVarargs
        public final ModifiableDateProperty options(Option<LocalDate>... options) {
            if (options != null) {
                this.options = List.of(options);
            }

            return this;
        }

        public ModifiableDateProperty options(OptionsFunction optionsFunction) {
            this.optionsFunction = optionsFunction;

            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableDateProperty that)) {
                return false;
            }

            if (!super.equals(o)) {
                return false;
            }

            return Objects.equals(optionsLookupDependsOn, that.optionsLookupDependsOn)
                && Objects.equals(options, that.options) && Objects.equals(optionsFunction, that.optionsFunction);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), optionsLookupDependsOn, options, optionsFunction);
        }

        @Override
        public ControlType getControlType() {
            if (options == null && optionsFunction == null) {
                return ControlType.DATE;
            } else {
                return ControlType.SELECT;
            }
        }

        @Override
        public Optional<List<? extends Option<LocalDate>>> getOptions() {
            return Optional.ofNullable(options);
        }

        @Override
        public Optional<OptionsDataSource> getOptionsDataSource() {
            return Optional.ofNullable(
                optionsFunction == null
                    ? null
                    : new OptionsDataSourceImpl(optionsLookupDependsOn, optionsFunction));
        }
    }

    public static final class ModifiableDateTimeProperty
        extends ModifiableValueProperty<LocalDateTime, ModifiableDateTimeProperty>
        implements Property.DateTimeProperty {

        private List<String> optionsLookupDependsOn;
        private List<Option<LocalDateTime>> options;
        private OptionsFunction optionsFunction;

        private ModifiableDateTimeProperty() {
            this(null);
        }

        private ModifiableDateTimeProperty(String name) {
            super(name, Type.DATE_TIME);
        }

        public ModifiableDateTimeProperty defaultValue(LocalDateTime defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ModifiableDateTimeProperty exampleValue(LocalDateTime exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ModifiableDateTimeProperty optionsLookupDependsOn(String... optionsLookupDependsOn) {
            if (optionsLookupDependsOn != null) {
                this.optionsLookupDependsOn = List.of(optionsLookupDependsOn);
            }

            return this;
        }

        @SafeVarargs
        public final ModifiableDateTimeProperty options(Option<LocalDateTime>... options) {
            if (options != null) {
                this.options = List.of(options);
            }

            return this;
        }

        public ModifiableDateTimeProperty options(OptionsFunction optionsFunction) {
            this.optionsFunction = optionsFunction;

            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableDateTimeProperty that)) {
                return false;
            }

            if (!super.equals(o)) {
                return false;
            }

            return Objects.equals(optionsLookupDependsOn, that.optionsLookupDependsOn)
                && Objects.equals(options, that.options) && Objects.equals(optionsFunction, that.optionsFunction);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), optionsLookupDependsOn, options, optionsFunction);
        }

        @Override
        public ControlType getControlType() {
            if (options == null && optionsFunction == null) {
                return ControlType.DATE_TIME;
            } else {
                return ControlType.SELECT;
            }
        }

        @Override
        public Optional<List<? extends Option<LocalDateTime>>> getOptions() {
            return Optional.ofNullable(options);
        }

        @Override
        public Optional<OptionsDataSource> getOptionsDataSource() {
            return Optional.ofNullable(
                optionsFunction == null
                    ? null
                    : new OptionsDataSourceImpl(optionsLookupDependsOn, optionsFunction));
        }
    }

    public static final class ModifiableDynamicPropertiesProperty
        extends ModifiableProperty<ModifiableDynamicPropertiesProperty>
        implements Property.DynamicPropertiesProperty {

        private String header;
        private List<String> propertiesLookupDependsOn;
        private PropertiesDataSource.PropertiesFunction propertiesFunction;

        public ModifiableDynamicPropertiesProperty(String name) {
            super(name, Type.DYNAMIC_PROPERTIES);
        }

        public ModifiableDynamicPropertiesProperty propertiesLookupDependsOn(String... propertiesLookupDependsOn) {
            if (propertiesLookupDependsOn != null) {
                this.propertiesLookupDependsOn = List.of(propertiesLookupDependsOn);
            }

            return this;
        }

        public ModifiableDynamicPropertiesProperty header(String header) {
            this.header = header;

            return this;
        }

        public ModifiableDynamicPropertiesProperty properties(ActionPropertiesFunction propertiesFunction) {
            this.propertiesFunction = propertiesFunction;

            return this;
        }

        public ModifiableDynamicPropertiesProperty properties(TriggerPropertiesFunction propertiesFunction) {
            this.propertiesFunction = propertiesFunction;

            return this;
        }

        @Override
        public Optional<String> getHeader() {
            return Optional.ofNullable(header);
        }

        @Override
        public PropertiesDataSource<?> getDynamicPropertiesDataSource() {
            if (propertiesFunction == null) {
                return null;
            }

            return new PropertiesDataSourceImpl(propertiesLookupDependsOn, propertiesFunction);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            if (!super.equals(o)) {
                return false;
            }

            ModifiableDynamicPropertiesProperty that = (ModifiableDynamicPropertiesProperty) o;

            return Objects.equals(propertiesLookupDependsOn, that.propertiesLookupDependsOn)
                && Objects.equals(propertiesFunction, that.propertiesFunction);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), propertiesLookupDependsOn, propertiesFunction);
        }
    }

    public static final class ModifiableFileEntryProperty
        extends ModifiableValueProperty<Map<String, ?>, ModifiableFileEntryProperty>
        implements Property.FileEntryProperty {

        private final List<? extends ValueProperty<?>> properties = List.of(
            string("extension").required(true), string("mimeType").required(true),
            string("name").required(true), string("url").required(true));

        private ModifiableFileEntryProperty() {
            this(null);
        }

        private ModifiableFileEntryProperty(String name) {
            super(name, Type.FILE_ENTRY);
        }

        @Override
        public ControlType getControlType() {
            return ControlType.FILE_ENTRY;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableFileEntryProperty that)) {
                return false;
            }

            if (!super.equals(o)) {
                return false;
            }

            return Objects.equals(properties, that.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), properties);
        }

        @Override
        public List<? extends ValueProperty<?>> getProperties() {
            return new ArrayList<>(properties);
        }
    }

    public static class ModifiableIntegerProperty
        extends ModifiableValueProperty<Long, ModifiableIntegerProperty>
        implements Property.IntegerProperty {

        private List<String> optionsLookupDependsOn;
        private Long maxValue;
        private Long minValue;
        private List<Option<Long>> options;
        private OptionsFunction optionsFunction;

        private ModifiableIntegerProperty() {
            this(null);
        }

        private ModifiableIntegerProperty(String name) {
            super(name, Type.INTEGER);
        }

        public ModifiableIntegerProperty defaultValue(long value) {
            this.defaultValue = value;

            return this;
        }

        public ModifiableIntegerProperty exampleValue(long exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ModifiableIntegerProperty optionsLookupDependsOn(String... optionsLookupDependsOn) {
            if (optionsLookupDependsOn != null) {
                this.optionsLookupDependsOn = List.of(optionsLookupDependsOn);
            }

            return this;
        }

        public ModifiableIntegerProperty maxValue(long maxValue) {
            this.maxValue = maxValue;

            return this;
        }

        public ModifiableIntegerProperty minValue(long minValue) {
            this.minValue = minValue;

            return this;
        }

        @SafeVarargs
        public final ModifiableIntegerProperty options(Option<Long>... options) {
            if (options != null) {
                this.options = List.of(options);
            }

            return this;
        }

        public ModifiableIntegerProperty options(List<Option<Long>> options) {
            if (options != null) {
                this.options = Collections.unmodifiableList(options);
            }

            return this;
        }

        public ModifiableIntegerProperty options(OptionsFunction optionsFunction) {
            this.optionsFunction = optionsFunction;

            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableIntegerProperty that)) {
                return false;
            }

            if (!super.equals(o)) {
                return false;
            }

            return Objects.equals(optionsLookupDependsOn, that.optionsLookupDependsOn)
                && Objects.equals(maxValue, that.maxValue) && Objects.equals(minValue, that.minValue)
                && Objects.equals(options, that.options) && Objects.equals(optionsFunction, that.optionsFunction);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), optionsLookupDependsOn, maxValue, minValue, options, optionsFunction);
        }

        @Override
        public ControlType getControlType() {
            if (options == null && optionsFunction == null) {
                return ControlType.INTEGER;
            } else {
                return ControlType.SELECT;
            }
        }

        @Override
        public Optional<Long> getMaxValue() {
            return Optional.ofNullable(maxValue);
        }

        @Override
        public Optional<Long> getMinValue() {
            return Optional.ofNullable(minValue);
        }

        @Override
        public Optional<List<? extends Option<Long>>> getOptions() {
            return Optional.ofNullable(options);
        }

        @Override
        public Optional<OptionsDataSource> getOptionsDataSource() {
            return Optional.ofNullable(
                optionsFunction == null
                    ? null
                    : new OptionsDataSourceImpl(optionsLookupDependsOn, optionsFunction));
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
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableJdbcComponentDefinition that)) {
                return false;
            }

            return version == that.version && Objects.equals(databaseJdbcName, that.databaseJdbcName)
                && Objects.equals(icon, that.icon) && Objects.equals(jdbcDriverClassName, that.jdbcDriverClassName)
                && Objects.equals(description, that.description) && Objects.equals(name, that.name)
                && Objects.equals(resources, that.resources) && Objects.equals(title, that.title);
        }

        @Override
        public int hashCode() {
            return Objects.hash(databaseJdbcName, icon, jdbcDriverClassName, description, name, resources, title,
                version);
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

    public static final class ModifiableNullProperty
        extends ModifiableValueProperty<Void, ModifiableNullProperty>
        implements Property.NullProperty {

        private ModifiableNullProperty() {
            this(null);
        }

        public ModifiableNullProperty(String name) {
            super(name, Type.NULL);
        }

        @Override
        public ControlType getControlType() {
            return null;
        }
    }

    public static final class ModifiableNumberProperty
        extends ModifiableValueProperty<Double, ModifiableNumberProperty>
        implements Property.NumberProperty {

        private List<String> optionsLookupDependsOn;
        private Integer maxNumberPrecision;
        private Double maxValue;
        private Integer minNumberPrecision;
        private Double minValue;
        private Integer numberPrecision;
        private List<Option<Double>> options;
        private OptionsFunction optionsFunction;

        private ModifiableNumberProperty() {
            this(null);
        }

        private ModifiableNumberProperty(String name) {
            super(name, Type.NUMBER);
        }

        public ModifiableNumberProperty defaultValue(int value) {
            this.defaultValue = (double) value;

            return this;
        }

        public ModifiableNumberProperty defaultValue(long value) {
            this.defaultValue = (double) value;

            return this;
        }

        public ModifiableNumberProperty defaultValue(float value) {
            this.defaultValue = (double) value;

            return this;
        }

        public ModifiableNumberProperty defaultValue(double value) {
            this.defaultValue = value;

            return this;
        }

        public ModifiableNumberProperty exampleValue(int exampleValue) {
            this.exampleValue = (double) exampleValue;

            return this;
        }

        public ModifiableNumberProperty exampleValue(long exampleValue) {
            this.exampleValue = (double) exampleValue;

            return this;
        }

        public ModifiableNumberProperty exampleValue(float exampleValue) {
            this.exampleValue = (double) exampleValue;

            return this;
        }

        public ModifiableNumberProperty exampleValue(double exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ModifiableNumberProperty optionsLookupDependsOn(String... optionsLookupDependsOn) {
            if (optionsLookupDependsOn != null) {
                this.optionsLookupDependsOn = List.of(optionsLookupDependsOn);
            }

            return this;
        }

        public ModifiableNumberProperty maxNumberPrecision(Integer maxNumberPrecision) {
            this.maxNumberPrecision = maxNumberPrecision;

            return this;
        }

        public ModifiableNumberProperty maxValue(double maxValue) {
            this.maxValue = maxValue;

            return this;
        }

        public ModifiableNumberProperty minNumberPrecision(Integer minNumberPrecision) {
            this.minNumberPrecision = minNumberPrecision;

            return this;
        }

        public ModifiableNumberProperty minValue(double minValue) {
            this.minValue = minValue;

            return this;
        }

        public ModifiableNumberProperty numberPrecision(Integer numberPrecision) {
            this.numberPrecision = numberPrecision;

            return this;
        }

        @SafeVarargs
        public final ModifiableNumberProperty options(Option<Double>... options) {
            if (options != null) {
                this.options = List.of(options);
            }

            return this;
        }

        public ModifiableNumberProperty options(OptionsFunction optionsFunction) {
            this.optionsFunction = optionsFunction;

            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableNumberProperty that)) {
                return false;
            }

            if (!super.equals(o)) {
                return false;
            }

            return Objects.equals(optionsLookupDependsOn, that.optionsLookupDependsOn)
                && Objects.equals(maxNumberPrecision, that.maxNumberPrecision)
                && Objects.equals(maxValue, that.maxValue)
                && Objects.equals(minNumberPrecision, that.minNumberPrecision)
                && Objects.equals(minValue, that.minValue) && Objects.equals(numberPrecision, that.numberPrecision)
                && Objects.equals(options, that.options) && Objects.equals(optionsFunction, that.optionsFunction);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), optionsLookupDependsOn, maxNumberPrecision, maxValue,
                minNumberPrecision, minValue, numberPrecision, options, optionsFunction);
        }

        @Override
        public ControlType getControlType() {
            if (options == null && optionsFunction == null) {
                return ControlType.NUMBER;
            } else {
                return ControlType.SELECT;
            }
        }

        @Override
        public Optional<Integer> getMaxNumberPrecision() {
            return Optional.ofNullable(maxNumberPrecision);
        }

        @Override
        public Optional<Double> getMaxValue() {
            return Optional.ofNullable(maxValue);
        }

        @Override
        public Optional<Double> getMinValue() {
            return Optional.ofNullable(minValue);
        }

        @Override
        public Optional<Integer> getMinNumberPrecision() {
            return Optional.ofNullable(minNumberPrecision);
        }

        @Override
        public Optional<Integer> getNumberPrecision() {
            return Optional.ofNullable(numberPrecision);
        }

        @Override
        public Optional<List<? extends Option<Double>>> getOptions() {
            return Optional.ofNullable(options);
        }

        @Override
        public Optional<OptionsDataSource> getOptionsDataSource() {
            return Optional.ofNullable(
                optionsFunction == null
                    ? null
                    : new OptionsDataSourceImpl(optionsLookupDependsOn, optionsFunction));
        }
    }

    public static final class ModifiableObjectProperty
        extends ModifiableValueProperty<Map<String, ?>, ModifiableObjectProperty>
        implements ObjectProperty {

        private List<? extends ModifiableValueProperty<?, ?>> additionalProperties;
        private List<String> optionsLookupDependsOn;
        private Boolean multipleValues;
        private List<Option<Object>> options;
        private OptionsFunction optionsFunction;
        private List<? extends ModifiableValueProperty<?, ?>> properties;

        private ModifiableObjectProperty() {
            this(null);
        }

        private ModifiableObjectProperty(String name) {
            super(name, Type.OBJECT);
        }

        public ModifiableObjectProperty defaultValue(Map<String, Object> defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ModifiableObjectProperty exampleValue(Map<String, Object> exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        @SafeVarargs
        public final <P extends ModifiableValueProperty<?, ?>> ModifiableObjectProperty additionalProperties(
            P... properties) {

            return additionalProperties(properties == null ? List.of() : List.of(properties));
        }

        public <P extends ModifiableValueProperty<?, ?>> ModifiableObjectProperty additionalProperties(
            List<? extends P> properties) {

            if (properties != null) {
                this.additionalProperties = new ArrayList<>(properties);
            }

            return this;
        }

        public ModifiableObjectProperty optionsLookupDependsOn(String... optionsLookupDependsOn) {
            if (optionsLookupDependsOn != null) {
                this.optionsLookupDependsOn = List.of(optionsLookupDependsOn);
            }

            return this;
        }

        public ModifiableObjectProperty multipleValues(boolean multipleValues) {
            this.multipleValues = multipleValues;

            return this;
        }

        @SafeVarargs
        public final ModifiableObjectProperty options(Option<Object>... options) {
            if (options != null) {
                this.options = List.of(options);
            }

            return this;
        }

        public ModifiableObjectProperty options(OptionsFunction optionsFunction) {
            this.optionsFunction = optionsFunction;

            return this;
        }

        @SafeVarargs
        public final <P extends ModifiableValueProperty<?, ?>> ModifiableObjectProperty properties(
            P... properties) {

            return properties(List.of(properties));
        }

        public <P extends ModifiableValueProperty<?, ?>> ModifiableObjectProperty properties(List<P> properties) {
            if (properties != null) {
                this.properties = Collections.unmodifiableList(properties);
            }

            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableObjectProperty that)) {
                return false;
            }

            if (!super.equals(o)) {
                return false;
            }

            return Objects.equals(additionalProperties, that.additionalProperties)
                && Objects.equals(optionsLookupDependsOn, that.optionsLookupDependsOn)
                && Objects.equals(multipleValues, that.multipleValues) && Objects.equals(options, that.options)
                && Objects.equals(optionsFunction, that.optionsFunction) && Objects.equals(properties, that.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), additionalProperties, optionsLookupDependsOn, multipleValues, options,
                optionsFunction, properties);
        }

        @Override
        public Optional<List<? extends ValueProperty<?>>> getAdditionalProperties() {
            return Optional.ofNullable(additionalProperties == null ? null : new ArrayList<>(additionalProperties));
        }

        @Override
        public ControlType getControlType() {
            if (options == null && optionsFunction == null) {
                return ControlType.OBJECT_BUILDER;
            } else {
                return ControlType.SELECT;
            }
        }

        public Optional<Boolean> getMultipleValues() {
            return Optional.ofNullable(multipleValues);
        }

        @Override
        public Optional<List<? extends Option<Object>>> getOptions() {
            return Optional.ofNullable(options);
        }

        @Override
        public Optional<OptionsDataSource> getOptionsDataSource() {
            return Optional.ofNullable(
                optionsFunction == null
                    ? null
                    : new OptionsDataSourceImpl(optionsLookupDependsOn, optionsFunction));
        }

        @Override
        public Optional<List<? extends ValueProperty<?>>> getProperties() {
            return Optional.ofNullable(properties == null ? null : new ArrayList<>(properties));
        }
    }

    public static final class ModifiableOption<T> implements Option<T> {

        private String description;
        private final String label;
        private final T value;

        private ModifiableOption(String label, T value) {
            this.label = label;
            this.value = value;
        }

        private ModifiableOption(String label, T value, String description) {
            this.label = label;
            this.value = value;
            this.description = description;
        }

        public ModifiableOption<?> description(String description) {
            this.description = description;

            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableOption<?> that)) {
                return false;
            }

            return Objects.equals(description, that.description) && Objects.equals(label, that.label)
                && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(description, label, value);
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public T getValue() {
            return value;
        }
    }

    public abstract static class ModifiableProperty<M extends ModifiableProperty<M>> implements Property {

        private Boolean advancedOption;
        private String description;
        private String displayCondition;
        private Boolean expressionEnabled; // Defaults to true
        private Boolean hidden;
        private Map<String, Object> metadata = new HashMap<>();
        private Boolean required;
        private final String name;
        private final Property.Type type;

        protected ModifiableProperty(String name, Property.Type type) {
            this.name = name;
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        public M advancedOption(boolean advancedOption) {
            this.advancedOption = advancedOption;

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        public M description(String description) {
            this.description = description;

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        public M displayCondition(String displayCondition) {
            this.displayCondition = displayCondition;

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        public M expressionEnabled(boolean expressionEnabled) {
            this.expressionEnabled = expressionEnabled;

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        public M hidden(boolean hidden) {
            this.hidden = hidden;

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        public M metadata(String key, String value) {
            if (metadata == null) {
                metadata = new HashMap<>();
            }

            this.metadata.put(key, value);

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        @SuppressFBWarnings("EI2")
        public M metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                this.metadata = metadata;
            }

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        public M required(boolean required) {
            this.required = required;

            return (M) this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableProperty<?> that)) {
                return false;
            }

            return Objects.equals(advancedOption, that.advancedOption) && Objects.equals(description, that.description)
                && Objects.equals(displayCondition, that.displayCondition)
                && Objects.equals(expressionEnabled, that.expressionEnabled) && Objects.equals(hidden, that.hidden)
                && Objects.equals(metadata, that.metadata) && Objects.equals(required, that.required)
                && Objects.equals(name, that.name) && type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(advancedOption, description, displayCondition, expressionEnabled, hidden, metadata,
                required, name, type);
        }

        @Override
        public Optional<Boolean> getAdvancedOption() {
            return Optional.ofNullable(advancedOption);
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<String> getDisplayCondition() {
            return Optional.ofNullable(displayCondition);
        }

        @Override
        public Optional<Boolean> getExpressionEnabled() {
            return Optional.ofNullable(expressionEnabled);
        }

        @Override
        public Optional<Boolean> getHidden() {
            return Optional.ofNullable(hidden);
        }

        @Override
        public Optional<Boolean> getRequired() {
            return Optional.ofNullable(required);
        }

        @Override
        public Map<String, Object> getMetadata() {
            return Collections.unmodifiableMap(metadata);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Property.Type getType() {
            return type;
        }
    }

    public static final class ModifiableStringProperty
        extends ModifiableValueProperty<String, ModifiableStringProperty>
        implements Property.StringProperty {

        private ControlType controlType;
        private String languageId;
        private List<String> optionsLookupDependsOn;
        private Integer maxLength;
        private Integer minLength;
        private List<Option<String>> options;
        private OptionsFunction optionsFunction;

        private ModifiableStringProperty() {
            this(null);
        }

        private ModifiableStringProperty(String name) {
            super(name, Type.STRING);
        }

        public ModifiableStringProperty controlType(ControlType controlType) {
            this.controlType = controlType;

            return this;
        }

        public ModifiableStringProperty defaultValue(String value) {
            this.defaultValue = value;

            return this;
        }

        public ModifiableStringProperty exampleValue(String exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ModifiableStringProperty languageId(String languageId) {
            this.languageId = languageId;

            return this;
        }

        public ModifiableStringProperty optionsLookupDependsOn(String... optionsLookupDependsOn) {
            if (optionsLookupDependsOn != null) {
                this.optionsLookupDependsOn = List.of(optionsLookupDependsOn);
            }

            return this;
        }

        public ModifiableStringProperty maxLength(int maxLength) {
            this.maxLength = maxLength;

            return this;
        }

        public ModifiableStringProperty minLength(int minLength) {
            this.minLength = minLength;

            return this;
        }

        @SafeVarargs
        public final ModifiableStringProperty options(Option<String>... options) {
            if (options != null) {
                this.options = List.of(options);
            }

            return this;
        }

        public ModifiableStringProperty options(List<? extends Option<String>> options) {
            this.options = new ArrayList<>(options);

            return this;
        }

        public ModifiableStringProperty options(OptionsFunction optionsFunction) {
            this.optionsFunction = optionsFunction;

            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableStringProperty that)) {
                return false;
            }

            if (!super.equals(o)) {
                return false;
            }

            return controlType == that.controlType
                && Objects.equals(optionsLookupDependsOn, that.optionsLookupDependsOn)
                && Objects.equals(maxLength, that.maxLength) && Objects.equals(minLength, that.minLength)
                && Objects.equals(options, that.options) && Objects.equals(optionsFunction, that.optionsFunction);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), controlType, optionsLookupDependsOn, maxLength, minLength, options,
                optionsFunction);
        }

        @Override
        public ControlType getControlType() {
            if (this.controlType == null) {
                if (options == null && optionsFunction == null) {
                    return ControlType.TEXT;
                } else {
                    return ControlType.SELECT;
                }
            } else {
                return controlType;
            }
        }

        @Override
        public Optional<String> getLanguageId() {
            return Optional.ofNullable(languageId);
        }

        @Override
        public Optional<Integer> getMaxLength() {
            return Optional.ofNullable(maxLength);
        }

        @Override
        public Optional<Integer> getMinLength() {
            return Optional.ofNullable(minLength);
        }

        @Override
        public Optional<List<? extends Option<String>>> getOptions() {
            return Optional.ofNullable(options == null ? null : new ArrayList<>(options));
        }

        @Override
        public Optional<OptionsDataSource> getOptionsDataSource() {
            return Optional.ofNullable(
                optionsFunction == null
                    ? null
                    : new OptionsDataSourceImpl(optionsLookupDependsOn, optionsFunction));
        }
    }

    public static final class ModifiableTimeProperty
        extends ModifiableValueProperty<LocalTime, ModifiableTimeProperty>
        implements Property.TimeProperty {

        private List<String> optionsLookupDependsOn;
        private List<Option<LocalTime>> options;
        private OptionsFunction optionsFunction;

        private ModifiableTimeProperty() {
            this(null);
        }

        private ModifiableTimeProperty(String name) {
            super(name, Type.TIME);
        }

        public ModifiableTimeProperty defaultValue(LocalTime defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ModifiableTimeProperty exampleValue(LocalTime exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ModifiableTimeProperty optionsLookupDependsOn(String... optionsLookupDependsOn) {
            if (optionsLookupDependsOn != null) {
                this.optionsLookupDependsOn = List.of(optionsLookupDependsOn);
            }

            return this;
        }

        @SafeVarargs
        public final ModifiableTimeProperty options(Option<LocalTime>... options) {
            if (options != null) {
                this.options = List.of(options);
            }

            return this;
        }

        public ModifiableTimeProperty options(OptionsFunction optionsFunction) {
            this.optionsFunction = optionsFunction;

            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableTimeProperty that)) {
                return false;
            }

            if (!super.equals(o)) {
                return false;
            }

            return Objects.equals(optionsLookupDependsOn, that.optionsLookupDependsOn)
                && Objects.equals(options, that.options) && Objects.equals(optionsFunction, that.optionsFunction);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), optionsLookupDependsOn, options, optionsFunction);
        }

        @Override
        public ControlType getControlType() {
            return ControlType.TIME;
        }

        @Override
        public Optional<List<? extends Option<LocalTime>>> getOptions() {
            return Optional.ofNullable(options);
        }

        @Override
        public Optional<OptionsDataSource> getOptionsDataSource() {
            return Optional.ofNullable(
                optionsFunction == null
                    ? null
                    : new OptionsDataSourceImpl(optionsLookupDependsOn, optionsFunction));
        }
    }

    public static final class ModifiableTriggerDefinition implements TriggerDefinition {

        private Boolean batch;
        private DeduplicateFunction deduplicateFunction;
        private boolean defaultOutputFunction;
        private Boolean deprecated;
        private String description;
        private DynamicWebhookDisableConsumer dynamicWebhookDisableConsumer;
        private DynamicWebhookEnableFunction dynamicWebhookEnableFunction;
        private DynamicWebhookRefreshFunction dynamicWebhookRefreshFunction;
        private DynamicWebhookRequestFunction dynamicWebhookRequestFunction;
        private Help help;
        private ListenerDisableConsumer listenerDisableConsumer;
        private ListenerEnableConsumer listenerEnableConsumer;
        private String name;
        private ModifiableValueProperty<?, ?> outputSchema;
        private TriggerOutputFunction outputFunction;
        private PollFunction pollFunction;
        private List<? extends Property> properties;
        private Object sampleOutput;
        private StaticWebhookRequestFunction staticWebhookRequestFunction;
        private String title;
        private TriggerType type;
        private Boolean webhookRawBody;
        private WebhookValidateFunction webhookValidateFunction;
        private TriggerWorkflowNodeDescriptionFunction workflowNodeDescriptionFunction;
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

        public ModifiableTriggerDefinition output() {
            this.defaultOutputFunction = true;

            return this;
        }

        public ModifiableTriggerDefinition output(DynamicWebhookTriggerOutputFunction output) {
            this.outputFunction = output;
            this.defaultOutputFunction = false;

            return this;
        }

        public ModifiableTriggerDefinition output(ListenerTriggerOutputFunction output) {
            this.outputFunction = output;
            this.defaultOutputFunction = false;

            return this;
        }

        public ModifiableTriggerDefinition output(PollTriggerOutputFunction output) {
            this.outputFunction = output;
            this.defaultOutputFunction = false;

            return this;
        }

        public ModifiableTriggerDefinition output(StaticWebhookTriggerOutputFunction output) {
            this.outputFunction = output;
            this.defaultOutputFunction = false;

            return this;
        }

        public <P extends ModifiableValueProperty<?, ?>> ModifiableTriggerDefinition outputSchema(P outputSchema) {
            this.outputSchema = outputSchema;
            this.defaultOutputFunction = true;

            return this;
        }

        public ModifiableTriggerDefinition poll(PollFunction poll) {
            this.pollFunction = poll;

            return this;
        }

        @SafeVarargs
        public final <P extends Property> ModifiableTriggerDefinition properties(P... properties) {
            if (properties != null) {
                this.properties = List.of(properties);
            }

            return this;
        }

        public ModifiableTriggerDefinition sampleOutput(Object sampleOutput) {
            this.sampleOutput = sampleOutput;

            return this;
        }

        public ModifiableTriggerDefinition staticWebhookRequest(StaticWebhookRequestFunction staticWebhookRequest) {
            this.staticWebhookRequestFunction = staticWebhookRequest;

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

        public ModifiableTriggerDefinition workflowNodeDescription(
            TriggerWorkflowNodeDescriptionFunction workflowNodeDescription) {

            this.workflowNodeDescriptionFunction = workflowNodeDescription;

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
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableTriggerDefinition that)) {
                return false;
            }

            return Objects.equals(batch, that.batch)
                && Objects.equals(deduplicateFunction, that.deduplicateFunction)
                && Objects.equals(deprecated, that.deprecated) && Objects.equals(description, that.description)
                && Objects.equals(dynamicWebhookDisableConsumer, that.dynamicWebhookDisableConsumer)
                && Objects.equals(dynamicWebhookEnableFunction, that.dynamicWebhookEnableFunction)
                && Objects.equals(dynamicWebhookRefreshFunction, that.dynamicWebhookRefreshFunction)
                && Objects.equals(dynamicWebhookRequestFunction, that.dynamicWebhookRequestFunction)
                && Objects.equals(help, that.help)
                && Objects.equals(listenerDisableConsumer, that.listenerDisableConsumer)
                && Objects.equals(listenerEnableConsumer, that.listenerEnableConsumer)
                && Objects.equals(name, that.name)
                && Objects.equals(outputSchema, that.outputSchema)
                && Objects.equals(defaultOutputFunction, that.defaultOutputFunction)
                && Objects.equals(outputFunction, that.outputFunction)
                && Objects.equals(sampleOutput, that.sampleOutput)
                && Objects.equals(pollFunction, that.pollFunction) && Objects.equals(properties, that.properties)
                && Objects.equals(staticWebhookRequestFunction, that.staticWebhookRequestFunction)
                && Objects.equals(title, that.title) && type == that.type
                && Objects.equals(webhookRawBody, that.webhookRawBody)
                && Objects.equals(webhookValidateFunction, that.webhookValidateFunction)
                && Objects.equals(workflowNodeDescriptionFunction, that.workflowNodeDescriptionFunction)
                && Objects.equals(workflowSyncExecution, that.workflowSyncExecution)
                && Objects.equals(workflowSyncValidation, that.workflowSyncValidation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(batch, deduplicateFunction, deprecated, description, dynamicWebhookDisableConsumer,
                dynamicWebhookEnableFunction, dynamicWebhookRefreshFunction, dynamicWebhookRequestFunction,
                help, listenerDisableConsumer, listenerEnableConsumer, name, outputSchema, defaultOutputFunction,
                outputFunction, sampleOutput, pollFunction, properties, staticWebhookRequestFunction, title, type,
                webhookRawBody, webhookValidateFunction, workflowNodeDescriptionFunction, workflowSyncExecution,
                workflowSyncValidation);
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
        public Optional<Output> getOutput() {
            return Optional.ofNullable(outputSchema == null ? null : new Output(outputSchema, sampleOutput));
        }

        @Override
        public Optional<TriggerOutputFunction> getOutputFunction() {
            return Optional.ofNullable(outputFunction);
        }

        @Override
        public Optional<PollFunction> getPoll() {
            return Optional.ofNullable(pollFunction);
        }

        @Override
        public Optional<List<? extends Property>> getProperties() {
            return Optional.ofNullable(properties);
        }

        @Override
        public Optional<StaticWebhookRequestFunction> getStaticWebhookRequest() {
            return Optional.ofNullable(staticWebhookRequestFunction);
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
        public Optional<TriggerWorkflowNodeDescriptionFunction> getWorkflowNodeDescriptionFunction() {
            return Optional.ofNullable(workflowNodeDescriptionFunction);
        }

        @Override
        public Optional<Boolean> getWorkflowSyncExecution() {
            return Optional.ofNullable(workflowSyncExecution);
        }

        @Override
        public Optional<Boolean> getWorkflowSyncValidation() {
            return Optional.ofNullable(workflowSyncValidation);
        }

        @Override
        public boolean isDefaultOutputFunction() {
            return defaultOutputFunction;
        }

        @Override
        public String toString() {
            return "ModifiableTriggerDefinition{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", description='" + description + '\'' +
                ", title='" + title + '\'' +
                ", batch=" + batch +
                ", deprecated=" + deprecated +
                ", help=" + help +
                ", outputSchema=" + outputSchema +
                ", properties=" + properties +
                ", sampleOutput=" + sampleOutput +
                ", webhookRawBody=" + webhookRawBody +
                ", workflowSyncExecution=" + workflowSyncExecution +
                ", workflowSyncValidation=" + workflowSyncValidation +
                '}';
        }
    }

    public abstract static class ModifiableValueProperty<V, P extends ModifiableValueProperty<V, P>>
        extends ModifiableProperty<P> implements ValueProperty<V> {

        protected V defaultValue;
        protected V exampleValue;
        private String label;
        private String placeholder;

        protected ModifiableValueProperty(String name, Type type) {
            super(name, type);
        }

        @SuppressWarnings("unchecked")
        public P label(String label) {
            this.label = label;

            return (P) this;
        }

        @SuppressWarnings("unchecked")
        public P placeholder(String placeholder) {
            this.placeholder = placeholder;

            return (P) this;
        }

        @Override
        public Optional<V> getDefaultValue() {
            return Optional.ofNullable(defaultValue);
        }

        @Override
        public Optional<V> getExampleValue() {
            return Optional.ofNullable(exampleValue);
        }

        @Override
        public Optional<String> getLabel() {
            return Optional.ofNullable(label);
        }

        @Override
        public Optional<String> getPlaceholder() {
            return Optional.ofNullable(placeholder);
        }
    }

    private static class OptionsDataSourceImpl implements OptionsDataSource {

        private final List<String> optionsLookupDependsOn;
        private final OptionsFunction options;

        private OptionsDataSourceImpl(List<String> loadOptionsDependOnPropertyNames, OptionsFunction options) {
            this.optionsLookupDependsOn = loadOptionsDependOnPropertyNames;
            this.options = Objects.requireNonNull(options);
        }

        @Override
        public Optional<List<String>> getOptionsLookupDependsOn() {
            return Optional
                .ofNullable(
                    optionsLookupDependsOn == null ? null : Collections.unmodifiableList(optionsLookupDependsOn));
        }

        @Override
        public OptionsFunction getOptions() {
            return options;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            OptionsDataSourceImpl that = (OptionsDataSourceImpl) o;

            return Objects.equals(optionsLookupDependsOn, that.optionsLookupDependsOn)
                && Objects.equals(options, that.options);
        }

        @Override
        public int hashCode() {
            return Objects.hash(optionsLookupDependsOn, options);
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

    private static class PropertiesDataSourceImpl
        implements PropertiesDataSource<PropertiesDataSource.PropertiesFunction> {

        private final List<String> propertiesLookupDependsOn;
        private final PropertiesFunction propertiesFunction;

        @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
        private PropertiesDataSourceImpl(List<String> loadPropertiesDependOn, PropertiesFunction propertiesFunction) {
            if (loadPropertiesDependOn == null || loadPropertiesDependOn.isEmpty()) {
                throw new IllegalArgumentException("propertiesLookupDependsOn is not defined.");
            }

            if (propertiesFunction == null) {
                throw new IllegalArgumentException("propertiesFunction is not defined.");
            }

            this.propertiesLookupDependsOn = loadPropertiesDependOn;
            this.propertiesFunction = propertiesFunction;
        }

        @Override
        public List<String> getPropertiesLookupDependsOn() {
            return Collections.unmodifiableList(propertiesLookupDependsOn);
        }

        @Override
        public PropertiesFunction getProperties() {
            return propertiesFunction;
        }
    }

    @SuppressFBWarnings("EI")
    private record ResourcesImpl(
        String documentationUrl, List<String> categories, Map<String, String> additionalUrls) implements Resources {

        @Override
        public Optional<List<String>> getCategories() {
            return Optional.ofNullable(categories == null ? null : Collections.unmodifiableList(categories));
        }

        @Override
        public Optional<Map<String, String>> getAdditionalUrls() {
            return Optional.ofNullable(additionalUrls == null ? null : new HashMap<>(additionalUrls));
        }
    }
}
