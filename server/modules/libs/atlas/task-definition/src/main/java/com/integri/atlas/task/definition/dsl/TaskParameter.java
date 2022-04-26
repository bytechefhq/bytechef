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

package com.integri.atlas.task.definition.dsl;

import static com.integri.atlas.task.definition.dsl.TaskParameterValue.parameterValue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public sealed interface TaskParameter
    permits
        TaskParameter.TaskParameterMap,
        TaskParameter.TaskParameterList,
        TaskParameter.TaskParameterListMap,
        TaskParameterValue {
    static TaskParameter parameter(boolean value) {
        return parameterValue(value);
    }

    static TaskParameter parameter(double value) {
        return parameterValue(value);
    }

    static TaskParameter parameter(float value) {
        return parameterValue(value);
    }

    static TaskParameter parameter(int value) {
        return parameterValue(value);
    }

    static TaskParameter parameter(long value) {
        return parameterValue(value);
    }

    static TaskParameter parameter(LocalDateTime value) {
        return parameterValue(value);
    }

    static TaskParameter parameter(String value) {
        return parameterValue(value);
    }

    static TaskParameter parameter(String k1, boolean value) {
        return parameter(k1, parameterValue(value));
    }

    static TaskParameterListMap parameter(String k1, Boolean... values) {
        return new TaskParameterListMap(
            Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()))
        );
    }

    static TaskParameter parameter(String k1, int value) {
        return parameter(k1, parameterValue(value));
    }

    static TaskParameterListMap parameter(String k1, Integer... values) {
        return new TaskParameterListMap(
            Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()))
        );
    }

    static TaskParameter parameter(String k1, long value) {
        return parameter(k1, parameterValue(value));
    }

    static TaskParameterListMap parameter(String k1, Long... values) {
        return new TaskParameterListMap(
            Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()))
        );
    }

    static TaskParameter parameter(String k1, float value) {
        return parameter(k1, parameterValue(value));
    }

    static TaskParameterListMap parameter(String k1, Float... values) {
        return new TaskParameterListMap(
            Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()))
        );
    }

    static TaskParameter parameter(String k1, double value) {
        return parameter(k1, parameterValue(value));
    }

    static TaskParameterListMap parameter(String k1, Double... values) {
        return new TaskParameterListMap(
            Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()))
        );
    }

    static TaskParameterMap parameter(String k1, String value) {
        return parameter(k1, parameterValue(value));
    }

    static TaskParameterListMap parameter(String k1, String... values) {
        return new TaskParameterListMap(
            Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()))
        );
    }

    static TaskParameterMap parameter(String k1, TaskParameter v1) {
        return new TaskParameterMap(Map.of(k1, v1));
    }

    static TaskParameterListMap parameter(String k1, List<TaskParameter> v1) {
        return new TaskParameterListMap(Map.of(k1, v1));
    }

    static TaskParameterMap parameter(String k1, TaskParameter v1, String k2, TaskParameter v2) {
        return new TaskParameterMap(Map.of(k1, v1, k2, v2));
    }

    static TaskParameterListMap parameter(String k1, List<TaskParameter> v1, String k2, List<TaskParameter> v2) {
        return new TaskParameterListMap(Map.of(k1, v1, k2, v2));
    }

    static TaskParameterMap parameter(
        String k1,
        TaskParameter v1,
        String k2,
        TaskParameter v2,
        String k3,
        TaskParameter v3
    ) {
        return new TaskParameterMap(Map.of(k1, v1, k2, v2, k3, v3));
    }

    static TaskParameterListMap parameter(
        String k1,
        List<TaskParameter> v1,
        String k2,
        List<TaskParameter> v2,
        String k3,
        List<TaskParameter> v3
    ) {
        return new TaskParameterListMap(Map.of(k1, v1, k2, v2, k3, v3));
    }

    static TaskParameterMap parameter(
        String k1,
        TaskParameter v1,
        String k2,
        TaskParameter v2,
        String k3,
        TaskParameter v3,
        String k4,
        TaskParameter v4
    ) {
        return new TaskParameterMap(Map.of(k1, v1, k2, v2, k3, v3, k4, v4));
    }

    static TaskParameterListMap parameter(
        String k1,
        List<TaskParameter> v1,
        String k2,
        List<TaskParameter> v2,
        String k3,
        List<TaskParameter> v3,
        String k4,
        List<TaskParameter> v4
    ) {
        return new TaskParameterListMap(Map.of(k1, v1, k2, v2, k3, v3, k4, v4));
    }

    static TaskParameterMap parameter(
        String k1,
        TaskParameter v1,
        String k2,
        TaskParameter v2,
        String k3,
        TaskParameter v3,
        String k4,
        TaskParameter v4,
        String k5,
        TaskParameter v5
    ) {
        return new TaskParameterMap(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5));
    }

    static TaskParameterListMap parameter(
        String k1,
        List<TaskParameter> v1,
        String k2,
        List<TaskParameter> v2,
        String k3,
        List<TaskParameter> v3,
        String k4,
        List<TaskParameter> v4,
        String k5,
        List<TaskParameter> v5
    ) {
        return new TaskParameterListMap(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5));
    }

    static TaskParameterMap parameter(
        String k1,
        TaskParameter v1,
        String k2,
        TaskParameter v2,
        String k3,
        TaskParameter v3,
        String k4,
        TaskParameter v4,
        String k5,
        TaskParameter v5,
        String k6,
        TaskParameter v6
    ) {
        return new TaskParameterMap(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6));
    }

    static TaskParameterListMap parameter(
        String k1,
        List<TaskParameter> v1,
        String k2,
        List<TaskParameter> v2,
        String k3,
        List<TaskParameter> v3,
        String k4,
        List<TaskParameter> v4,
        String k5,
        List<TaskParameter> v5,
        String k6,
        List<TaskParameter> v6
    ) {
        return new TaskParameterListMap(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6));
    }

    static TaskParameterMap parameter(
        String k1,
        TaskParameter v1,
        String k2,
        TaskParameter v2,
        String k3,
        TaskParameter v3,
        String k4,
        TaskParameter v4,
        String k5,
        TaskParameter v5,
        String k6,
        TaskParameter v6,
        String k7,
        TaskParameter v7
    ) {
        return new TaskParameterMap(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7));
    }

    static TaskParameterListMap parameter(
        String k1,
        List<TaskParameter> v1,
        String k2,
        List<TaskParameter> v2,
        String k3,
        List<TaskParameter> v3,
        String k4,
        List<TaskParameter> v4,
        String k5,
        List<TaskParameter> v5,
        String k6,
        List<TaskParameter> v6,
        String k7,
        List<TaskParameter> v7
    ) {
        return new TaskParameterListMap(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7));
    }

    static TaskParameterMap parameter(
        String k1,
        TaskParameter v1,
        String k2,
        TaskParameter v2,
        String k3,
        TaskParameter v3,
        String k4,
        TaskParameter v4,
        String k5,
        TaskParameter v5,
        String k6,
        TaskParameter v6,
        String k7,
        TaskParameter v7,
        String k8,
        TaskParameter v8
    ) {
        return new TaskParameterMap(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8));
    }

    static TaskParameterListMap parameter(
        String k1,
        List<TaskParameter> v1,
        String k2,
        List<TaskParameter> v2,
        String k3,
        List<TaskParameter> v3,
        String k4,
        List<TaskParameter> v4,
        String k5,
        List<TaskParameter> v5,
        String k6,
        List<TaskParameter> v6,
        String k7,
        List<TaskParameter> v7,
        String k8,
        List<TaskParameter> v8
    ) {
        return new TaskParameterListMap(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8));
    }

    static TaskParameterMap parameter(
        String k1,
        TaskParameter v1,
        String k2,
        TaskParameter v2,
        String k3,
        TaskParameter v3,
        String k4,
        TaskParameter v4,
        String k5,
        TaskParameter v5,
        String k6,
        TaskParameter v6,
        String k7,
        TaskParameter v7,
        String k8,
        TaskParameter v8,
        String k9,
        TaskParameter v9
    ) {
        return new TaskParameterMap(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9));
    }

    static TaskParameterListMap parameter(
        String k1,
        List<TaskParameter> v1,
        String k2,
        List<TaskParameter> v2,
        String k3,
        List<TaskParameter> v3,
        String k4,
        List<TaskParameter> v4,
        String k5,
        List<TaskParameter> v5,
        String k6,
        List<TaskParameter> v6,
        String k7,
        List<TaskParameter> v7,
        String k8,
        List<TaskParameter> v8,
        String k9,
        List<TaskParameter> v9
    ) {
        return new TaskParameterListMap(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9));
    }

    static TaskParameterMap parameter(
        String k1,
        TaskParameter v1,
        String k2,
        TaskParameter v2,
        String k3,
        TaskParameter v3,
        String k4,
        TaskParameter v4,
        String k5,
        TaskParameter v5,
        String k6,
        TaskParameter v6,
        String k7,
        TaskParameter v7,
        String k8,
        TaskParameter v8,
        String k9,
        TaskParameter v9,
        String k10,
        TaskParameter v10
    ) {
        return new TaskParameterMap(
            Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10)
        );
    }

    static TaskParameterListMap parameter(
        String k1,
        List<TaskParameter> v1,
        String k2,
        List<TaskParameter> v2,
        String k3,
        List<TaskParameter> v3,
        String k4,
        List<TaskParameter> v4,
        String k5,
        List<TaskParameter> v5,
        String k6,
        List<TaskParameter> v6,
        String k7,
        List<TaskParameter> v7,
        String k8,
        List<TaskParameter> v8,
        String k9,
        List<TaskParameter> v9,
        String k10,
        List<TaskParameter> v10
    ) {
        return new TaskParameterListMap(
            Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10)
        );
    }

    static TaskParameterListMap parameter(String k1, TaskParameter... taskParameters) {
        return new TaskParameterListMap(Map.of(k1, List.of(taskParameters)));
    }

    static TaskParameterList parameter(TaskParameter... taskParameters) {
        return new TaskParameterList(List.of(taskParameters));
    }

    static TaskParameter parameter(TaskParameterMap value) {
        return value;
    }

    static TaskParameterList parameters(Boolean... values) {
        return new TaskParameterList(
            Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList())
        );
    }

    static TaskParameterList parameters(TaskParameterMap... values) {
        return new TaskParameterList(Stream.of(values).collect(Collectors.toList()));
    }

    static TaskParameterList parameters(Integer... values) {
        return new TaskParameterList(
            Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList())
        );
    }

    static TaskParameterList parameters(LocalDateTime... values) {
        return new TaskParameterList(
            Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList())
        );
    }

    static TaskParameterList parameters(Long... values) {
        return new TaskParameterList(
            Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList())
        );
    }

    static TaskParameterList parameters(Float... values) {
        return new TaskParameterList(
            Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList())
        );
    }

    static TaskParameterList parameters(Double... values) {
        return new TaskParameterList(
            Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList())
        );
    }

    static TaskParameterList parameters(String... values) {
        return new TaskParameterList(
            Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList())
        );
    }

    final class TaskParameterList extends ArrayList<TaskParameter> implements TaskParameter {

        private TaskParameterList(Collection<? extends TaskParameter> c) {
            super(c);
        }
    }

    final class TaskParameterMap extends HashMap<String, TaskParameter> implements TaskParameter {

        private TaskParameterMap(Map<? extends String, ? extends TaskParameter> m) {
            super(m);
        }
    }

    final class TaskParameterListMap extends HashMap<String, List<TaskParameter>> implements TaskParameter {

        private TaskParameterListMap(Map<? extends String, ? extends List<TaskParameter>> m) {
            super(m);
        }
    }
}
