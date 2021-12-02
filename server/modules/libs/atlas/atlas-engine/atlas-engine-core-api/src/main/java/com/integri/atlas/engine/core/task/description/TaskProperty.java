/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.core.task.description;

import static com.integri.atlas.engine.core.task.description.TaskParameter.parameter;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public abstract sealed class TaskProperty<T extends TaskProperty<?>>
    permits
        TaskProperty.BinaryTaskProperty,
        TaskProperty.BooleanTaskProperty,
        TaskProperty.ColorTaskProperty,
        TaskProperty.DateTimeTaskProperty,
        TaskProperty.CollectionTaskProperty,
        TaskProperty.GroupTaskProperty,
        TaskProperty.JSONTaskProperty,
        TaskProperty.MultiSelectTaskProperty,
        TaskProperty.NumberTaskProperty,
        TaskProperty.SelectTaskProperty,
        TaskProperty.StringTaskProperty {

    protected TaskParameter defaultValue;
    protected String description;
    protected String displayName;
    protected DisplayOption displayOption;
    protected String name;
    protected Boolean required;
    protected TaskPropertyType type;
    protected TaskPropertyTypeOption typeOption;

    private TaskProperty() {}

    public static BinaryTaskProperty BINARY_PROPERTY(String name) {
        return new BinaryTaskProperty(name);
    }

    public static BooleanTaskProperty BOOLEAN_PROPERTY(String name) {
        return new BooleanTaskProperty(name);
    }

    public static ColorTaskProperty COLOR_PROPERTY(String name) {
        return new ColorTaskProperty(name);
    }

    public static DateTimeTaskProperty DATE_TIME_PROPERTY(String name) {
        return new DateTimeTaskProperty(name);
    }

    public static CollectionTaskProperty COLLECTION_PROPERTY(String name) {
        return new CollectionTaskProperty(name);
    }

    public static GroupTaskProperty GROUP_PROPERTY(String name) {
        return new GroupTaskProperty(name);
    }

    public static JSONTaskProperty JSON_PROPERTY(String name) {
        return new JSONTaskProperty(name);
    }

    public static MultiSelectTaskProperty MULTI_SELECT_PROPERTY(String name) {
        return new MultiSelectTaskProperty(name);
    }

    public static NumberTaskProperty NUMBER_PROPERTY(String name) {
        return new NumberTaskProperty(name);
    }

    public static SelectTaskProperty SELECT_PROPERTY(String name) {
        return new SelectTaskProperty(name);
    }

    public static StringTaskProperty STRING_PROPERTY(String name) {
        return new StringTaskProperty(name);
    }

    public static DisplayOption hide(String k1) {
        return DisplayOption.displayOption().hide(k1);
    }

    public static DisplayOption hide(String k1, Boolean... values) {
        return DisplayOption.displayOption().hide(k1, values);
    }

    public static DisplayOption hide(String k1, Integer... values) {
        return DisplayOption.displayOption().hide(k1, values);
    }

    public static DisplayOption hide(String k1, Long... values) {
        return DisplayOption.displayOption().hide(k1, values);
    }

    public static DisplayOption hide(String k1, Float... values) {
        return DisplayOption.displayOption().hide(k1, values);
    }

    public static DisplayOption hide(String k1, Double... values) {
        return DisplayOption.displayOption().hide(k1, values);
    }

    public static DisplayOption hide(String k1, String... values) {
        return DisplayOption.displayOption().hide(k1, values);
    }

    public static DisplayOption hide(String k1, List<TaskParameterValue> v1) {
        return DisplayOption.displayOption().hide(k1, v1);
    }

    public static DisplayOption hide(String k1, List<TaskParameterValue> v1, String k2, List<TaskParameterValue> v2) {
        return DisplayOption.displayOption().hide(k1, v1, k2, v2);
    }

    public static DisplayOption hide(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3
    ) {
        return DisplayOption.displayOption().hide(k1, v1, k2, v2, k3, v3);
    }

    public static DisplayOption hide(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3,
        String k4,
        List<TaskParameterValue> v4
    ) {
        return DisplayOption.displayOption().hide(k1, v1, k2, v2, k3, v3, k4, v4);
    }

    public static DisplayOption hide(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3,
        String k4,
        List<TaskParameterValue> v4,
        String k5,
        List<TaskParameterValue> v5
    ) {
        return DisplayOption.displayOption().hide(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
    }

    public static DisplayOption hide(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3,
        String k4,
        List<TaskParameterValue> v4,
        String k5,
        List<TaskParameterValue> v5,
        String k6,
        List<TaskParameterValue> v6
    ) {
        return DisplayOption.displayOption().hide(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
    }

    public static DisplayOption hide(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3,
        String k4,
        List<TaskParameterValue> v4,
        String k5,
        List<TaskParameterValue> v5,
        String k6,
        List<TaskParameterValue> v6,
        String k7,
        List<TaskParameterValue> v7
    ) {
        return DisplayOption.displayOption().hide(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
    }

    public static DisplayOption hide(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3,
        String k4,
        List<TaskParameterValue> v4,
        String k5,
        List<TaskParameterValue> v5,
        String k6,
        List<TaskParameterValue> v6,
        String k7,
        List<TaskParameterValue> v7,
        String k8,
        List<TaskParameterValue> v8
    ) {
        return DisplayOption.displayOption().hide(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
    }

    public static DisplayOption hide(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3,
        String k4,
        List<TaskParameterValue> v4,
        String k5,
        List<TaskParameterValue> v5,
        String k6,
        List<TaskParameterValue> v6,
        String k7,
        List<TaskParameterValue> v7,
        String k8,
        List<TaskParameterValue> v8,
        String k9,
        List<TaskParameterValue> v9
    ) {
        return DisplayOption
            .displayOption()
            .hide(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
    }

    public static DisplayOption hide(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3,
        String k4,
        List<TaskParameterValue> v4,
        String k5,
        List<TaskParameterValue> v5,
        String k6,
        List<TaskParameterValue> v6,
        String k7,
        List<TaskParameterValue> v7,
        String k8,
        List<TaskParameterValue> v8,
        String k9,
        List<TaskParameterValue> v9,
        String k10,
        List<TaskParameterValue> v10
    ) {
        return DisplayOption
            .displayOption()
            .hide(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
    }

    public static DisplayOption show(String k1) {
        return DisplayOption.displayOption().hide(k1, List.of());
    }

    public static DisplayOption show(String k1, Boolean... values) {
        return DisplayOption
            .displayOption()
            .hide(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));
    }

    public static DisplayOption show(String k1, Integer... values) {
        return DisplayOption
            .displayOption()
            .hide(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));
    }

    public static DisplayOption show(String k1, Long... values) {
        return DisplayOption
            .displayOption()
            .hide(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));
    }

    public static DisplayOption show(String k1, Float... values) {
        return DisplayOption
            .displayOption()
            .hide(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));
    }

    public static DisplayOption show(String k1, Double... values) {
        return DisplayOption
            .displayOption()
            .hide(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));
    }

    public static DisplayOption show(String k1, String... values) {
        return DisplayOption
            .displayOption()
            .show(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));
    }

    public static DisplayOption show(String k1, TaskParameterValue... v1) {
        return DisplayOption.displayOption().show(k1, List.of(v1));
    }

    public static DisplayOption show(String k1, List<TaskParameterValue> v1) {
        return DisplayOption.displayOption().show(k1, v1);
    }

    public static DisplayOption show(String k1, List<TaskParameterValue> v1, String k2, List<TaskParameterValue> v2) {
        return DisplayOption.displayOption().show(k1, v1, k2, v2);
    }

    public static DisplayOption show(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3
    ) {
        return DisplayOption.displayOption().show(k1, v1, k2, v2, k3, v3);
    }

    public static DisplayOption show(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3,
        String k4,
        List<TaskParameterValue> v4
    ) {
        return DisplayOption.displayOption().show(k1, v1, k2, v2, k3, v3, k4, v4);
    }

    public static DisplayOption show(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3,
        String k4,
        List<TaskParameterValue> v4,
        String k5,
        List<TaskParameterValue> v5
    ) {
        return DisplayOption.displayOption().show(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
    }

    public static DisplayOption show(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3,
        String k4,
        List<TaskParameterValue> v4,
        String k5,
        List<TaskParameterValue> v5,
        String k6,
        List<TaskParameterValue> v6
    ) {
        return DisplayOption.displayOption().show(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
    }

    public static DisplayOption show(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3,
        String k4,
        List<TaskParameterValue> v4,
        String k5,
        List<TaskParameterValue> v5,
        String k6,
        List<TaskParameterValue> v6,
        String k7,
        List<TaskParameterValue> v7
    ) {
        return DisplayOption.displayOption().show(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
    }

    public static DisplayOption show(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3,
        String k4,
        List<TaskParameterValue> v4,
        String k5,
        List<TaskParameterValue> v5,
        String k6,
        List<TaskParameterValue> v6,
        String k7,
        List<TaskParameterValue> v7,
        String k8,
        List<TaskParameterValue> v8
    ) {
        return DisplayOption.displayOption().show(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
    }

    public static DisplayOption show(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3,
        String k4,
        List<TaskParameterValue> v4,
        String k5,
        List<TaskParameterValue> v5,
        String k6,
        List<TaskParameterValue> v6,
        String k7,
        List<TaskParameterValue> v7,
        String k8,
        List<TaskParameterValue> v8,
        String k9,
        List<TaskParameterValue> v9
    ) {
        return DisplayOption
            .displayOption()
            .show(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
    }

    public static DisplayOption show(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3,
        String k4,
        List<TaskParameterValue> v4,
        String k5,
        List<TaskParameterValue> v5,
        String k6,
        List<TaskParameterValue> v6,
        String k7,
        List<TaskParameterValue> v7,
        String k8,
        List<TaskParameterValue> v8,
        String k9,
        List<TaskParameterValue> v9,
        String k10,
        List<TaskParameterValue> v10
    ) {
        return DisplayOption
            .displayOption()
            .show(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
    }

    public static TaskPropertyTypeOption loadOptionsDependsOn(String... loadOptionsDependsOn) {
        return TaskPropertyTypeOption.propertyTypeOption().loadOptionsDependsOn(loadOptionsDependsOn);
    }

    public static TaskPropertyTypeOption loadOptionsMethod(String loadOptionsMethod) {
        return TaskPropertyTypeOption.propertyTypeOption().loadOptionsMethod(loadOptionsMethod);
    }

    public static TaskPropertyTypeOption maxValue(Double maxValue) {
        return TaskPropertyTypeOption.propertyTypeOption().maxValue(maxValue);
    }

    public static TaskPropertyTypeOption minValue(double minValue) {
        return TaskPropertyTypeOption.propertyTypeOption().minValue(minValue);
    }

    public static TaskPropertyTypeOption multipleValues(Boolean multipleValues) {
        return TaskPropertyTypeOption.propertyTypeOption().multipleValues(multipleValues);
    }

    public static TaskPropertyTypeOption multipleValueButtonText(String multipleValueButtonText) {
        return TaskPropertyTypeOption.propertyTypeOption().multipleValueButtonText(multipleValueButtonText);
    }

    public static TaskPropertyTypeOption numberPrecision(Integer numberPrecision) {
        return TaskPropertyTypeOption.propertyTypeOption().numberPrecision(numberPrecision);
    }

    public static List<TaskProperty<?>> properties(TaskProperty<?>... taskProperties) {
        return List.of(taskProperties);
    }

    @SuppressWarnings("unchecked")
    public T description(String description) {
        this.description = description;

        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T displayName(String displayName) {
        this.displayName = displayName;

        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T displayOption(DisplayOption displayOption) {
        this.displayOption = displayOption;

        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T typeOption(TaskPropertyTypeOption typeOption) {
        this.typeOption = typeOption;

        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T required(Boolean required) {
        this.required = required;

        return (T) this;
    }

    public TaskParameter getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public DisplayOption getDisplayOption() {
        return displayOption;
    }

    public String getName() {
        return name;
    }

    public TaskPropertyTypeOption getTypeOption() {
        return typeOption;
    }

    public Boolean isRequired() {
        return required;
    }

    public TaskPropertyType getType() {
        return type;
    }

    public static final class BinaryTaskProperty extends TaskProperty<BinaryTaskProperty> {

        public BinaryTaskProperty(String name) {
            this.name = name;
            this.type = TaskPropertyType.BINARY;
        }

        public BinaryTaskProperty defaultValue(JsonNode defaultValue) {
            this.defaultValue = parameter(defaultValue);

            return this;
        }
    }

    public static final class BooleanTaskProperty extends TaskProperty<BooleanTaskProperty> {

        public BooleanTaskProperty(String name) {
            this.name = name;
            this.type = TaskPropertyType.BOOLEAN;
        }

        public BooleanTaskProperty defaultValue(boolean defaultValue) {
            this.defaultValue = parameter(defaultValue);

            return this;
        }
    }

    public static final class ColorTaskProperty extends TaskProperty<ColorTaskProperty> {

        public ColorTaskProperty(String name) {
            this.name = name;
            this.type = TaskPropertyType.COLOR;
        }

        public ColorTaskProperty defaultValue(String defaultValue) {
            this.defaultValue = parameter(defaultValue);

            return this;
        }
    }

    public static final class DateTimeTaskProperty extends TaskProperty<DateTimeTaskProperty> {

        public DateTimeTaskProperty(String name) {
            this.name = name;
            this.type = TaskPropertyType.DATE_TIME;
        }

        public DateTimeTaskProperty defaultValue(LocalDateTime defaultValue) {
            this.defaultValue = parameter(defaultValue);

            return this;
        }
    }

    public static final class CollectionTaskProperty extends TaskProperty<CollectionTaskProperty> {

        private List<TaskProperty<?>> options;
        private String placeholder;

        public CollectionTaskProperty(String name) {
            this.name = name;
            this.type = TaskPropertyType.COLLECTION;
        }

        public CollectionTaskProperty defaultValue(int value) {
            defaultValue = parameter(value);

            return this;
        }

        public CollectionTaskProperty defaultValue(long value) {
            defaultValue = parameter(value);

            return this;
        }

        public CollectionTaskProperty defaultValue(float value) {
            defaultValue = parameter(value);

            return this;
        }

        public CollectionTaskProperty defaultValue(double value) {
            defaultValue = parameter(value);

            return this;
        }

        public CollectionTaskProperty defaultValue(String value) {
            defaultValue = parameter(value);

            return this;
        }

        public CollectionTaskProperty defaultValue(Boolean... values) {
            defaultValue = parameter(values);

            return this;
        }

        public CollectionTaskProperty defaultValue(Integer... values) {
            defaultValue = parameter(values);

            return this;
        }

        public CollectionTaskProperty defaultValue(Long... values) {
            defaultValue = parameter(values);

            return this;
        }

        public CollectionTaskProperty defaultValue(Float... values) {
            defaultValue = parameter(values);

            return this;
        }

        public CollectionTaskProperty defaultValue(Double... values) {
            defaultValue = parameter(values);

            return this;
        }

        public CollectionTaskProperty defaultValue(String... values) {
            defaultValue = parameter(values);

            return this;
        }

        public CollectionTaskProperty defaultValue(TaskParameter defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public CollectionTaskProperty defaultValue(TaskParameter... defaultValue) {
            this.defaultValue = parameter(defaultValue);

            return this;
        }

        public List<TaskProperty<?>> getOptions() {
            return options;
        }

        public String getPlaceholder() {
            return placeholder;
        }

        public CollectionTaskProperty options(TaskProperty<?>... options) {
            this.options = List.of(options);

            return this;
        }

        public CollectionTaskProperty placeholder(String placeholder) {
            this.placeholder = placeholder;

            return this;
        }
    }

    public static final class GroupTaskProperty extends TaskProperty<GroupTaskProperty> {

        private List<TaskProperty<?>> fields;

        public GroupTaskProperty(String name) {
            this.name = name;
            this.type = TaskPropertyType.COLLECTION;
        }

        public GroupTaskProperty fields(TaskProperty<?>... fields) {
            this.fields = List.of(fields);

            return this;
        }

        public List<TaskProperty<?>> getFields() {
            return fields;
        }
    }

    public static final class JSONTaskProperty extends TaskProperty<JSONTaskProperty> {

        public JSONTaskProperty(String name) {
            this.name = name;
            this.type = TaskPropertyType.JSON;
        }

        public JSONTaskProperty defaultValue(JsonNode defaultValue) {
            this.defaultValue = parameter(defaultValue);

            return this;
        }
    }

    public static final class MultiSelectTaskProperty extends TaskProperty<MultiSelectTaskProperty> {

        private List<TaskPropertyOption> options;

        public MultiSelectTaskProperty(String name) {
            this.name = name;
            this.type = TaskPropertyType.MULTI_SELECT;
        }

        public MultiSelectTaskProperty defaultValue(Integer... value) {
            this.defaultValue = parameter(value);

            return this;
        }

        public MultiSelectTaskProperty defaultValue(String... value) {
            this.defaultValue = parameter(value);

            return this;
        }

        public MultiSelectTaskProperty options(TaskPropertyOption... options) {
            this.options = List.of(options);

            return this;
        }

        public List<TaskPropertyOption> getOptions() {
            return options;
        }
    }

    public static final class NumberTaskProperty extends TaskProperty<NumberTaskProperty> {

        private String placeholder;

        public NumberTaskProperty(String name) {
            this.name = name;
            this.type = TaskPropertyType.NUMBER;
        }

        public NumberTaskProperty defaultValue(int value) {
            this.defaultValue = parameter(value);

            return this;
        }

        public NumberTaskProperty defaultValue(long value) {
            this.defaultValue = parameter(value);

            return this;
        }

        public NumberTaskProperty defaultValue(float value) {
            this.defaultValue = parameter(value);

            return this;
        }

        public NumberTaskProperty defaultValue(double value) {
            this.defaultValue = parameter(value);

            return this;
        }

        public NumberTaskProperty placeholder(String placeholder) {
            this.placeholder = placeholder;

            return this;
        }

        public String getPlaceholder() {
            return placeholder;
        }
    }

    public static final class SelectTaskProperty extends TaskProperty<SelectTaskProperty> {

        private List<TaskPropertyOption> options;
        private String placeholder;

        public SelectTaskProperty(String name) {
            this.name = name;
            this.type = TaskPropertyType.SELECT;
        }

        public SelectTaskProperty defaultValue(int defaultValue) {
            this.defaultValue = parameter(defaultValue);

            return this;
        }

        public SelectTaskProperty defaultValue(String defaultValue) {
            this.defaultValue = parameter(defaultValue);

            return this;
        }

        public SelectTaskProperty options(TaskPropertyOption... options) {
            this.options = List.of(options);

            return this;
        }

        public SelectTaskProperty placeholder(String placeholder) {
            this.placeholder = placeholder;

            return this;
        }

        public List<TaskPropertyOption> getOptions() {
            return options;
        }

        public String getPlaceholder() {
            return placeholder;
        }
    }

    public static final class StringTaskProperty extends TaskProperty<StringTaskProperty> {

        private String placeholder;

        public StringTaskProperty(String name) {
            this.name = name;
            this.type = TaskPropertyType.STRING;
        }

        public StringTaskProperty defaultValue(String value) {
            this.defaultValue = parameter(value);

            return this;
        }

        public StringTaskProperty placeholder(String placeholder) {
            this.placeholder = placeholder;

            return this;
        }

        public String getPlaceholder() {
            return placeholder;
        }
    }
}
