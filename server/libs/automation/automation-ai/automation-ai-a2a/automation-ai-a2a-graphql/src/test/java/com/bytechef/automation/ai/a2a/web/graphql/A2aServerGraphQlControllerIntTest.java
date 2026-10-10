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

package com.bytechef.automation.ai.a2a.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.facade.A2aServerFacade;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.automation.ai.a2a.web.graphql.config.AutomationA2aGraphQlConfigurationSharedMocks;
import com.bytechef.automation.ai.a2a.web.graphql.config.AutomationA2aGraphQlTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationA2aGraphQlTestConfiguration.class,
    A2aServerGraphQlController.class
})
@GraphQlTest(
    controllers = A2aServerGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:graphql/"
    })
@AutomationA2aGraphQlConfigurationSharedMocks
class A2aServerGraphQlControllerIntTest {

    @Autowired
    private A2aServerFacade a2aServerFacade;

    @Autowired
    private A2aServerService a2aServerService;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    @WithMockUser
    void testA2aServersReturnsTheTagsOfEachServer() {
        A2aServer a2aServer = createA2aServer(5L, "agent", Environment.PRODUCTION);

        when(a2aServerService.getA2aServers()).thenReturn(List.of(a2aServer));
        when(a2aServerFacade.getA2aServerTags(List.of(a2aServer))).thenReturn(
            Map.of(a2aServer, List.of(createTag(7L, "sales"))));

        graphQlTester
            .document("""
                query {
                    a2aServers {
                        tags {
                            id
                            name
                        }
                    }
                }
                """)
            .execute()
            .path("a2aServers[0].tags[0].id")
            .entity(String.class)
            .isEqualTo("7")
            .path("a2aServers[0].tags[0].name")
            .entity(String.class)
            .isEqualTo("sales");
    }

    @Test
    @WithMockUser
    void testA2aServerTagsReturnsTheTagsUsedByA2aServers() {
        when(a2aServerFacade.getA2aServerTags()).thenReturn(List.of(createTag(7L, "sales")));

        graphQlTester
            .document("""
                query {
                    a2aServerTags {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("a2aServerTags[0].name")
            .entity(String.class)
            .isEqualTo("sales");
    }

    @Test
    @WithMockUser
    void testUpdateA2aServerTagsPassesTheExistingAndNewTagsToTheFacade() {
        when(a2aServerFacade.updateA2aServerTags(eq(5L), any())).thenReturn(
            List.of(createTag(7L, "sales"), createTag(8L, "support")));

        graphQlTester
            .document("""
                mutation {
                    updateA2aServerTags(id: 5, tags: [{id: 7, name: "sales"}, {name: "support"}]) {
                        id
                    }
                }
                """)
            .execute()
            .path("updateA2aServerTags[1].id")
            .entity(String.class)
            .isEqualTo("8");

        ArgumentCaptor<List<Tag>> tagsArgumentCaptor = ArgumentCaptor.captor();

        verify(a2aServerFacade).updateA2aServerTags(eq(5L), tagsArgumentCaptor.capture());

        assertThat(tagsArgumentCaptor.getValue())
            .extracting(Tag::getId, Tag::getName)
            .containsExactly(tuple(7L, "sales"), tuple(null, "support"));
    }

    @Test
    @WithMockUser
    void testA2aServersResolvesTheSecretKeyThroughTheGatedServiceMethod() {
        A2aServer a2aServer = createA2aServer(5L, "agent", Environment.PRODUCTION);

        when(a2aServerService.getA2aServers()).thenReturn(List.of(a2aServer));
        when(a2aServerService.getA2aServerSecretKey(5L)).thenReturn("gated-secret");

        graphQlTester
            .document("""
                query {
                    a2aServers {
                        id
                        name
                        environmentId
                        enabled
                        secretKey
                    }
                }
                """)
            .execute()
            .path("a2aServers[0].id")
            .entity(String.class)
            .isEqualTo("5")
            .path("a2aServers[0].name")
            .entity(String.class)
            .isEqualTo("agent")
            .path("a2aServers[0].environmentId")
            .entity(String.class)
            .isEqualTo(String.valueOf(Environment.PRODUCTION.ordinal()))
            .path("a2aServers[0].enabled")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("a2aServers[0].secretKey")
            .entity(String.class)
            .isEqualTo("gated-secret");

        verify(a2aServerService).getA2aServerSecretKey(5L);
    }

    @Test
    @WithMockUser
    void testA2aServersReturnsTheLastModifiedDateAsEpochMilliseconds() {
        A2aServer a2aServer = createA2aServer(5L, "agent", Environment.PRODUCTION);

        ReflectionTestUtils.setField(a2aServer, "lastModifiedDate", Instant.ofEpochMilli(1_791_000_000_000L));

        when(a2aServerService.getA2aServers()).thenReturn(List.of(a2aServer));

        graphQlTester
            .document("""
                query {
                    a2aServers {
                        lastModifiedDate
                    }
                }
                """)
            .execute()
            .path("a2aServers[0].lastModifiedDate")
            .entity(Long.class)
            .isEqualTo(1_791_000_000_000L);
    }

    @Test
    @WithMockUser
    void testA2aServersReturnsANullSecretKeyForCallersWhoAreNotTenantAdmins() {
        A2aServer a2aServer = createA2aServer(5L, "agent", Environment.PRODUCTION);

        when(a2aServerService.getA2aServers()).thenReturn(List.of(a2aServer));
        when(a2aServerService.getA2aServerSecretKey(5L)).thenThrow(new AccessDeniedException("denied"));

        graphQlTester
            .document("""
                query {
                    a2aServers {
                        id
                        secretKey
                    }
                }
                """)
            .execute()
            .path("a2aServers[0].id")
            .entity(String.class)
            .isEqualTo("5")
            .path("a2aServers[0].secretKey")
            .valueIsNull();
    }

    @Test
    void testA2aServersRequiresAuthentication() {
        graphQlTester
            .document("""
                query {
                    a2aServers {
                        id
                    }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors)
                .singleElement()
                .extracting(ResponseError::getErrorType)
                .isEqualTo(ErrorType.UNAUTHORIZED))
            .path("a2aServers")
            .valueIsNull();

        verifyNoInteractions(a2aServerService);
    }

