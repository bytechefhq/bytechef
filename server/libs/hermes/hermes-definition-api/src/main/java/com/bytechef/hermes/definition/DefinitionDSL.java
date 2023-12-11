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

package com.bytechef.hermes.definition;

import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableArrayProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableBooleanProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableDateProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableDateTimeProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableIntegerProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableNullProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableNumberProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableObjectProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableStringProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableTimeProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableValueProperty;
import com.bytechef.hermes.definition.OptionsDataSource.OptionsFunction;
import com.bytechef.hermes.definition.PropertiesDataSource.PropertiesFunction;
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

public abstract class DefinitionDSL {

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

    public static ModifiableOption<Boolean> option(String name, boolean value) {
        return new ModifiableOption<>(name, value);
    }

    public static ModifiableOption<Boolean> option(String name, boolean value, String description) {
        return new ModifiableOption<>(name, value, description);
    }

    public static ModifiableOption<Double> option(String name, double value) {
        return new ModifiableOption<>(name, value);
    }

    public static ModifiableOption<Double> option(String name, double value, String description) {
        return new ModifiableOption<>(name, value, description);
    }

    public static ModifiableOption<Integer> option(String name, int value) {
        return new ModifiableOption<>(name, value);
    }

    public static ModifiableOption<Integer> option(String name, int value, String description) {
        return new ModifiableOption<>(name, value, description);
    }

    public static ModifiableOption<LocalDate> option(String name, LocalDate value) {
        return new ModifiableOption<>(name, value);
    }

    public static ModifiableOption<LocalDate> option(String name, LocalDate value, String description) {
        return new ModifiableOption<>(name, value, description);
    }

    public static ModifiableOption<LocalDateTime> option(String name, LocalDateTime value) {
        return new ModifiableOption<>(name, value);
    }

    public static ModifiableOption<LocalDateTime> option(String name, LocalDateTime value, String description) {
        return new ModifiableOption<>(name, value, description);
    }

    public static ModifiableOption<Object> option(String name, Object value) {
        return new ModifiableOption<>(name, value);
    }

    public static ModifiableOption<Object> option(String name, Object value, String description) {
        return new ModifiableOption<>(name, value, description);
    }

    public static ModifiableOption<String> option(String name, String value) {
        return new ModifiableOption<>(name, value);
    }

