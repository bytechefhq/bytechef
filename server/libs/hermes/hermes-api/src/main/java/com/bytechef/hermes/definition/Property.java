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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Schema(name = "Property", description = "A base property.")
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Property.AnyProperty.class, name = "ANY"),
    @JsonSubTypes.Type(value = Property.ArrayProperty.class, name = "ARRAY"),
    @JsonSubTypes.Type(value = Property.BooleanProperty.class, name = "BOOLEAN"),
    @JsonSubTypes.Type(value = Property.DateTimeProperty.class, name = "DATE_TIME"),
    @JsonSubTypes.Type(value = Property.IntegerProperty.class, name = "INTEGER"),
    @JsonSubTypes.Type(value = Property.NumberProperty.class, name = "NUMBER"),
    @JsonSubTypes.Type(value = Property.ObjectProperty.class, name = "OBJECT"),
    @JsonSubTypes.Type(value = Property.StringProperty.class, name = "STRING")
})
public abstract sealed class Property<P> permits Property.AnyProperty, Property.ValueProperty {

    public enum ControlType {
        CODE,
        PASSWORD
    }

    public enum Type {
        ANY,
        ARRAY,
        BOOLEAN,
        DATE,
        DATE_TIME,
        INTEGER,
        NULL,
        NUMBER,
        OBJECT,
        STRING
    }

    private Boolean advancedOption;
    private String description;
    private DisplayOption displayOption;
    private Boolean hidden;
    private String label;
    private Map<String, Object> metadata;
    private final String name;
    private String placeholder;
    private Boolean required;
    private final Type type;

    private Property(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public P advancedOption(boolean additional) {
        this.advancedOption = additional;

        return (P) this;
    }

    @SuppressWarnings("unchecked")
    public P description(String description) {
        this.description = description;

        return (P) this;
    }

    @SuppressWarnings("unchecked")
    public P displayOption(DisplayOption.DisplayOptionCondition... displayOptionConditions) {
        this.displayOption = DisplayOption.of(List.of(displayOptionConditions));

        return (P) this;
    }

    @SuppressWarnings("unchecked")
    public P hidden(boolean hidden) {
        this.hidden = hidden;

        return (P) this;
    }

    @SuppressWarnings("unchecked")
    public P label(String label) {
        this.label = label;

        return (P) this;
    }

    @SuppressWarnings("unchecked")
    public P metadata(String key, String value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }

        this.metadata.put(key, value);

        return (P) this;
    }

    @SuppressWarnings("unchecked")
    @SuppressFBWarnings("EI2")
    public P metadata(Map<String, Object> metadata) {
        this.metadata = metadata;

        return (P) this;
    }

    @SuppressWarnings("unchecked")
    public P placeholder(String placeholder) {
        this.placeholder = placeholder;

        return (P) this;
    }

    @SuppressWarnings("unchecked")
    public P required(Boolean required) {
        this.required = required;

        return (P) this;
    }

    @Schema(name = "additional", description = "If the property should be grouped under additional properties.")
    public Boolean getAdvancedOption() {
        return advancedOption;
    }

    @Schema(name = "description", description = "The property description.")
    public String getDescription() {
        return description;
    }

    @Schema(name = "description", description = "The property description.")
    public DisplayOption getDisplayOption() {
        return displayOption;
    }

    @Schema(name = "hidden", description = "If the property should be visible or not.")
    public Boolean getHidden() {
        return hidden;
    }

    @Schema(name = "label", description = "The property label.")
    public String getLabel() {
        return label;
    }

    @Schema(name = "metadata", description = "Additional data that can be used during processing.")
    public Map<String, Object> getMetadata() {
        return metadata == null ? null : new HashMap<>(metadata);
    }

    @Schema(name = "name", description = "The property name.")
    public String getName() {
        return name;
    }

    @Schema(name = "placeholder", description = "The property placeholder.")
    public String getPlaceholder() {
        return placeholder;
    }

    @Schema(name = "required", description = "If the property is required or not.")
    public Boolean getRequired() {
        return required;
    }

    @Schema(name = "type", description = "The property type.")
    public Type getType() {
        return type;
    }

    @Schema(name = "AnyProperty", description = "An any property type.")
    @JsonTypeName("ANY")
    public static final class AnyProperty extends Property<AnyProperty> {

        private List<? extends Property<?>> types;

        private AnyProperty() {
            super(null, Type.ANY);
        }

        public AnyProperty(String name) {
            super(name, Type.ANY);
        }

        public AnyProperty types(Property<?>... properties) {
            this.types = List.of(properties);

            return this;
        }

        @Schema(name = "types", description = "Possible types of properties that can be used.")
        public List<? extends Property<?>> getTypes() {
            return types;
        }
    }

