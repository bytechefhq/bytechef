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

package com.bytechef.ai.agent.skill.web.graphql;

import com.bytechef.ai.agent.skill.domain.AiAgentSkill;
import com.bytechef.ai.agent.skill.facade.AiAgentSkillFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Base64;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@SuppressFBWarnings("EI") // Spring GraphQL controllers intentionally return domain objects for serialization
class AiAgentSkillGraphQlController {

    private final AiAgentSkillFacade aiAgentSkillFacade;

    AiAgentSkillGraphQlController(AiAgentSkillFacade aiAgentSkillFacade) {
        this.aiAgentSkillFacade = aiAgentSkillFacade;
    }

    @QueryMapping
    List<AiAgentSkill> aiAgentSkills() {
        return aiAgentSkillFacade.getAiAgentSkills();
    }

    @QueryMapping
    AiAgentSkill aiAgentSkill(@Argument long id) {
        return aiAgentSkillFacade.getAiAgentSkill(id);
    }

    @QueryMapping
    List<String> aiAgentSkillFilePaths(@Argument long id) {
        return aiAgentSkillFacade.getAiAgentSkillFilePaths(id);
    }

    @QueryMapping
    String aiAgentSkillFileContent(@Argument long id, @Argument String path) {
        return aiAgentSkillFacade.getAiAgentSkillFileContent(id, path);
    }

    @MutationMapping
    AiAgentSkill createAiAgentSkill(
        @Argument String name, @Argument @Nullable String description,
        @Argument String filename, @Argument String fileBytes) {

        byte[] bytes;

        try {
            bytes = Base64.getDecoder()
                .decode(fileBytes);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new IllegalArgumentException(
                "The uploaded file data is not valid Base64. Please try uploading again.",
                illegalArgumentException);
        }

        return aiAgentSkillFacade.createAiAgentSkill(name, description, filename, bytes);
    }

    @MutationMapping
    AiAgentSkill createAiAgentSkillFromInstructions(
        @Argument String name, @Argument @Nullable String description, @Argument String instructions) {

        return aiAgentSkillFacade.createAiAgentSkillFromInstructions(name, description, instructions);
    }

    @MutationMapping
    AiAgentSkill updateAiAgentSkill(
        @Argument long id, @Argument String name, @Argument @Nullable String description) {

        return aiAgentSkillFacade.updateAiAgentSkill(id, name, description);
    }

    @MutationMapping
    boolean deleteAiAgentSkill(@Argument long id) {
        aiAgentSkillFacade.deleteAiAgentSkill(id);

        return true;
    }
}
