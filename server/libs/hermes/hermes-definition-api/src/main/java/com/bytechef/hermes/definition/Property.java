
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
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableIntegerProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableNullProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableNumberProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableObjectProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableOneOfProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableStringProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableValueProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
// CHECKSTYLE:OFF
public sealed interface Property<P extends Property<P>> permits Property.OneOfProperty,Property.NullProperty,Property.ValueProperty,ModifiableProperty {

    enum ControlType {
        CODE,
        PASSWORD
    }

    enum Type {
        ARRAY,
        BOOLEAN,
        DATE,
        DATE_TIME,
        INTEGER,
        NULL,
        NUMBER,
        OBJECT,
        ONE_OF,
        STRING
    }

    Boolean getAdvancedOption();

    String getDescription();

    DisplayOption getDisplayOption();

    Boolean getHidden();

    String getLabel();

    Map<String, Object> getMetadata();

    String getName();

    String getPlaceholder();

    Boolean getRequired();

    Type getType();

    sealed interface OneOfProperty
        extends Property<OneOfProperty>permits ModifiableOneOfProperty {

        List<? extends Property<?>> getTypes();
    }

    sealed interface ValueProperty<V, P extends ValueProperty<V, P>> extends
        Property<P>permits ArrayProperty,BooleanProperty,DateProperty,DateTimeProperty,IntegerProperty,NumberProperty,ObjectProperty,StringProperty,ModifiableValueProperty {

        V getDefaultValue();

        V getExampleValue();

        List<PropertyOption> getOptions();

        PropertyOptionDataSource getOptionsDataSource();
    }

    sealed interface ArrayProperty
        extends ValueProperty<Object[], ArrayProperty>permits ModifiableArrayProperty {

        List<Property<?>> getItems();

        Boolean getMultipleValues();
    }

    sealed interface BooleanProperty extends
        ValueProperty<Boolean, BooleanProperty>permits ModifiableBooleanProperty {
    }

    sealed interface DateProperty
        extends ValueProperty<LocalDate, DateProperty>permits ModifiableDateProperty {
    }

    sealed interface DateTimeProperty extends
        ValueProperty<LocalDateTime, DateTimeProperty>permits ModifiableDateTimeProperty {
    }

    sealed interface IntegerProperty extends
        ValueProperty<Integer, IntegerProperty>permits ModifiableIntegerProperty {

        Integer getMaxValue();

        Integer getMinValue();
    }

    sealed interface NullProperty extends Property<NullProperty>permits ModifiableNullProperty {
    }

    sealed interface NumberProperty
        extends ValueProperty<Double, NumberProperty>permits ModifiableNumberProperty {

        Integer getMaxValue();

        Integer getMinValue();

        Integer getNumberPrecision();
    }

    sealed interface ObjectProperty
        extends ValueProperty<Object, ObjectProperty>permits ModifiableObjectProperty {

        List<? extends Property<?>> getAdditionalProperties();

        Boolean getMultipleValues();

        String getObjectType();

        List<? extends Property<?>> getProperties();
    }

    sealed interface StringProperty
        extends ValueProperty<String, StringProperty>permits ModifiableStringProperty {

        ControlType getControlType();
    }
}
// CHECKSTYLE:ON