    @Test
    void testCreateA2aServer() {
        ArgumentCaptor<A2aServer> a2aServerArgumentCaptor = ArgumentCaptor.forClass(A2aServer.class);

        when(a2aServerService.create(a2aServerArgumentCaptor.capture()))
            .thenAnswer(invocation -> withId(invocation.getArgument(0), 5L));

        graphQlTester
            .document("""
                mutation {
                    createA2aServer(input: {
                        name: "agent",
                        description: "An agent",
                        environmentId: "1",
                        authenticationRequired: true
                    }) {
                        id
                        name
                        description
                        environmentId
                        authenticationRequired
                    }
                }
                """)
            .execute()
            .path("createA2aServer.id")
            .entity(String.class)
            .isEqualTo("5")
            .path("createA2aServer.environmentId")
            .entity(String.class)
            .isEqualTo("1")
            .path("createA2aServer.authenticationRequired")
            .entity(Boolean.class)
            .isEqualTo(true);

        A2aServer a2aServer = a2aServerArgumentCaptor.getValue();

        assertThat(a2aServer.getName()).isEqualTo("agent");
        assertThat(a2aServer.getDescription()).isEqualTo("An agent");
        assertThat(a2aServer.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(a2aServer.isAuthenticationRequired()).isTrue();
    }

    @Test
    void testCreateA2aServerRejectsAnUnknownEnvironmentId() {
        graphQlTester
            .document("""
                mutation {
                    createA2aServer(input: {name: "agent", environmentId: "99"}) {
                        id
                    }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors)
                .singleElement()
                .satisfies(error -> {
                    assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    assertThat(error.getMessage()).isEqualTo("Unknown environment id: 99");
                }))
            .path("createA2aServer")
            .valueIsNull();

        verifyNoInteractions(a2aServerService);
    }

    @Test
    void testUpdateA2aServerAppliesOnlyTheProvidedFields() {
        A2aServer a2aServer = createA2aServer(5L, "agent", Environment.PRODUCTION);

        a2aServer.setDescription("Original description");

        when(a2aServerService.getA2aServer(5L)).thenReturn(a2aServer);
        when(a2aServerService.update(any(A2aServer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        graphQlTester
            .document("""
                mutation {
                    updateA2aServer(id: "5", input: {name: "renamed", enabled: false}) {
                        id
                        name
                        description
                        enabled
                    }
                }
                """)
            .execute()
            .path("updateA2aServer.name")
            .entity(String.class)
            .isEqualTo("renamed")
            .path("updateA2aServer.description")
            .entity(String.class)
            .isEqualTo("Original description")
            .path("updateA2aServer.enabled")
            .entity(Boolean.class)
            .isEqualTo(false);

        verify(a2aServerService).update(a2aServer);
    }

    @Test
    void testDeleteA2aServer() {
        graphQlTester
            .document("""
                mutation {
                    deleteA2aServer(id: "5")
                }
                """)
            .execute()
            .path("deleteA2aServer")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(a2aServerService).delete(5L);
    }

    private static A2aServer createA2aServer(long id, String name, Environment environment) {
        A2aServer a2aServer = new A2aServer(name, null, environment);

        a2aServer.setId(id);

        return a2aServer;
    }

    private static A2aServer withId(A2aServer a2aServer, long id) {
        a2aServer.setId(id);

        return a2aServer;
    }

    private static Tag createTag(long id, String name) {
        Tag tag = new Tag();

        tag.setId(id);
        tag.setName(name);

        return tag;
    }
}
