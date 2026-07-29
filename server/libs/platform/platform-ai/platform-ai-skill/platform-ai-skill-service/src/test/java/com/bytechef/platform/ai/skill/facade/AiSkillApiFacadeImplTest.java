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

package com.bytechef.platform.ai.skill.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.service.CopilotSkillGenerator;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class AiSkillApiFacadeImplTest {

    private final AiSkillFacade aiSkillFacade = Mockito.mock(AiSkillFacade.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<CopilotSkillGenerator> copilotSkillGeneratorProvider =
        Mockito.mock(ObjectProvider.class);
    private final AiSkillApiFacadeImpl aiSkillApiFacade =
        new AiSkillApiFacadeImpl(aiSkillFacade, copilotSkillGeneratorProvider);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testOwnerCanDelete() {
        authenticate("alice");
        when(aiSkillFacade.getAiSkill(1L)).thenReturn(skill("alice"));

        aiSkillApiFacade.deleteAiSkill(1L);

        verify(aiSkillFacade).deleteAiSkill(1L);
    }

    @Test
    void testNonOwnerCannotDelete() {
        authenticate("bob");
        when(aiSkillFacade.getAiSkill(1L)).thenReturn(skill("alice"));

        assertThatThrownBy(() -> aiSkillApiFacade.deleteAiSkill(1L)).isInstanceOf(AccessDeniedException.class);

        verify(aiSkillFacade, never()).deleteAiSkill(anyLong());
    }

    @Test
    void testNonOwnerCannotUpdateOrRead() {
        authenticate("bob");
        when(aiSkillFacade.getAiSkill(1L)).thenReturn(skill("alice"));

        assertThatThrownBy(() -> aiSkillApiFacade.updateAiSkill(1L, "n", "d"))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> aiSkillApiFacade.updateAiSkillContent(1L, "SKILL.md", "x"))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> aiSkillApiFacade.getAiSkillFileContent(1L, "SKILL.md"))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> aiSkillApiFacade.getAiSkillWithDownload(1L))
            .isInstanceOf(AccessDeniedException.class);

        verify(aiSkillFacade, never()).updateAiSkill(anyLong(), anyString(), anyString());
        verify(aiSkillFacade, never()).updateAiSkillContent(anyLong(), anyString(), anyString());
        verify(aiSkillFacade, never()).getAiSkillFileContent(anyLong(), anyString());
        verify(aiSkillFacade, never()).getAiSkillWithDownload(anyLong());
    }

    @Test
    void testAdminBypass() {
        authenticate("root", "ROLE_ADMIN");
        when(aiSkillFacade.getAiSkill(1L)).thenReturn(skill("alice"));

        aiSkillApiFacade.deleteAiSkill(1L);

        verify(aiSkillFacade).deleteAiSkill(1L);
    }

    @Test
    void testNullCreatedByDenied() {
        authenticate("alice");
        when(aiSkillFacade.getAiSkill(1L)).thenReturn(skill(null));

        assertThatThrownBy(() -> aiSkillApiFacade.getAiSkill(1L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testUnauthenticatedDenied() {
        when(aiSkillFacade.getAiSkill(1L)).thenReturn(skill("alice"));

        assertThatThrownBy(() -> aiSkillApiFacade.getAiSkill(1L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetAiSkillsFiltersToOwnerForNonAdmin() {
        authenticate("alice");
        when(aiSkillFacade.getAiSkills()).thenReturn(List.of(skill("alice"), skill("bob")));

        List<AiSkill> result = aiSkillApiFacade.getAiSkills();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()
            .getCreatedBy()).isEqualTo("alice");
    }

    @Test
    void testGetAiSkillsReturnsAllForAdmin() {
        authenticate("root", "ROLE_ADMIN");
        when(aiSkillFacade.getAiSkills()).thenReturn(List.of(skill("alice"), skill("bob")));

        assertThat(aiSkillApiFacade.getAiSkills()).hasSize(2);
    }

    private static AiSkill skill(String createdBy) {
        AiSkill aiSkill = new AiSkill();

        aiSkill.setCreatedBy(createdBy);

        return aiSkill;
    }

    private static void authenticate(String login, String... authorities) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    login, null, java.util.Arrays.stream(authorities)
                        .map(SimpleGrantedAuthority::new)
                        .toList()));
    }
}
