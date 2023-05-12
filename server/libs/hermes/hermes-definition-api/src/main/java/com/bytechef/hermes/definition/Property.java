
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

import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableArrayProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableBooleanProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableDateProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableDateTimeProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableDynamicPropertiesProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableIntegerProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableNullProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableNumberProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableObjectProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableOneOfProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableStringProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableTimeProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableValueProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Property.ArrayProperty.class, name = "ARRAY"),
    @JsonSubTypes.Type(value = Property.BooleanProperty.class, name = "BOOLEAN"),
    @JsonSubTypes.Type(value = Property.DateTimeProperty.class, name = "DATE"),
    @JsonSubTypes.Type(value = Property.DateTimeProperty.class, name = "DATE_TIME"),
    @JsonSubTypes.Type(value = Property.DateTimeProperty.class, name = "DYNAMIC_PROPERTIES"),
    @JsonSubTypes.Type(value = Property.IntegerProperty.class, name = "INTEGER"),
    @JsonSubTypes.Type(value = Property.NumberProperty.class, name = "NUMBER"),
    @JsonSubTypes.Type(value = Property.NullProperty.class, name = "NULL"),
    @JsonSubTypes.Type(value = Property.ObjectProperty.class, name = "OBJECT"),
    @JsonSubTypes.Type(value = Property.OneOfProperty.class, name = "ONE_OF"),
    @JsonSubTypes.Type(value = Property.StringProperty.class, name = "STRING"),
    @JsonSubTypes.Type(value = Property.TimeProperty.class, name = "TIME"),
})
// CHECKSTYLE:OFF
public sealed interface Property<P extends Property<P>>
    permits ModifiableProperty, Property.DynamicPropertiesProperty, Property.NullProperty, Property.OneOfProperty,
    Property.ValueProperty {

    /**
     *
     */
    enum ControlType {
        CHECKBOX,
        CODE_EDITOR,
        EMAIL,
        EXPRESSION,
        DATE,
        DATE_TIME,
        OBJECT_BUILDER,
        INPUT_EMAIL,
        INPUT_INTEGER,
        INPUT_NUMBER,
        INPUT_PASSWORD,
        INPUT_PHONE,
        INPUT_TEXT,
        INPUT_URL,
        MULTI_SELECT,
        PHONE,
        SCHEMA_DESIGNER,
        SELECT,
        SUBDOMAIN,
        TEXT_AREA,
        TIME,
        URL
    }

    /**
     *
     */
    enum Type {
        ARRAY,
        BOOLEAN,
        DATE,
        DATE_TIME,
        INTEGER,
        NULL,
        NUMBER,
        OBJECT,
        ONE_OF,
        STRING,
        TIME,
    }

    Object accept(PropertyVisitor propertyVisitor);

    /**
     *
     */
    Optional<Boolean> getAdvancedOption();

    /**
     *
     */
    Optional<String> getDescription();

    /**
     *
     */
    Optional<String> getDisplayCondition();

    /**
     *
     */
    Optional<Boolean> getExpressionEnabled();

    /**
     *
     */
    Optional<Boolean> getHidden();

    /**
     *
     */
    Optional<String> getLabel();

    /**
     *
     */
    Map<String, Object> getMetadata();

    /**
     *
     */
    String getName();

    /**
     *
     */
    Optional<String> getPlaceholder();

    /**
     *
     */
    Optional<Boolean> getRequired();

    /**
     *
     */
    Type getType();

    interface PropertyVisitor {

        Object visit(ArrayProperty arrayProperty);

        Object visit(BooleanProperty booleanProperty);

        Object visit(DateProperty dateProperty);

        Object visit(DateTimeProperty dateTimeProperty);

        Object visit(DynamicPropertiesProperty dateProperty);

        Object visit(IntegerProperty integerProperty);

        Object visit(NullProperty nullProperty);

        Object visit(NumberProperty numberProperty);

        Object visit(OneOfProperty oneOfProperty);

        Object visit(ObjectProperty objectProperty);

        Object visit(StringProperty stringProperty);

        Object visit(TimeProperty timeProperty);
    }

    /**
     *
     */
    @JsonDeserialize(as = ModifiableDynamicPropertiesProperty.class)
    sealed interface DynamicPropertiesProperty
        extends Property<DynamicPropertiesProperty> permits ModifiableDynamicPropertiesProperty {

        /**
         *
         */
        DynamicPropertiesDataSource getDynamicPropertiesDataSource();
    }

    /**
     *
     */
    @JsonDeserialize(as = DefinitionDSL.ModifiableProperty.ModifiableOneOfProperty.class)
    sealed interface OneOfProperty
        extends Property<OneOfProperty> permits ModifiableOneOfProperty {

        /**
         *
         */
        List<? extends Property<?>> getTypes();
    }

    /**
     *
     */
    @JsonDeserialize(as = ModifiableValueProperty.class)
    sealed interface ValueProperty<V, P extends ValueProperty<V, P>> extends
        Property<P> permits ArrayProperty, BooleanProperty, DateProperty, DateTimeProperty, IntegerProperty,
        NumberProperty, ObjectProperty, StringProperty, TimeProperty, ModifiableValueProperty {

        /**
         *
         */
        ControlType getControlType();

        /**
         *
         */
        Optional<V> getDefaultValue();

        /**
         *
         */
        Optional<V> getExampleValue();
    }

    /**
     *
     */
    @JsonDeserialize(as = ModifiableArrayProperty.class)
    sealed interface ArrayProperty
        extends ValueProperty<Object[], ArrayProperty>, DynamicOptionsProperty permits ModifiableArrayProperty {

        /**
         *
         */
        List<Property<?>> getItems();

        /**
         *
         */
        Optional<Boolean> getMultipleValues();

        /**
         *
         */
        Optional<List<Option<?>>> getOptions();

        /**
         *
         */
        Optional<OptionsDataSource> getOptionsDataSource();
    }

    /**
     *
     */
    @JsonDeserialize(as = ModifiableBooleanProperty.class)
    sealed interface BooleanProperty extends
        ValueProperty<Boolean, BooleanProperty>, OptionsProperty permits ModifiableBooleanProperty {
    }

    /**
     *
     */
    @JsonDeserialize(as = ModifiableDateProperty.class)
    sealed interface DateProperty
        extends ValueProperty<LocalDate, DateProperty> permits ModifiableDateProperty {

        /**
         *
         */
        Optional<List<Option<?>>> getOptions();

        /**
         *
         */
        OptionsDataSource getOptionsDataSource();
    }

    /**
     *
     */
    @JsonDeserialize(as = ModifiableDateTimeProperty.class)
    sealed interface DateTimeProperty extends
        ValueProperty<LocalDateTime, DateTimeProperty> permits ModifiableDateTimeProperty {

        /**
         *
         */
        Optional<List<Option<?>>> getOptions();

        /**
         *
         */
        OptionsDataSource getOptionsDataSource();
    }

    /**
     *
     */
    @JsonDeserialize(as = ModifiableIntegerProperty.class)
    sealed interface IntegerProperty extends
        ValueProperty<Integer, IntegerProperty>, DynamicOptionsProperty permits ModifiableIntegerProperty {

        /**
         *
         */
        Optional<Integer> getMaxValue();

        /**
         *
         */
        Optional<Integer> getMinValue();

        /**
         *
         */
        Optional<List<Option<?>>> getOptions();

        /**
         *
         */
        Optional<OptionsDataSource> getOptionsDataSource();
    }

    /**
     *
     */
    @JsonDeserialize(as = ModifiableNullProperty.class)
    sealed interface NullProperty extends Property<NullProperty> permits ModifiableNullProperty {
    }

    /**
     *
     */
    @JsonDeserialize(as = ModifiableNumberProperty.class)
    sealed interface NumberProperty
        extends ValueProperty<Double, NumberProperty>, DynamicOptionsProperty permits ModifiableNumberProperty {

        /**
         *
         */
        Optional<Integer> getMaxValue();

        /**
         *
         */
        Optional<Integer> getMinValue();

        /**
         *
         */
        Optional<Integer> getNumberPrecision();

        /**
         *
         */
        Optional<List<Option<?>>> getOptions();

        /**
         *
         */
        Optional<OptionsDataSource> getOptionsDataSource();
    }

    /**
     *
     */
    @JsonDeserialize(as = ModifiableObjectProperty.class)
    sealed interface ObjectProperty
        extends ValueProperty<Object, ObjectProperty>, DynamicOptionsProperty permits ModifiableObjectProperty {

        /**
         *
         */
        Optional<List<? extends Property<?>>> getAdditionalProperties();

        /**
         *
         */
        Optional<Boolean> getMultipleValues();

        /**
         *
         */
        Optional<String> getObjectType();

        /**
         *
         */
        Optional<List<Option<?>>> getOptions();

        /**
         *
         */
        Optional<OptionsDataSource> getOptionsDataSource();

        /**
         *
         */
        Optional<List<? extends Property<?>>> getProperties();
    }

    /**
     *
     */
    @JsonDeserialize(as = ModifiableStringProperty.class)
    sealed interface StringProperty
        extends ValueProperty<String, StringProperty>, DynamicOptionsProperty permits ModifiableStringProperty {

        /**
         *
         */
        enum SampleDataType {
            CSV, JSON, XML
        }

        /**
         *
         */
        Optional<List<Option<?>>> getOptions();

        /**
         *
         */
        Optional<OptionsDataSource> getOptionsDataSource();

        /**
         * TODO
         */
        Optional<SampleDataType> getSampleDataType();
    }

    /**
     *
     */
    @JsonDeserialize(as = ModifiableTimeProperty.class)
    sealed interface TimeProperty extends
        ValueProperty<LocalTime, TimeProperty> permits ModifiableTimeProperty {

        /**
         *
         */
        int getHour();

        /**
         *
         */
        int getMinute();

        /**
         *
         */
        int getSecond();
    }
}
// CHECKSTYLE:ON
