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

package com.bytechef.component.ai.agent.utils.util;

import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.ID;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Marko Kriskovic
 * @author Ivica Cardic
 */
public class AiAgentUtilsUtils {

    private static final Pattern PERFORM_FUNCTION_PATTERN = Pattern.compile(
        "(?:function\\s+perform\\s*\\(|def\\s+perform\\s*[\\s(]|perform\\s*<-\\s*function\\s*\\(|\\bperform\\s*\\(Map)");
    private static final String SCRIPTS_DIRECTORY = "scripts/";

    public static boolean isScriptEntry(String entryName) {
        String normalized = entryName.replace('\\', '/');

        return normalized.startsWith(SCRIPTS_DIRECTORY) || normalized.contains("/" + SCRIPTS_DIRECTORY);
    }

    public static boolean hasPerformFunction(String content) {
        return PERFORM_FUNCTION_PATTERN.matcher(content)
            .find();
    }

    public static ActionDefinition.OptionsFunction<Long> buildSkillOptions(AiSkillFacade aiSkillFacade) {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            List<AiSkill> aiSkills = aiSkillFacade.getAiSkills();

            return aiSkills
                .stream()
                .map(skill -> option(skill.getName(), skill.getId()
                    .longValue(), skill.getDescription()))
                .toList();
        };
    }

    public static ActionDefinition.OptionsFunction<String> buildSkillFilePathOptions(AiSkillFacade aiSkillFacade) {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            Long id = inputParameters.getLong(ID);

            if (id == null) {
                return List.of();
            }

            return aiSkillFacade.getAiSkillFilePaths(id)
                .stream()
                .map(path -> option(path, path))
                .toList();
        };
    }

    private AiAgentUtilsUtils() {
    }
}
