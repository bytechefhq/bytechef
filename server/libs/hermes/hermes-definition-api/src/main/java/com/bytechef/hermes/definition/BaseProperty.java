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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface BaseProperty {

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
        extends InputProperty, OptionsProperty, OutputProperty<List<Object>>, ValueProperty<List<Object>> {

        /**
         *
         */
        Optional<Long> getMaxItems();

        /**
         *
         */
        Optional<Long> getMinItems();

        /**
         *
         */
        Optional<Boolean> getMultipleValues();
    }

    /**
     *
     */
    interface BooleanProperty extends InputProperty, OptionsProperty, OutputProperty<Boolean>, ValueProperty<Boolean> {
    }

    /**
     *
     */
    interface DateProperty extends InputProperty, OptionsProperty, OutputProperty<LocalDate>, ValueProperty<LocalDate> {
    }

    /**
     *
     */
    interface DateTimeProperty
        extends InputProperty, OptionsProperty, OutputProperty<LocalDateTime>, ValueProperty<LocalDateTime> {
    }

    /**
     *
     */
    interface InputProperty extends BaseProperty {
    }

    /**
     *
     */
    interface IntegerProperty extends InputProperty, OptionsProperty, OutputProperty<Long>, ValueProperty<Long> {

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
    interface NumberProperty extends InputProperty, OptionsProperty, OutputProperty<Double>, ValueProperty<Double> {

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
    interface ObjectProperty extends InputProperty, OptionsProperty, OutputProperty<Map<String, Object>>,
        ValueProperty<Map<String, Object>> {

        /**
         *
         */
        Optional<Boolean> getMultipleValues();

        /**
         *
         */
        Optional<String> getObjectType();
    }

    /**
     *
     * @param <V>
     */
    interface OutputProperty<V> extends ValueProperty<V> {
    }

    /**
     *
     */
    interface StringProperty extends InputProperty, OptionsProperty, OutputProperty<String>, ValueProperty<String> {

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
    interface TimeProperty extends InputProperty, OptionsProperty, OutputProperty<LocalTime>, ValueProperty<LocalTime> {
    }

    /**
     *
     */
    interface ValueProperty<V> extends BaseProperty {

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
// CHECKSTYLE:ON
