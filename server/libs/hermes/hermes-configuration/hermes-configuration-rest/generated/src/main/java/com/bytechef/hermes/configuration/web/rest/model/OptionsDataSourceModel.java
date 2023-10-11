
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
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.*;
import jakarta.annotation.Generated;

/**
 * Defines function that should dynamically load options for the property.
 */

@Schema(
    name = "OptionsDataSource", description = "Defines function that should dynamically load options for the property.")
@JsonTypeName("OptionsDataSource")
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-10-06T20:36:49.034607+02:00[Europe/Zagreb]")
public class OptionsDataSourceModel {

    @Valid
    private List<String> loadOptionsDependsOn;

    public OptionsDataSourceModel loadOptionsDependsOn(List<String> loadOptionsDependsOn) {
        this.loadOptionsDependsOn = loadOptionsDependsOn;
        return this;
    }

    public OptionsDataSourceModel addLoadOptionsDependsOnItem(String loadOptionsDependsOnItem) {
        if (this.loadOptionsDependsOn == null) {
            this.loadOptionsDependsOn = new ArrayList<>();
        }
        this.loadOptionsDependsOn.add(loadOptionsDependsOnItem);
        return this;
    }

    /**
     * The list of property names on which value change the property options should load/reload.
     * 
     * @return loadOptionsDependsOn
     */

    @Schema(
        name = "loadOptionsDependsOn",
        description = "The list of property names on which value change the property options should load/reload.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("loadOptionsDependsOn")
    public List<String> getLoadOptionsDependsOn() {
        return loadOptionsDependsOn;
    }

    public void setLoadOptionsDependsOn(List<String> loadOptionsDependsOn) {
        this.loadOptionsDependsOn = loadOptionsDependsOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OptionsDataSourceModel optionsDataSource = (OptionsDataSourceModel) o;
        return Objects.equals(this.loadOptionsDependsOn, optionsDataSource.loadOptionsDependsOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loadOptionsDependsOn);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class OptionsDataSourceModel {\n");
        sb.append("    loadOptionsDependsOn: ")
            .append(toIndentedString(loadOptionsDependsOn))
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
