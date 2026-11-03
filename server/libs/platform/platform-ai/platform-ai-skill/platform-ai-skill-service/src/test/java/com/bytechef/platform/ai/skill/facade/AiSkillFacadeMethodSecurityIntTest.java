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
import com.bytechef.platform.ai.skill.config.AiSkillMethodSecurityTestConfiguration;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.repository.AiSkillRepository;
import com.bytechef.platform.ai.skill.service.AiSkillService;
import com.bytechef.platform.security.web.authentication.TestNonPlatformUserAuthentication;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = {
    AiSkillIntTestConfiguration.class, AiSkillMethodSecurityTestConfiguration.class
})
@Import(PostgreSQLContainerConfiguration.class)
class AiSkillFacadeMethodSecurityIntTest {

    @Autowired
    private AiSkillFacade aiSkillFacade;

    @Autowired
    private AiSkillRepository aiSkillRepository;

    @MockitoSpyBean
    private AiSkillService aiSkillService;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        aiSkillRepository.deleteAll();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("byIdOperations")
    void testByIdMethodRequiresResourceOwner(String methodName, BiConsumer<AiSkillFacade, Long> operation) {
        AiSkill aiSkill = createSkillAs("alice", "alice-skill");

        long id = aiSkill.getId();

        authenticate("bob");

        assertThatThrownBy(() -> operation.accept(aiSkillFacade, id))
            .isInstanceOf(AccessDeniedException.class);

        AiSkill persistedAiSkill = aiSkillRepository.findById(id)
            .orElseThrow();

        assertThat(persistedAiSkill.getName()).isEqualTo("alice-skill");
        assertThat(persistedAiSkill.getSkillFile()).isEqualTo(aiSkill.getSkillFile());
    }

    @Test
    void testOwnerAndTenantAdminPassTheByIdGate() {
        AiSkill aiSkill = createSkillAs("alice", "alice-skill");

        long id = aiSkill.getId();

        authenticate("alice");

        AiSkill ownedAiSkill = aiSkillFacade.getAiSkill(id);

        assertThat(ownedAiSkill.getName()).isEqualTo("alice-skill");

        authenticate("root", "ROLE_ADMIN");

        assertThat(aiSkillFacade.getAiSkillFilePaths(id)).contains("SKILL.md");

        aiSkillFacade.deleteAiSkill(id);

        assertThat(aiSkillRepository.findById(id)).isEmpty();
    }

