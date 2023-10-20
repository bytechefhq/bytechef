
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
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
        DYNAMIC_PROPERTIES,
        INTEGER,
        NULL,
        NUMBER,
        OBJECT,
        ONE_OF,
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
    sealed interface DynamicPropertiesProperty
        extends Property<DynamicPropertiesProperty> permits ModifiableDynamicPropertiesProperty {

        /**
         *
         */
        PropertiesDataSource getDynamicPropertiesDataSource();
    }

    /**
     *
     */
    sealed interface OneOfProperty
        extends Property<OneOfProperty> permits ModifiableOneOfProperty {

        /**
         *
         */
        Optional<List<? extends Property<?>>> getTypes();
    }

    /**
     *
     */
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
    sealed interface ArrayProperty
        extends ValueProperty<Object[], ArrayProperty>, DynamicOptionsProperty permits ModifiableArrayProperty {

        /**
         *
         */
        Optional<List<Property<?>>> getItems();

        /**
         *
         */
        Optional<Boolean> getMultipleValues();
    }

    /**
     *
     */
    sealed interface BooleanProperty extends
        ValueProperty<Boolean, BooleanProperty>, OptionsProperty permits ModifiableBooleanProperty {
    }

    /**
     *
     */
    sealed interface DateProperty
        extends ValueProperty<LocalDate, DateProperty>, DynamicOptionsProperty permits ModifiableDateProperty {
    }

    /**
     *
     */
    sealed interface DateTimeProperty extends
        ValueProperty<LocalDateTime, DateTimeProperty>, DynamicOptionsProperty permits ModifiableDateTimeProperty {
    }

    /**
     *
     */
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
    }

    /**
     *
     */
    sealed interface NullProperty extends Property<NullProperty> permits ModifiableNullProperty {
    }

    /**
     *
     */
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
    }

    /**
     *
     */
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
        Optional<List<? extends Property<?>>> getProperties();
    }

    /**
     *
     */
    sealed interface StringProperty
        extends ValueProperty<String, StringProperty>, DynamicOptionsProperty permits ModifiableStringProperty {

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
        ValueProperty<LocalTime, TimeProperty>, DynamicOptionsProperty permits ModifiableTimeProperty {
    }
}
// CHECKSTYLE:ON
