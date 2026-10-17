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

package com.bytechef.automation.ai.a2a.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

import com.bytechef.automation.ai.a2a.config.A2aIntTestConfiguration;
import com.bytechef.automation.ai.a2a.config.A2aIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.a2a.config.A2aMethodSecurityIntTestConfiguration;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.NestedTestConfiguration;

/**
 * @author Ivica Cardic
 */
class A2aServerFacadeIntTest {

    @Nested
    @SpringBootTest(classes = A2aIntTestConfiguration.class)
    @Import(PostgreSQLContainerConfiguration.class)
    @A2aIntTestConfigurationSharedMocks
    class Persistence {

        private static final long OTHER_WORKSPACE_ID = 2L;
        private static final long WORKSPACE_ID = 1L;

        @Autowired
        private A2aServerFacade a2aServerFacade;

        @Autowired
        private A2aServerRepository a2aServerRepository;

        @Autowired
        private A2aServerService a2aServerService;

        @Autowired
        private TagRepository tagRepository;

        @AfterEach
        void afterEach() {
            a2aServerRepository.deleteAll();
            tagRepository.deleteAll();
        }

        @Test
        void testUpdateA2aServerTagsCreatesNewTagsAndAssignsThem() {
            A2aServer a2aServer = a2aServerService.create(new A2aServer("agent", null, Environment.DEVELOPMENT));

            Tag salesTag = tagRepository.save(new Tag("sales"));

            List<Tag> tags = a2aServerFacade.updateA2aServerTags(
                a2aServer.getId(),
                List.of(new Tag(Validate.notNull(salesTag.getId(), "id"), "sales"), new Tag("support")));

            assertThat(tags).extracting(Tag::getName)
                .containsExactlyInAnyOrder("sales", "support");
            assertThat(tags).allSatisfy(tag -> assertThat(tag.getId()).isNotNull());
            assertThat(a2aServerRepository.findById(a2aServer.getId())
                .orElseThrow()
                .getTagIds()).containsExactlyInAnyOrderElementsOf(
                    tags.stream()
                        .map(Tag::getId)
                        .toList());
        }

        @Test
        void testUpdateA2aServerTagsWithNoTagsClearsThem() {
            A2aServer a2aServer = a2aServerService.create(new A2aServer("agent", null, Environment.DEVELOPMENT));

            a2aServerFacade.updateA2aServerTags(a2aServer.getId(), List.of(new Tag("sales")));

            assertThat(a2aServerFacade.updateA2aServerTags(a2aServer.getId(), List.of())).isEmpty();
            assertThat(a2aServerRepository.findById(a2aServer.getId())
                .orElseThrow()
                .getTagIds()).isEmpty();
        }

        @Test
        void testGetA2aServerTagsReturnsTheTagsOfEachServerAndAllUsedTags() {
            A2aServer salesA2aServer = a2aServerService.create(workspaceA2aServer("sales", WORKSPACE_ID));
            A2aServer supportA2aServer = a2aServerService.create(workspaceA2aServer("support", WORKSPACE_ID));

            a2aServerService.create(workspaceA2aServer("other", OTHER_WORKSPACE_ID));

            tagRepository.save(new Tag("unused"));

            a2aServerFacade.updateA2aServerTags(salesA2aServer.getId(), List.of(new Tag("sales")));
            a2aServerFacade.updateA2aServerTags(supportA2aServer.getId(), List.of(new Tag("support")));

            List<A2aServer> a2aServers = a2aServerFacade.getWorkspaceA2aServers(WORKSPACE_ID);

            Map<A2aServer, List<Tag>> a2aServerTags = a2aServerFacade.getA2aServerTags(a2aServers);

            assertThat(a2aServers).allSatisfy(
                a2aServer -> assertThat(a2aServerTags).hasEntrySatisfying(
                    a2aServer, tags -> assertThat(tags).extracting(Tag::getName)
                        .containsExactly(a2aServer.getName())));
            assertThat(a2aServers).extracting(A2aServer::getName)
                .containsExactly("sales", "support");
            assertThat(a2aServerFacade.getA2aServerTags(WORKSPACE_ID)).extracting(Tag::getName)
                .containsExactlyInAnyOrder("sales", "support");
        }

        private static A2aServer workspaceA2aServer(String name, long workspaceId) {
            A2aServer a2aServer = new A2aServer(name, null, Environment.DEVELOPMENT);

            a2aServer.setWorkspaceId(workspaceId);

            return a2aServer;
        }
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(classes = A2aMethodSecurityIntTestConfiguration.class)
    class MethodSecurity {

        @Autowired
        private A2aServerFacade a2aServerFacade;

        @Autowired
        private A2aServerRepository a2aServerRepository;

        @Autowired
        private PermissionService permissionService;

        @BeforeEach
        void beforeEach() {
            A2aServer a2aServer = new A2aServer("agent", null, Environment.DEVELOPMENT);

            a2aServer.setId(1L);

            when(a2aServerRepository.findById(1L)).thenReturn(Optional.of(a2aServer));
            when(a2aServerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();

            reset(a2aServerRepository, permissionService);
        }

        @Test
        void testCallerWithoutTheScopesCannotCreateUpdateOrDeleteAServer() {
            authenticate();

            assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> a2aServerFacade.createA2aServer(
                    7L, new A2aServer("agent", null, Environment.DEVELOPMENT)));
            assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> a2aServerFacade.updateA2aServer(1L, "renamed", null, null, null));
            assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> a2aServerFacade.deleteA2aServer(1L));

            verify(permissionService).hasResourceScope(7L, "Workspace", "A2A_CREATE");
            verify(permissionService, times(2)).hasResourceScope(1L, "A2aServer", "A2A_EDIT");
            verify(a2aServerRepository, never()).save(any());
            verify(a2aServerRepository, never()).deleteById(anyLong());
        }

        @Test
        void testWorkspaceEditorCreatesTheServerInTheWorkspace() {
            authenticate();

            when(permissionService.hasResourceScope(7L, "Workspace", "A2A_CREATE")).thenReturn(true);

            A2aServer a2aServer = a2aServerFacade.createA2aServer(
                7L, new A2aServer("agent", null, Environment.DEVELOPMENT));

            assertThat(a2aServer.getWorkspaceId()).isEqualTo(7L);
        }

        @Test
        void testWorkspaceEditorUpdatesAndDeletesTheServer() {
            authenticate();

            when(permissionService.hasResourceScope(1L, "A2aServer", "A2A_EDIT")).thenReturn(true);

            A2aServer a2aServer = a2aServerFacade.updateA2aServer(1L, "renamed", null, false, null);

            assertThat(a2aServer.getName()).isEqualTo("renamed");
            assertThat(a2aServer.isEnabled()).isFalse();

            a2aServerFacade.deleteA2aServer(1L);

            verify(a2aServerRepository).deleteById(1L);
        }

        private static void authenticate() {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        }
    }
}