    @Schema(name = "ValueProperty", description = "A base property for all value based properties.")
    public abstract static sealed class ValueProperty<V, P extends ValueProperty<V, P>> extends Property<P>
            permits ArrayProperty,
                    BooleanProperty,
                    DateProperty,
                    DateTimeProperty,
                    IntegerProperty,
                    NullProperty,
                    NumberProperty,
                    ObjectProperty,
                    StringProperty {

        protected V defaultValue;
        protected V exampleValue;
        private List<PropertyOption> options;
        private OptionsDataSource optionsDataSource;

        private ValueProperty(Type type) {
            this(null, type);
        }

        public ValueProperty(String name, Type type) {
            super(name, type);
        }

        @SuppressWarnings("unchecked")
        public P options(PropertyOption... options) {
            this.options = List.of(options);

            return (P) this;
        }

        @SuppressWarnings("unchecked")
        public P optionsDataSource(OptionsDataSource optionsDataSource) {
            this.optionsDataSource = optionsDataSource;

            return (P) this;
        }

        @Schema(name = "defaultValue", description = "The property default value.")
        public V getDefaultValue() {
            return defaultValue;
        }

        @Schema(name = "exampleValue", description = "The property example value.")
        public V getExampleValue() {
            return exampleValue;
        }

        @Schema(name = "options", description = "The list of valid property options.")
        public List<PropertyOption> getOptions() {
            return options;
        }

        public OptionsDataSource getOptionsDataSource() {
            return optionsDataSource;
        }
    }

    @JsonTypeName("ARRAY")
    @Schema(name = "ArrayProperty", description = "An array property type.")
    public static final class ArrayProperty extends ValueProperty<Object[], ArrayProperty> {

        private List<? extends Property> items;

        private ArrayProperty() {
            super(Type.ARRAY);
        }

        public ArrayProperty(String name) {
            super(name, Type.ARRAY);
        }

