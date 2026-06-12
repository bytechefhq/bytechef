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
import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.ID;
import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.NAME;
import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.PATH;
import static com.bytechef.component.ai.agent.utils.util.AiAgentUtilsUtils.buildSkillOptions;
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
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Kriskovic
 */
public class AiAgentUtilsAppendFilesToAiSkillAction {

    public static ModifiableActionDefinition of(AiSkillFacade aiSkillFacade) {
        return action("appendFilesToAiSkill")
            .title("Append Files to AI Skill")
            .description("Appends new files to an existing AI skill archive.")
            .properties(
                integer(ID)
                    .label("ID")
                    .description("The ID of the AI skill to append files to.")
                    .options(buildSkillOptions(aiSkillFacade))
                    .required(true),
                array(ADDITIONAL_FILES)
                    .label("Additional Files")
                    .description("Files to add to the skill archive.")
                    .required(true)
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
                            integer(ID),
                            string(NAME),
                            string(DESCRIPTION))))
            .perform((PerformFunction) (inputParameters, connectionParameters, context) -> perform(
                inputParameters, aiSkillFacade));
    }

    private AiAgentUtilsAppendFilesToAiSkillAction() {
    }

    protected static Object perform(Parameters inputParameters, AiSkillFacade aiSkillFacade) {
        long id = inputParameters.getRequiredLong(ID);
        Map<String, String> additionalFiles = toAdditionalFilesMap(
            inputParameters.getRequiredList(ADDITIONAL_FILES));

        AiSkill aiSkill = aiSkillFacade.createAdditionalFilesInSkill(id, additionalFiles);

        return Map.of(
            ID, aiSkill.getId(),
            NAME, aiSkill.getName(),
            DESCRIPTION, aiSkill.getDescription() != null ? aiSkill.getDescription() : "");
    }

    private static Map<String, String> toAdditionalFilesMap(List<?> fileEntries) {
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
