
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

package com.bytechef.hermes.definition.registry.domain;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.definition.Property.Type;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Objects;
import java.util.Optional;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AnyProperty.class, name = "ANY"),
    @JsonSubTypes.Type(value = ArrayProperty.class, name = "ARRAY"),
    @JsonSubTypes.Type(value = BooleanProperty.class, name = "BOOLEAN"),
    @JsonSubTypes.Type(value = DateProperty.class, name = "DATE"),
    @JsonSubTypes.Type(value = DateTimeProperty.class, name = "DATE_TIME"),
    @JsonSubTypes.Type(value = DynamicPropertiesProperty.class, name = "DYNAMIC_PROPERTIES"),
    @JsonSubTypes.Type(value = IntegerProperty.class, name = "INTEGER"),
    @JsonSubTypes.Type(value = NumberProperty.class, name = "NUMBER"),
    @JsonSubTypes.Type(value = NullProperty.class, name = "NULL"),
    @JsonSubTypes.Type(value = ObjectProperty.class, name = "OBJECT"),
    @JsonSubTypes.Type(value = StringProperty.class, name = "STRING"),
    @JsonSubTypes.Type(value = TimeProperty.class, name = "TIME"),
})
// CHECKSTYLE:OFF
public abstract class Property {

    private boolean advancedOption;
    private String description;
    private String displayCondition;
    private boolean expressionEnabled; // Defaults to true
    private boolean hidden;
    private String label;
    private String placeholder;
    private boolean required;
    private String name;
    private Type type;

    protected Property() {
    }

    public Property(com.bytechef.hermes.definition.Property property) {
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

    @SuppressWarnings("unchecked")
    public static <P extends Property> P toProperty(com.bytechef.hermes.definition.Property property) {
        return switch (property.getType()) {
            case ANY -> (P) toAnyProperty((com.bytechef.hermes.definition.Property.AnyProperty) property);
            case ARRAY -> (P) toArrayProperty((com.bytechef.hermes.definition.Property.ArrayProperty) property);
            case BOOLEAN -> (P) toBooleanProperty((com.bytechef.hermes.definition.Property.BooleanProperty) property);
            case DATE -> (P) toDateProperty((com.bytechef.hermes.definition.Property.DateProperty) property);
            case DATE_TIME ->
                (P) toDateTimeProperty((com.bytechef.hermes.definition.Property.DateTimeProperty) property);
            case DYNAMIC_PROPERTIES -> (P) toDynamicPropertiesProperty(
                (com.bytechef.hermes.definition.Property.DynamicPropertiesProperty) property);
            case INTEGER -> (P) toIntegerProperty((com.bytechef.hermes.definition.Property.IntegerProperty) property);
            case NULL -> (P) toNullProperty((com.bytechef.hermes.definition.Property.NullProperty) property);
            case NUMBER -> (P) toNumberProperty((com.bytechef.hermes.definition.Property.NumberProperty) property);
            case OBJECT -> (P) toObjectProperty((com.bytechef.hermes.definition.Property.ObjectProperty) property);
            case STRING -> (P) toStringProperty((com.bytechef.hermes.definition.Property.StringProperty) property);
            case TIME -> (P) toTimeProperty((com.bytechef.hermes.definition.Property.TimeProperty) property);
        };
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Property that))
            return false;
        return advancedOption == that.advancedOption && expressionEnabled == that.expressionEnabled
            && hidden == that.hidden && required == that.required && Objects.equals(description, that.description)
            && Objects.equals(displayCondition, that.displayCondition) && Objects.equals(label, that.label)
            && Objects.equals(placeholder, that.placeholder) && Objects.equals(name, that.name) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(advancedOption, description, displayCondition, expressionEnabled, hidden, label,
            placeholder, required, name, type);
    }

    @Override
    public String toString() {
        return "Property" +
            "advancedOption=" + advancedOption +
            ", description='" + description + '\'' +
            ", displayCondition='" + displayCondition + '\'' +
            ", expressionEnabled=" + expressionEnabled +
            ", hidden=" + hidden +
            ", label='" + label + '\'' +
            ", placeholder='" + placeholder + '\'' +
            ", required=" + required +
            ", name='" + name + '\'' +
            ", type=" + type +
            '}';
    }

    private static AnyProperty toAnyProperty(com.bytechef.hermes.definition.Property.AnyProperty anyProperty) {
        return new AnyProperty(anyProperty);
    }

    private static ArrayProperty toArrayProperty(com.bytechef.hermes.definition.Property.ArrayProperty arrayProperty) {
        return new ArrayProperty(arrayProperty);
    }

    private static BooleanProperty
        toBooleanProperty(com.bytechef.hermes.definition.Property.BooleanProperty booleanProperty) {
        return new BooleanProperty(booleanProperty);
    }

    private static DateProperty toDateProperty(com.bytechef.hermes.definition.Property.DateProperty dateProperty) {
        return new DateProperty(dateProperty);
    }

    private static DateTimeProperty
        toDateTimeProperty(com.bytechef.hermes.definition.Property.DateTimeProperty dateTimeProperty) {
        return new DateTimeProperty(dateTimeProperty);
    }

    private static DynamicPropertiesProperty toDynamicPropertiesProperty(
        com.bytechef.hermes.definition.Property.DynamicPropertiesProperty dynamicPropertiesProperty) {

        return new DynamicPropertiesProperty(dynamicPropertiesProperty);
    }

    private static IntegerProperty
        toIntegerProperty(com.bytechef.hermes.definition.Property.IntegerProperty integerProperty) {
        return new IntegerProperty(integerProperty);
    }

    private static NullProperty toNullProperty(com.bytechef.hermes.definition.Property.NullProperty nullProperty) {
        return new NullProperty(nullProperty);
    }

    private static NumberProperty
        toNumberProperty(com.bytechef.hermes.definition.Property.NumberProperty numberProperty) {
        return new NumberProperty(numberProperty);
    }

    private static ObjectProperty
        toObjectProperty(com.bytechef.hermes.definition.Property.ObjectProperty objectProperty) {
        return new ObjectProperty(objectProperty);
    }

    private static StringProperty
        toStringProperty(com.bytechef.hermes.definition.Property.StringProperty stringProperty) {
        return new StringProperty(stringProperty);
    }

    private static TimeProperty toTimeProperty(com.bytechef.hermes.definition.Property.TimeProperty timeProperty) {
        return new TimeProperty(timeProperty);
    }

    public interface PropertyVisitor {

        Object visit(AnyProperty anyProperty);

        Object visit(ArrayProperty arrayProperty);

        Object visit(BooleanProperty booleanProperty);

        Object visit(DateProperty dateProperty);

        Object visit(DateTimeProperty dateTimeProperty);

        Object visit(DynamicPropertiesProperty dynamicPropertiesProperty);

        Object visit(IntegerProperty integerProperty);

        Object visit(NullProperty nullProperty);

        Object visit(NumberProperty numberProperty);

        Object visit(ObjectProperty objectProperty);

        Object visit(StringProperty stringProperty);

        Object visit(TimeProperty timeProperty);
    }
}
