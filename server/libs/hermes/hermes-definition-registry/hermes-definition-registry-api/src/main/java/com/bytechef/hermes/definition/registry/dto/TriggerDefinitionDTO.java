
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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record TriggerDefinitionDTO(
    boolean batch, String description, boolean editorDescriptionDataSource, Optional<HelpDTO> help, String name,
    List<? extends PropertyDTO> outputSchema, boolean outputSchemaDataSource, List<? extends PropertyDTO> properties,
    Optional<Object> sampleOutput, boolean sampleOutputDataSource, String title, TriggerType type) {

    public TriggerDefinitionDTO(TriggerDefinition triggerDefinition) {
        this(
            OptionalUtils.orElse(triggerDefinition.getBatch(), false),
            getDescription(triggerDefinition),
            OptionalUtils.mapOrElse(
                triggerDefinition.getEditorDescriptionDataSource(), editorDescriptionDataSource -> true, false),
            OptionalUtils.mapOptional(triggerDefinition.getHelp(), HelpDTO::new), triggerDefinition.getName(),
            CollectionUtils.map(
                OptionalUtils.orElse(triggerDefinition.getOutputSchema(), Collections.emptyList()),
                PropertyDTO::toPropertyDTO),
            OptionalUtils.mapOrElse(
                triggerDefinition.getOutputSchemaDataSource(), outputSchemaDataSource -> true, false),
            CollectionUtils.map(
                OptionalUtils.orElse(triggerDefinition.getProperties(), Collections.emptyList()),
                PropertyDTO::toPropertyDTO),
            triggerDefinition.getSampleOutput(),
            OptionalUtils.mapOrElse(
                triggerDefinition.getSampleOutputDataSource(), sampleOutputDataSource -> true, false),
            getTitle(triggerDefinition), triggerDefinition.getType());
    }

    public static String getDescription(TriggerDefinition triggerDefinition) {
        return OptionalUtils.orElse(
            triggerDefinition.getDescription(),
            ComponentDefinitionDTO.getTitle(
                triggerDefinition.getComponentName(),
                OptionalUtils.orElse(triggerDefinition.getComponentTitle(), null)) + ": " +
                getTitle(triggerDefinition));
    }

    public static String getTitle(TriggerDefinition triggerDefinition) {
        return OptionalUtils.orElse(triggerDefinition.getTitle(), triggerDefinition.getName());
    }
}
