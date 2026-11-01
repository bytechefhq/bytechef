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
import static org.mockito.Mockito.doReturn;

import com.bytechef.platform.ai.skill.config.AiSkillIntTestConfiguration;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.repository.AiSkillRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
@SpringBootTest(classes = AiSkillIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AiSkillApiFacadeIntTest {

    private static final String SKILL_MD = "---\nname: \"%s\"\ndescription: \"\"\n---\n\nInstructions";

    @Autowired
    private AiSkillApiFacade aiSkillApiFacade;

    @MockitoSpyBean
    private AiSkillFacade aiSkillFacade;

    @Autowired
    private AiSkillRepository aiSkillRepository;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        aiSkillRepository.deleteAll();
    }

    @Test
    void testOwnerCanDelete() {
        AiSkill aiSkill = createSkillAs("alice", "alice-skill");

        authenticate("alice");

        aiSkillApiFacade.deleteAiSkill(aiSkill.getId());

        assertThat(aiSkillRepository.findById(aiSkill.getId())).isEmpty();
    }

    @Test
    void testNonOwnerCannotDelete() {
        AiSkill aiSkill = createSkillAs("alice", "alice-skill");

        authenticate("bob");

        assertThatThrownBy(() -> aiSkillApiFacade.deleteAiSkill(aiSkill.getId()))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(aiSkillRepository.findById(aiSkill.getId())).isPresent();
    }

    @Test
    void testNonOwnerCannotUpdateOrRead() {
        AiSkill aiSkill = createSkillAs("alice", "alice-skill");

        long id = aiSkill.getId();

        authenticate("bob");

        assertThatThrownBy(() -> aiSkillApiFacade.updateAiSkill(id, "renamed", "d"))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> aiSkillApiFacade.updateAiSkillContent(id, "SKILL.md", "x"))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> aiSkillApiFacade.getAiSkillFileContent(id, "SKILL.md"))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> aiSkillApiFacade.getAiSkillWithDownload(id))
            .isInstanceOf(AccessDeniedException.class);

        AiSkill persistedAiSkill = aiSkillRepository.findById(id)
            .orElseThrow();

        assertThat(persistedAiSkill.getName()).isEqualTo("alice-skill");
        assertThat(persistedAiSkill.getSkillFile()).isEqualTo(aiSkill.getSkillFile());
    }

    @Test
    void testAdminBypass() {
        AiSkill aiSkill = createSkillAs("alice", "alice-skill");

        authenticate("root", "ROLE_ADMIN");

        aiSkillApiFacade.deleteAiSkill(aiSkill.getId());

        assertThat(aiSkillRepository.findById(aiSkill.getId())).isEmpty();
    }

    @Test
    void testNullCreatedByDenied() {
        AiSkill aiSkill = createSkillAs("alice", "alice-skill");

        AiSkill ownerlessAiSkill = aiSkillRepository.findById(aiSkill.getId())
            .orElseThrow();

        // created_by is NOT NULL in the schema, so an owner-less row cannot exist in Postgres; the spy returns the
        // persisted row with its owner cleared to keep the fail-closed check covered.
        ownerlessAiSkill.setCreatedBy(null);

        doReturn(ownerlessAiSkill).when(aiSkillFacade)
            .getAiSkill(aiSkill.getId());

        authenticate("alice");

        assertThatThrownBy(() -> aiSkillApiFacade.getAiSkill(aiSkill.getId()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testUnauthenticatedDenied() {
        AiSkill aiSkill = createSkillAs("alice", "alice-skill");

        assertThatThrownBy(() -> aiSkillApiFacade.getAiSkill(aiSkill.getId()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetAiSkillsFiltersToOwnerForNonAdmin() {
        createSkillAs("alice", "alice-skill");
        createSkillAs("bob", "bob-skill");

        authenticate("alice");

        List<AiSkill> result = aiSkillApiFacade.getAiSkills();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()
            .getCreatedBy()).isEqualTo("alice");
    }

    @Test
    void testGetAiSkillsReturnsAllForAdmin() {
        createSkillAs("alice", "alice-skill");
        createSkillAs("bob", "bob-skill");

        authenticate("root", "ROLE_ADMIN");

        assertThat(aiSkillApiFacade.getAiSkills()).hasSize(2);
    }

    private AiSkill createSkillAs(String login, String name) {
        authenticate(login);

        AiSkill aiSkill = aiSkillApiFacade.createAiSkillFromInstructions(name, null, SKILL_MD.formatted(name));

        SecurityContextHolder.clearContext();

        assertThat(aiSkill.getCreatedBy()).isEqualTo(login);

        return aiSkill;
    }

    private static void authenticate(String login, String... authorities) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                login, null, Arrays.stream(authorities)
                    .map(SimpleGrantedAuthority::new)
                    .toList()));

        SecurityContextHolder.setContext(securityContext);
    }
}
