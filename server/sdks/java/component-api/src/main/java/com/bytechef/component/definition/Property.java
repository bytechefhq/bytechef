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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
public interface Property {

    /**
     *
     */
    enum ControlType {
        ARRAY_BUILDER,
        CHECKBOX,
        CODE_EDITOR,
        DATE,
        DATE_TIME,
        EMAIL,
        INTEGER,
        MULTI_SELECT,
        NUMBER,
        OBJECT_BUILDER,
        PASSWORD,
        PHONE,
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
    Map<String, Object> getMetadata();

    /**
     *
     */
    String getName();

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
    interface ArrayProperty
        extends DynamicOptionsProperty, InputProperty, OutputProperty<List<Object>>, Property,
        ValueProperty<List<Object>> {

        /**
         *
         * @return
         */
        Optional<List<? extends Property.ValueProperty<?>>> getItems();

        /**
         *
         * @return
         */
        Optional<Long> getMaxItems();

        /**
         *
         * @return
         */
        Optional<Long> getMinItems();

        /**
         *
         * @return
         */
        Optional<Boolean> getMultipleValues();
    }

    /**
     *
     */
    interface BooleanProperty
        extends InputProperty, OptionsProperty, OutputProperty<Boolean>, Property, ValueProperty<Boolean> {
    }

    /**
     *
     */
    interface DateProperty
        extends DynamicOptionsProperty, InputProperty, OutputProperty<LocalDate>, Property, ValueProperty<LocalDate> {
    }

    /**
     *
     */
    interface DateTimeProperty
        extends DynamicOptionsProperty, InputProperty, OutputProperty<LocalDateTime>, Property,
        ValueProperty<LocalDateTime> {
    }

    /**
     *
     */
    interface DynamicPropertiesProperty extends InputProperty, Property {

        /**
         *
         */
        Optional<String> getHeader();

        /**
         *
         */
        PropertiesDataSource getDynamicPropertiesDataSource();
    }

    /**
     *
     */
    interface InputProperty extends Property {
    }

    /**
     *
     */
    interface IntegerProperty
        extends DynamicOptionsProperty, InputProperty, OutputProperty<Long>, Property, ValueProperty<Long> {

        /**
         *
         */
        Optional<Long> getMaxValue();

        /**
         *
         */
        Optional<Long> getMinValue();
    }

    /**
     *
     */
    interface NullProperty extends InputProperty, OutputProperty<Void>, ValueProperty<Void> {
    }

    /**
     *
     */
    interface NumberProperty
        extends DynamicOptionsProperty, InputProperty, OutputProperty<Double>, Property, ValueProperty<Double> {

        /**
         *
         */
        Optional<Integer> getMaxNumberPrecision();

        /**
         *
         */
        Optional<Double> getMaxValue();

        /**
         *
         */
        Optional<Double> getMinValue();

        /**
         *
         */
        Optional<Integer> getMinNumberPrecision();

        /**
         *
         */
        Optional<Integer> getNumberPrecision();
    }

    /**
     *
     */
    interface ObjectProperty
        extends DynamicOptionsProperty, InputProperty, OutputProperty<Map<String, Object>>, Property,
        ValueProperty<Map<String, Object>> {

        /**
         *
         */
        Optional<List<? extends Property.ValueProperty<?>>> getAdditionalProperties();

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
        Optional<List<? extends Property.ValueProperty<?>>> getProperties();
    }

    /**
     *
     * @param <V>
     */
    interface OutputProperty<V> extends Property.ValueProperty<V> {
    }

    /**
     *
     */
    interface StringProperty
        extends DynamicOptionsProperty, InputProperty, OutputProperty<String>, Property, ValueProperty<String> {

        /**
         *
         * @return
         */
        Optional<Integer> getMaxLength();

        /**
         *
         * @return
         */
        Optional<Integer> getMinLength();
    }

    /**
     *
     */
    interface TimeProperty
        extends DynamicOptionsProperty, InputProperty, OutputProperty<LocalTime>, Property, ValueProperty<LocalTime> {
    }

    /**
     *
     * @param <V>
     */
    interface ValueProperty<V> extends Property {

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

        /**
         *
         */
        Optional<String> getLabel();

        /**
         *
         */
        Optional<String> getPlaceholder();
    }
}
