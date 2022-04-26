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

/**
 * @author Ivica Cardic
 */
public class TaskPropertyTypeOption {

    private List<String> loadOptionsDependsOn;
    private String loadOptionsMethod;
    private Integer maxValue;
    private Integer minValue;
    private Boolean multipleValues;
    private String multipleValueButtonText;
    private Integer numberPrecision;

    private TaskPropertyTypeOption() {}

    static TaskPropertyTypeOption propertyTypeOption() {
        return new TaskPropertyTypeOption();
    }

    public TaskPropertyTypeOption loadOptionsDependsOn(String... loadOptionsDependsOn) {
        this.loadOptionsDependsOn = List.of(loadOptionsDependsOn);

        return this;
    }

    public TaskPropertyTypeOption loadOptionsMethod(String loadOptionsMethod) {
        this.loadOptionsMethod = loadOptionsMethod;

        return this;
    }

    public TaskPropertyTypeOption maxValue(int maxValue) {
        this.maxValue = maxValue;

        return this;
    }

    public TaskPropertyTypeOption minValue(int minValue) {
        this.minValue = minValue;

        return this;
    }

    public TaskPropertyTypeOption multipleValues(Boolean multipleValues) {
        this.multipleValues = multipleValues;

        return this;
    }

    public TaskPropertyTypeOption multipleValueButtonText(String multipleValueButtonText) {
        this.multipleValueButtonText = multipleValueButtonText;

        return this;
    }

    public TaskPropertyTypeOption numberPrecision(Integer numberPrecision) {
        this.numberPrecision = numberPrecision;

        return this;
    }

    public List<String> getLoadOptionsDependsOn() {
        return loadOptionsDependsOn;
    }

    public String getLoadOptionsMethod() {
        return loadOptionsMethod;
    }

    public Integer getMaxValue() {
        return maxValue;
    }

    public Integer getMinValue() {
        return minValue;
    }

    public Boolean getMultipleValues() {
        return multipleValues;
    }

    public String getMultipleValueButtonText() {
        return multipleValueButtonText;
    }

    public Integer getNumberPrecision() {
        return numberPrecision;
    }
}
