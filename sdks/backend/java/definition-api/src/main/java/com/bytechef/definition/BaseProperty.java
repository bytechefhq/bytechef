/*
 * Copyright 2025 ByteChef
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
 * Represents the base contract shared by all workflow property definitions, exposing the common metadata (name,
 * description, visibility, and requirement flags) that applies regardless of the property's concrete value type.
 *
 * @author Ivica Cardic
 */
public interface BaseProperty {

    /**
     * Returns whether this property is shown only when advanced options are revealed.
     *
     * @return an {@link Optional} containing {@code true} if the property is an advanced option, or an empty
     *         {@link Optional} if unspecified
     */
    Optional<Boolean> getAdvancedOption();

    /**
     * Returns the optional description explaining the purpose of this property.
     *
     * @return an {@link Optional} containing the description, or an empty {@link Optional} if none is set
     */
    Optional<String> getDescription();

    /**
     * Returns the optional display condition expression that controls when this property is visible.
     *
     * @return an {@link Optional} containing the display condition expression, or an empty {@link Optional} if none is
     *         set
     */
    Optional<String> getDisplayCondition();

    /**
     * Returns whether expressions may be used to compute this property's value.
     *
     * @return an {@link Optional} containing {@code true} if expressions are enabled, or an empty {@link Optional} if
     *         unspecified
     */
    Optional<Boolean> getExpressionEnabled();

    /**
     * Returns whether this property is hidden from the user interface.
     *
     * @return an {@link Optional} containing {@code true} if the property is hidden, or an empty {@link Optional} if
     *         unspecified
     */
    Optional<Boolean> getHidden();

    /**
     * Returns the additional metadata associated with this property.
     *
     * @return a map of metadata keys to their values
     */
    Map<String, Object> getMetadata();

    /**
     * Returns the unique name identifying this property.
     *
     * @return the property name
     */
    String getName();

    /**
     * Returns whether a value for this property is required.
     *
     * @return an {@link Optional} containing {@code true} if the property is required, or an empty {@link Optional} if
     *         unspecified
     */
    default Boolean getRequired() {
        return Boolean.FALSE;
    }

    /**
     * Represents a property whose value is an ordered list of items, each described by a nested property definition.
     *
     * @param <I> the type of the nested item property definitions
     */
    interface BaseArrayProperty<I extends BaseProperty> extends BaseValueProperty<List<?>> {

        /**
         * Returns the property definitions describing the items allowed in the array.
         *
         * @return an {@link Optional} containing the list of item property definitions, or an empty {@link Optional} if
         *         none are defined
         */
        Optional<List<? extends I>> getItems();

        /**
         * Returns the maximum number of items permitted in the array.
         *
         * @return an {@link Optional} containing the maximum item count, or an empty {@link Optional} if unbounded
         */
        Optional<Long> getMaxItems();

        /**
         * Returns the minimum number of items permitted in the array.
         *
         * @return an {@link Optional} containing the minimum item count, or an empty {@link Optional} if unbounded
         */
        Optional<Long> getMinItems();

        /**
         * Returns whether the array accepts multiple values.
         *
         * @return an {@link Optional} containing {@code true} if multiple values are allowed, or an empty
         *         {@link Optional} if unspecified
         */
        Optional<Boolean> getMultipleValues();
    }

    /**
     * Represents a property whose value is a boolean.
     */
    interface BaseBooleanProperty extends BaseValueProperty<Boolean> {
    }

    /**
     * Represents a property whose value is a {@link LocalDate}.
     */
    interface BaseDateProperty extends BaseValueProperty<LocalDate> {
    }

    /**
     * Represents a property whose value is a {@link LocalDateTime}.
     */
    interface BaseDateTimeProperty extends BaseValueProperty<LocalDateTime> {
    }

    /**
     * Represents a property whose value is a file entry, described by a fixed set of nested property definitions.
     *
     * @param <P> the type of the nested property definitions describing the file entry
     */
    interface BaseFileEntryProperty<P extends BaseValueProperty<?>> extends BaseValueProperty<Map<String, ?>> {

        /**
         * Returns the property definitions describing the fields of the file entry.
         *
         * @return the list of nested property definitions
         */
        List<? extends P> getProperties();
    }

    /**
     * Represents a property whose value is an integer.
     */
    interface BaseIntegerProperty extends BaseValueProperty<Long> {

        /**
         * Returns the maximum value permitted for this property.
         *
         * @return an {@link Optional} containing the maximum value, or an empty {@link Optional} if unbounded
         */
        Optional<Long> getMaxValue();

        /**
         * Returns the minimum value permitted for this property.
         *
         * @return an {@link Optional} containing the minimum value, or an empty {@link Optional} if unbounded
         */
        Optional<Long> getMinValue();
    }

