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

/**
 * @author Ivica Cardic
 */
@Schema(name = "Display", description = "Info about the component.")
public class Display {

    protected String description;
    protected String icon;
    protected String label;

    public Display(String label) {
        this.label = label;
    }

    public Display description(String description) {
        this.description = description;

        return this;
    }

    public Display icon(String icon) {
        this.icon = icon;

        return this;
    }

    @Schema(name = "description", description = "The component description.")
    public String getDescription() {
        return description;
    }

    @Schema(name = "icon", description = " The component icon.")
    public String getIcon() {
        return icon;
    }

    @Schema(name = "label", description = " The component label.")
    public String getLabel() {
        return label;
    }
}
