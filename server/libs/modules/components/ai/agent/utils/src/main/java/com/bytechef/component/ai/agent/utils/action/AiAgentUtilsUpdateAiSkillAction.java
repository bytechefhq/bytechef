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

import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.CONTENT;
import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.DESCRIPTION;
import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.FILES;
import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.ID;
import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.NAME;
import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.PATH;
import static com.bytechef.component.ai.agent.utils.util.AiAgentUtilsUtils.buildSkillFilePathOptions;
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
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class AiAgentUtilsUpdateAiSkillAction {

    public static ModifiableActionDefinition of(AiSkillFacade aiSkillFacade) {
        return action("updateAiSkill")
            .title("Update AI Skill")
            .description(
                "Updates an AI skill. Provide name/description to rename the skill, or files to update archive contents.")
            .properties(
                integer(ID)
                    .label("ID")
                    .description("The ID of the AI skill to update.")
                    .options(buildSkillOptions(aiSkillFacade))
                    .required(true),
                string(NAME)
                    .label("Name")
                    .description("New name for the skill. Required when updating name or description.")
                    .maxLength(64)
                    .required(false),
                string(DESCRIPTION)
                    .label("Description")
                    .description("New description for the skill.")
                    .maxLength(1024)
                    .required(false),
                array(FILES)
                    .label("Files")
                    .description(
                        "File contents to update inside the skill archive. Each entry replaces the file at the given path")
                    .required(false)
                    .items(
                        object()
                            .properties(
                                string(PATH)
                                    .label("Path")
                                    .description(
                                        "File path inside the skill archive. SKILL.md is the root file.")
                                    .options(buildSkillFilePathOptions(aiSkillFacade))
                                    .optionsLookupDependsOn(ID)
                                    .required(true),
                                string(CONTENT)
                                    .label("Content")
                                    .description("New text content for the file.")
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

    private AiAgentUtilsUpdateAiSkillAction() {
    }

    protected static Object perform(Parameters inputParameters, AiSkillFacade aiSkillFacade) {
        long id = inputParameters.getRequiredLong(ID);
        String name = inputParameters.getString(NAME);
        String description = inputParameters.getString(DESCRIPTION);
        List<?> files = inputParameters.getList(FILES);

        AiSkill aiSkill = null;

        if (name != null || description != null) {
            String resolvedName = name;

            if (resolvedName == null) {
                resolvedName = aiSkillFacade.getAiSkill(id)
                    .getName();
            }

            aiSkill = aiSkillFacade.updateAiSkill(id, resolvedName, description);
        }

        if (files != null && !files.isEmpty()) {
            for (Object entry : files) {
                if (entry instanceof Map<?, ?> fileMap) {
                    String path = (String) fileMap.get(PATH);
                    String content = (String) fileMap.get(CONTENT);

                    if (content != null) {
                        String resolvedPath = (path != null && !path.isBlank()) ? path : "SKILL.md";

                        if ((resolvedPath.endsWith(".md") || resolvedPath.endsWith(".mdx"))
                            && !content.startsWith("---")) {
                            if (aiSkill == null) {
                                aiSkill = aiSkillFacade.getAiSkill(id);
                            }

                            content = buildSkillMdFrontmatter(aiSkill.getName(), aiSkill.getDescription()) + content;
                        }

                        aiSkill = aiSkillFacade.updateAiSkillContent(id, path, content);
                    }
                }
            }
        }

        if (aiSkill == null) {
            aiSkill = aiSkillFacade.getAiSkill(id);
        }

        return Map.of(
            ID, aiSkill.getId(),
            NAME, aiSkill.getName(),
            DESCRIPTION, aiSkill.getDescription() != null ? aiSkill.getDescription() : "");
    }

    private static String buildSkillMdFrontmatter(String name, String description) {
        StringBuilder frontmatter = new StringBuilder("---\nname: ").append(name)
            .append('\n');

        if (description != null && !description.isBlank()) {
            frontmatter.append("description: ")
                .append(description)
                .append('\n');
        }

        frontmatter.append("---\n");

        return frontmatter.toString();
    }
}
