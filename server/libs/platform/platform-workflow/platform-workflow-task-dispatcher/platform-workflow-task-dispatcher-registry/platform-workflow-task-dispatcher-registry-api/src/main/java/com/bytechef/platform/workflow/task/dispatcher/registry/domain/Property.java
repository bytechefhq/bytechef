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

package com.bytechef.platform.workflow.task.dispatcher.registry.domain;

import com.bytechef.hermes.definition.BaseProperty;
import com.bytechef.platform.registry.domain.AbstractProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
    @JsonSubTypes.Type(value = IntegerProperty.class, name = "INTEGER"),
    @JsonSubTypes.Type(value = NumberProperty.class, name = "NUMBER"),
    @JsonSubTypes.Type(value = NullProperty.class, name = "NULL"),
    @JsonSubTypes.Type(value = ObjectProperty.class, name = "OBJECT"),
    @JsonSubTypes.Type(value = StringProperty.class, name = "STRING"),
    @JsonSubTypes.Type(value = TimeProperty.class, name = "TIME"),
})
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
public abstract class Property extends AbstractProperty {

    protected Property() {
    }

    public Property(BaseProperty property) {
        super(property);
    }

    @SuppressWarnings("unchecked")
    public static <P extends Property> P
        toProperty(com.bytechef.platform.workflow.task.dispatcher.definition.Property property) {
        return switch (property.getType()) {
            case ARRAY -> (P) toArrayProperty(
                (com.bytechef.platform.workflow.task.dispatcher.definition.Property.ArrayProperty) property);
            case BOOLEAN -> (P) toBooleanProperty(
                (com.bytechef.platform.workflow.task.dispatcher.definition.Property.BooleanProperty) property);
            case DATE -> (P) toDateProperty(
                (com.bytechef.platform.workflow.task.dispatcher.definition.Property.DateProperty) property);
            case DATE_TIME ->
                (P) toDateTimeProperty(
                    (com.bytechef.platform.workflow.task.dispatcher.definition.Property.DateTimeProperty) property);
            case DYNAMIC_PROPERTIES -> throw new IllegalStateException(
                "DYNAMIC_PROPERTIES property type is not supported");
            case INTEGER -> (P) toIntegerProperty(
                (com.bytechef.platform.workflow.task.dispatcher.definition.Property.IntegerProperty) property);
            case NULL -> (P) toNullProperty(
                (com.bytechef.platform.workflow.task.dispatcher.definition.Property.NullProperty) property);
            case NUMBER -> (P) toNumberProperty(
                (com.bytechef.platform.workflow.task.dispatcher.definition.Property.NumberProperty) property);
            case OBJECT -> (P) toObjectProperty(
                (com.bytechef.platform.workflow.task.dispatcher.definition.Property.ObjectProperty) property);
            case STRING -> (P) toStringProperty(
                (com.bytechef.platform.workflow.task.dispatcher.definition.Property.StringProperty) property);
            case TIME -> (P) toTimeProperty((BaseProperty.TimeProperty) property);
        };
    }

    public abstract Object accept(PropertyVisitor propertyVisitor);

    private static ArrayProperty toArrayProperty(
        com.bytechef.platform.workflow.task.dispatcher.definition.Property.ArrayProperty arrayProperty) {

        return new ArrayProperty(arrayProperty);
    }

    private static BooleanProperty toBooleanProperty(
        com.bytechef.platform.workflow.task.dispatcher.definition.Property.BooleanProperty booleanProperty) {

        return new BooleanProperty(booleanProperty);
    }

    private static DateProperty toDateProperty(
        com.bytechef.platform.workflow.task.dispatcher.definition.Property.DateProperty dateProperty) {

        return new DateProperty(dateProperty);
    }

    private static DateTimeProperty toDateTimeProperty(
        com.bytechef.platform.workflow.task.dispatcher.definition.Property.DateTimeProperty dateTimeProperty) {

        return new DateTimeProperty(dateTimeProperty);
    }

    private static IntegerProperty toIntegerProperty(
        com.bytechef.platform.workflow.task.dispatcher.definition.Property.IntegerProperty integerProperty) {

        return new IntegerProperty(integerProperty);
    }

    private static NullProperty toNullProperty(
        com.bytechef.platform.workflow.task.dispatcher.definition.Property.NullProperty nullProperty) {

        return new NullProperty(nullProperty);
    }

    private static NumberProperty toNumberProperty(
        com.bytechef.platform.workflow.task.dispatcher.definition.Property.NumberProperty numberProperty) {

        return new NumberProperty(numberProperty);
    }

    private static ObjectProperty toObjectProperty(
        com.bytechef.platform.workflow.task.dispatcher.definition.Property.ObjectProperty objectProperty) {

        return new ObjectProperty(objectProperty);
    }

    private static StringProperty toStringProperty(
        com.bytechef.platform.workflow.task.dispatcher.definition.Property.StringProperty stringProperty) {

        return new StringProperty(stringProperty);
    }

    private static TimeProperty toTimeProperty(BaseProperty.TimeProperty timeProperty) {
        return new TimeProperty(timeProperty);
    }

    public interface PropertyVisitor {

        Object visit(ArrayProperty arrayProperty);

        Object visit(BooleanProperty booleanProperty);

        Object visit(DateProperty dateProperty);

        Object visit(DateTimeProperty dateTimeProperty);

        Object visit(IntegerProperty integerProperty);

        Object visit(NullProperty nullProperty);

        Object visit(NumberProperty numberProperty);

        Object visit(ObjectProperty objectProperty);

        Object visit(StringProperty stringProperty);

        Object visit(TimeProperty timeProperty);
    }
}
