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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.service.AiSkillService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AiSkillOwnershipResolverTest {

    private final AiSkillService aiSkillService = mock(AiSkillService.class);
    private final UserService userService = mock(UserService.class);

    private final AiSkillOwnershipResolver aiSkillOwnershipResolver =
        new AiSkillOwnershipResolver(aiSkillService, userService);

    @Test
    void testResourceType() {
        assertThat(aiSkillOwnershipResolver.resourceType()).isEqualTo("AiSkill");
    }

    @Test
    void testResolvesOwnerFromCreatedByLogin() {
        User user = new User();

        user.setId(7L);

        when(aiSkillService.fetchAiSkill(1L)).thenReturn(Optional.of(aiSkill("alice")));
        when(userService.fetchUserByLogin("alice")).thenReturn(Optional.of(user));

        assertThat(aiSkillOwnershipResolver.resolveOwner(1L)).isEqualTo(ResourceOwner.ofUser(7L));
    }

    @Test
    void testUnknownWhenSkillMissing() {
        when(aiSkillService.fetchAiSkill(1L)).thenReturn(Optional.empty());

        assertThat(aiSkillOwnershipResolver.resolveOwner(1L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testUnknownWhenCreatedByMissing() {
        when(aiSkillService.fetchAiSkill(1L)).thenReturn(Optional.of(aiSkill(null)));

        assertThat(aiSkillOwnershipResolver.resolveOwner(1L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testUnknownWhenOwnerLoginDoesNotResolve() {
        when(aiSkillService.fetchAiSkill(1L)).thenReturn(Optional.of(aiSkill("ghost")));
        when(userService.fetchUserByLogin("ghost")).thenReturn(Optional.empty());

        assertThat(aiSkillOwnershipResolver.resolveOwner(1L)).isEqualTo(ResourceOwner.unknown());
    }

    private static AiSkill aiSkill(String createdBy) {
        AiSkill aiSkill = new AiSkill();

        aiSkill.setCreatedBy(createdBy);

        return aiSkill;
    }
}
