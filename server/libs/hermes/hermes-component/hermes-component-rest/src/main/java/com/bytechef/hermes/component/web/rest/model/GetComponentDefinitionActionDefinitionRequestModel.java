
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
    name = "GetComponentDefinitionActionDefinitionRequestModel",
    description = "An action is a a portion of reusable code that accomplish a specific task. When building a workflow, each action is represented as a task inside the workflow. The task 'type' property is defined as [component name]/v[component version]/[action name]. Action properties are used to set properties of the task inside the workflow.")
public record GetComponentDefinitionActionDefinitionRequestModel(
    @Schema(name = "name", description = "The action name.") String name, Display display) {
}
