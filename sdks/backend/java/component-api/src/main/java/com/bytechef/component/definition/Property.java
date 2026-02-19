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
 * @author Ivica Cardic
 */
public interface Property extends BaseProperty {

    /**
     *
     */
    enum ControlType implements BaseControlType {
        ARRAY_BUILDER,
        CODE_EDITOR,
        DATE,
        DATE_TIME,
        EMAIL,
        FILE_ENTRY,
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
     *
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
     *
     */
    Type getType();

    /**
     *
     */
    interface ArrayProperty
        extends BaseArrayProperty<ValueProperty<?>>, DynamicOptionsProperty<Object>, ValueProperty<List<?>> {

        /**
         *
         * @return
         */
        Optional<List<? extends Property.ValueProperty<?>>> getItems();
    }

    /**
     *
     */
    interface BooleanProperty extends BaseBooleanProperty, OptionsProperty<Boolean>, ValueProperty<Boolean> {
    }

    /**
     *
     */
    interface DateProperty extends BaseDateProperty, DynamicOptionsProperty<LocalDate>, ValueProperty<LocalDate> {
    }

    /**
     *
     */
    interface DateTimeProperty
        extends BaseDateTimeProperty, DynamicOptionsProperty<LocalDateTime>, ValueProperty<LocalDateTime> {
    }

    /**
     *
     */
    interface DynamicPropertiesProperty extends Property {

        /**
         *
         */
        Optional<String> getHeader();

        /**
         *
         */
        PropertiesDataSource<?> getDynamicPropertiesDataSource();
    }

    /**
     *
     */
    interface FileEntryProperty extends BaseFileEntryProperty<ValueProperty<?>>, ValueProperty<Map<String, ?>> {

        /**
         *
         * @return
         */
        List<? extends Property.ValueProperty<?>> getProperties();
    }

    /**
     *
     */
    interface IntegerProperty extends BaseIntegerProperty, DynamicOptionsProperty<Long>, ValueProperty<Long> {
    }

    /**
     *
     */
    interface NullProperty extends BaseNullProperty, ValueProperty<Void> {
    }

    /**
     *
     */
    interface NumberProperty extends BaseNumberProperty, DynamicOptionsProperty<Double>, ValueProperty<Double> {
    }

    /**
     *
     */
    interface ObjectProperty
        extends BaseObjectProperty<ValueProperty<?>>, DynamicOptionsProperty<Object>, ValueProperty<Map<String, ?>> {

        /**
         *
         */
        Optional<List<? extends Property.ValueProperty<?>>> getAdditionalProperties();

        /**
         *
         */
        Optional<List<? extends Property.ValueProperty<?>>> getProperties();
    }

    /**
     *
     */
    interface StringProperty extends BaseStringProperty, DynamicOptionsProperty<String>, ValueProperty<String> {

        /**
         *
         * @return
         */
        Optional<String> getLanguageId();
    }

    /**
     *
     */
    interface TimeProperty extends BaseTimeProperty, DynamicOptionsProperty<LocalTime>, ValueProperty<LocalTime> {
    }

    /**
     *
     * @param <V>
     */
    interface ValueProperty<V> extends BaseValueProperty<V>, Property {

        /**
         *
         */
        ControlType getControlType();
    }
}
