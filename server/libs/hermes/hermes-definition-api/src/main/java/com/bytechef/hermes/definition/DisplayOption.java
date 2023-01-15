
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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@JsonDeserialize(as = DefinitionDSL.ModifiableDisplayOption.class)
@Schema(name = "DisplayOption", description = "Defines rules when a property should be shown or hidden.")
public sealed interface DisplayOption permits DefinitionDSL.ModifiableDisplayOption {

    @Schema(
        name = "hide",
        description = "The map of property names and list of values to check against if the property should be hidden.")
    Map<String, List<Object>> getHide();

    @Schema(
        name = "show",
        description = "The map of property names and list of values to check against if the property should be shown.")
    Map<String, List<Object>> getShow();
}
