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

package com.bytechef.platform.ai.skill.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.service.AiSkillService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class AiSkillOwnershipResolver implements ResourceOwnershipResolver {

    private final AiSkillService aiSkillService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public AiSkillOwnershipResolver(AiSkillService aiSkillService, UserService userService) {
        this.aiSkillService = aiSkillService;
        this.userService = userService;
    }

    @Override
    public String resourceType() {
        return "AiSkill";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return aiSkillService.fetchAiSkill(id)
            .map(AiSkill::getCreatedBy)
            .flatMap(userService::fetchUserByLogin)
            .map(User::getId)
            .map(ResourceOwner::ofUser)
            .orElseGet(ResourceOwner::unknown);
    }
}
