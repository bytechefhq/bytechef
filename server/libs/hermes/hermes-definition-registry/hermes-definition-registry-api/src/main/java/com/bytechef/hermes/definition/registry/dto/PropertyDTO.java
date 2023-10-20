
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

package com.bytechef.hermes.definition.registry.dto;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Property.AnyProperty;
import com.bytechef.hermes.definition.Property.ArrayProperty;
import com.bytechef.hermes.definition.Property.BooleanProperty;
import com.bytechef.hermes.definition.Property.DateProperty;
import com.bytechef.hermes.definition.Property.DateTimeProperty;
import com.bytechef.hermes.definition.Property.DynamicPropertiesProperty;
import com.bytechef.hermes.definition.Property.IntegerProperty;
import com.bytechef.hermes.definition.Property.NullProperty;
import com.bytechef.hermes.definition.Property.NumberProperty;
import com.bytechef.hermes.definition.Property.ObjectProperty;
import com.bytechef.hermes.definition.Property.StringProperty;
import com.bytechef.hermes.definition.Property.TimeProperty;
import com.bytechef.hermes.definition.Property.Type;

import java.util.Optional;

public abstract class PropertyDTO {

    private final boolean advancedOption;
    private final String description;
    private final String displayCondition;
    private final boolean expressionEnabled; // Defaults to true
    private final boolean hidden;
    private final String label;
    private final String placeholder;
    private final boolean required;
    private final String name;
    private final Type type;

    public PropertyDTO(Property property) {
        this.advancedOption = OptionalUtils.orElse(property.getAdvancedOption(), false);
        this.description = OptionalUtils.orElse(property.getDescription(), null);
        this.displayCondition = OptionalUtils.orElse(property.getDisplayCondition(), null);
        this.expressionEnabled = OptionalUtils.orElse(property.getExpressionEnabled(), true);
        this.hidden = OptionalUtils.orElse(property.getHidden(), false);
        this.label = OptionalUtils.orElse(property.getLabel(), property.getName());
        this.placeholder = OptionalUtils.orElse(property.getPlaceholder(), null);
        this.required = OptionalUtils.orElse(property.getRequired(), false);
        this.name = property.getName();
        this.type = property.getType();
    }

    public abstract Object accept(PropertyVisitor propertyVisitor);

    public boolean getAdvancedOption() {
        return advancedOption;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<String> getDisplayCondition() {
        return Optional.ofNullable(displayCondition);
    }

    public boolean getExpressionEnabled() {
        return expressionEnabled;
    }

    public boolean getHidden() {
        return hidden;
    }

    public Optional<String> getLabel() {
        return Optional.ofNullable(label);
    }

    public Optional<String> getPlaceholder() {
        return Optional.ofNullable(placeholder);
    }

    public boolean getRequired() {
        return required;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Type getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public static <P extends PropertyDTO> P toPropertyDTO(Property property) {
        return switch (property.getType()) {
            case ANY -> (P) toAnyPropertyDTO((AnyProperty) property);
            case ARRAY -> (P) toArrayPropertyDTO((ArrayProperty) property);
            case BOOLEAN -> (P) toBooleanPropertyDTO((BooleanProperty) property);
            case DATE -> (P) toDatePropertyDTO((DateProperty) property);
            case DATE_TIME -> (P) toDateTimePropertyDTO((DateTimeProperty) property);
            case DYNAMIC_PROPERTIES -> (P) toDynamicPropertiesPropertyDTO((DynamicPropertiesProperty) property);
            case INTEGER -> (P) toIntegerPropertyDTO((IntegerProperty) property);
            case NULL -> (P) toNullPropertyDTO((NullProperty) property);
            case NUMBER -> (P) toNumberPropertyDTO((NumberProperty) property);
            case OBJECT -> (P) toObjectPropertyDTO((ObjectProperty) property);
            case STRING -> (P) toStringPropertyDTO((StringProperty) property);
            case TIME -> (P) toTimePropertyDTO((TimeProperty) property);
        };
    }

    private static AnyPropertyDTO toAnyPropertyDTO(AnyProperty anyProperty) {
        return new AnyPropertyDTO(anyProperty);
    }

    private static ArrayPropertyDTO toArrayPropertyDTO(ArrayProperty arrayProperty) {
        return new ArrayPropertyDTO(arrayProperty);
    }

    private static BooleanPropertyDTO toBooleanPropertyDTO(BooleanProperty booleanProperty) {
        return new BooleanPropertyDTO(booleanProperty);
    }

    private static DatePropertyDTO toDatePropertyDTO(DateProperty datePropertyDTO) {
        return new DatePropertyDTO(datePropertyDTO);
    }

    private static DateTimePropertyDTO toDateTimePropertyDTO(DateTimeProperty dateTimePropertyDTO) {
        return new DateTimePropertyDTO(dateTimePropertyDTO);
    }

    private static DynamicPropertiesPropertyDTO toDynamicPropertiesPropertyDTO(
        DynamicPropertiesProperty dynamicPropertiesProperty) {

        return new DynamicPropertiesPropertyDTO(dynamicPropertiesProperty);
    }

    private static IntegerPropertyDTO toIntegerPropertyDTO(IntegerProperty integerProperty) {
        return new IntegerPropertyDTO(integerProperty);
    }

    private static NullPropertyDTO toNullPropertyDTO(NullProperty nullProperty) {
        return new NullPropertyDTO(nullProperty);
    }

    private static NumberPropertyDTO toNumberPropertyDTO(NumberProperty numberProperty) {
        return new NumberPropertyDTO(numberProperty);
    }

    private static ObjectPropertyDTO toObjectPropertyDTO(ObjectProperty objectProperty) {
        return new ObjectPropertyDTO(objectProperty);
    }

    private static StringPropertyDTO toStringPropertyDTO(StringProperty stringProperty) {
        return new StringPropertyDTO(stringProperty);
    }

    private static TimePropertyDTO toTimePropertyDTO(TimeProperty timeProperty) {
        return new TimePropertyDTO(timeProperty);
    }

    public interface PropertyVisitor {

        Object visit(AnyPropertyDTO anyPropertyDTO);

        Object visit(ArrayPropertyDTO arrayPropertyDTO);

        Object visit(BooleanPropertyDTO booleanPropertyDTO);

        Object visit(DatePropertyDTO datePropertyDTO);

        Object visit(DateTimePropertyDTO dateTimePropertyDTO);

        Object visit(DynamicPropertiesPropertyDTO dynamicPropertiesPropertyDTO);

        Object visit(IntegerPropertyDTO integerPropertyDTO);

        Object visit(NullPropertyDTO nullPropertyDTO);

        Object visit(NumberPropertyDTO numberPropertyDTO);

        Object visit(ObjectPropertyDTO objectPropertyDTO);

        Object visit(StringPropertyDTO stringPropertyDTO);

        Object visit(TimePropertyDTO timePropertyDTO);
    }
}
