
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
@Schema(name = "PropertyOption", description = "Defines valid property value.")
public sealed interface PropertyOption permits DefinitionDSL.ModifiablePropertyOption {

    @Schema(name = "description", description = "Description of the option.")
    String getDescription();

    DisplayOption getDisplayOption();

    @Schema(name = "name", description = "Name of the option.")
    String getName();

    @Schema(name = "value", description = "Value of the option")
    Object getValue();
}
