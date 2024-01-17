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

package com.bytechef.definition;

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
    interface BaseArrayProperty<I extends BaseProperty> extends BaseValueProperty<List<?>> {

        /**
         *
         * @return
         */
        Optional<List<? extends I>> getItems();

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
    interface BaseBooleanProperty extends BaseValueProperty<Boolean> {
    }

    /**
     *
     */
    interface BaseDateProperty extends BaseValueProperty<LocalDate> {
    }

    /**
     *
     */
    interface BaseDateTimeProperty extends BaseValueProperty<LocalDateTime> {
    }

    /**
     *
     */
    interface BaseFileEntryProperty<P extends BaseValueProperty<?>> extends BaseValueProperty<Map<String, ?>> {

        /**
         *
         * @return
         */
        List<? extends P> getProperties();
    }

    /**
     *
     */
    interface BaseIntegerProperty extends BaseValueProperty<Long> {

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
    interface BaseNullProperty extends BaseValueProperty<Void> {
    }

    /**
     *
     */
    interface BaseNumberProperty extends BaseValueProperty<Double> {

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
    interface BaseObjectProperty<P extends BaseProperty> extends BaseValueProperty<Map<String, ?>> {

        /**
         *
         */
        Optional<List<? extends P>> getAdditionalProperties();

        /**
         *
         */
        Optional<Boolean> getMultipleValues();

        /**
         *
         */
        Optional<List<? extends P>> getProperties();
    }

    /**
     *
     */
    interface BaseStringProperty extends BaseValueProperty<String> {

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
    interface BaseTimeProperty extends BaseValueProperty<LocalTime> {
    }

    /**
     *
     * @param <V>
     */
    interface BaseValueProperty<V> extends BaseProperty {

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
