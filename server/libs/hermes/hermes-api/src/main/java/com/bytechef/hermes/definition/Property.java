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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
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
    @JsonSubTypes.Type(value = Property.NullProperty.class, name = "NULL"),
    @JsonSubTypes.Type(value = Property.NumberProperty.class, name = "NUMBER"),
    @JsonSubTypes.Type(value = Property.ObjectProperty.class, name = "OBJECT"),
    @JsonSubTypes.Type(value = Property.OptionProperty.class, name = "OPTION"),
    @JsonSubTypes.Type(value = Property.StringProperty.class, name = "STRING"),
})
public abstract sealed class Property<P extends Property<P>>
        permits Property.NullProperty, Property.OptionProperty, Property.TypeProperty {

    public enum EditorType {
        CODE
    }

    public enum Type {
        ANY,
        ARRAY,
        BOOLEAN,
        DATE_TIME,
        INTEGER,
        NULL,
        NUMBER,
        OBJECT,
        OPTION,
        STRING,
    }

    protected String description;
    protected DisplayOption displayOption;
    protected String label;
    protected String name;
    protected String placeholder;
    protected Type type;

    private Property() {}

    private Property(String name) {
        this.name = name;
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
    public P displayOption(DisplayOption.DisplayOptionEntry... displayOptionEntries) {
        this.displayOption = DisplayOption.build(List.of(displayOptionEntries));

        return (P) this;
    }

    @SuppressWarnings("unchecked")
    public P placeholder(String placeholder) {
        this.placeholder = placeholder;

        return (P) this;
    }

    public String getDescription() {
        return description;
    }

    public String getLabel() {
        return label;
    }

    public DisplayOption getDisplayOption() {
        return displayOption;
    }

    public String getName() {
        return name;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public Type getType() {
        return type;
    }

    @JsonTypeName("OPTION")
    public static final class OptionProperty extends Property<OptionProperty> {

        private Boolean multipleValues;
        private List<Property<?>> options;

        public OptionProperty() {
            super(null);

            this.type = Type.OPTION;
        }

        public OptionProperty multipleValues(Boolean multipleValues) {
            this.multipleValues = multipleValues;

            return this;
        }

        public OptionProperty options(Property<?>... options) {
            this.options = Stream.of(options).filter(Objects::nonNull).toList();

            return this;
        }

        public Boolean getMultipleValues() {
            return multipleValues;
        }

        public List<Property<?>> getOptions() {
            return options;
        }
    }

    public static sealed class TypeProperty<P extends TypeProperty<P>> extends Property<P>
            permits AnyProperty, ValueProperty {

        protected Boolean required;

        TypeProperty(String name) {
            super(name);
        }

        @SuppressWarnings("unchecked")
        public P required(Boolean required) {
            this.required = required;

            return (P) this;
        }

        public Boolean getRequired() {
            return required;
        }
    }

    @JsonTypeName("ANY")
    public static final class AnyProperty extends TypeProperty<AnyProperty> {

        private List<? extends TypeProperty<?>> types;

        private AnyProperty() {
            super(null);

            this.type = Type.ANY;
        }

        public AnyProperty(String name) {
            super(name);

            this.type = Type.ANY;
        }

        public AnyProperty types(TypeProperty<?>... properties) {
            this.types = List.of(properties);

            return this;
        }

        public List<? extends TypeProperty<?>> getTypes() {
            return types;
        }
    }

    public static sealed class ValueProperty<V, P extends ValueProperty<V, P>> extends TypeProperty<P>
            permits ArrayProperty, ObjectProperty, PrimitiveValueProperty {

        protected V defaultValue;
        protected V exampleValue;
        protected List<String> loadOptionsDependsOn;
        protected String loadOptionsMethod;

        private ValueProperty() {
            super(null);
        }

        public ValueProperty(String name) {
            super(name);
        }

        @SuppressWarnings("unchecked")
        public P loadOptionsDependsOn(String... propertyNames) {
            this.loadOptionsDependsOn = List.of(propertyNames);

            return (P) this;
        }

        @SuppressWarnings("unchecked")
        public P loadOptionsMethod(String loadOptionsMethod) {
            this.loadOptionsMethod = loadOptionsMethod;

            return (P) this;
        }

        public V getDefaultValue() {
            return defaultValue;
        }

        public V getExampleValue() {
            return exampleValue;
        }

        public List<String> getLoadOptionsDependsOn() {
            return loadOptionsDependsOn;
        }

        public String getLoadOptionsMethod() {
            return loadOptionsMethod;
        }
    }

    public static sealed class PrimitiveValueProperty<V, P extends PrimitiveValueProperty<V, P>>
            extends ValueProperty<V, P>
            permits BooleanProperty, DateTimeProperty, IntegerProperty, NumberProperty, StringProperty {

        protected List<PropertyOption> propertyOptions;

        private PrimitiveValueProperty() {
            super(null);
        }

        public PrimitiveValueProperty(String name) {
            super(name);
        }

        @SuppressWarnings("unchecked")
        public P options(PropertyOption... propertyOptions) {
            this.propertyOptions = List.of(propertyOptions);

            return (P) this;
        }

        public List<PropertyOption> getOptions() {
            return propertyOptions;
        }
    }

    @JsonTypeName("ARRAY")
    public static final class ArrayProperty extends ValueProperty<Object[], ArrayProperty> {

        private List<? extends TypeProperty<?>> items;

        private ArrayProperty() {}

        public ArrayProperty(String name) {
            super(name);
            this.type = Type.ARRAY;
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

        public ArrayProperty items(TypeProperty<?>... items) {
            this.items = List.of(items);

            return this;
        }

        public List<? extends TypeProperty<?>> getItems() {
            return items;
        }
    }

    @JsonTypeName("OBJECT")
    public static final class ObjectProperty extends ValueProperty<Object, ObjectProperty> {

        private Boolean additionalProperties;
        private List<? extends TypeProperty<?>> properties;

        private ObjectProperty() {}

        public ObjectProperty(String name) {
            super(name);
            this.type = Type.OBJECT;
        }

        public ObjectProperty additionalProperties(boolean additionalProperties) {
            this.additionalProperties = additionalProperties;

            return this;
        }

        public ObjectProperty defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ObjectProperty exampleValue(Object exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ObjectProperty properties(TypeProperty<?>... properties) {
            this.properties = List.of(properties);

            return this;
        }

        public Boolean getAdditionalProperties() {
            return additionalProperties;
        }

        public List<? extends TypeProperty<?>> getProperties() {
            return properties;
        }
    }

    @JsonTypeName("BOOLEAN")
    public static final class BooleanProperty extends PrimitiveValueProperty<Boolean, BooleanProperty> {

        private BooleanProperty() {}

        public BooleanProperty(String name) {
            super(name);
            this.type = Type.BOOLEAN;
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

    @JsonTypeName("DATE_TIME")
    public static final class DateTimeProperty extends PrimitiveValueProperty<LocalDateTime, DateTimeProperty> {

        private DateTimeProperty() {}

        public DateTimeProperty(String name) {
            super(name);
            this.type = Type.DATE_TIME;
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
    public static final class NullProperty extends Property<NullProperty> {

        private NullProperty() {}

        public NullProperty(String name) {
            super(name);
            this.type = Type.NULL;
        }
    }

    @JsonTypeName("NUMBER")
    public static final class NumberProperty extends PrimitiveValueProperty<Double, NumberProperty> {

        private Integer maxValue;
        private Integer minValue;
        private Integer numberPrecision;

        private NumberProperty() {}

        public NumberProperty(String name) {
            super(name);
            this.type = Type.NUMBER;
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

        public Integer getMaxValue() {
            return maxValue;
        }

        public Integer getMinValue() {
            return minValue;
        }

        public Integer getNumberPrecision() {
            return numberPrecision;
        }
    }

    @JsonTypeName("INTEGER")
    public static final class IntegerProperty extends PrimitiveValueProperty<Integer, IntegerProperty> {

        private Integer maxValue;
        private Integer minValue;

        private IntegerProperty() {}

        public IntegerProperty(String name) {
            super(name);
            this.type = Type.INTEGER;
        }

        public IntegerProperty defaultValue(Integer value) {
            this.defaultValue = value;

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

        public Integer getMaxValue() {
            return maxValue;
        }

        public Integer getMinValue() {
            return minValue;
        }
    }

    @JsonTypeName("STRING")
    public static final class StringProperty extends PrimitiveValueProperty<String, StringProperty> {

        EditorType editorType;

        private StringProperty() {}

        public StringProperty(String name) {
            super(name);
            this.type = Type.STRING;
        }

        public StringProperty defaultValue(String value) {
            this.defaultValue = value;

            return this;
        }

        public StringProperty editorType(EditorType editorType) {
            this.editorType = editorType;

            return this;
        }

        public EditorType getEditorType() {
            return editorType;
        }
    }
}
