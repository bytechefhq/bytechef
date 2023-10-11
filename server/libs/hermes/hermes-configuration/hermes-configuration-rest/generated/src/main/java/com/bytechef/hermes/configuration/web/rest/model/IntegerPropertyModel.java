
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

package com.bytechef.hermes.configuration.web.rest.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.*;
import jakarta.annotation.Generated;

/**
 * An integer property type.
 */

@Schema(name = "IntegerProperty", description = "An integer property type.")

@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-10-06T20:36:49.034607+02:00[Europe/Zagreb]")
public class IntegerPropertyModel extends ValuePropertyModel {

    private Integer maxValue;

    private Integer minValue;

    @Valid
    private List<@Valid OptionModel> options;

    private OptionsDataSourceModel optionsDataSource;

    public IntegerPropertyModel() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public IntegerPropertyModel(PropertyTypeModel type) {
        super();
    }

    public IntegerPropertyModel maxValue(Integer maxValue) {
        this.maxValue = maxValue;
        return this;
    }

    /**
     * The maximum property value.
     * 
     * @return maxValue
     */

    @Schema(
        name = "maxValue", description = "The maximum property value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("maxValue")
    public Integer getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Integer maxValue) {
        this.maxValue = maxValue;
    }

    public IntegerPropertyModel minValue(Integer minValue) {
        this.minValue = minValue;
        return this;
    }

    /**
     * The minimum property value.
     * 
     * @return minValue
     */

    @Schema(
        name = "minValue", description = "The minimum property value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("minValue")
    public Integer getMinValue() {
        return minValue;
    }

    public void setMinValue(Integer minValue) {
        this.minValue = minValue;
    }

    public IntegerPropertyModel options(List<@Valid OptionModel> options) {
        this.options = options;
        return this;
    }

    public IntegerPropertyModel addOptionsItem(OptionModel optionsItem) {
        if (this.options == null) {
            this.options = new ArrayList<>();
        }
        this.options.add(optionsItem);
        return this;
    }

    /**
     * The list of valid property options.
     * 
     * @return options
     */
    @Valid
    @Schema(
        name = "options", description = "The list of valid property options.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("options")
    public List<@Valid OptionModel> getOptions() {
        return options;
    }

    public void setOptions(List<@Valid OptionModel> options) {
        this.options = options;
    }

    public IntegerPropertyModel optionsDataSource(OptionsDataSourceModel optionsDataSource) {
        this.optionsDataSource = optionsDataSource;
        return this;
    }

    /**
     * Get optionsDataSource
     * 
     * @return optionsDataSource
     */
    @Valid
    @Schema(name = "optionsDataSource", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("optionsDataSource")
    public OptionsDataSourceModel getOptionsDataSource() {
        return optionsDataSource;
    }

    public void setOptionsDataSource(OptionsDataSourceModel optionsDataSource) {
        this.optionsDataSource = optionsDataSource;
    }

    public IntegerPropertyModel controlType(ControlTypeModel controlType) {
        super.controlType(controlType);
        return this;
    }

    public IntegerPropertyModel defaultValue(Object defaultValue) {
        super.defaultValue(defaultValue);
        return this;
    }

    public IntegerPropertyModel exampleValue(Object exampleValue) {
        super.exampleValue(exampleValue);
        return this;
    }

    public IntegerPropertyModel advancedOption(Boolean advancedOption) {
        super.advancedOption(advancedOption);
        return this;
    }

    public IntegerPropertyModel description(String description) {
        super.description(description);
        return this;
    }

    public IntegerPropertyModel displayCondition(String displayCondition) {
        super.displayCondition(displayCondition);
        return this;
    }

    public IntegerPropertyModel expressionEnabled(Boolean expressionEnabled) {
        super.expressionEnabled(expressionEnabled);
        return this;
    }

    public IntegerPropertyModel hidden(Boolean hidden) {
        super.hidden(hidden);
        return this;
    }

    public IntegerPropertyModel label(String label) {
        super.label(label);
        return this;
    }

    public IntegerPropertyModel name(String name) {
        super.name(name);
        return this;
    }

    public IntegerPropertyModel placeholder(String placeholder) {
        super.placeholder(placeholder);
        return this;
    }

    public IntegerPropertyModel required(Boolean required) {
        super.required(required);
        return this;
    }

    public IntegerPropertyModel type(PropertyTypeModel type) {
        super.type(type);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IntegerPropertyModel integerProperty = (IntegerPropertyModel) o;
        return Objects.equals(this.maxValue, integerProperty.maxValue) &&
            Objects.equals(this.minValue, integerProperty.minValue) &&
            Objects.equals(this.options, integerProperty.options) &&
            Objects.equals(this.optionsDataSource, integerProperty.optionsDataSource) &&
            super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxValue, minValue, options, optionsDataSource, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class IntegerPropertyModel {\n");
        sb.append("    ")
            .append(toIndentedString(super.toString()))
            .append("\n");
        sb.append("    maxValue: ")
            .append(toIndentedString(maxValue))
            .append("\n");
        sb.append("    minValue: ")
            .append(toIndentedString(minValue))
            .append("\n");
        sb.append("    options: ")
            .append(toIndentedString(options))
            .append("\n");
        sb.append("    optionsDataSource: ")
            .append(toIndentedString(optionsDataSource))
            .append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString()
            .replace("\n", "\n    ");
    }
}
