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

package com.bytechef.platform.workflow.task.dispatcher.definition;

import com.bytechef.definition.BaseControlType;
import com.bytechef.definition.BaseProperty;
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
public interface Property extends BaseProperty {

    /**
     *
     */
    enum ControlType implements BaseControlType {
        ARRAY_BUILDER,
        DATE,
        DATE_TIME,
        EMAIL,
        FILE_ENTRY,
        FORMULA_MODE,
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
        FILE_ENTRY,
        INTEGER,
        NULL,
        NUMBER,
        OBJECT,
        STRING,
        TASK,
        TIME,
    }

    /**
     *
     */
    Type getType();

    /**
     *
     */
    interface ArrayProperty extends BaseArrayProperty<Property>, OptionsProperty<Object>, ValueProperty<List<?>> {

        /**
         *
         * @return
         */
        Optional<List<? extends Property>> getItems();
    }

    /**
     *
     */
    interface BooleanProperty extends BaseBooleanProperty, OptionsProperty<Boolean>, ValueProperty<Boolean> {
    }

    /**
     *
     */
    interface DateProperty extends BaseDateProperty, OptionsProperty<LocalDate>, ValueProperty<LocalDate> {
    }

    /**
     *
     */
    interface DateTimeProperty
        extends BaseDateTimeProperty, OptionsProperty<LocalDateTime>, ValueProperty<LocalDateTime> {
    }

    /**
     *
     */
    interface FileEntryProperty extends BaseFileEntryProperty<ValueProperty<?>>, ValueProperty<Map<String, ?>> {

        /**
         *
         * @return
         */
        List<? extends ValueProperty<?>> getProperties();
    }

    /**
     *
     */
    interface IntegerProperty extends BaseIntegerProperty, OptionsProperty<Long>, ValueProperty<Long> {
    }

    /**
     *
     */
    interface NullProperty extends BaseNullProperty, ValueProperty<Void> {
    }

    /**
     *
     */
    interface NumberProperty extends BaseNumberProperty, OptionsProperty<Double>, ValueProperty<Double> {
    }

    /**
     *
     */
    interface ObjectProperty
        extends BaseObjectProperty<Property>, OptionsProperty<Object>, ValueProperty<Map<String, ?>> {

        /**
         *
         * @return
         */
        Optional<List<? extends Property>> getAdditionalProperties();

        /**
         *
         * @return
         */
        Optional<List<? extends Property>> getProperties();
    }

    /**
     *
     */
    interface StringProperty extends BaseStringProperty, OptionsProperty<String>, ValueProperty<String> {
    }

    /**
     *
     */
    interface TaskProperty extends Property {
    }

    /**
     *
     */
    interface TimeProperty extends BaseTimeProperty, OptionsProperty<LocalTime>, ValueProperty<LocalTime> {
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
