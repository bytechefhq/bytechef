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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public class DisplayOption {

    private Map<String, List<TaskParameterValue>> hide;
    private Map<String, List<TaskParameterValue>> show;

    private DisplayOption() {}

    public static DisplayOption displayOption() {
        return new DisplayOption();
    }

    public DisplayOption hide(String k1) {
        hide = Map.of(k1, List.of());

        return this;
    }

    public DisplayOption hide(String k1, Boolean... values) {
        this.hide = Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));

        return this;
    }

    public DisplayOption hide(String k1, Integer... values) {
        this.hide = Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));

        return this;
    }

    public DisplayOption hide(String k1, Long... values) {
        this.hide = Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));

        return this;
    }

    public DisplayOption hide(String k1, Float... values) {
        this.hide = Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));

        return this;
    }

    public DisplayOption hide(String k1, Double... values) {
        this.hide = Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));

        return this;
    }

    public DisplayOption hide(String k1, String... values) {
        this.hide = Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));

        return this;
    }

    public DisplayOption hide(String k1, List<TaskParameterValue> v1) {
        this.hide = Map.of(k1, v1);

        return this;
    }

    public DisplayOption hide(String k1, List<TaskParameterValue> v1, String k2, List<TaskParameterValue> v2) {
        this.hide = Map.of(k1, v1, k2, v2);

        return this;
    }

    public DisplayOption hide(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3
    ) {
        this.hide = Map.of(k1, v1, k2, v2, k3, v3);

        return this;
    }

    public DisplayOption hide(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3,
        String k4,
        List<TaskParameterValue> v4
    ) {
        this.hide = Map.of(k1, v1, k2, v2, k3, v3, k4, v4);

        return this;
    }

    public DisplayOption hide(
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
        this.hide = Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);

        return this;
    }

    public DisplayOption hide(
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
        this.hide = Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);

        return this;
    }

    public DisplayOption hide(
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
        this.hide = Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);

        return this;
    }

    public DisplayOption hide(
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
        this.hide = Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);

        return this;
    }

    public DisplayOption hide(
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
        this.hide = Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);

        return this;
    }

    public DisplayOption hide(
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
        this.hide = Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);

        return this;
    }

    public DisplayOption show(String k1) {
        show = Map.of(k1, List.of());

        return this;
    }

    public DisplayOption show(String k1, Boolean... values) {
        this.show = Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));

        return this;
    }

    public DisplayOption show(String k1, Integer... values) {
        this.show = Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));

        return this;
    }

    public DisplayOption show(String k1, Long... values) {
        this.show = Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));

        return this;
    }

    public DisplayOption show(String k1, Float... values) {
        this.show = Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));

        return this;
    }

    public DisplayOption show(String k1, Double... values) {
        this.show = Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));

        return this;
    }

    public DisplayOption show(String k1, String... values) {
        this.show = Map.of(k1, Stream.of(values).map(TaskParameterValue::parameterValue).collect(Collectors.toList()));

        return this;
    }

    public DisplayOption show(String k1, TaskParameterValue... v1) {
        this.show = Map.of(k1, List.of(v1));

        return this;
    }

    public DisplayOption show(String k1, List<TaskParameterValue> v1) {
        this.show = Map.of(k1, v1);

        return this;
    }

    public DisplayOption show(String k1, List<TaskParameterValue> v1, String k2, List<TaskParameterValue> v2) {
        this.show = Map.of(k1, v1, k2, v2);

        return this;
    }

    public DisplayOption show(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3
    ) {
        this.show = Map.of(k1, v1, k2, v2, k3, v3);

        return this;
    }

    public DisplayOption show(
        String k1,
        List<TaskParameterValue> v1,
        String k2,
        List<TaskParameterValue> v2,
        String k3,
        List<TaskParameterValue> v3,
        String k4,
        List<TaskParameterValue> v4
    ) {
        this.show = Map.of(k1, v1, k2, v2, k3, v3, k4, v4);

        return this;
    }

    public DisplayOption show(
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
        this.show = Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);

        return this;
    }

    public DisplayOption show(
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
        this.show = Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);

        return this;
    }

    public DisplayOption show(
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
        this.show = Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);

        return this;
    }

    public DisplayOption show(
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
        this.show = Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);

        return this;
    }

    public DisplayOption show(
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
        this.show = Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);

        return this;
    }

    public DisplayOption show(
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
        this.show = Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);

        return this;
    }

    public Map<String, List<TaskParameterValue>> getHide() {
        return hide;
    }

    public Map<String, List<TaskParameterValue>> getShow() {
        return show;
    }
}
