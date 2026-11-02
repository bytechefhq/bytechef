/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.workflow.validator.model;

import com.bytechef.platform.domain.BaseOption;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Property information record for the response.
 */
@SuppressFBWarnings("EI")
public record PropertyInfo(
    @JsonProperty("name") @JsonPropertyDescription("The name of the property") String name,
    @JsonProperty("type") @JsonPropertyDescription("The type of the property") String type,
    @JsonProperty("description") @JsonPropertyDescription("The description of the property") String description,
    @JsonProperty("required") @JsonPropertyDescription("Whether the property is required") boolean required,
    @JsonProperty("expressionEnabled") @JsonPropertyDescription("Whether expressions are enabled for this property") boolean expressionEnabled,
    @JsonProperty("displayCondition") @JsonPropertyDescription("The display condition for the property") String displayCondition,
    @JsonProperty("options") @JsonPropertyDescription("Available options for the property") List<String> options,
    @JsonProperty("nestedProperties") @JsonPropertyDescription("Nested properties for object/array/file_entry types") List<PropertyInfo> nestedProperties,
    @JsonProperty("resourceType") @JsonPropertyDescription("The internal resource type the property value references, if any") String resourceType,
    @JsonIgnore @Nullable List<Object> optionValues) {

    public PropertyInfo(
        String name, String type, String description, boolean required, boolean expressionEnabled,
        String displayCondition, List<String> options, List<PropertyInfo> nestedProperties, String resourceType) {

        this(
            name, type, description, required, expressionEnabled, displayCondition, options, nestedProperties,
            resourceType, null);
    }

    public PropertyInfo(
        String name, String type, String description, boolean required, boolean expressionEnabled,
        String displayCondition, List<PropertyInfo> nestedProperties) {

        this(name, type, description, required, expressionEnabled, displayCondition, null, nestedProperties, null,
            null);
    }

    public PropertyInfo(
        String name, String type, String description, boolean required, boolean expressionEnabled,
        String displayCondition, List<String> options, List<PropertyInfo> nestedProperties) {

        this(
            name, type, description, required, expressionEnabled, displayCondition, options, nestedProperties, null,
            null);
    }

    /**
     * Returns the values of a property's fixed options, or {@code null} when the property has no options or its options
     * come from an options data source and therefore cannot be checked offline.
     */
    public static @Nullable List<Object> toFixedOptionValues(
        @Nullable List<? extends BaseOption> options, boolean optionsDataSourcePresent) {

        if (optionsDataSourcePresent || options == null || options.isEmpty()) {
            return null;
        }

        return options.stream()
            .map(BaseOption::getValue)
            .toList();
    }
}
