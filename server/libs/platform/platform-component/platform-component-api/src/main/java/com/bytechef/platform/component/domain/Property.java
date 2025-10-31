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

package com.bytechef.platform.component.domain;

import com.bytechef.component.definition.Property.Type;
import com.bytechef.platform.domain.BaseProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;

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
public abstract class Property extends BaseProperty {

    protected Type type;

    protected Property() {
    }

    public Property(com.bytechef.component.definition.Property property) {
        super(property);

        this.type = property.getType();
    }

    @SuppressWarnings("unchecked")
    public static <P extends Property> P toProperty(com.bytechef.component.definition.Property property) {
        if (property == null) {
            return null;
        }

        return switch (property.getType()) {
            case ARRAY -> (P) toArrayProperty((com.bytechef.component.definition.Property.ArrayProperty) property);
            case BOOLEAN -> (P) toBooleanProperty(
                (com.bytechef.component.definition.Property.BooleanProperty) property);
            case DATE -> (P) toDateProperty((com.bytechef.component.definition.Property.DateProperty) property);
            case DATE_TIME -> (P) toDateTimeProperty(
                (com.bytechef.component.definition.Property.DateTimeProperty) property);
            case DYNAMIC_PROPERTIES -> (P) toDynamicPropertiesProperty(
                (com.bytechef.component.definition.Property.DynamicPropertiesProperty) property);
            case INTEGER -> (P) toIntegerProperty(
                (com.bytechef.component.definition.Property.IntegerProperty) property);
            case FILE_ENTRY ->
                (P) toFileEntryProperty((com.bytechef.component.definition.Property.FileEntryProperty) property);
            case NULL -> (P) toNullProperty((com.bytechef.component.definition.Property.NullProperty) property);
            case NUMBER -> (P) toNumberProperty((com.bytechef.component.definition.Property.NumberProperty) property);
            case OBJECT -> (P) toObjectProperty((com.bytechef.component.definition.Property.ObjectProperty) property);
            case STRING -> (P) toStringProperty((com.bytechef.component.definition.Property.StringProperty) property);
            case TIME -> (P) toTimeProperty((com.bytechef.component.definition.Property.TimeProperty) property);
        };
    }

    public abstract Object accept(PropertyVisitor propertyVisitor);

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && type == ((Property) o).getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type);
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Property{" +
            "type=" + type +
            "} " + super.toString();
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
