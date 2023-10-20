
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

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record TriggerDefinitionBasicDTO(
    boolean batch, String description, Optional<HelpDTO> help, String name, String title, TriggerType type) {

    public TriggerDefinitionBasicDTO(TriggerDefinition triggerDefinition) {
        this(
            OptionalUtils.orElse(triggerDefinition.getBatch(), false),
            TriggerDefinitionDTO.getDescription(triggerDefinition),
            OptionalUtils.mapOptional(triggerDefinition.getHelp(), HelpDTO::new), triggerDefinition.getName(),
            TriggerDefinitionDTO.getTitle(triggerDefinition), triggerDefinition.getType());
    }
}
