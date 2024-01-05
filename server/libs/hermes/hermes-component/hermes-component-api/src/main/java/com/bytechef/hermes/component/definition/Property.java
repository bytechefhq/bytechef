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

package com.bytechef.hermes.component.definition;

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
public interface Property extends com.bytechef.hermes.definition.Property {

    /**
     *
     */
    interface ArrayProperty
        extends com.bytechef.hermes.definition.Property.ArrayProperty, DynamicOptionsProperty, InputProperty,
        OutputProperty<List<Object>>, Property, ValueProperty<List<Object>> {
    }

    /**
     *
     */
    interface BooleanProperty
        extends com.bytechef.hermes.definition.Property.BooleanProperty, InputProperty, OutputProperty<Boolean>,
        Property, ValueProperty<Boolean> {
    }

    /**
     *
     */
    interface DateProperty
        extends com.bytechef.hermes.definition.Property.DateProperty, DynamicOptionsProperty, InputProperty,
        OutputProperty<LocalDate>, Property, ValueProperty<LocalDate> {
    }

    /**
     *
     */
    interface DateTimeProperty
        extends com.bytechef.hermes.definition.Property.DateTimeProperty, DynamicOptionsProperty, InputProperty,
        OutputProperty<LocalDateTime>, Property, ValueProperty<LocalDateTime> {
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
    interface InputProperty extends com.bytechef.hermes.definition.Property.InputProperty, Property {
    }

    /**
     *
     */
    interface IntegerProperty
        extends com.bytechef.hermes.definition.Property.IntegerProperty, DynamicOptionsProperty, InputProperty,
        OutputProperty<Long>, Property, ValueProperty<Long> {
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
        extends com.bytechef.hermes.definition.Property.NumberProperty, DynamicOptionsProperty, InputProperty,
        OutputProperty<Double>, Property, ValueProperty<Double> {
    }

    /**
     *
     */
    interface ObjectProperty
        extends com.bytechef.hermes.definition.Property.ObjectProperty, DynamicOptionsProperty, InputProperty,
        OutputProperty<Map<String, Object>>, Property, ValueProperty<Map<String, Object>> {
    }

    /**
     *
     * @param <V>
     */
    interface OutputProperty<V>
        extends com.bytechef.hermes.definition.Property.OutputProperty<V>, Property.ValueProperty<V> {
    }

    /**
     *
     */
    interface StringProperty
        extends com.bytechef.hermes.definition.Property.StringProperty, DynamicOptionsProperty, InputProperty,
        OutputProperty<String>, Property, ValueProperty<String> {
    }

    /**
     *
     */
    interface TimeProperty
        extends com.bytechef.hermes.definition.Property.TimeProperty, DynamicOptionsProperty, InputProperty,
        OutputProperty<LocalTime>, Property, ValueProperty<LocalTime> {
    }

    interface ValueProperty<V> extends com.bytechef.hermes.definition.Property.ValueProperty<V>, Property {
    }
}
