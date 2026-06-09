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

import static com.bytechef.component.ai.agent.utils.constant.AiAgentUtilsConstants.ID;
import static com.bytechef.component.ai.agent.utils.util.AiAgentUtilsUtils.buildSkillOptions;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;

import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.ee.platform.ai.skill.facade.AiSkillFacade;

/**
 * @author Marko Kriskovic
 */
public class AiAgentUtilsDeleteAiSkillAction {

    public static ModifiableActionDefinition of(AiSkillFacade aiSkillFacade) {
        return action("deleteAiSkill")
            .title("Delete AI Skill")
            .description("Deletes an existing AI skill by its ID.")
            .properties(
                integer(ID)
                    .label("ID")
                    .description("The ID of the AI skill to delete.")
                    .options(buildSkillOptions(aiSkillFacade))
                    .required(true))
            .perform((PerformFunction) (inputParameters, connectionParameters, context) -> perform(
                inputParameters, aiSkillFacade));
    }

    private AiAgentUtilsDeleteAiSkillAction() {
    }

    protected static Object perform(Parameters inputParameters, AiSkillFacade aiSkillFacade) {
        long id = inputParameters.getRequiredLong(ID);

        aiSkillFacade.deleteAiSkill(id);

        return null;
    }
}
