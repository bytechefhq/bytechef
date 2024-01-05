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

package com.bytechef.hermes.task.dispatcher.definition;

import com.bytechef.hermes.definition.Help;
import com.bytechef.hermes.definition.Option;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Property.InputProperty;
import com.bytechef.hermes.definition.Property.OutputProperty;
import com.bytechef.hermes.definition.Property.ValueProperty;
import com.bytechef.hermes.definition.Resources;
import com.bytechef.hermes.task.dispatcher.definition.OutputSchemaDataSource.OutputSchemaFunction;
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
public final class TaskDispatcherDSL {

    public static ModifiableArrayProperty array() {
        return new ModifiableArrayProperty();
    }

    public static ModifiableArrayProperty array(String name) {
        return new ModifiableArrayProperty(name);
    }

    public static ModifiableBooleanProperty bool() {
        return new ModifiableBooleanProperty();
    }

    public static ModifiableBooleanProperty bool(String name) {
        return new ModifiableBooleanProperty(name);
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

    public static ModifiableIntegerProperty integer() {
        return new ModifiableIntegerProperty();
    }

    public static ModifiableIntegerProperty integer(String name) {
        return new ModifiableIntegerProperty(name);
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

    public static ModifiableOption<Boolean> option(String name, boolean value) {
        return new ModifiableOption<>(name, value);
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

    public static ModifiableOption<Integer> option(String label, int value) {
        return new ModifiableOption<>(label, value);
    }

    public static ModifiableOption<Integer> option(String label, int value, String description) {
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

    public static ModifiableObjectProperty task() {
        return task(null);
    }

    public static ModifiableObjectProperty task(String name) {
        return buildObject(name, "The task or task dispatcher to use.", "TASK");
    }

    public static ModifiableTaskDispatcherDefinition taskDispatcher(String name) {
        return new ModifiableTaskDispatcherDefinition(name);
    }

    private static ModifiableObjectProperty buildObject(
        String name, String description, String objectType) {

        return new ModifiableObjectProperty(name)
            .description(description)
            .objectType(objectType);
    }

    public static class ModifiableArrayProperty
        extends ModifiableValueProperty<List<Object>, ModifiableArrayProperty>
        implements Property.ArrayProperty {

        private List<? extends ModifiableValueProperty<?, ?>> items;
        private Long maxItems;
        private Long minItems;
        private Boolean multipleValues;
        private List<Option<?>> options;

        private ModifiableArrayProperty() {
            this(null);
        }

        private ModifiableArrayProperty(String name) {
            super(name, Type.ARRAY);
        }

        public ModifiableArrayProperty defaultValue(Boolean... defaultValue) {
            this.defaultValue = defaultValue == null ? List.of() : List.of(defaultValue);

            return this;
        }

        public ModifiableArrayProperty defaultValue(Integer... defaultValue) {
            this.defaultValue = defaultValue == null ? List.of() : List.of(defaultValue);

            return this;
        }

        public ModifiableArrayProperty defaultValue(Long... defaultValue) {
            this.defaultValue = defaultValue == null ? List.of() : List.of(defaultValue);

            return this;
        }

        public ModifiableArrayProperty defaultValue(Float... defaultValue) {
            this.defaultValue = defaultValue == null ? List.of() : List.of(defaultValue);

            return this;
        }

        public ModifiableArrayProperty defaultValue(Double... defaultValue) {
            this.defaultValue = defaultValue == null ? List.of() : List.of(defaultValue);

            return this;
        }

        public ModifiableArrayProperty defaultValue(String... defaultValue) {
            this.defaultValue = defaultValue == null ? List.of() : List.of(defaultValue);

            return this;
        }

        @SafeVarargs
        public final ModifiableArrayProperty defaultValue(Map<String, ?>... defaultValue) {
            this.defaultValue = defaultValue == null ? List.of() : List.of(defaultValue);

            return this;
        }

        public ModifiableArrayProperty exampleValue(Boolean... exampleValue) {
            this.exampleValue = exampleValue == null ? List.of() : List.of(exampleValue);

            return this;
        }

        public ModifiableArrayProperty exampleValue(Integer... exampleValue) {
            this.exampleValue = exampleValue == null ? List.of() : List.of(exampleValue);

            return this;
        }

        public ModifiableArrayProperty exampleValue(Long... exampleValue) {
            this.exampleValue = exampleValue == null ? List.of() : List.of(exampleValue);

            return this;
        }

        public ModifiableArrayProperty exampleValue(Float... exampleValue) {
            this.exampleValue = exampleValue == null ? List.of() : List.of(exampleValue);

            return this;
        }

        public ModifiableArrayProperty exampleValue(Double... exampleValue) {
            this.exampleValue = exampleValue == null ? List.of() : List.of(exampleValue);

            return this;
        }

        public ModifiableArrayProperty exampleValue(String... exampleValue) {
            this.exampleValue = exampleValue == null ? List.of() : List.of(exampleValue);

            return this;
        }

        @SafeVarargs
        public final ModifiableArrayProperty exampleValue(Map<String, ?>... exampleValue) {
            this.exampleValue = exampleValue == null ? List.of() : List.of(exampleValue);

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

        public ModifiableArrayProperty options(List<? extends Option<?>> options) {
            this.options = new ArrayList<>(options);

            return this;
        }

        @Override
        public ControlType getControlType() {
            if (options == null || options.isEmpty()) {
                return ControlType.ARRAY_BUILDER;
            } else {
                return ControlType.MULTI_SELECT;
            }
        }

        @Override
        public Optional<List<Option<?>>> getOptions() {
            return Optional.ofNullable(options);
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
    }

    public static final class ModifiableBooleanProperty
        extends ModifiableValueProperty<Boolean, ModifiableBooleanProperty>
        implements Property.BooleanProperty {

        private final List<Option<?>> options = List.of(
            option("True", true),
            option("False", true));

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
            return ControlType.CHECKBOX;
        }

        @Override
        public Optional<List<Option<?>>> getOptions() {
            return Optional.of(options);
        }
    }

    public static class ModifiableDateProperty
        extends ModifiableValueProperty<LocalDate, ModifiableDateProperty>
        implements Property.DateProperty {

        private List<Option<?>> options;

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

        @SafeVarargs
        public final ModifiableDateProperty options(Option<LocalDate>... options) {
            if (options != null) {
                this.options = List.of(options);
            }

            return this;
        }

        @Override
        public ControlType getControlType() {
            if (options == null || options.isEmpty()) {
                return ControlType.DATE;
            } else {
                return ControlType.SELECT;
            }
        }

        @Override
        public Optional<List<Option<?>>> getOptions() {
            return Optional.ofNullable(options);
        }
    }

    public static class ModifiableDateTimeProperty
        extends ModifiableValueProperty<LocalDateTime, ModifiableDateTimeProperty>
        implements Property.DateTimeProperty {

        private List<Option<?>> options;

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

        @SafeVarargs
        public final ModifiableDateTimeProperty options(Option<LocalDateTime>... options) {
            if (options != null) {
                this.options = List.of(options);
            }

            return this;
        }

        @Override
        public ControlType getControlType() {
            if (options == null || options.isEmpty()) {
                return ControlType.DATE_TIME;
            } else {
                return ControlType.SELECT;
            }
        }

        @Override
        public Optional<List<Option<?>>> getOptions() {
            return Optional.ofNullable(options);
        }
    }

    public static class ModifiableIntegerProperty
        extends ModifiableValueProperty<Long, ModifiableIntegerProperty>
        implements Property.IntegerProperty {

        private List<Option<?>> options;
        private Long maxValue;
        private Long minValue;

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

        public ModifiableIntegerProperty maxValue(long maxValue) {
            this.maxValue = maxValue;

            return this;
        }

        public ModifiableIntegerProperty minValue(long minValue) {
            this.minValue = minValue;

            return this;
        }

        @SafeVarargs
        public final ModifiableIntegerProperty options(Option<Integer>... options) {
            if (options != null) {
                this.options = List.of(options);
            }

            return this;
        }

        public ModifiableIntegerProperty options(List<Option<Integer>> options) {
            if (options != null) {
                this.options = Collections.unmodifiableList(options);
            }

            return this;
        }

        @Override
        public ControlType getControlType() {
            if (options == null || options.isEmpty()) {
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
        public Optional<List<Option<?>>> getOptions() {
            return Optional.ofNullable(options);
        }
    }

    public static class ModifiableNumberProperty
        extends ModifiableValueProperty<Double, ModifiableNumberProperty>
        implements Property.NumberProperty {

        private List<Option<?>> options;
        private Integer maxNumberPrecision;
        private Double maxValue;
        private Integer minNumberPrecision;
        private Double minValue;
        private Integer numberPrecision;

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
        public final ModifiableNumberProperty options(Option<? extends Number>... options) {
            if (options != null) {
                this.options = List.of(options);
            }

            return this;
        }

        @Override
        public ControlType getControlType() {
            if (options == null || options.isEmpty()) {
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
        public Optional<List<Option<?>>> getOptions() {
            return Optional.ofNullable(options);
        }
    }

    public static class ModifiableObjectProperty
        extends ModifiableValueProperty<Map<String, Object>, ModifiableObjectProperty>
        implements Property.ObjectProperty {

        private List<Option<?>> options;
        private List<? extends ModifiableValueProperty<?, ?>> additionalProperties;
        private Boolean multipleValues;
        private String objectType;
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

        public ModifiableObjectProperty multipleValues(boolean multipleValues) {
            this.multipleValues = multipleValues;

            return this;
        }

        public ModifiableObjectProperty objectType(String objectType) {
            this.objectType = objectType;

            return this;
        }

        @SafeVarargs
        public final ModifiableObjectProperty options(Option<Object>... options) {
            if (options != null) {
                this.options = List.of(options);
            }

            return this;
        }

        @SafeVarargs
        public final <P extends ModifiableValueProperty<?, ?>> ModifiableObjectProperty properties(
            P... properties) {

            return properties(List.of(properties));
        }

        public <P extends ModifiableValueProperty<?, ?>> ModifiableObjectProperty properties(List<P> properties) {
            if (properties != null) {
                for (Property property : properties) {
                    String name = property.getName();

                    if (name == null || name.isEmpty()) {
                        throw new IllegalArgumentException("Name cannot be empty for property.");
                    }
                }

                this.properties = properties.stream()
                    .distinct()
                    .toList();
            }

            return this;
        }

        @Override
        public Optional<List<? extends ValueProperty<?>>> getAdditionalProperties() {
            return Optional.ofNullable(
                additionalProperties == null ? null : new ArrayList<>(additionalProperties));
        }

        @Override
        public ControlType getControlType() {
            if (options == null || options.isEmpty()) {
                return ControlType.OBJECT_BUILDER;
            } else {
                return ControlType.SELECT;
            }
        }

        public Optional<Boolean> getMultipleValues() {
            return Optional.ofNullable(multipleValues);
        }

        @Override
        public Optional<String> getObjectType() {
            return Optional.ofNullable(objectType);
        }

        @Override
        public Optional<List<Option<?>>> getOptions() {
            return Optional.ofNullable(options);
        }

        @Override
        public Optional<List<? extends ValueProperty<?>>> getProperties() {
            return Optional.ofNullable(properties == null ? null : new ArrayList<>(properties));
        }
    }

    public static final class ModifiableOption<T> implements Option<T> {

        private String description;
        private String displayCondition;
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

        public ModifiableOption<?> displayCondition(String displayCondition) {
            this.displayCondition = displayCondition;

            return this;
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public String getDisplayCondition() {
            return displayCondition;
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
        private String displayCondition;
        private Boolean expressionEnabled; // Defaults to true
        private Boolean hidden;
        private Map<String, Object> metadata = new HashMap<>();
        private Boolean required;
        private final String name;
        private final Type type;

        protected ModifiableProperty(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        public M advancedOption(boolean advancedOption) {
            this.advancedOption = advancedOption;

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
            this.metadata = metadata;

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

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ModifiableProperty<?> that = (ModifiableProperty<?>) o;

            return Objects.equals(displayCondition, that.displayCondition) && Objects.equals(name, that.name)
                && type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(displayCondition, name, type);
        }

        @Override
        public Optional<Boolean> getAdvancedOption() {
            return Optional.ofNullable(advancedOption);
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
        public Type getType() {
            return type;
        }
    }

    public static class ModifiableStringProperty
        extends ModifiableValueProperty<String, ModifiableStringProperty>
        implements Property.StringProperty {

        private List<Option<?>> options;
        private ControlType controlType;
        private Integer maxLength;
        private Integer minLength;

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

        @Override
        public ControlType getControlType() {
            if (this.controlType == null) {
                if (options == null || options.isEmpty()) {
                    return ControlType.TEXT;
                } else {
                    return ControlType.SELECT;
                }
            } else {
                return controlType;
            }
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
        public Optional<List<Option<?>>> getOptions() {
            return Optional.ofNullable(options == null ? null : new ArrayList<>(options));
        }
    }

    public static final class ModifiableTaskDispatcherDefinition implements TaskDispatcherDefinition {

        private String description;
        private Help help;
        private String icon;
        private final String name;
        private OutputProperty<?> outputSchemaProperty;
        private OutputSchemaFunction outputSchemaFunction;
        private List<? extends InputProperty> properties;
        private Resources resources;
        private List<? extends ModifiableValueProperty<?, ?>> taskProperties;
        private String title;
        private List<? extends ModifiableValueProperty<?, ?>> variableProperties;
        private int version = 1;

        private ModifiableTaskDispatcherDefinition(String name) {
            this.name = name;
        }

        public ModifiableTaskDispatcherDefinition description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableTaskDispatcherDefinition help(Help help) {
            this.help = help;

            return this;
        }

        public ModifiableTaskDispatcherDefinition icon(String icon) {
            this.icon = icon;

            return this;
        }

        public <P extends OutputProperty<?>> ModifiableTaskDispatcherDefinition outputSchema(
            P property) {

            this.outputSchemaProperty = Objects.requireNonNull(property);

            return this;
        }

        public ModifiableTaskDispatcherDefinition outputSchema(OutputSchemaFunction outputSchema) {
            this.outputSchemaFunction = outputSchema;

            return this;
        }

        public ModifiableTaskDispatcherDefinition resources(String documentationUrl) {
            this.resources = new ResourcesImpl(documentationUrl, null, null);

            return this;
        }

        public ModifiableTaskDispatcherDefinition resources(String documentationUrl, List<String> categories) {
            this.resources = new ResourcesImpl(documentationUrl, null, null);

            return this;
        }

        public ModifiableTaskDispatcherDefinition resources(
            String documentationUrl, List<String> categories, Map<String, String> additionalUrls) {

            this.resources = new ResourcesImpl(documentationUrl, categories, additionalUrls);

            return this;
        }

        @SafeVarargs
        public final <P extends InputProperty> ModifiableTaskDispatcherDefinition properties(
            P... properties) {

            if (properties != null) {
                this.properties = List.of(properties);
            }

            return this;
        }

        @SafeVarargs
        public final <P extends ModifiableValueProperty<?, ?>> ModifiableTaskDispatcherDefinition taskProperties(
            P... taskProperties) {

            this.taskProperties = List.of(taskProperties);

            return this;
        }

        public ModifiableTaskDispatcherDefinition title(String title) {
            this.title = title;

            return this;
        }

        public ModifiableTaskDispatcherDefinition version(int version) {
            this.version = version;

            return this;
        }

        @SafeVarargs
        public final <P extends ModifiableValueProperty<?, ?>> ModifiableTaskDispatcherDefinition variableProperties(
            P... variableProperties) {

            this.variableProperties = List.of(variableProperties);

            return this;
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<Help> getHelp() {
            return Optional.ofNullable(help);
        }

        @Override
        public Optional<String> getIcon() {
            return Optional.ofNullable(icon);
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
        public Optional<Resources> getResources() {
            return Optional.ofNullable(resources);
        }

        @Override
        public Optional<List<? extends ValueProperty<?>>> getTaskProperties() {
            return Optional.ofNullable(taskProperties);
        }

        @Override
        public Optional<String> getTitle() {
            return Optional.ofNullable(title);
        }

        @Override
        public Optional<List<? extends ValueProperty<?>>> getVariableProperties() {
            return Optional.ofNullable(variableProperties);
        }

        @Override
        public int getVersion() {
            return version;
        }
    }

    public static class ModifiableTimeProperty
        extends ModifiableValueProperty<LocalTime, ModifiableTimeProperty>
        implements Property.TimeProperty {

        private List<Option<?>> options;

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

        @SafeVarargs
        public final ModifiableTimeProperty options(Option<LocalTime>... options) {
            if (options != null) {
                this.options = List.of(options);
            }

            return this;
        }

        @Override
        public ControlType getControlType() {
            return ControlType.TIME;
        }

        @Override
        public Optional<List<Option<?>>> getOptions() {
            return Optional.ofNullable(options);
        }
    }

    public abstract static class ModifiableValueProperty<V, P extends ModifiableValueProperty<V, P>>
        extends ModifiableProperty<P> implements Property.ValueProperty<V> {

        protected V defaultValue;
        private String description;
        protected V exampleValue;
        private String label;
        private String placeholder;

        protected ModifiableValueProperty(String name, Type type) {
            super(name, type);
        }

        @SuppressWarnings("unchecked")
        public P description(String description) {
            this.description = description;

            return (P) this;
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
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
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

    private record OutputSchemaDataSourceImpl(OutputSchemaFunction outputSchema) implements OutputSchemaDataSource {

        @Override
        public OutputSchemaFunction getOutputSchema() {
            return outputSchema;
        }
    }

    private static final class ResourcesImpl implements Resources {

        private final Map<String, String> additionalUrls;
        private final List<String> categories;
        private final String documentationUrl;

        @SuppressFBWarnings("EI")
        public ResourcesImpl(String documentationUrl, List<String> categories, Map<String, String> additionalUrls) {
            this.additionalUrls = additionalUrls;
            this.categories = categories;
            this.documentationUrl = documentationUrl;
        }

        @Override
        public Optional<List<String>> getCategories() {
            return Optional.ofNullable(categories == null ? null : Collections.unmodifiableList(categories));
        }

        @Override
        public String getDocumentationUrl() {
            return documentationUrl;
        }

        @Override
        public Optional<Map<String, String>> getAdditionalUrls() {
            return Optional.ofNullable(additionalUrls == null ? null : new HashMap<>(additionalUrls));
        }
    }
}
