/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.ai.agent.utils.action;

import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.ADDITIONAL_FILES;
import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.CONTENT;
import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.DESCRIPTION;
import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.INSTRUCTIONS;
import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.NAME;
import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.PATH;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.ee.platform.ai.skill.facade.AiSkillFacade;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Kriskovic
 */
public class AiAgentUtilsCreateAiSkillAction {
    public static ModifiableActionDefinition of(AiSkillFacade aiSkillFacade) {
        return action("createAiSkill")
            .title("Create AI Skill")
            .description("Creates a new AI skill from instructions.")
            .properties(
                string(NAME)
                    .label("Name")
                    .description("The name of the AI skill.")
                    .maxLength(64)
                    .required(true),
                string(DESCRIPTION)
                    .label("Description")
                    .description("An optional description of the AI skill.")
                    .maxLength(1024)
                    .required(false),
                string(INSTRUCTIONS)
                    .label("Instructions")
                    .description("The instructions that define the main skill's behavior (SKILL.md).")
                    .controlType(Property.ControlType.TEXT_AREA)
                    .required(true),
                array(ADDITIONAL_FILES)
                    .label("Additional Files")
                    .description("Optional extra files to include in the skill archive.")
                    .required(false)
                    .items(
                        object()
                            .properties(
                                string(PATH)
                                    .label("Path")
                                    .description("The file path inside the skill archive.")
                                    .required(true),
                                string(CONTENT)
                                    .label("Content")
                                    .description("The text content of the file.")
                                    .controlType(Property.ControlType.TEXT_AREA)
                                    .required(true))))
            .output(
                outputSchema(
                    object()
                        .properties(
                            integer("id"),
                            string(NAME),
                            string(DESCRIPTION))))
            .perform((PerformFunction) (inputParameters, connectionParameters, context) -> perform(
                inputParameters, aiSkillFacade));
    }

    private AiAgentUtilsCreateAiSkillAction() {
    }

    protected static Object perform(Parameters inputParameters, AiSkillFacade aiSkillFacade) {
        String name = inputParameters.getRequiredString(NAME);
        String description = inputParameters.getString(DESCRIPTION);
        String instructions = inputParameters.getRequiredString(INSTRUCTIONS);

        Map<String, String> additionalFiles = toAdditionalFilesMap(inputParameters.getList(ADDITIONAL_FILES));

        var aiSkill = aiSkillFacade.createAiSkillFromInstructions(
            name, description, instructions, additionalFiles.isEmpty() ? null : additionalFiles);

        return Map.of(
            "id", aiSkill.getId(),
            NAME, aiSkill.getName(),
            DESCRIPTION, aiSkill.getDescription() != null ? aiSkill.getDescription() : "");
    }

    private static Map<String, String> toAdditionalFilesMap(List<?> fileEntries) {
        if (fileEntries == null || fileEntries.isEmpty()) {
            return Map.of();
        }

        Map<String, String> result = new LinkedHashMap<>();

        for (Object entry : fileEntries) {
            if (entry instanceof Map<?, ?> fileMap) {
                String filePath = (String) fileMap.get(PATH);
                String fileContent = (String) fileMap.get(CONTENT);

                if (filePath != null && fileContent != null) {
                    result.put(filePath, fileContent);
                }
            }
        }

        return result;
    }
}
