
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
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Schema(name = "Property", description = "A base property.")
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

    @Schema(name = "advancedOption", description = "If the property should be grouped under advanced options.")
    Boolean getAdvancedOption();

    @Schema(name = "description", description = "The property description.")
    String getDescription();

    @Schema(name = "description", description = "The property description.")
    DisplayOption getDisplayOption();

    @Schema(name = "hidden", description = "If the property should be visible or not.")
    Boolean getHidden();

    @Schema(name = "label", description = "The property label.")
    String getLabel();

    @Schema(name = "metadata", description = "Additional data that can be used during processing.")
    Map<String, Object> getMetadata();

    @Schema(name = "name", description = "The property name.")
    String getName();

    @Schema(name = "placeholder", description = "The property placeholder.")
    String getPlaceholder();

    @Schema(name = "required", description = "If the property is required or not.")
    Boolean getRequired();

    @Schema(name = "type", description = "The property type.")
    Type getType();

    @Schema(name = "OneOfProperty", description = "A one of property type.")
    sealed interface OneOfProperty
        extends Property<OneOfProperty>permits ModifiableOneOfProperty {

        @Schema(name = "types", description = "Possible types of properties that can be used.")
        List<? extends Property<?>> getTypes();
    }

    @Schema(name = "ValueProperty", description = "A base property for all value based properties.")
    sealed interface ValueProperty<V, P extends ValueProperty<V, P>> extends
        Property<P>permits ArrayProperty,BooleanProperty,DateProperty,DateTimeProperty,IntegerProperty,NumberProperty,ObjectProperty,StringProperty,ModifiableValueProperty {

        @Schema(name = "defaultValue", description = "The property default value.")
        V getDefaultValue();

        @Schema(name = "exampleValue", description = "The property example value.")
        V getExampleValue();

        @Schema(name = "options", description = "The list of valid property options.")
        List<PropertyOption> getOptions();

        PropertyOptionDataSource getOptionsDataSource();
    }

    @Schema(name = "ArrayProperty", description = "An array property type.")
    sealed interface ArrayProperty
        extends ValueProperty<Object[], ArrayProperty>permits ModifiableArrayProperty {

        @Schema(name = "items", description = "Types of the array items.")
        List<Property<?>> getItems();
    }

    @Schema(name = "BooleanProperty", description = "A boolean property type.")
    sealed interface BooleanProperty extends
        ValueProperty<Boolean, BooleanProperty>permits ModifiableBooleanProperty {
    }

    @Schema(name = "DateProperty", description = "A date property type.")
    sealed interface DateProperty
        extends ValueProperty<LocalDate, DateProperty>permits ModifiableDateProperty {
    }

    @Schema(name = "DateTimeProperty", description = "A date-time property type.")
    sealed interface DateTimeProperty extends
        ValueProperty<LocalDateTime, DateTimeProperty>permits ModifiableDateTimeProperty {
    }

    @Schema(name = "IntegerProperty", description = "An integer property type.")
    sealed interface IntegerProperty extends
        ValueProperty<Integer, IntegerProperty>permits ModifiableIntegerProperty {

        @Schema(name = "maxValue", description = "The maximum property value.")
        Integer getMaxValue();

        @Schema(name = "minValue", description = "The minimum property value.")
        Integer getMinValue();
    }

    @Schema(name = "NullProperty", description = "A null property type.")
    sealed interface NullProperty extends Property<NullProperty>permits ModifiableNullProperty {
    }

    @Schema(name = "NumberProperty", description = "A number property type.")
    sealed interface NumberProperty
        extends ValueProperty<Double, NumberProperty>permits ModifiableNumberProperty {

        @Schema(name = "maxValue", description = "The maximum property value.")
        Integer getMaxValue();

        @Schema(name = "minValue", description = "The minimum property value.")
        Integer getMinValue();

        @Schema(name = "numberPrecision", description = "The number value precision.")
        Integer getNumberPrecision();
    }

    @Schema(name = "ObjectProperty", description = "An object property type.")
    sealed interface ObjectProperty
        extends ValueProperty<Object, ObjectProperty>permits ModifiableObjectProperty {

        @Schema(name = "additionalProperties", description = "Types of dynamically defined properties.")
        List<Property<?>> getAdditionalProperties();

        @Schema(name = "objectType", description = "The object type.")
        String getObjectType();

        @Schema(name = "properties", description = "The list of valid object property types.")
        List<? extends Property<?>> getProperties();
    }

    @Schema(name = "StringProperty", description = "A string property.")
    sealed interface StringProperty
        extends ValueProperty<String, StringProperty>permits ModifiableStringProperty {

        ControlType getControlType();
    }
}
// CHECKSTYLE:ON
