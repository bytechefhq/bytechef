
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

package com.bytechef.hermes.component.web.rest.model;

import com.bytechef.hermes.definition.Display;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "GetComponentDefinitionsRequestModel",
    description = "A component contains a set of reusable code(actions) that accomplish specific tasks, triggers(TODO) and connections if there is a need for a connection to an outside service.")
public record GetComponentDefinitionsRequestModel(
    @Schema(name = "name", description = "The component name.") String name, Display display, int version) {
}
