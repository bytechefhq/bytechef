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

package com.bytechef.platform.workflow.task.dispatcher.definition;

import com.bytechef.hermes.definition.BaseProperty;
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
    interface ArrayProperty
        extends BaseProperty.ArrayProperty, InputProperty, OutputProperty<List<Object>>, Property,
        ValueProperty<List<Object>> {

        /**
         *
         * @return
         */
        Optional<List<? extends Property.ValueProperty<?>>> getItems();
    }

    /**
     *
     */
    interface BooleanProperty
        extends BaseProperty.BooleanProperty, InputProperty, OutputProperty<Boolean>, Property, ValueProperty<Boolean> {
    }

    /**
     *
     */
    interface DateProperty
        extends BaseProperty.DateProperty, InputProperty, OutputProperty<LocalDate>, Property,
        ValueProperty<LocalDate> {
    }

    /**
     *
     */
    interface DateTimeProperty
        extends BaseProperty.DateTimeProperty, InputProperty, OutputProperty<LocalDateTime>, Property,
        ValueProperty<LocalDateTime> {
    }

    /**
     *
     */
    interface InputProperty extends BaseProperty.InputProperty, Property {
    }

    /**
     *
     */
    interface IntegerProperty
        extends BaseProperty.IntegerProperty, InputProperty, OutputProperty<Long>, Property, ValueProperty<Long> {
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
        extends BaseProperty.NumberProperty, InputProperty, OutputProperty<Double>, Property, ValueProperty<Double> {
    }

    /**
     *
     */
    interface ObjectProperty
        extends BaseProperty.ObjectProperty, InputProperty, OutputProperty<Map<String, Object>>, Property,
        ValueProperty<Map<String, Object>> {

        /**
         *
         * @return
         */
        Optional<List<? extends Property.ValueProperty<?>>> getAdditionalProperties();

        /**
         *
         * @return
         */
        Optional<List<? extends Property.ValueProperty<?>>> getProperties();
    }

    /**
     *
     * @param <V>
     */
    interface OutputProperty<V> extends BaseProperty.OutputProperty<V>, ValueProperty<V> {
    }

    /**
     *
     */
    interface StringProperty
        extends BaseProperty.StringProperty, InputProperty, OutputProperty<String>, Property, ValueProperty<String> {
    }

    /**
     *
     */
    interface TimeProperty
        extends BaseProperty.TimeProperty, InputProperty, OutputProperty<LocalTime>, Property,
        ValueProperty<LocalTime> {
    }

    interface ValueProperty<V> extends BaseProperty.ValueProperty<V>, Property {
    }
}
