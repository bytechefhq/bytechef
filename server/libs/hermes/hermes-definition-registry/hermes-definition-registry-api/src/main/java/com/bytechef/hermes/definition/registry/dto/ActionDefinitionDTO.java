
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
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.Help;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ActionDefinitionDTO(
    boolean batch, String description, boolean editorDescriptionDataSource, Optional<HelpDTO> help,
    String name, List<? extends PropertyDTO> outputSchema, boolean outputSchemaDataSource,
    List<? extends PropertyDTO> properties, Optional<Object> sampleOutput, boolean sampleOutputDataSource,
    String title) {

    public ActionDefinitionDTO(ActionDefinition actionDefinition, ComponentDefinition componentDefinition) {
        this(
            OptionalUtils.orElse(actionDefinition.getBatch(), false),
            getDescription(actionDefinition, componentDefinition),
            OptionalUtils.mapOrElse(
                actionDefinition.getEditorDescriptionDataSource(), editorDescriptionDataSource -> true, false),
            getHelp(actionDefinition.getHelp()), actionDefinition.getName(),
            CollectionUtils.map(
                OptionalUtils.orElse(actionDefinition.getOutputSchema(), Collections.emptyList()),
                PropertyDTO::toPropertyDTO),
            OptionalUtils.mapOrElse(
                actionDefinition.getOutputSchemaDataSource(), outputSchemaDataSource -> true, false),
            CollectionUtils.map(
                OptionalUtils.orElse(actionDefinition.getProperties(), Collections.emptyList()),
                PropertyDTO::toPropertyDTO),
            actionDefinition.getSampleOutput(),
            OptionalUtils.mapOrElse(
                actionDefinition.getSampleOutputDataSource(), sampleOutputDataSource -> true, false),
            getTitle(actionDefinition));
    }

    public static String getDescription(ActionDefinition actionDefinition, ComponentDefinition componentDefinition) {
        return OptionalUtils.orElse(
            actionDefinition.getDescription(),
            ComponentDefinitionDTO.getTitle(componentDefinition) + ": " + getTitle(actionDefinition));
    }

    public static String getTitle(ActionDefinition actionDefinition) {
        return OptionalUtils.orElse(actionDefinition.getTitle(), actionDefinition.getName());
    }

    private static Optional<HelpDTO> getHelp(Optional<Help> help) {
        return help.map(HelpDTO::new);
    }
}
