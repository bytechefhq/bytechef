
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
import java.util.function.Function;

/**
 * @author Ivica Cardic
 */
@JsonDeserialize(as = DefinitionDSL.ModifiablePropertyOptionDataSource.class)
@Schema(
    name = "PropertyOptionDataSource",
    description = "Defines function that should dynamically load options for the property.")
public sealed interface PropertyOptionDataSource permits DefinitionDSL.ModifiablePropertyOptionDataSource {

    @Schema(
        name = "loadOptionsDependsOn",
        description = "The list of property names on which value change the property options should load/reload.")
    List<String> getLoadOptionsDependsOn();

    /**
     * The function that should dynamically load options for the property.
     *
     * @return a load options function implementation
     */
    Function<Object, Object> getLoadOptionsFunction();
}
