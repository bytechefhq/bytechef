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
import static com.integri.atlas.engine.core.task.description.TaskParameterValue.parameterValue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public final class TaskProperty implements TaskPropertyOption {

    private TaskParameter defaultValue;
    private String description;
    private String displayName;
    private DisplayOption displayOption;
    private String name;
    private List<TaskPropertyOption> propertyOptions;
    private String placeholder;
    private Boolean required;
    private TaskPropertyType type;
    private TaskPropertyTypeOption propertyTypeOption;

    private TaskProperty() {}

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

    public static TaskProperty property() {
        return new TaskProperty();
    }

    public static List<TaskProperty> properties(TaskProperty... taskProperties) {
        return List.of(taskProperties);
    }

    public TaskProperty defaultValue(int value) {
        defaultValue = parameterValue(value);

        return this;
    }

    public TaskProperty defaultValue(long value) {
        defaultValue = parameterValue(value);

        return this;
    }

    public TaskProperty defaultValue(float value) {
        defaultValue = parameterValue(value);

        return this;
    }

    public TaskProperty defaultValue(double value) {
        defaultValue = parameterValue(value);

        return this;
    }

    public TaskProperty defaultValue(String value) {
        defaultValue = parameterValue(value);

        return this;
    }

    public TaskProperty defaultValue(Boolean... values) {
        defaultValue = parameter(values);

        return this;
    }

    public TaskProperty defaultValue(Integer... values) {
        defaultValue = parameter(values);

        return this;
    }

    public TaskProperty defaultValue(Long... values) {
        defaultValue = parameter(values);

        return this;
    }

    public TaskProperty defaultValue(Float... values) {
        defaultValue = parameter(values);

        return this;
    }

    public TaskProperty defaultValue(Double... values) {
        defaultValue = parameter(values);

        return this;
    }

    public TaskProperty defaultValue(String... values) {
        defaultValue = parameter(values);

        return this;
    }

    public TaskProperty defaultValue(TaskParameter defaultValue) {
        this.defaultValue = defaultValue;

        return this;
    }

    public TaskProperty defaultValue(TaskParameter... defaultValue) {
        this.defaultValue = parameter(defaultValue);

        return this;
    }

    public TaskProperty description(String description) {
        this.description = description;

        return this;
    }

    public TaskProperty displayName(String displayName) {
        this.displayName = displayName;

        return this;
    }

    public TaskProperty displayOption(DisplayOption displayOption) {
        this.displayOption = displayOption;

        return this;
    }

    public TaskProperty name(String name) {
        this.name = name;

        return this;
    }

    public TaskProperty placeholder(String placeholder) {
        this.placeholder = placeholder;

        return this;
    }

    public TaskProperty propertyOptions(TaskPropertyOption... options) {
        this.propertyOptions = List.of(options);

        return this;
    }

    public TaskProperty propertyTypeOption(TaskPropertyTypeOption typeOption) {
        this.propertyTypeOption = typeOption;

        return this;
    }

    public TaskProperty required(Boolean required) {
        this.required = required;

        return this;
    }

    public TaskProperty type(TaskPropertyType type) {
        this.type = type;

        return this;
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

    public List<TaskPropertyOption> getPropertyOptions() {
        return propertyOptions;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public TaskPropertyTypeOption getPropertyTypeOption() {
        return propertyTypeOption;
    }

    public Boolean isRequired() {
        return required;
    }

    public TaskPropertyType getType() {
        return type;
    }
}
