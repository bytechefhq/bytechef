
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
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.*;
import jakarta.annotation.Generated;

/**
 * An any of property type.
 */

@Schema(name = "AnyProperty", description = "An any of property type.")

@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-10-06T20:36:49.034607+02:00[Europe/Zagreb]")
public class AnyPropertyModel extends ValuePropertyModel {

    public AnyPropertyModel controlType(ControlTypeModel controlType) {
        super.controlType(controlType);
        return this;
    }

    public AnyPropertyModel defaultValue(Object defaultValue) {
        super.defaultValue(defaultValue);
        return this;
    }

    public AnyPropertyModel exampleValue(Object exampleValue) {
        super.exampleValue(exampleValue);
        return this;
    }

    public AnyPropertyModel advancedOption(Boolean advancedOption) {
        super.advancedOption(advancedOption);
        return this;
    }

    public AnyPropertyModel description(String description) {
        super.description(description);
        return this;
    }

    public AnyPropertyModel displayCondition(String displayCondition) {
        super.displayCondition(displayCondition);
        return this;
    }

    public AnyPropertyModel expressionEnabled(Boolean expressionEnabled) {
        super.expressionEnabled(expressionEnabled);
        return this;
    }

    public AnyPropertyModel hidden(Boolean hidden) {
        super.hidden(hidden);
        return this;
    }

    public AnyPropertyModel label(String label) {
        super.label(label);
        return this;
    }

    public AnyPropertyModel name(String name) {
        super.name(name);
        return this;
    }

    public AnyPropertyModel placeholder(String placeholder) {
        super.placeholder(placeholder);
        return this;
    }

    public AnyPropertyModel required(Boolean required) {
        super.required(required);
        return this;
    }

    public AnyPropertyModel type(PropertyTypeModel type) {
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
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class AnyPropertyModel {\n");
        sb.append("    ")
            .append(toIndentedString(super.toString()))
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