        public ArrayProperty exampleValue(Boolean... exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ArrayProperty exampleValue(Integer... exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ArrayProperty exampleValue(Long... exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ArrayProperty exampleValue(Float... exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ArrayProperty exampleValue(Double... exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ArrayProperty exampleValue(String... exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ArrayProperty exampleValue(Map<String, ?>... exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ArrayProperty defaultValue(Boolean... defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ArrayProperty defaultValue(Integer... defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ArrayProperty defaultValue(Long... defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ArrayProperty defaultValue(Float... defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ArrayProperty defaultValue(Double... defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ArrayProperty defaultValue(String... defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ArrayProperty defaultValue(Map<String, ?>... defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ArrayProperty items(Property... items) {
            this.items = List.of(items);

            return this;
        }

        @Schema(name = "items", description = "An array property type.")
        public List<? extends Property> getItems() {
            return items;
        }
    }

    @JsonTypeName("BOOLEAN")
    @Schema(name = "BooleanProperty", description = "A boolean property type.")
    public static final class BooleanProperty extends ValueProperty<Boolean, BooleanProperty> {

        private BooleanProperty() {
            super(Type.BOOLEAN);
        }

        public BooleanProperty(String name) {
            super(name, Type.BOOLEAN);
        }

        public BooleanProperty defaultValue(boolean defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public BooleanProperty exampleValue(boolean exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }
    }

    @JsonTypeName("DATE")
    @Schema(name = "DateProperty", description = "A date property type.")
    public static final class DateProperty extends ValueProperty<LocalDate, DateProperty> {

        private DateProperty() {
            super(Type.DATE);
        }

        public DateProperty(String name) {
            super(name, Type.DATE);
        }

        public DateProperty defaultValue(LocalDate defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public DateProperty exampleValue(LocalDate exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }
    }

    @JsonTypeName("DATE_TIME")
    @Schema(name = "DateTimeProperty", description = "A date-time property type.")
    public static final class DateTimeProperty extends ValueProperty<LocalDateTime, DateTimeProperty> {

        private DateTimeProperty() {
            super(Type.DATE_TIME);
        }

        public DateTimeProperty(String name) {
            super(name, Type.DATE_TIME);
        }

        public DateTimeProperty defaultValue(LocalDateTime defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public DateTimeProperty exampleValue(LocalDateTime exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }
    }

    @JsonTypeName("NULL")
    @Schema(name = "NullProperty", description = "A null property type.")
    public static final class NullProperty extends ValueProperty<Integer, NullProperty> {

        private NullProperty() {
            super(Type.NULL);
        }

        public NullProperty(String name) {
            super(name, Type.NULL);
        }
    }

    @JsonTypeName("NUMBER")
    @Schema(name = "NumberProperty", description = "A number property type.")
    public static final class NumberProperty extends ValueProperty<Double, NumberProperty> {

        private Integer maxValue;
        private Integer minValue;
        private Integer numberPrecision;

        private NumberProperty() {
            super(Type.NUMBER);
        }

        public NumberProperty(String name) {
            super(name, Type.NUMBER);
        }

        public NumberProperty defaultValue(int value) {
            this.defaultValue = (double) value;

            return this;
        }

        public NumberProperty defaultValue(long value) {
            this.defaultValue = (double) value;

            return this;
        }

        public NumberProperty defaultValue(float value) {
            this.defaultValue = (double) value;

            return this;
        }

        public NumberProperty defaultValue(double value) {
            this.defaultValue = value;

            return this;
        }

        public NumberProperty exampleValue(int value) {
            this.exampleValue = (double) value;

            return this;
        }

        public NumberProperty exampleValue(long value) {
            this.exampleValue = (double) value;

            return this;
        }

        public NumberProperty exampleValue(float value) {
            this.exampleValue = (double) value;

            return this;
        }

        public NumberProperty exampleValue(double value) {
            this.exampleValue = value;

            return this;
        }

        public NumberProperty numberPrecision(Integer numberPrecision) {
            this.numberPrecision = numberPrecision;

            return this;
        }

        public NumberProperty maxValue(int maxValue) {
            this.maxValue = maxValue;

            return this;
        }

        public NumberProperty minValue(int minValue) {
            this.minValue = minValue;

            return this;
        }

        @Schema(name = "maxValue", description = "The maximum property value.")
        public Integer getMaxValue() {
            return maxValue;
        }

        @Schema(name = "minValue", description = "The minimum property value.")
        public Integer getMinValue() {
            return minValue;
        }

        @Schema(name = "numberPrecision", description = "The number value precision.")
        public Integer getNumberPrecision() {
            return numberPrecision;
        }
    }

    @JsonTypeName("INTEGER")
    @Schema(name = "IntegerProperty", description = "An integer property type.")
    public static final class IntegerProperty extends ValueProperty<Integer, IntegerProperty> {

        private Integer maxValue;
        private Integer minValue;

        private IntegerProperty() {
            super(Type.INTEGER);
        }

        public IntegerProperty(String name) {
            super(name, Type.INTEGER);
        }

        public IntegerProperty defaultValue(int value) {
            this.defaultValue = value;

            return this;
        }

        public IntegerProperty exampleValue(int exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public IntegerProperty maxValue(int maxValue) {
            this.maxValue = maxValue;

            return this;
        }

        public IntegerProperty minValue(int minValue) {
            this.minValue = minValue;

            return this;
        }

        @Schema(name = "maxValue", description = "The maximum property value.")
        public Integer getMaxValue() {
            return maxValue;
        }

        @Schema(name = "minValue", description = "The minimum property value.")
        public Integer getMinValue() {
            return minValue;
        }
    }

    @JsonTypeName("OBJECT")
    @Schema(name = "ObjectProperty", description = "An object property type.")
    public static final class ObjectProperty extends ValueProperty<Object, ObjectProperty> {

        private Boolean additionalProperties;
        private String objectType;
        private List<? extends Property<?>> properties;

        private ObjectProperty() {
            super(Type.OBJECT);
        }

        public ObjectProperty(String name) {
            super(name, Type.OBJECT);
        }

        public ObjectProperty additionalProperties(boolean additionalProperties) {
            this.additionalProperties = additionalProperties;

            return this;
        }

        public ObjectProperty defaultValue(Map<String, Object> defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ObjectProperty exampleValue(Map<String, Object> exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ObjectProperty objectType(String objectType) {
            this.objectType = objectType;

            return this;
        }

        public ObjectProperty properties(Property<?>... properties) {
            this.properties = List.of(properties);

            return this;
        }

        @Schema(name = "additionalProperties", description = "The object can contain dynamically defined properties.")
        public Boolean getAdditionalProperties() {
            return additionalProperties;
        }

        @Schema(name = "objectType", description = "The object type.")
        public String getObjectType() {
            return objectType;
        }

        @Schema(name = "properties", description = "The list of valid object property types.")
        public List<? extends Property<?>> getProperties() {
            return properties;
        }
    }

    @JsonTypeName("STRING")
    @Schema(name = "StringProperty", description = "A string property.")
    public static final class StringProperty extends ValueProperty<String, StringProperty> {

        ControlType controlType;

        private StringProperty() {
            super(Type.STRING);
        }

        public StringProperty(String name) {
            super(name, Type.STRING);
        }

        public StringProperty defaultValue(String value) {
            this.defaultValue = value;

            return this;
        }

        public StringProperty exampleValue(String exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public StringProperty controlType(ControlType controlType) {
            this.controlType = controlType;

            return this;
        }

        public ControlType getControlType() {
            return controlType;
        }
    }
}
