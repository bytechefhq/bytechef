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

package com.bytechef.platform.component.registry.domain;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.Property.Type;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ArrayProperty.class, name = "ARRAY"),
    @JsonSubTypes.Type(value = BooleanProperty.class, name = "BOOLEAN"),
    @JsonSubTypes.Type(value = DateProperty.class, name = "DATE"),
    @JsonSubTypes.Type(value = DateTimeProperty.class, name = "DATE_TIME"),
    @JsonSubTypes.Type(value = DynamicPropertiesProperty.class, name = "DYNAMIC_PROPERTIES"),
    @JsonSubTypes.Type(value = FileEntryProperty.class, name = "FILE_ENTRY"),
    @JsonSubTypes.Type(value = IntegerProperty.class, name = "INTEGER"),
    @JsonSubTypes.Type(value = NumberProperty.class, name = "NUMBER"),
    @JsonSubTypes.Type(value = NullProperty.class, name = "NULL"),
    @JsonSubTypes.Type(value = ObjectProperty.class, name = "OBJECT"),
    @JsonSubTypes.Type(value = StringProperty.class, name = "STRING"),
    @JsonSubTypes.Type(value = TimeProperty.class, name = "TIME"),
})
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
public abstract class Property {

    private boolean advancedOption;
    private String description;
    private String displayCondition;
    private boolean expressionEnabled; // Defaults to true
    private boolean hidden;
    private boolean required;
    private String name;
    private Type type;

    protected Property() {
    }

    public Property(com.bytechef.component.definition.Property property) {
        this.advancedOption = OptionalUtils.orElse(property.getAdvancedOption(), false);
        this.description = OptionalUtils.orElse(property.getDescription(), null);
        this.displayCondition = OptionalUtils.orElse(property.getDisplayCondition(), null);
        this.expressionEnabled = OptionalUtils.orElse(property.getExpressionEnabled(), true);
        this.hidden = OptionalUtils.orElse(property.getHidden(), false);
        this.required = OptionalUtils.orElse(property.getRequired(), false);
        this.name = property.getName();
        this.type = property.getType();
    }

    @SuppressWarnings("unchecked")
    public static <P extends Property> P toProperty(com.bytechef.component.definition.Property property) {
        return switch (property.getType()) {
            case ARRAY ->
                (P) toArrayProperty((com.bytechef.component.definition.Property.ArrayProperty) property);
            case BOOLEAN ->
                (P) toBooleanProperty((com.bytechef.component.definition.Property.BooleanProperty) property);
            case DATE -> (P) toDateProperty((com.bytechef.component.definition.Property.DateProperty) property);
            case DATE_TIME ->
                (P) toDateTimeProperty((com.bytechef.component.definition.Property.DateTimeProperty) property);
            case DYNAMIC_PROPERTIES -> (P) toDynamicPropertiesProperty(
                (com.bytechef.component.definition.Property.DynamicPropertiesProperty) property);
            case INTEGER ->
                (P) toIntegerProperty((com.bytechef.component.definition.Property.IntegerProperty) property);
            case FILE_ENTRY ->
                (P) toFileEntryProperty((com.bytechef.component.definition.Property.FileEntryProperty) property);
            case NULL -> (P) toNullProperty((com.bytechef.component.definition.Property.NullProperty) property);
            case NUMBER ->
                (P) toNumberProperty((com.bytechef.component.definition.Property.NumberProperty) property);
            case OBJECT ->
                (P) toObjectProperty((com.bytechef.component.definition.Property.ObjectProperty) property);
            case STRING ->
                (P) toStringProperty((com.bytechef.component.definition.Property.StringProperty) property);
            case TIME -> (P) toTimeProperty((com.bytechef.component.definition.Property.TimeProperty) property);
        };
    }