    @Test
    void testMethodRequiresAuthentication() {
        assertThatThrownBy(() -> aiSkillFacade.createAiSkill("skill", null, "skill.skill", new byte[0]))
            .isInstanceOfAny(AccessDeniedException.class, AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> aiSkillFacade.createAiSkillFromInstructions("skill", null, "do it"))
            .isInstanceOfAny(AccessDeniedException.class, AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> aiSkillFacade.createAiSkillFromInstructions("skill", null, "do it", Map.of()))
            .isInstanceOfAny(AccessDeniedException.class, AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(aiSkillFacade::getAiSkillTags)
            .isInstanceOfAny(AccessDeniedException.class, AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> aiSkillFacade.getTags(List.of(1L)))
            .isInstanceOfAny(AccessDeniedException.class, AuthenticationCredentialsNotFoundException.class);

        assertThat(aiSkillRepository.findAll()).isEmpty();
    }

    @Test
    void testAnonymousCallerIsDenied() {
        AiSkill aiSkill = createSkillAs("alice", "alice-skill");

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new AnonymousAuthenticationToken("key", "anonymous",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        SecurityContextHolder.setContext(securityContext);

        assertThatThrownBy(() -> aiSkillFacade.createAiSkillFromInstructions("skill", null, "do it"))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(aiSkillFacade::getAiSkillTags).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> aiSkillFacade.getAiSkill(aiSkill.getId())).isInstanceOf(AccessDeniedException.class);

        assertThat(aiSkillRepository.findAll()).hasSize(1);
    }

    @Test
    void testOwnerlessSkillIsDenied() {
        AiSkill aiSkill = createSkillAs("alice", "alice-skill");

        AiSkill ownerlessAiSkill = aiSkillRepository.findById(aiSkill.getId())
            .orElseThrow();

        ownerlessAiSkill.setCreatedBy(null);

        doReturn(Optional.of(ownerlessAiSkill)).when(aiSkillService)
            .fetchAiSkill(aiSkill.getId());

        authenticate("alice");

        assertThatThrownBy(() -> aiSkillFacade.getAiSkill(aiSkill.getId())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testConnectedUserWhoseExternalIdIsAPlatformLoginIsRefusedEverySkillOperation() {
        AiSkill aiSkill = createSkillAs("alice", "alice-skill");

        long id = aiSkill.getId();

        SecurityContextHolder.getContext()
            .setAuthentication(TestNonPlatformUserAuthentication.of("alice"));

        assertThatThrownBy(aiSkillFacade::getAiSkills).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> aiSkillFacade.getAiSkill(id)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> aiSkillFacade.deleteAiSkill(id)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> aiSkillFacade.updateAiSkill(id, "renamed", null))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> aiSkillFacade.createAiSkillFromInstructions("skill", null, "do it"))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(aiSkillRepository.findAll())
            .singleElement()
            .extracting(AiSkill::getName)
            .isEqualTo("alice-skill");
    }

    @Test
    void testGetAiSkillsIsFilteredToOwnedSkills() {
        createSkillAs("alice", "alice-skill");
        createSkillAs("bob", "bob-skill");

        authenticate("alice");

        assertThat(aiSkillFacade.getAiSkills())
            .extracting(AiSkill::getName)
            .containsExactly("alice-skill");

        authenticate("root", "ROLE_ADMIN");

        assertThat(aiSkillFacade.getAiSkills())
            .extracting(AiSkill::getName)
            .containsExactlyInAnyOrder("alice-skill", "bob-skill");
    }

    @Test
    void testGetAiSkillTagsReadsOnlyTheCallersSkills() {
        AiSkill aliceAiSkill = createSkillAs("alice", "alice-skill");
        AiSkill bobAiSkill = createSkillAs("bob", "bob-skill");

        authenticate("alice");

        aiSkillFacade.updateAiSkillTags(aliceAiSkill.getId(), List.of(new Tag("alice-tag")));

        authenticate("bob");

        aiSkillFacade.updateAiSkillTags(bobAiSkill.getId(), List.of(new Tag("bob-tag")));

        authenticate("alice");

        assertThat(aiSkillFacade.getAiSkillTags())
            .extracting(Tag::getName)
            .containsExactly("alice-tag");
    }

    static Stream<Arguments> byIdOperations() {
        return Stream.of(
            byIdOperation(
                "createAdditionalFilesInSkill",
                (facade, id) -> facade.createAdditionalFilesInSkill(id, Map.of("notes.md", "x"))),
            byIdOperation("deleteAiSkill", (facade, id) -> facade.deleteAiSkill(id)),
            byIdOperation("getAiSkill", (facade, id) -> facade.getAiSkill(id)),
            byIdOperation("getAiSkillDownload", (facade, id) -> facade.getAiSkillDownload(id)),
            byIdOperation("getAiSkillWithDownload", (facade, id) -> facade.getAiSkillWithDownload(id)),
            byIdOperation("getAiSkillFileContent", (facade, id) -> facade.getAiSkillFileContent(id, "SKILL.md")),
            byIdOperation("getAiSkillFilePaths", (facade, id) -> facade.getAiSkillFilePaths(id)),
            byIdOperation("removeFileInSkill", (facade, id) -> facade.removeFileInSkill(id, "notes.md")),
            byIdOperation("updateAiSkill", (facade, id) -> facade.updateAiSkill(id, "renamed", null)),
            byIdOperation("updateAiSkillTags",
                (facade, id) -> facade.updateAiSkillTags(id, List.of(new Tag("stolen")))),
            byIdOperation("updateAiSkillContent", (facade, id) -> facade.updateAiSkillContent(id, "SKILL.md", "x")));
    }

    private static Arguments byIdOperation(String methodName, BiConsumer<AiSkillFacade, Long> operation) {
        return Arguments.of(methodName, operation);
    }

    private AiSkill createSkillAs(String login, String name) {
        authenticate(login);

        AiSkill aiSkill = aiSkillFacade.createAiSkillFromInstructions(name, null, "Instructions");

        SecurityContextHolder.clearContext();

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