    /**
     * Represents a property whose value is always {@code null}.
     */
    interface BaseNullProperty extends BaseValueProperty<Void> {
    }

    /**
     * Represents a property whose value is a floating-point number.
     */
    interface BaseNumberProperty extends BaseValueProperty<Double> {

        /**
         * Returns the maximum number of decimal places permitted for this property's value.
         *
         * @return an {@link Optional} containing the maximum precision, or an empty {@link Optional} if unspecified
         */
        Optional<Integer> getMaxNumberPrecision();

        /**
         * Returns the maximum value permitted for this property.
         *
         * @return an {@link Optional} containing the maximum value, or an empty {@link Optional} if unbounded
         */
        Optional<Double> getMaxValue();

        /**
         * Returns the minimum value permitted for this property.
         *
         * @return an {@link Optional} containing the minimum value, or an empty {@link Optional} if unbounded
         */
        Optional<Double> getMinValue();

        /**
         * Returns the minimum number of decimal places permitted for this property's value.
         *
         * @return an {@link Optional} containing the minimum precision, or an empty {@link Optional} if unspecified
         */
        Optional<Integer> getMinNumberPrecision();

        /**
         * Returns the number of decimal places used when displaying this property's value.
         *
         * @return an {@link Optional} containing the number precision, or an empty {@link Optional} if unspecified
         */
        Optional<Integer> getNumberPrecision();
    }

    /**
     * Represents a property whose value is an object with named nested properties.
     *
     * @param <P> the type of the nested property definitions
     */
    interface BaseObjectProperty<P extends BaseProperty> extends BaseValueProperty<Map<String, ?>> {

        /**
         * Returns the property definitions describing dynamically added properties not covered by the fixed schema.
         *
         * @return an {@link Optional} containing the list of additional property definitions, or an empty
         *         {@link Optional} if none are defined
         */
        Optional<List<? extends P>> getAdditionalProperties();

        /**
         * Returns whether the object accepts multiple values.
         *
         * @return an {@link Optional} containing {@code true} if multiple values are allowed, or an empty
         *         {@link Optional} if unspecified
         */
        Optional<Boolean> getMultipleValues();

        /**
         * Returns the fixed property definitions describing the fields of the object.
         *
         * @return an {@link Optional} containing the list of nested property definitions, or an empty {@link Optional}
         *         if none are defined
         */
        Optional<List<? extends P>> getProperties();
    }

    /**
     * Represents a property whose value is a string.
     */
    interface BaseStringProperty extends BaseValueProperty<String> {

        /**
         * Returns the maximum length permitted for the string value.
         *
         * @return an {@link Optional} containing the maximum length, or an empty {@link Optional} if unbounded
         */
        Optional<Integer> getMaxLength();

        /**
         * Returns the minimum length permitted for the string value.
         *
         * @return an {@link Optional} containing the minimum length, or an empty {@link Optional} if unbounded
         */
        Optional<Integer> getMinLength();

        /**
         * Returns the regular expression that the string value must match.
         *
         * @return an {@link Optional} containing the validation regex, or an empty {@link Optional} if none is set
         */
        Optional<String> getRegex();

        /**
         * Returns whether the selectable options for this property are loaded dynamically at runtime.
         *
         * @return an {@link Optional} containing {@code true} if options are loaded dynamically, or an empty
         *         {@link Optional} if unspecified
         */
        Optional<Boolean> getOptionsLoadedDynamically();
    }

    /**
     * Represents a property whose value is a {@link LocalTime}.
     */
    interface BaseTimeProperty extends BaseValueProperty<LocalTime> {
    }

    /**
     * Represents a property that carries an actual value, adding value-related metadata such as the control type,
     * default and example values, label, and placeholder on top of the common {@link BaseProperty} contract.
     *
     * @param <V> the type of the property's value
     */
    interface BaseValueProperty<V> extends BaseProperty {

        /**
         * Returns the control type used to render and edit this property's value.
         *
         * @return the control type
         */
        BaseControlType getControlType();

        /**
         * Returns the default value applied when no value is supplied.
         *
         * @return an {@link Optional} containing the default value, or an empty {@link Optional} if none is set
         */
        Optional<V> getDefaultValue();

        /**
         * Returns an example value illustrating the expected input for this property.
         *
         * @return an {@link Optional} containing the example value, or an empty {@link Optional} if none is set
         */
        Optional<V> getExampleValue();

        /**
         * Returns the human-readable label displayed for this property.
         *
         * @return an {@link Optional} containing the label, or an empty {@link Optional} if none is set
         */
        Optional<String> getLabel();

        /**
         * Returns the placeholder text shown in the input control when no value is entered.
         *
         * @return an {@link Optional} containing the placeholder text, or an empty {@link Optional} if none is set
         */
        Optional<String> getPlaceholder();
    }
}