    public abstract Object accept(PropertyVisitor propertyVisitor);

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Property that)) {
            return false;
        }

        return advancedOption == that.advancedOption && expressionEnabled == that.expressionEnabled
            && hidden == that.hidden && required == that.required
            && Objects.equals(displayCondition, that.displayCondition)
            && Objects.equals(name, that.name) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            advancedOption, displayCondition, expressionEnabled, hidden, required, name, type);
    }

    public boolean getAdvancedOption() {
        return advancedOption;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getDisplayCondition() {
        return displayCondition;
    }

    public boolean getExpressionEnabled() {
        return expressionEnabled;
    }

    public boolean getHidden() {
        return hidden;
    }

    public boolean getRequired() {
        return required;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Property" +
            "advancedOption=" + advancedOption +
            ", description='" + description + '\'' +
            ", displayCondition='" + displayCondition + '\'' +
            ", expressionEnabled=" + expressionEnabled +
            ", hidden=" + hidden +
            ", required=" + required +
            ", name='" + name + '\'' +
            ", type=" + type +
            '}';
    }

    private static ArrayProperty toArrayProperty(
        com.bytechef.component.definition.Property.ArrayProperty arrayProperty) {

        return new ArrayProperty(arrayProperty);
    }

    private static BooleanProperty toBooleanProperty(
        com.bytechef.component.definition.Property.BooleanProperty booleanProperty) {

        return new BooleanProperty(booleanProperty);
    }

    private static DateProperty toDateProperty(
        com.bytechef.component.definition.Property.DateProperty dateProperty) {

        return new DateProperty(dateProperty);
    }

    private static DateTimeProperty toDateTimeProperty(
        com.bytechef.component.definition.Property.DateTimeProperty dateTimeProperty) {

        return new DateTimeProperty(dateTimeProperty);
    }

    private static DynamicPropertiesProperty toDynamicPropertiesProperty(
        com.bytechef.component.definition.Property.DynamicPropertiesProperty dynamicPropertiesProperty) {

        return new DynamicPropertiesProperty(dynamicPropertiesProperty);
    }

    private static FileEntryProperty toFileEntryProperty(
        com.bytechef.component.definition.Property.FileEntryProperty fileEntryProperty) {

        return new FileEntryProperty(fileEntryProperty);
    }

    private static IntegerProperty toIntegerProperty(
        com.bytechef.component.definition.Property.IntegerProperty integerProperty) {

        return new IntegerProperty(integerProperty);
    }

    private static NullProperty toNullProperty(
        com.bytechef.component.definition.Property.NullProperty nullProperty) {

        return new NullProperty(nullProperty);
    }

    private static NumberProperty toNumberProperty(
        com.bytechef.component.definition.Property.NumberProperty numberProperty) {

        return new NumberProperty(numberProperty);
    }

    private static ObjectProperty toObjectProperty(
        com.bytechef.component.definition.Property.ObjectProperty objectProperty) {

        return new ObjectProperty(objectProperty);
    }

    private static StringProperty toStringProperty(
        com.bytechef.component.definition.Property.StringProperty stringProperty) {

        return new StringProperty(stringProperty);
    }

    private static TimeProperty toTimeProperty(com.bytechef.component.definition.Property.TimeProperty timeProperty) {
        return new TimeProperty(timeProperty);
    }

    public interface PropertyVisitor {

        Object visit(ArrayProperty arrayProperty);

        Object visit(BooleanProperty booleanProperty);

        Object visit(DateProperty dateProperty);

        Object visit(DateTimeProperty dateTimeProperty);

        Object visit(DynamicPropertiesProperty dynamicPropertiesProperty);

        Object visit(IntegerProperty integerProperty);

        Object visit(FileEntryProperty fileEntryProperty);

        Object visit(NullProperty nullProperty);

        Object visit(NumberProperty numberProperty);

        Object visit(ObjectProperty objectProperty);

        Object visit(StringProperty stringProperty);

        Object visit(TimeProperty timeProperty);
    }
}
