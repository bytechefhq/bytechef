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
@Schema(name = "Display", description = "A display information.")
public sealed class Display permits DefinitionDSL.ModifiableDisplay {

    protected String category;
    protected String description;
    protected String icon;
    protected String label;
    protected String subtitle;
    protected String[] tags;

    protected Display(String label) {
        this.label = label;
    }

    @Schema(name = "category", description = "The category of the component.")
    public String getCategory() {
        return category;
    }

    @Schema(name = "description", description = "The description.")
    public String getDescription() {
        return description;
    }

    @Schema(name = "icon", description = " The icon.")
    public String getIcon() {
        return icon;
    }

    @Schema(name = "label", description = "The label.")
    public String getLabel() {
        return label;
    }

    @Schema(name = "subtitle", description = "Additional explanation.")
    public String getSubtitle() {
        return subtitle;
    }

    @Schema(name = "tags", description = "Tags for the component.")
    public String[] getTags() {
        return tags;
    }
}
