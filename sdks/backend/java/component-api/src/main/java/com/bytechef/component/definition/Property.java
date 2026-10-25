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

package com.bytechef.component.definition;

import com.bytechef.definition.BaseControlType;
import com.bytechef.definition.BaseProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a configurable property of a component, action, or trigger. Each property has a data {@link Type} and, for
 * value-bearing properties, a {@link ControlType} that determines how it is rendered and edited in the workflow editor.
 * Specialized sub-interfaces model the individual data types (string, number, array, object, and so on).
 *
 * @author Ivica Cardic
 */
public interface Property extends BaseProperty {

    /**
     * Enumerates the UI control types used to render and edit a property's value in the workflow editor.
     */
    enum ControlType implements BaseControlType {
        ARRAY_BUILDER,
        CODE_EDITOR,
        DATE,
        DATE_TIME,
        EMAIL,
        FILE_ENTRY,
        FORMULA_MODE,
        INTEGER,
        JSON_SCHEMA_BUILDER,
        MULTI_SELECT,
        NUMBER,
        NULL,
        OBJECT_BUILDER,
        PASSWORD,
        PHONE,
        RICH_TEXT,
        SELECT,
        TEXT,
        TEXT_AREA,
        TIME,
        URL
    }

    /**
     * Enumerates the data types a property may hold, determining how its value is interpreted and validated.
     */
    enum Type {
        ARRAY,
        BOOLEAN,
        DATE,
        DATE_TIME,
        DYNAMIC_PROPERTIES,
        FILE_ENTRY,
        INTEGER,
        NULL,
        NUMBER,
        OBJECT,
        STRING,
        TIME,
    }

    /**
     * Returns the data type of this property.
     *
     * @return the property {@link Type}
     */
    Type getType();

    /**
     * Represents a property holding a list of values, where each element conforms to one of the declared item
     * properties.
     */
    interface ArrayProperty
        extends BaseArrayProperty<ValueProperty<?>>, DynamicOptionsProperty<Object>, ValueProperty<List<?>> {

        /**
         * Returns the property definitions describing the allowed types of the array's elements.
         *
         * @return the list of item properties, or an empty list if the element type is unconstrained
         */
        default List<? extends Property.ValueProperty<?>> getItems() {
            return List.of();
        }
    }

    /**
     * Represents a property holding a boolean value.
     */
    interface BooleanProperty extends BaseBooleanProperty, OptionsProperty<Boolean>, ValueProperty<Boolean> {
    }

    /**
     * Represents a property holding a {@link LocalDate} value.
     */
    interface DateProperty extends BaseDateProperty, DynamicOptionsProperty<LocalDate>, ValueProperty<LocalDate> {
    }

    /**
     * Represents a property holding a {@link LocalDateTime} value.
     */
    interface DateTimeProperty
        extends BaseDateTimeProperty, DynamicOptionsProperty<LocalDateTime>, ValueProperty<LocalDateTime> {
    }

    /**
     * Represents a property whose nested sub-properties are resolved dynamically at runtime from a
     * {@link PropertiesDataSource}.
     */
    interface DynamicPropertiesProperty extends Property {

        /**
         * Returns the header text displayed above the dynamically resolved properties, when set.
         *
         * @return an {@link Optional} containing the header text, or an empty {@link Optional} if none is set
         */
        default Optional<String> getHeader() {
            return Optional.empty();
        }

        /**
         * Returns the data source that resolves this property's nested properties at runtime.
         *
         * @return the dynamic properties data source
         */
        PropertiesDataSource<?> getDynamicPropertiesDataSource();
    }

    /**
     * Represents a property holding a {@link FileEntry}, modeled as a map of its constituent fields.
     */
    interface FileEntryProperty extends BaseFileEntryProperty<ValueProperty<?>>, ValueProperty<Map<String, ?>> {

        /**
         * Returns the property definitions describing the fields of the file entry.
         *
         * @return the list of properties that make up the file entry
         */
        List<? extends Property.ValueProperty<?>> getProperties();
    }

    /**
     * Represents a property holding an integer value.
     */
    interface IntegerProperty extends BaseIntegerProperty, DynamicOptionsProperty<Long>, ValueProperty<Long> {
    }

    /**
     * Represents a property whose value is always {@code null}.
     */
    interface NullProperty extends BaseNullProperty, ValueProperty<Void> {
    }

    /**
     * Represents a property holding a floating-point number value.
     */
    interface NumberProperty extends BaseNumberProperty, DynamicOptionsProperty<Double>, ValueProperty<Double> {
    }

    /**
     * Represents a property holding an object value, modeled as a map of named sub-properties.
     */
    interface ObjectProperty
        extends BaseObjectProperty<ValueProperty<?>>, DynamicOptionsProperty<Object>, ValueProperty<Map<String, ?>> {

        /**
         * Returns the property definitions describing values allowed for keys beyond the explicitly declared ones.
         *
         * @return the additional-property definitions, or an empty list if additional properties are not permitted
         */
        default List<? extends Property.ValueProperty<?>> getAdditionalProperties() {
            return List.of();
        }

        /**
         * Returns the property definitions describing the object's explicitly declared fields.
         *
         * @return the list of properties, or an empty list if none are declared
         */
        default List<? extends Property.ValueProperty<?>> getProperties() {
            return List.of();
        }
    }

    /**
     * Represents a property holding a string value.
     */
    interface StringProperty extends BaseStringProperty, DynamicOptionsProperty<String>, ValueProperty<String> {

        /**
         * Returns the identifier of the programming or markup language the string is edited in, used to enable syntax
         * highlighting in a code editor control.
         *
         * @return an {@link Optional} containing the language identifier, or an empty {@link Optional} if none is set
         */
        default Optional<String> getLanguageId() {
            return Optional.empty();
        }
    }

    /**
     * Represents a property holding a {@link LocalTime} value.
     */
    interface TimeProperty extends BaseTimeProperty, DynamicOptionsProperty<LocalTime>, ValueProperty<LocalTime> {
    }

    /**
     * Represents a property that carries an actual value of type {@code V}, as opposed to a structural or grouping
     * property.
     *
     * @param <V> the type of the value held by the property
     */
    interface ValueProperty<V> extends BaseValueProperty<V>, Property {

        /**
         * Returns the UI control used to render and edit this property's value.
         *
         * @return the property's {@link ControlType}
         */
        ControlType getControlType();
    }
}
