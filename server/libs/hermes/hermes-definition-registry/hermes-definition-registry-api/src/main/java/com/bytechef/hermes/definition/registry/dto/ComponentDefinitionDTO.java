
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

package com.bytechef.hermes.definition.registry.dto;

import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ComponentDefinitionDTO(
    List<ActionDefinitionBasicDTO> actions, ConnectionDefinitionBasicDTO connection, Display display, String name,
    Resources resources, List<TriggerDefinitionBasicDTO> triggers, int version) {

    public ComponentDefinitionDTO(String name) {
        this(null, null, null, name, null, null, 0);
    }
}
