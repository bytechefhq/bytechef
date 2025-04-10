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

package com.bytechef.platform.ai.config;

import com.bytechef.platform.domain.BaseProperty;
import java.util.List;

public class PropertyDecorator {
    private final BaseProperty property;
    private Type type;
    private final Location location;

    public PropertyDecorator(BaseProperty property) {
        this.property = property;

        switch (property) {
            case com.bytechef.platform.workflow.task.dispatcher.domain.ArrayProperty ignored -> {
                this.type = Type.ARRAY;
                this.location = Location.TAKS_DISPATCHER;
            }
            case com.bytechef.platform.component.domain.ArrayProperty ignored -> {
                this.type = Type.ARRAY;
                this.location = Location.COMPONENT;
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.BooleanProperty ignored -> {
                this.type = Type.BOOLEAN;
                this.location = Location.TAKS_DISPATCHER;
            }
            case com.bytechef.platform.component.domain.BooleanProperty ignored -> {
                this.type = Type.BOOLEAN;
                this.location = Location.COMPONENT;
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.DateProperty ignored -> {
                this.type = Type.DATE;
                this.location = Location.TAKS_DISPATCHER;
            }
            case com.bytechef.platform.component.domain.DateProperty ignored -> {
                this.type = Type.DATE;
                this.location = Location.COMPONENT;
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.DateTimeProperty ignored -> {
                this.type = Type.DATE_TIME;
                this.location = Location.TAKS_DISPATCHER;
            }
            case com.bytechef.platform.component.domain.DateTimeProperty ignored -> {
                this.type = Type.DATE_TIME;
                this.location = Location.COMPONENT;
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.TaskProperty ignored -> {
                this.type = Type.TASK;
                this.location = Location.TAKS_DISPATCHER;
            }
            case com.bytechef.platform.component.domain.DynamicPropertiesProperty ignored -> {
                this.type = Type.DYNAMIC_PROPERTIES;
                this.location = Location.COMPONENT;
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.IntegerProperty ignored -> {
                this.type = Type.INTEGER;
                this.location = Location.TAKS_DISPATCHER;
            }
            case com.bytechef.platform.component.domain.IntegerProperty ignored -> {
                this.type = Type.INTEGER;
                this.location = Location.COMPONENT;
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.FileEntryProperty ignored -> {
                this.type = Type.FILE_ENTRY;
                this.location = Location.TAKS_DISPATCHER;
            }
            case com.bytechef.platform.component.domain.FileEntryProperty ignored -> {
                this.type = Type.FILE_ENTRY;
                this.location = Location.COMPONENT;
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.NullProperty ignored -> {
                this.type = Type.NULL;
                this.location = Location.TAKS_DISPATCHER;
            }
            case com.bytechef.platform.component.domain.NullProperty ignored -> {
                this.type = Type.NULL;
                this.location = Location.COMPONENT;
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.NumberProperty ignored -> {
                this.type = Type.NUMBER;
                this.location = Location.TAKS_DISPATCHER;
            }
            case com.bytechef.platform.component.domain.NumberProperty ignored -> {
                this.type = Type.NUMBER;
                this.location = Location.COMPONENT;
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty ignored -> {
                this.type = Type.OBJECT;
                this.location = Location.TAKS_DISPATCHER;
            }
            case com.bytechef.platform.component.domain.ObjectProperty ignored -> {
                this.type = Type.OBJECT;
                this.location = Location.COMPONENT;
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.StringProperty ignored -> {
                this.type = Type.STRING;
                this.location = Location.TAKS_DISPATCHER;
            }
            case com.bytechef.platform.component.domain.StringProperty ignored -> {
                this.type = Type.STRING;
                this.location = Location.COMPONENT;
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.TimeProperty ignored -> {
                this.type = Type.TIME;
                this.location = Location.TAKS_DISPATCHER;
            }
            case com.bytechef.platform.component.domain.TimeProperty ignored -> {
                this.type = Type.TIME;
                this.location = Location.COMPONENT;
            }
            default -> {
                this.type = Type.NULL;
                this.location = Location.TAKS_DISPATCHER;
            }
        }
    }

    enum Type {
        ARRAY,
        BOOLEAN,
        DATE,
        DATE_TIME,
        DYNAMIC_PROPERTIES,
        FILE_ENTRY,
        INTEGER,
        NULL,
        NUMBER,
        OBJECT,
        STRING,
        TASK,
        TIME,
    }

    enum Location {
        TAKS_DISPATCHER,
        COMPONENT
    }

    public BaseProperty getProperty() {
        return property;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return property.getName();
    }

    public List<PropertyDecorator> getItems() {
        return switch (location) {
            case TAKS_DISPATCHER -> toPropertyDecoratorList(
                ((com.bytechef.platform.workflow.task.dispatcher.domain.ArrayProperty) property).getItems());
            case COMPONENT ->
                toPropertyDecoratorList(((com.bytechef.platform.component.domain.ArrayProperty) property).getItems());
        };
    }

    public List<PropertyDecorator> getObjectProperties() {
        return switch (location) {
            case TAKS_DISPATCHER -> toPropertyDecoratorList(
                ((com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty) property).getProperties());
            case COMPONENT -> toPropertyDecoratorList(
                ((com.bytechef.platform.component.domain.ObjectProperty) property).getProperties());
        };
    }

    public List<PropertyDecorator> getFileEntryProperties() {
        return switch (location) {
            case TAKS_DISPATCHER -> toPropertyDecoratorList(
                ((com.bytechef.platform.workflow.task.dispatcher.domain.FileEntryProperty) property).getProperties());
            case COMPONENT -> toPropertyDecoratorList(
                ((com.bytechef.platform.component.domain.FileEntryProperty) property).getProperties());
        };
    }

    public static List<PropertyDecorator> toPropertyDecoratorList(List<? extends BaseProperty> properties) {
        return properties.stream()
            .map(PropertyDecorator::new)
            .toList();
    }
}
