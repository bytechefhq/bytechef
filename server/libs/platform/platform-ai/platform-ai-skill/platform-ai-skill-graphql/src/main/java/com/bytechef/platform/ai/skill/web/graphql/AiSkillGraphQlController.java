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

package com.bytechef.platform.ai.skill.web.graphql;

import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.facade.AiSkillApiFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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
class AiSkillGraphQlController {

    private final AiSkillApiFacade aiSkillApiFacade;

    AiSkillGraphQlController(AiSkillApiFacade aiSkillApiFacade) {
        this.aiSkillApiFacade = aiSkillApiFacade;
    }

    @QueryMapping
    List<AiSkill> aiSkills() {
        return aiSkillApiFacade.getAiSkills();
    }

    @QueryMapping
    AiSkill aiSkill(@Argument long id) {
        return aiSkillApiFacade.getAiSkill(id);
    }

    @QueryMapping
    List<String> aiSkillFilePaths(@Argument long id) {
        return aiSkillApiFacade.getAiSkillFilePaths(id);
    }

    @QueryMapping
    String aiSkillFileContent(@Argument long id, @Argument String path) {
        return aiSkillApiFacade.getAiSkillFileContent(id, path);
    }

    @MutationMapping
    AiSkill createAiSkill(
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

        return aiSkillApiFacade.createAiSkill(name, description, filename, bytes);
    }

    @MutationMapping
    AiSkill createAiSkillFromInstructions(
        @Argument String name, @Argument @Nullable String description, @Argument String instructions) {

        return aiSkillApiFacade.createAiSkillFromInstructions(name, description, instructions);
    }

    @MutationMapping
    AiSkill generateAiSkill(@Argument String prompt, @Argument int environmentId) {
        return aiSkillApiFacade.generateAiSkill(prompt, environmentId);
    }

    @MutationMapping
    AiSkill updateAiSkill(
        @Argument long id, @Argument String name, @Argument @Nullable String description) {

        return aiSkillApiFacade.updateAiSkill(id, name, description);
    }

    @MutationMapping
    AiSkill updateAiSkillContent(@Argument long id, @Argument @Nullable String path, @Argument String content) {
        return aiSkillApiFacade.updateAiSkillContent(id, path, content);
    }

    @MutationMapping
    AiSkill createAdditionalFilesInSkill(@Argument long id, @Argument Map<String, String> additionalFiles) {
        return aiSkillApiFacade.createAdditionalFilesInSkill(id, additionalFiles);
    }

    @MutationMapping
    AiSkill removeFileInSkill(@Argument long id, @Argument String path) {
        return aiSkillApiFacade.removeFileInSkill(id, path);
    }

    @MutationMapping
    boolean deleteAiSkill(@Argument long id) {
        aiSkillApiFacade.deleteAiSkill(id);

        return true;
    }
}
