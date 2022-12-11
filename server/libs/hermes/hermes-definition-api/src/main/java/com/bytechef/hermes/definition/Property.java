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
    @JsonSubTypes.Type(value = Property.ArrayProperty.class, name = "ARRAY"),
    @JsonSubTypes.Type(value = Property.BooleanProperty.class, name = "BOOLEAN"),
    @JsonSubTypes.Type(value = Property.DateTimeProperty.class, name = "DATE_TIME"),
    @JsonSubTypes.Type(value = Property.IntegerProperty.class, name = "INTEGER"),
    @JsonSubTypes.Type(value = Property.NumberProperty.class, name = "NUMBER"),
    @JsonSubTypes.Type(value = Property.ObjectProperty.class, name = "OBJECT"),
    @JsonSubTypes.Type(value = Property.OneOfProperty.class, name = "ONE_OF"),
    @JsonSubTypes.Type(value = Property.StringProperty.class, name = "STRING")
})
public sealed class Property<P extends Property<P>>
        permits Property.OneOfProperty, Property.NullProperty, Property.ValueProperty {

    public enum ControlType {
        CODE,
        PASSWORD
    }

    public enum Type {
        ARRAY,
        BOOLEAN,
        DATE,
        DATE_TIME,
        INTEGER,
        NULL,
        NUMBER,
        OBJECT,
        ONE_OF,
        STRING
    }

    protected Boolean advancedOption;
    protected String description;
    protected DisplayOption displayOption;
    protected Boolean hidden;
    protected String label;
    protected Map<String, Object> metadata;
    protected String placeholder;
    protected Boolean required;
    private final String name;
    private final Type type;

    protected Property(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Schema(name = "advancedOption", description = "If the property should be grouped under advanced options.")
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

    @Schema(name = "OneOfProperty", description = "A one of property type.")
    @JsonTypeName("OneOf")
    public static sealed class OneOfProperty extends Property<OneOfProperty>
            permits DefinitionDSL.ModifiableProperty.ModifiableOneOfProperty {

        protected List<? extends Property<?>> types = List.of(
                new ArrayProperty(null),
                new BooleanProperty(null),
                new DateProperty(null),
                new DateTimeProperty(null),
                new IntegerProperty(null),
                new NullProperty(null),
                new NumberProperty(null),
                new ObjectProperty(null),
                new StringProperty(null));

        protected OneOfProperty(String name) {
            super(name, Type.ONE_OF);
        }

        @Schema(name = "types", description = "Possible types of properties that can be used.")
        public List<? extends Property<?>> getTypes() {
            return types;
        }
    }

    @Schema(name = "ValueProperty", description = "A base property for all value based properties.")
    public abstract static sealed class ValueProperty<V, P extends ValueProperty<V, P>> extends Property<P>
            permits Property.ArrayProperty,
                    Property.BooleanProperty,
                    Property.DateProperty,
                    Property.DateTimeProperty,
                    Property.IntegerProperty,
                    Property.NumberProperty,
                    Property.ObjectProperty,
                    Property.StringProperty {

        protected V defaultValue;
        protected V exampleValue;
        protected List<PropertyOption> options;
        protected PropertyOptionDataSource optionDataSource;

        private ValueProperty(Type type) {
            this(null, type);
        }

        protected ValueProperty(String name, Type type) {
            super(name, type);
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

        public PropertyOptionDataSource getOptionsDataSource() {
            return optionDataSource;
        }
    }

    @JsonTypeName("ARRAY")
    @Schema(name = "ArrayProperty", description = "An array property type.")
    public static sealed class ArrayProperty extends ValueProperty<Object[], ArrayProperty>
            permits DefinitionDSL.ModifiableProperty.ModifiableArrayProperty {

        protected List<Property<?>> items;

        protected ArrayProperty(String name) {
            super(name, Type.ARRAY);
        }

        @Schema(name = "items", description = "Types of the array items.")
        public List<Property<?>> getItems() {
            return items;
        }
    }

    @JsonTypeName("BOOLEAN")
    @Schema(name = "BooleanProperty", description = "A boolean property type.")
    public static sealed class BooleanProperty extends ValueProperty<Boolean, BooleanProperty>
            permits DefinitionDSL.ModifiableProperty.ModifiableBooleanProperty {

        protected BooleanProperty(String name) {
            super(name, Type.BOOLEAN);
        }
    }

    @JsonTypeName("DATE")
    @Schema(name = "DateProperty", description = "A date property type.")
    public static sealed class DateProperty extends ValueProperty<LocalDate, DateProperty>
            permits DefinitionDSL.ModifiableProperty.ModifiableDateProperty {

        protected DateProperty(String name) {
            super(name, Type.DATE);
        }
    }

    @JsonTypeName("DATE_TIME")
    @Schema(name = "DateTimeProperty", description = "A date-time property type.")
    public static sealed class DateTimeProperty extends ValueProperty<LocalDateTime, DateTimeProperty>
            permits DefinitionDSL.ModifiableProperty.ModifiableDateTimeProperty {

        protected DateTimeProperty(String name) {
            super(name, Type.DATE_TIME);
        }
    }

    @JsonTypeName("NULL")
    @Schema(name = "NullProperty", description = "A null property type.")
    public static final class NullProperty extends Property<NullProperty> {

        protected NullProperty(String name) {
            super(name, Type.NULL);
        }
    }

    @JsonTypeName("NUMBER")
    @Schema(name = "NumberProperty", description = "A number property type.")
    public static sealed class NumberProperty extends ValueProperty<Double, NumberProperty>
            permits DefinitionDSL.ModifiableProperty.ModifiableNumberProperty {

        protected Integer maxValue;
        protected Integer minValue;
        protected Integer numberPrecision;

        protected NumberProperty(String name) {
            super(name, Type.NUMBER);
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
    public static sealed class IntegerProperty extends ValueProperty<Integer, IntegerProperty>
            permits DefinitionDSL.ModifiableProperty.ModifiableIntegerProperty {

        protected Integer maxValue;
        protected Integer minValue;

        protected IntegerProperty(String name) {
            super(name, Type.INTEGER);
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
    public static sealed class ObjectProperty extends ValueProperty<Object, ObjectProperty>
            permits DefinitionDSL.ModifiableProperty.ModifiableObjectProperty {

        protected List<Property<?>> additionalProperties;
        protected String objectType;
        protected List<? extends Property<?>> properties;

        protected ObjectProperty(String name) {
            super(name, Type.OBJECT);
        }

        @Schema(name = "additionalProperties", description = "Types of dynamically defined properties.")
        public List<Property<?>> getAdditionalProperties() {
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
    public static sealed class StringProperty extends ValueProperty<String, StringProperty>
            permits DefinitionDSL.ModifiableProperty.ModifiableStringProperty {

        protected ControlType controlType;

        protected StringProperty(String name) {
            super(name, Type.STRING);
        }

        public ControlType getControlType() {
            return controlType;
        }
    }
}
