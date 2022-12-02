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

package com.bytechef.hermes.definition;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@Schema(name = "PropertyOption", description = "Defines valid property value.")
public final class PropertyOption {

    private String description;
    private DisplayOption displayOption;
    private String name;
    private Object value;

    private PropertyOption() {}

    public PropertyOption(String name, boolean value, String description) {
        this.name = name;
        this.value = value;
        this.description = description;
    }

    public PropertyOption(String name, int value, String description) {
        this.name = name;
        this.value = value;
        this.description = description;
    }

    public PropertyOption(String name, LocalDate value, String description) {
        this.name = name;
        this.value = value;
        this.description = description;
    }

    public PropertyOption(String name, LocalDateTime value, String description) {
        this.name = name;
        this.value = value;
        this.description = description;
    }

    public PropertyOption(String name, Object value, String description) {
        this.name = name;
        this.value = value;
        this.description = description;
    }

    public PropertyOption(String name, String value, String description) {
        this.name = name;
        this.value = value;
        this.description = description;
    }

    public PropertyOption description(String description) {
        this.description = description;

        return this;
    }

    public PropertyOption displayOption(DisplayOption.DisplayOptionCondition... displayOptionEntries) {
        this.displayOption = DisplayOption.of(List.of(displayOptionEntries));

        return this;
    }

    @Schema(name = "description", description = "Description of the option.")
    public String getDescription() {
        return description;
    }

    public DisplayOption getDisplayOption() {
        return displayOption;
    }

    @Schema(name = "name", description = "Name of the option.")
    public String getName() {
        return name;
    }

    @Schema(name = "value", description = "Value of the option")
    public Object getValue() {
        return value;
    }
}