    public static ModifiableOption<String> option(String name, String value, String description) {
        return new ModifiableOption<>(name, value, description);
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

    protected static ModifiableObjectProperty buildObject(
        String name, String description, String objectType) {

        return new ModifiableObjectProperty(name)
            .description(description)
            .objectType(objectType);
    }

    @SafeVarargs
    protected static <P extends ModifiableValueProperty<?, ?>> ModifiableObjectProperty buildObject(
        String name, String description, String objectType, P... properties) {

        return new ModifiableObjectProperty(name)
            .description(description)
            .objectType(objectType)
            .properties(properties);
    }

    // CHECKSTYLE:OFF
    public static sealed abstract class ModifiableProperty<M extends ModifiableProperty<M>>
        implements Property permits ModifiableProperty.ModifiableDynamicPropertiesProperty,
        ModifiableValueProperty {

        private Boolean advancedOption;
        private String description;
        private String displayCondition;
        private Boolean expressionEnabled; // Defaults to true
        private Boolean hidden;
        private String label;
        private Map<String, Object> metadata = new HashMap<>();
        private String placeholder;
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
        public M label(String label) {
            this.label = label;

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
        public M placeholder(String placeholder) {
            this.placeholder = placeholder;

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
        public Optional<String> getLabel() {
            return Optional.ofNullable(label);
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
        public Optional<String> getPlaceholder() {
            return Optional.ofNullable(placeholder);
        }

        @Override
        public Type getType() {
            return type;
        }

        public static final class ModifiableArrayProperty
            extends ModifiableValueProperty<Object[], ModifiableArrayProperty>
            implements Property.ArrayProperty, ModifiableInputProperty, ModifiableOutputProperty<Object[]> {

            private List<? extends ModifiableValueProperty<?, ?>> items;
            private Boolean multipleValues;
            private List<String> loadOptionsDependsOn;
            private List<Option<?>> options;
            private OptionsFunction optionsFunction;

            private ModifiableArrayProperty() {
                this(null);
            }

            private ModifiableArrayProperty(String name) {
                super(name, Type.ARRAY);
            }

            public ModifiableArrayProperty defaultValue(Boolean... defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableArrayProperty defaultValue(Integer... defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableArrayProperty defaultValue(Long... defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableArrayProperty defaultValue(Float... defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableArrayProperty defaultValue(Double... defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableArrayProperty defaultValue(String... defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            @SafeVarargs
            public final ModifiableArrayProperty defaultValue(Map<String, ?>... defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableArrayProperty exampleValue(Boolean... exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableArrayProperty exampleValue(Integer... exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableArrayProperty exampleValue(Long... exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableArrayProperty exampleValue(Float... exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableArrayProperty exampleValue(Double... exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableArrayProperty exampleValue(String... exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            @SafeVarargs
            public final ModifiableArrayProperty exampleValue(Map<String, ?>... exampleValue) {
                this.exampleValue = exampleValue;

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

            public ModifiableArrayProperty loadOptionsDependsOn(String... loadOptionsDependsOn) {
                if (loadOptionsDependsOn != null) {
                    this.loadOptionsDependsOn = List.of(loadOptionsDependsOn);
                }

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

            public ModifiableArrayProperty options(OptionsFunction optionsFunction) {
                this.optionsFunction = optionsFunction;

                return this;
            }

            @Override
            public ControlType getControlType() {
                if ((options == null || options.isEmpty()) && optionsFunction == null) {
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
            public Optional<Boolean> getMultipleValues() {
                return Optional.ofNullable(multipleValues);
            }

            @Override
            public Optional<List<Option<?>>> getOptions() {
                return Optional.ofNullable(options);
            }

            @Override
            public Optional<OptionsDataSource> getOptionsDataSource() {
                return Optional.ofNullable(
                    optionsFunction == null
                        ? null
                        : new OptionsDataSourceImpl(loadOptionsDependsOn, optionsFunction));
            }
        }

        public static final class ModifiableBooleanProperty
            extends ModifiableValueProperty<Boolean, ModifiableBooleanProperty>
            implements ModifiableInputProperty, ModifiableOutputProperty<Boolean>, Property.BooleanProperty {

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

        public static final class ModifiableDateProperty
            extends ModifiableValueProperty<LocalDate, ModifiableDateProperty>
            implements ModifiableOutputProperty<LocalDate>, ModifiableInputProperty, Property.DateProperty {

            private List<String> loadOptionsDependsOn;
            private List<Option<?>> options;
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

            public ModifiableDateProperty loadOptionsDependsOn(String... loadOptionsDependsOn) {
                if (loadOptionsDependsOn != null) {
                    this.loadOptionsDependsOn = List.of(loadOptionsDependsOn);
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
            public ControlType getControlType() {
                if ((options == null || options.isEmpty()) && optionsFunction == null) {
                    return ControlType.DATE;
                } else {
                    return ControlType.SELECT;
                }
            }

            @Override
            public Optional<List<Option<?>>> getOptions() {
                return Optional.ofNullable(options);
            }

            @Override
            public Optional<OptionsDataSource> getOptionsDataSource() {
                return Optional.ofNullable(
                    optionsFunction == null
                        ? null
                        : new OptionsDataSourceImpl(loadOptionsDependsOn, optionsFunction));
            }
        }

        public static final class ModifiableDateTimeProperty
            extends ModifiableValueProperty<LocalDateTime, ModifiableDateTimeProperty>
            implements ModifiableInputProperty, ModifiableOutputProperty<LocalDateTime>, Property.DateTimeProperty {

            private List<String> loadOptionsDependsOn;
            private List<Option<?>> options;
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

            public ModifiableDateTimeProperty loadOptionsDependsOn(String... loadOptionsDependsOn) {
                if (loadOptionsDependsOn != null) {
                    this.loadOptionsDependsOn = List.of(loadOptionsDependsOn);
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
            public ControlType getControlType() {
                if ((options == null || options.isEmpty()) && optionsFunction == null) {
                    return ControlType.DATE_TIME;
                } else {
                    return ControlType.SELECT;
                }
            }

            @Override
            public Optional<List<Option<?>>> getOptions() {
                return Optional.ofNullable(options);
            }

            @Override
            public Optional<OptionsDataSource> getOptionsDataSource() {
                return Optional.ofNullable(
                    optionsFunction == null
                        ? null
                        : new OptionsDataSourceImpl(loadOptionsDependsOn, optionsFunction));
            }
        }

        public static final class ModifiableDynamicPropertiesProperty
            extends ModifiableProperty<ModifiableDynamicPropertiesProperty>
            implements ModifiableInputProperty, Property.DynamicPropertiesProperty {

            private List<String> loadPropertiesDependsOn;
            private PropertiesFunction propertiesFunction;

            public ModifiableDynamicPropertiesProperty(String name) {
                super(name, Type.DYNAMIC_PROPERTIES);
            }

            public ModifiableDynamicPropertiesProperty loadPropertiesDependsOn(String... loadPropertiesDependsOn) {
                if (loadPropertiesDependsOn != null) {
                    this.loadPropertiesDependsOn = List.of(loadPropertiesDependsOn);
                }

                return this;
            }

            public ModifiableDynamicPropertiesProperty properties(PropertiesFunction propertiesFunction) {
                this.propertiesFunction = propertiesFunction;

                return this;
            }

            @Override
            public PropertiesDataSource getDynamicPropertiesDataSource() {
                if (propertiesFunction == null) {
                    return null;
                }

                return new PropertiesDataSourceImpl(loadPropertiesDependsOn, propertiesFunction);
            }
        }

        public static final class ModifiableIntegerProperty
            extends ModifiableValueProperty<Integer, ModifiableIntegerProperty>
            implements ModifiableInputProperty, ModifiableOutputProperty<Integer>, Property.IntegerProperty {

            private Integer maxValue;
            private Integer minValue;
            private List<String> loadOptionsDependsOn;
            private List<Option<?>> options;
            private OptionsFunction optionsFunction;

            private ModifiableIntegerProperty() {
                this(null);
            }

            private ModifiableIntegerProperty(String name) {
                super(name, Type.INTEGER);
            }

            public ModifiableIntegerProperty defaultValue(int value) {
                this.defaultValue = value;

                return this;
            }

            public ModifiableIntegerProperty exampleValue(int exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableIntegerProperty loadOptionsDependsOn(String... loadOptionsDependsOn) {
                if (loadOptionsDependsOn != null) {
                    this.loadOptionsDependsOn = List.of(loadOptionsDependsOn);
                }

                return this;
            }

            public ModifiableIntegerProperty maxValue(int maxValue) {
                this.maxValue = maxValue;

                return this;
            }

            public ModifiableIntegerProperty minValue(int minValue) {
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

            public ModifiableIntegerProperty options(OptionsFunction optionsFunction) {
                this.optionsFunction = optionsFunction;

                return this;
            }

            @Override
            public ControlType getControlType() {
                if ((options == null || options.isEmpty()) && optionsFunction == null) {
                    return ControlType.INTEGER;
                } else {
                    return ControlType.SELECT;
                }
            }

            @Override
            public Optional<Integer> getMaxValue() {
                return Optional.ofNullable(maxValue);
            }

            @Override
            public Optional<Integer> getMinValue() {
                return Optional.ofNullable(minValue);
            }

            @Override
            public Optional<List<Option<?>>> getOptions() {
                return Optional.ofNullable(options);
            }

            @Override
            public Optional<OptionsDataSource> getOptionsDataSource() {
                return Optional.ofNullable(
                    optionsFunction == null
                        ? null
                        : new OptionsDataSourceImpl(loadOptionsDependsOn, optionsFunction));
            }
        }

        public sealed interface ModifiableInputProperty extends Property.InputProperty
            permits ModifiableArrayProperty, ModifiableBooleanProperty, ModifiableDateProperty,
            ModifiableDateTimeProperty, ModifiableDynamicPropertiesProperty, ModifiableIntegerProperty,
            ModifiableNullProperty, ModifiableNumberProperty, ModifiableObjectProperty, ModifiableStringProperty,
            ModifiableTimeProperty {

        }

        public static final class ModifiableNullProperty
            extends ModifiableValueProperty<Void, ModifiableNullProperty>
            implements ModifiableInputProperty, ModifiableOutputProperty<Void>, Property.NullProperty {

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
            implements ModifiableInputProperty, ModifiableOutputProperty<Double>, Property.NumberProperty {

            private Double maxValue;
            private Double minValue;
            private Integer numberPrecision;
            private List<String> loadOptionsDependsOn;
            private List<Option<?>> options;
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

            public ModifiableNumberProperty loadOptionsDependsOn(String... loadOptionsDependsOn) {
                if (loadOptionsDependsOn != null) {
                    this.loadOptionsDependsOn = List.of(loadOptionsDependsOn);
                }

                return this;
            }

            public ModifiableNumberProperty maxValue(double maxValue) {
                this.maxValue = maxValue;

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

            public ModifiableNumberProperty options(OptionsFunction optionsFunction) {
                this.optionsFunction = optionsFunction;

                return this;
            }

            @Override
            public ControlType getControlType() {
                if ((options == null || options.isEmpty()) && optionsFunction == null) {
                    return ControlType.NUMBER;
                } else {
                    return ControlType.SELECT;
                }
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
            public Optional<Integer> getNumberPrecision() {
                return Optional.ofNullable(numberPrecision);
            }

            @Override
            public Optional<List<Option<?>>> getOptions() {
                return Optional.ofNullable(options);
            }

            @Override
            public Optional<OptionsDataSource> getOptionsDataSource() {
                return Optional.ofNullable(
                    optionsFunction == null
                        ? null
                        : new OptionsDataSourceImpl(loadOptionsDependsOn, optionsFunction));
            }
        }

        public static final class ModifiableObjectProperty
            extends ModifiableValueProperty<Object, ModifiableObjectProperty>
            implements ModifiableInputProperty, ModifiableOutputProperty<Object>, Property.ObjectProperty {

            private List<? extends ModifiableValueProperty<?, ?>> additionalProperties;
            private List<String> loadOptionsDependsOn;
            private Boolean multipleValues;
            private String objectType;
            private List<Option<?>> options;
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

            public ModifiableObjectProperty loadOptionsDependsOn(String... loadOptionsDependsOn) {
                if (loadOptionsDependsOn != null) {
                    this.loadOptionsDependsOn = List.of(loadOptionsDependsOn);
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
                if ((options == null || options.isEmpty()) && optionsFunction == null) {
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
            public Optional<OptionsDataSource> getOptionsDataSource() {
                return Optional.ofNullable(
                    optionsFunction == null
                        ? null
                        : new OptionsDataSourceImpl(loadOptionsDependsOn, optionsFunction));
            }

            @Override
            public Optional<List<? extends ValueProperty<?>>> getProperties() {
                return Optional.ofNullable(properties == null ? null : new ArrayList<>(properties));
            }
        }

        public sealed interface ModifiableOutputProperty<V> extends Property.OutputProperty<V>
            permits ModifiableArrayProperty, ModifiableBooleanProperty, ModifiableDateProperty,
            ModifiableDateTimeProperty, ModifiableIntegerProperty, ModifiableNullProperty, ModifiableNumberProperty,
            ModifiableObjectProperty, ModifiableStringProperty, ModifiableTimeProperty {
        }

        public static final class ModifiableStringProperty
            extends ModifiableValueProperty<String, ModifiableStringProperty>
            implements ModifiableInputProperty, ModifiableOutputProperty<String>, Property.StringProperty {

            private ControlType controlType;
            private List<String> loadOptionsDependsOn;
            private Integer maxLength;
            private Integer minLength;
            private List<Option<?>> options;
            private OptionsFunction optionsFunction;
            private SampleDataType sampleDataType;

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

            public ModifiableStringProperty loadOptionsDependsOn(String... loadOptionsDependsOn) {
                if (loadOptionsDependsOn != null) {
                    this.loadOptionsDependsOn = List.of(loadOptionsDependsOn);
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

            public ModifiableStringProperty sampleDataType(SampleDataType sampleDataType) {
                this.sampleDataType = sampleDataType;

                return this;
            }

            @Override
            public ControlType getControlType() {
                if (this.controlType == null) {
                    if ((options == null || options.isEmpty()) && optionsFunction == null) {
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

            @Override
            public Optional<OptionsDataSource> getOptionsDataSource() {
                return Optional.ofNullable(
                    optionsFunction == null
                        ? null
                        : new OptionsDataSourceImpl(loadOptionsDependsOn, optionsFunction));
            }

            @Override
            public Optional<SampleDataType> getSampleDataType() {
                return Optional.ofNullable(sampleDataType);
            }
        }

        public static final class ModifiableTimeProperty
            extends ModifiableValueProperty<LocalTime, ModifiableTimeProperty>
            implements ModifiableInputProperty, ModifiableOutputProperty<LocalTime>, Property.TimeProperty {

            private List<String> loadOptionsDependsOn;
            private List<Option<?>> options;
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

            public ModifiableTimeProperty loadOptionsDependsOn(String... loadOptionsDependsOn) {
                if (loadOptionsDependsOn != null) {
                    this.loadOptionsDependsOn = List.of(loadOptionsDependsOn);
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
            public ControlType getControlType() {
                return ControlType.TIME;
            }

            @Override
            public Optional<List<Option<?>>> getOptions() {
                return Optional.ofNullable(options);
            }

            @Override
            public Optional<OptionsDataSource> getOptionsDataSource() {
                return Optional.ofNullable(
                    optionsFunction == null
                        ? null
                        : new OptionsDataSourceImpl(loadOptionsDependsOn, optionsFunction));
            }
        }

        public abstract static sealed class ModifiableValueProperty<V, P extends ModifiableValueProperty<V, P>>
            extends ModifiableProperty<P>
            implements
            Property.ValueProperty<V> permits ModifiableArrayProperty, ModifiableBooleanProperty,
            ModifiableDateProperty, ModifiableDateTimeProperty, ModifiableIntegerProperty, ModifiableNullProperty,
            ModifiableNumberProperty, ModifiableObjectProperty, ModifiableStringProperty, ModifiableTimeProperty {

            protected V defaultValue;
            protected V exampleValue;

            protected ModifiableValueProperty(String name, Type type) {
                super(name, type);
            }

            @Override
            public Optional<V> getDefaultValue() {
                return Optional.ofNullable(defaultValue);
            }

            @Override
            public Optional<V> getExampleValue() {
                return Optional.ofNullable(exampleValue);
            }
        }
    }
    // CHECKSTYLE:ON

    public static final class ModifiableOption<T> implements Option<T> {

        private String description;
        private String displayCondition;
        private final String name;
        private final T value;

        private ModifiableOption(String name, T value) {
            this.name = name;
            this.value = value;
        }

        private ModifiableOption(String name, T value, String description) {
            this.name = name;
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
        public String getName() {
            return name;
        }

        @Override
        public T getValue() {
            return value;
        }
    }

    private static class OptionsDataSourceImpl implements OptionsDataSource {

        private final List<String> loadOptionsDependsOn;
        private final OptionsFunction options;

        private OptionsDataSourceImpl(List<String> loadOptionsDependOnPropertyNames, OptionsFunction options) {
            this.loadOptionsDependsOn = loadOptionsDependOnPropertyNames;
            this.options = Objects.requireNonNull(options);
        }

        @Override
        public Optional<List<String>> getLoadOptionsDependsOn() {
            return Optional
                .ofNullable(loadOptionsDependsOn == null ? null : Collections.unmodifiableList(loadOptionsDependsOn));
        }

        @Override
        public OptionsFunction getOptions() {
            return options;
        }
    }

    protected static final class ResourcesImpl implements Resources {

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

    private static class PropertiesDataSourceImpl implements PropertiesDataSource {

        private final List<String> loadPropertiesDependsOn;
        private final PropertiesFunction propertiesFunction;

        @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
        private PropertiesDataSourceImpl(List<String> loadPropertiesDependOn, PropertiesFunction propertiesFunction) {
            if (loadPropertiesDependOn == null || loadPropertiesDependOn.isEmpty()) {
                throw new IllegalStateException("loadPropertiesDependsOn is not defined.");
            }

            if (propertiesFunction == null) {
                throw new IllegalStateException("propertiesFunction is not defined.");
            }

            this.loadPropertiesDependsOn = loadPropertiesDependOn;
            this.propertiesFunction = propertiesFunction;
        }

        @Override
        public List<String> getLoadPropertiesDependsOn() {
            return Collections.unmodifiableList(loadPropertiesDependsOn);
        }

        @Override
        public PropertiesFunction getProperties() {
            return propertiesFunction;
        }
    }
}
