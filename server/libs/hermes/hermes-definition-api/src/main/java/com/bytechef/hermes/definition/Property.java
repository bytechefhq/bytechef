
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
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableAnyProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableArrayProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableBooleanProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableDateProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableDateTimeProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableDynamicPropertiesProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableInputProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableIntegerProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableNullProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableNumberProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableObjectProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableOutputProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableStringProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableTimeProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableValueProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public sealed interface Property
    permits ModifiableProperty, Property.DynamicPropertiesProperty, Property.InputProperty, Property.ValueProperty {

    /**
     *
     */
    enum ControlType {
        CHECKBOX,
        CODE_EDITOR,
        DATE,
        DATE_TIME,
        EMAIL,
        EXPRESSION,
        INTEGER,
        MULTI_SELECT,
        NUMBER,
        OBJECT_BUILDER,
        PASSWORD,
        PHONE,
        SCHEMA_DESIGNER,
        SELECT,
        TEXT,
        TEXT_AREA,
        TIME,
        URL
    }

    /**
     *
     */
    enum Type {
        ANY,
        ARRAY,
        BOOLEAN,
        DATE,
        DATE_TIME,
        DYNAMIC_PROPERTIES,
        INTEGER,
        NULL,
        NUMBER,
        OBJECT,
        STRING,
        TIME,
    }

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

    /**
     *
     */
    sealed interface DynamicPropertiesProperty extends InputProperty, Property
        permits ModifiableDynamicPropertiesProperty {

        /**
         *
         */
        PropertiesDataSource getDynamicPropertiesDataSource();
    }

    /**
     *
     */
    sealed interface ValueProperty<V> extends Property
        permits AnyProperty, ArrayProperty, BooleanProperty, DateProperty, DateTimeProperty, IntegerProperty,
        NullProperty, NumberProperty, ObjectProperty, OutputProperty, StringProperty, TimeProperty,
        ModifiableValueProperty {

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
    sealed interface AnyProperty extends OutputProperty<Object>, ValueProperty<Object>
        permits ModifiableAnyProperty {
    }

    /**
     *
     */
    sealed interface ArrayProperty
        extends DynamicOptionsProperty, InputProperty, OutputProperty<Object[]>, ValueProperty<Object[]>
        permits ModifiableArrayProperty {

        /**
         *
         */
        Optional<List<? extends Property>> getItems();

        /**
         *
         */
        Optional<Boolean> getMultipleValues();
    }

    /**
     *
     */
    sealed interface BooleanProperty
        extends InputProperty, OutputProperty<Boolean>, OptionsProperty, ValueProperty<Boolean>
        permits ModifiableBooleanProperty {
    }

    /**
     *
     */
    sealed interface DateProperty
        extends DynamicOptionsProperty, InputProperty, OutputProperty<LocalDate>, ValueProperty<LocalDate>
        permits ModifiableDateProperty {
    }

    /**
     *
     */
    sealed interface DateTimeProperty extends
        DynamicOptionsProperty, InputProperty, OutputProperty<LocalDateTime>, ValueProperty<LocalDateTime>
        permits ModifiableDateTimeProperty {
    }

    sealed interface InputProperty extends Property
        permits ArrayProperty, BooleanProperty, DateProperty, DateTimeProperty, DynamicPropertiesProperty,
        IntegerProperty, ModifiableInputProperty, NullProperty, NumberProperty, ObjectProperty, StringProperty,
        TimeProperty {
    }

    /**
     *
     */
    sealed interface IntegerProperty extends
        DynamicOptionsProperty, InputProperty, OutputProperty<Integer>, ValueProperty<Integer>
        permits ModifiableIntegerProperty {

        /**
         *
         */
        Optional<Integer> getMaxValue();

        /**
         *
         */
        Optional<Integer> getMinValue();
    }

    /**
     *
     */
    sealed interface NullProperty extends InputProperty, OutputProperty<Void>, ValueProperty<Void>
        permits ModifiableNullProperty {
    }

    /**
     *
     */
    sealed interface NumberProperty
        extends DynamicOptionsProperty, InputProperty, OutputProperty<Double>, ValueProperty<Double>
        permits ModifiableNumberProperty {

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
    }

    /**
     *
     */
    sealed interface ObjectProperty
        extends DynamicOptionsProperty, InputProperty, OutputProperty<Object>, ValueProperty<Object>
        permits ModifiableObjectProperty {

        /**
         *
         */
        Optional<List<? extends Property>> getAdditionalProperties();

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
        Optional<List<? extends Property>> getProperties();
    }

    sealed interface OutputProperty<V> extends ValueProperty<V>
        permits AnyProperty, ArrayProperty, BooleanProperty, DateProperty, DateTimeProperty,
        IntegerProperty, NullProperty, ModifiableOutputProperty, NumberProperty, ObjectProperty, StringProperty,
        TimeProperty {
    }

    /**
     *
     */
    sealed interface StringProperty
        extends DynamicOptionsProperty, InputProperty, OutputProperty<String>, ValueProperty<String>
        permits ModifiableStringProperty {

        /**
         *
         */
        enum SampleDataType {
            CSV, JSON, XML
        }

        /**
         * TODO
         */
        Optional<SampleDataType> getSampleDataType();
    }

    /**
     *
     */
    sealed interface TimeProperty extends
        DynamicOptionsProperty, InputProperty, OutputProperty<LocalTime>, ValueProperty<LocalTime>
        permits ModifiableTimeProperty {
    }
}
// CHECKSTYLE:ON
