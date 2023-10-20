
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

package com.bytechef.hermes.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefinitionDSL {

    public static ModifiableProperty.ModifiableArrayProperty array() {
        return new ModifiableProperty.ModifiableArrayProperty(null);
    }

    public static ModifiableProperty.ModifiableArrayProperty array(String name) {
        return new ModifiableProperty.ModifiableArrayProperty(name);
    }

    public static ModifiableProperty.ModifiableBooleanProperty bool() {
        return new ModifiableProperty.ModifiableBooleanProperty(null);
    }

    public static ModifiableProperty.ModifiableBooleanProperty bool(String name) {
        return new ModifiableProperty.ModifiableBooleanProperty(name);
    }

    public static ModifiableProperty.ModifiableDateProperty date() {
        return new ModifiableProperty.ModifiableDateProperty(null);
    }

    public static ModifiableProperty.ModifiableDateProperty date(String name) {
        return new ModifiableProperty.ModifiableDateProperty(name);
    }

    public static ModifiableProperty.ModifiableDateTimeProperty dateTime() {
        return new ModifiableProperty.ModifiableDateTimeProperty(null);
    }

    public static ModifiableProperty.ModifiableDateTimeProperty dateTime(String name) {
        return new ModifiableProperty.ModifiableDateTimeProperty(name);
    }

    public static ModifiableProperty.ModifiableIntegerProperty integer() {
        return new ModifiableProperty.ModifiableIntegerProperty(null);
    }

    public static ModifiableProperty.ModifiableIntegerProperty integer(String name) {
        return new ModifiableProperty.ModifiableIntegerProperty(name);
    }

    public static ModifiableProperty.ModifiableNullProperty nullable() {
        return new ModifiableProperty.ModifiableNullProperty(null);
    }

    public static ModifiableProperty.ModifiableNullProperty nullable(String name) {
        return new ModifiableProperty.ModifiableNullProperty(name);
    }

    public static ModifiableProperty.ModifiableNumberProperty number() {
        return new ModifiableProperty.ModifiableNumberProperty(null);
    }

    public static ModifiableProperty.ModifiableNumberProperty number(String name) {
        return new ModifiableProperty.ModifiableNumberProperty(name);
    }

    public static ModifiableProperty.ModifiableObjectProperty object() {
        return new ModifiableProperty.ModifiableObjectProperty(null);
    }

    public static ModifiableProperty.ModifiableObjectProperty object(String name) {
        return new ModifiableProperty.ModifiableObjectProperty(name);
    }

    public static ModifiableProperty.ModifiableOneOfProperty oneOf() {
        return oneOf(null);
    }

    public static ModifiableProperty.ModifiableOneOfProperty oneOf(String name) {
        return new ModifiableProperty.ModifiableOneOfProperty(name);
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
        return new ModifiableOption<Object>(name, value, description);
    }

    public static ModifiableOption<String> option(String name, String value) {
        return new ModifiableOption<>(name, value);
    }

    public static ModifiableOption<String> option(String name, String value, String description) {
        return new ModifiableOption<>(name, value, description);
    }

    public static ModifiableResources resources() {
        return new ModifiableResources();
    }

    public static ModifiableProperty.ModifiableStringProperty string() {
        return new ModifiableProperty.ModifiableStringProperty();
    }

    public static ModifiableProperty.ModifiableStringProperty string(String name) {
        return new ModifiableProperty.ModifiableStringProperty(name);
    }

    public static ModifiableProperty.ModifiableTimeProperty time() {
        return new ModifiableProperty.ModifiableTimeProperty();
    }

    public static ModifiableProperty.ModifiableTimeProperty time(String name) {
        return new ModifiableProperty.ModifiableTimeProperty(name);
    }

    protected static ModifiableProperty.ModifiableObjectProperty buildObject(
        String name, String description, String objectType) {
        return new ModifiableProperty.ModifiableObjectProperty(name)
            .description(description)
            .objectType(objectType);
    }

    protected static ModifiableProperty.ModifiableObjectProperty buildObject(
        String name, String description, String objectType, Property<?>... properties) {
        return new ModifiableProperty.ModifiableObjectProperty(name)
            .description(description)
            .objectType(objectType)
            .properties(properties);
    }

    protected static class ModifiableDynamicPropertiesDataSource implements DynamicPropertiesDataSource {

        private final List<String> loadPropertiesDependsOn;

        protected ModifiableDynamicPropertiesDataSource(List<String> loadPropertiesDependOnPropertyNames) {
            this.loadPropertiesDependsOn = loadPropertiesDependOnPropertyNames;
        }

        @Override
        public List<String> getLoadPropertiesDependsOn() {
            return Collections.unmodifiableList(loadPropertiesDependsOn);
        }
    }

    // CHECKSTYLE:OFF
    public static sealed abstract class ModifiableProperty<M extends ModifiableProperty<M, P>, P extends Property<P>>
        implements
        Property<P>
        permits ModifiableProperty.ModifiableDynamicPropertiesProperty, ModifiableProperty.ModifiableNullProperty,
        ModifiableProperty.ModifiableOneOfProperty, ModifiableProperty.ModifiableValueProperty {

        private Boolean advancedOption;
        private String description;
        private String displayCondition;
        private Boolean expressionEnabled;
        private Boolean hidden;
        private String label;

        @JsonIgnore
        private Map<String, Object> metadata = new HashMap<>();
        private String placeholder;
        private Boolean required;
        private String name;
        private Property.Type type;

        protected ModifiableProperty() {
        }

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
        public Optional<Boolean> getExpressionDisabled() {
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
        public Property.Type getType() {
            return type;
        }

        @JsonTypeName("ARRAY")
        public static final class ModifiableArrayProperty
            extends ModifiableValueProperty<Object[], ModifiableArrayProperty, ArrayProperty>
            implements Property.ArrayProperty {

            private List<Property<?>> items;
            private Boolean multipleValues = true;
            private List<Option<?>> options;
            private OptionsDataSource optionsDataSource;

            private ModifiableArrayProperty() {
                this(null);
            }

            private ModifiableArrayProperty(String name) {
                super(name, Type.ARRAY);
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

            public ModifiableArrayProperty exampleValue(Map<String, ?>... exampleValue) {
                this.exampleValue = exampleValue;

                return this;
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

            public ModifiableArrayProperty defaultValue(Map<String, ?>... defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableArrayProperty items(Property<?>... items) {
                if (items != null) {
                    this.items = List.of(items);
                }

                return this;
            }

            public ModifiableArrayProperty multipleValues(boolean multipleValues) {
                this.multipleValues = multipleValues;

                return this;
            }

            public ModifiableArrayProperty options(Option... options) {
                this.options = List.of(options);

                return this;
            }

            public ModifiableArrayProperty optionsDataSource(OptionsDataSource optionsDataSource) {
                this.optionsDataSource = optionsDataSource;

                return this;
            }

            @Override
            public ControlType getControlType() {
                if ((options == null || options.isEmpty()) && optionsDataSource == null) {
                    return ControlType.JSON_BUILDER;
                } else {
                    return ControlType.MULTI_SELECT;
                }
            }

            @Override
            public List<Property<?>> getItems() {
                return items;
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
                return Optional.ofNullable(optionsDataSource);
            }

            @Override
            public Object accept(PropertyVisitor propertyVisitor) {
                return propertyVisitor.visit(this);
            }
        }

        @JsonTypeName("BOOLEAN")
        public static final class ModifiableBooleanProperty
            extends ModifiableValueProperty<Boolean, ModifiableBooleanProperty, BooleanProperty>
            implements Property.BooleanProperty {

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
            public Object accept(PropertyVisitor propertyVisitor) {
                return propertyVisitor.visit(this);
            }
        }

        @JsonTypeName("DATE")
        public static final class ModifiableDateProperty extends
            ModifiableValueProperty<LocalDate, ModifiableDateProperty, DateProperty> implements Property.DateProperty {

            private List<Option<?>> options;
            private OptionsDataSource optionsDataSource;

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

            public ModifiableDateProperty options(Option... options) {
                this.options = List.of(options);

                return this;
            }

            public ModifiableDateProperty optionsDataSource(OptionsDataSource optionsDataSource) {
                this.optionsDataSource = optionsDataSource;

                return this;
            }

            @Override
            public ControlType getControlType() {
                if ((options == null || options.isEmpty()) && optionsDataSource == null) {
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
            public OptionsDataSource getOptionsDataSource() {
                return optionsDataSource;
            }

            @Override
            public Object accept(PropertyVisitor propertyVisitor) {
                return propertyVisitor.visit(this);
            }
        }

        @JsonTypeName("DATE_TIME")
        public static final class ModifiableDateTimeProperty
            extends ModifiableValueProperty<LocalDateTime, ModifiableDateTimeProperty, DateTimeProperty>
            implements Property.DateTimeProperty {

            private List<Option<?>> options;
            private OptionsDataSource optionsDataSource;

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

            public ModifiableDateTimeProperty options(Option... options) {
                this.options = List.of(options);

                return this;
            }

            public ModifiableDateTimeProperty optionsDataSource(OptionsDataSource optionsDataSource) {
                this.optionsDataSource = optionsDataSource;

                return this;
            }

            @Override
            public ControlType getControlType() {
                if ((options == null || options.isEmpty()) && optionsDataSource == null) {
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
            public OptionsDataSource getOptionsDataSource() {
                return optionsDataSource;
            }

            @Override
            public Object accept(PropertyVisitor propertyVisitor) {
                return propertyVisitor.visit(this);
            }
        }

        @JsonTypeName("INTEGER")
        public static final class ModifiableIntegerProperty
            extends ModifiableValueProperty<Integer, ModifiableIntegerProperty, IntegerProperty>
            implements Property.IntegerProperty {

            private Integer maxValue;
            private Integer minValue;
            private List<Option<?>> options;
            private OptionsDataSource optionsDataSource;

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

            public ModifiableIntegerProperty maxValue(int maxValue) {
                this.maxValue = maxValue;

                return this;
            }

            public ModifiableIntegerProperty minValue(int minValue) {
                this.minValue = minValue;

                return this;
            }

            public ModifiableIntegerProperty options(Option... options) {
                this.options = List.of(options);

                return this;
            }

            public ModifiableIntegerProperty optionsDataSource(OptionsDataSource optionsDataSource) {
                this.optionsDataSource = optionsDataSource;

                return this;
            }

            @Override
            public ControlType getControlType() {
                if ((options == null || options.isEmpty()) && optionsDataSource == null) {
                    return ControlType.INPUT_INTEGER;
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
                return Optional.ofNullable(optionsDataSource);
            }

            @Override
            public Object accept(PropertyVisitor propertyVisitor) {
                return propertyVisitor.visit(this);
            }
        }

        @JsonTypeName("NULL")
        public static final class ModifiableNullProperty
            extends ModifiableProperty<ModifiableNullProperty, NullProperty>
            implements Property.NullProperty {

            private ModifiableNullProperty() {
                this(null);
            }

            public ModifiableNullProperty(String name) {
                super(name, Type.NULL);
            }

            @Override
            public Object accept(PropertyVisitor propertyVisitor) {
                return propertyVisitor.visit(this);
            }
        }

        @JsonTypeName("NUMBER")
        public static final class ModifiableNumberProperty
            extends ModifiableValueProperty<Double, ModifiableNumberProperty, NumberProperty>
            implements Property.NumberProperty {

            private Integer maxValue;
            private Integer minValue;
            private Integer numberPrecision;
            private List<Option<?>> options;
            private OptionsDataSource optionsDataSource;

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

            public ModifiableNumberProperty maxValue(int maxValue) {
                this.maxValue = maxValue;

                return this;
            }

            public ModifiableNumberProperty minValue(int minValue) {
                this.minValue = minValue;

                return this;
            }

            public ModifiableNumberProperty numberPrecision(Integer numberPrecision) {
                this.numberPrecision = numberPrecision;

                return this;
            }

            public ModifiableNumberProperty options(Option... options) {
                this.options = List.of(options);

                return this;
            }

            public ModifiableNumberProperty optionsDataSource(OptionsDataSource optionsDataSource) {
                this.optionsDataSource = optionsDataSource;

                return this;
            }

            @Override
            public ControlType getControlType() {
                if ((options == null || options.isEmpty()) && optionsDataSource == null) {
                    return ControlType.INPUT_NUMBER;
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
            public Optional<Integer> getNumberPrecision() {
                return Optional.ofNullable(numberPrecision);
            }

            @Override
            public Optional<List<Option<?>>> getOptions() {
                return Optional.ofNullable(options);
            }

            @Override
            public Optional<OptionsDataSource> getOptionsDataSource() {
                return Optional.ofNullable(optionsDataSource);
            }

            @Override
            public Object accept(PropertyVisitor propertyVisitor) {
                return propertyVisitor.visit(this);
            }
        }

        @JsonTypeName("OBJECT")
        public static final class ModifiableObjectProperty
            extends ModifiableValueProperty<Object, ModifiableObjectProperty, ObjectProperty>
            implements Property.ObjectProperty {

            private List<? extends Property<?>> additionalProperties;
            private Boolean multipleValues = true;
            private String objectType;
            private List<Option<?>> options;
            private OptionsDataSource optionsDataSource;
            private List<? extends Property<?>> properties;

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

            public ModifiableObjectProperty additionalProperties(Property<?>... additionalProperties) {
                if (additionalProperties != null) {
                    this.additionalProperties = List.of(additionalProperties);
                }

                return this;
            }

            @SuppressWarnings("rawtypes")
            public ModifiableObjectProperty additionalProperties(List<Property> additionalProperties) {
                if (additionalProperties != null) {
                    if (properties.size() > 0) {
                        Property<?> firstProperty = properties.get(0);

                        String firstName = firstProperty.getName();

                        boolean emptyName = firstName == null || firstName.isEmpty();

                        for (Property<?> property : properties) {
                            String name = property.getName();

                            if (emptyName && !(name == null || name.isEmpty()) ||
                                !emptyName && (name == null || name.isEmpty())) {

                                throw new IllegalArgumentException(
                                    emptyName
                                        ? "Additional properties defined have to have empty names."
                                        : "Additional properties defined cannot to have empty names.");
                            }
                        }
                    }

                    this.additionalProperties = additionalProperties.stream()
                        .map(property -> (Property<?>) property)
                        .toList();
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

            public ModifiableObjectProperty options(Option... options) {
                this.options = List.of(options);

                return this;
            }

            public ModifiableObjectProperty optionsDataSource(OptionsDataSource optionsDataSource) {
                this.optionsDataSource = optionsDataSource;

                return this;
            }

            public ModifiableObjectProperty properties(Property<?>... properties) {
                if (properties != null) {
                    this.properties = List.of(properties);
                }

                return this;
            }

            @SuppressWarnings("rawtypes")
            public ModifiableObjectProperty properties(List<Property> properties) {
                if (properties != null) {
                    for (Property<?> property : properties) {
                        String name = property.getName();

                        if (name == null || name.isEmpty()) {
                            throw new IllegalArgumentException("Name cannot be empty for property.");
                        }
                    }

                    this.properties = properties.stream()
                        .map(property -> (Property<?>) property)
                        .toList();
                }

                return this;
            }

            @Override
            public Optional<List<? extends Property<?>>> getAdditionalProperties() {
                return Optional.ofNullable(
                    additionalProperties == null ? null : new ArrayList<>(additionalProperties));
            }

            @Override
            public ControlType getControlType() {
                if (objectType != null) {
                    return ControlType.INPUT_TEXT;
                }
                if ((options == null || options.isEmpty()) && optionsDataSource == null) {
                    return ControlType.JSON_BUILDER;
                } else {
                    return ControlType.MULTI_SELECT;
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
                return Optional.ofNullable(optionsDataSource);
            }

            @Override
            public Optional<List<? extends Property<?>>> getProperties() {
                return Optional.ofNullable(properties == null ? null : new ArrayList<>(properties));
            }

            @Override
            public Object accept(PropertyVisitor propertyVisitor) {
                return propertyVisitor.visit(this);
            }
        }

        @JsonTypeName("DYNAMIC_PROPERTIES")
        public static final class ModifiableDynamicPropertiesProperty
            extends ModifiableProperty<ModifiableDynamicPropertiesProperty, DynamicPropertiesProperty>
            implements Property.DynamicPropertiesProperty {

            @JsonIgnore
            private DynamicPropertiesDataSource dynamicPropertiesDataSource;

            public ModifiableDynamicPropertiesProperty dynamicPropertiesDataSource(
                DynamicPropertiesDataSource dynamicPropertiesDataSource) {

                this.dynamicPropertiesDataSource = dynamicPropertiesDataSource;

                return this;
            }

            @Override
            public DynamicPropertiesDataSource getDynamicPropertiesDataSource() {
                return dynamicPropertiesDataSource;
            }

            @Override
            public Object accept(PropertyVisitor propertyVisitor) {
                return propertyVisitor.visit(this);
            }
        }

        @JsonTypeName("ONE_OF")
        public static final class ModifiableOneOfProperty
            extends ModifiableProperty<ModifiableOneOfProperty, OneOfProperty> implements Property.OneOfProperty {

            private List<? extends Property<?>> types = List.of(
                new ModifiableArrayProperty(null),
                new ModifiableBooleanProperty(null),
                new ModifiableDateProperty(null),
                new ModifiableDateTimeProperty(null),
                new ModifiableIntegerProperty(null),
                new ModifiableNumberProperty(null),
                new ModifiableObjectProperty(null),
                new ModifiableStringProperty(null));

            private ModifiableOneOfProperty() {
                super(null, Type.ONE_OF);
            }

            private ModifiableOneOfProperty(String name) {
                super(name, Type.ONE_OF);
            }

            public ModifiableOneOfProperty types(Property<?>... types) {
                this.types = List.of(types);

                return this;
            }

            public List<? extends Property<?>> getTypes() {
                return types;
            }

            @Override
            public Object accept(PropertyVisitor propertyVisitor) {
                return propertyVisitor.visit(this);
            }
        }

        @JsonTypeName("STRING")
        public static final class ModifiableStringProperty
            extends ModifiableValueProperty<String, ModifiableStringProperty, StringProperty>
            implements Property.StringProperty {

            private ControlType controlType;
            private List<Option<?>> options;
            private OptionsDataSource optionsDataSource;
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

            public ModifiableStringProperty options(Option... options) {
                this.options = List.of(options);

                return this;
            }

            public ModifiableStringProperty options(List<ModifiableOption<String>> options) {
                this.options = new ArrayList<>(options);

                return this;
            }

            public ModifiableStringProperty optionsDataSource(OptionsDataSource optionsDataSource) {
                this.optionsDataSource = optionsDataSource;

                return this;
            }

            public ModifiableStringProperty sampleDataType(SampleDataType sampleDataType) {
                this.sampleDataType = sampleDataType;

                return this;
            }

            @Override
            public Object accept(PropertyVisitor propertyVisitor) {
                return propertyVisitor.visit(this);
            }

            @Override
            public ControlType getControlType() {
                if (this.controlType == null) {
                    if ((options == null || options.isEmpty()) && optionsDataSource == null) {
                        return ControlType.INPUT_TEXT;
                    } else {
                        return ControlType.SELECT;
                    }
                } else {
                    return controlType;
                }
            }

            @Override
            public Optional<List<Option<?>>> getOptions() {
                return Optional.ofNullable(options == null ? null : new ArrayList<>(options));
            }

            @Override
            public Optional<OptionsDataSource> getOptionsDataSource() {
                return Optional.ofNullable(optionsDataSource);
            }

            @Override
            public Optional<SampleDataType> getSampleDataType() {
                return Optional.ofNullable(sampleDataType);
            }
        }

        @JsonTypeName("TIME")
        public static final class ModifiableTimeProperty
            extends ModifiableValueProperty<LocalTime, ModifiableTimeProperty, TimeProperty>
            implements Property.TimeProperty {

            private int hour;
            private int minute;
            private int second;

            private ModifiableTimeProperty() {
                this(null);
            }

            private ModifiableTimeProperty(String name) {
                super(name, Type.TIME);
            }

            public ModifiableTimeProperty hour(int hour) {
                this.hour = hour;

                return this;
            }

            public ModifiableTimeProperty minute(int minute) {
                this.minute = minute;

                return this;
            }

            public ModifiableTimeProperty second(int second) {
                this.second = second;

                return this;
            }

            @Override
            public ControlType getControlType() {
                return ControlType.TIME;
            }

            @Override
            public Object accept(PropertyVisitor propertyVisitor) {
                return propertyVisitor.visit(this);
            }

            @Override
            public int getHour() {
                return hour;
            }

            @Override
            public int getMinute() {
                return minute;
            }

            @Override
            public int getSecond() {
                return second;
            }
        }

        public abstract static sealed class ModifiableValueProperty<V, M extends ModifiableValueProperty<V, M, P>, P extends ValueProperty<V, P>>
            extends ModifiableProperty<M, P>
            implements
            Property.ValueProperty<V, P> permits ModifiableArrayProperty, ModifiableBooleanProperty,
            ModifiableDateProperty, ModifiableDateTimeProperty, ModifiableIntegerProperty, ModifiableNumberProperty,
            ModifiableObjectProperty, ModifiableStringProperty, ModifiableTimeProperty {

            protected V defaultValue;
            protected V exampleValue;

            private ModifiableValueProperty(Property.Type type) {
                this(null, type);
            }

            protected ModifiableValueProperty(String name, Property.Type type) {
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
        private String name;
        private T value;

        private ModifiableOption() {
        }

        private ModifiableOption(String name, T value) {
            this.name = name;
            this.value = value;
        }

        private ModifiableOption(String name, T value, String description) {
            this.name = name;
            this.value = value;
            this.description = description;
        }

        public ModifiableOption description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableOption displayCondition(String displayCondition) {
            this.displayCondition = displayCondition;

            return this;
        }

        @Override
        public String getDescription() {
            return description;
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
        public Object getValue() {
            return value;
        }
    }

    protected static class ModifiableOptionsDataSource implements OptionsDataSource {

        private final List<String> loadOptionsDependsOn;

        protected ModifiableOptionsDataSource(List<String> loadOptionsDependOnPropertyNames) {
            this.loadOptionsDependsOn = loadOptionsDependOnPropertyNames;
        }

        @Override
        public List<String> getLoadOptionsDependsOn() {
            return Collections.unmodifiableList(loadOptionsDependsOn);
        }
    }

    public static final class ModifiableResources implements Resources {

        private Map<String, String> additionalUrls;
        private String[] categories;
        private String documentationUrl;

        private ModifiableResources() {
        }

        public Resources additionalUrls(Map<String, String> additionalUrls) {
            this.additionalUrls = new HashMap<>(additionalUrls);

            return this;
        }

        public Resources categories(String[] categories) {
            this.categories = categories.clone();

            return this;
        }

        public Resources documentationUrl(String documentationUrl) {
            this.documentationUrl = documentationUrl;

            return this;
        }

        @Override
        public Optional<String[]> getCategories() {
            return Optional.ofNullable(categories == null ? null : categories.clone());
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
