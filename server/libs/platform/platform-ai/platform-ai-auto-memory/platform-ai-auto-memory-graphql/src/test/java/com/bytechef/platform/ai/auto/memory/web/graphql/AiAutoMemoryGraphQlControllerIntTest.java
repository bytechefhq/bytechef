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

package com.bytechef.platform.ai.auto.memory.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryConcurrentModificationException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryNotFoundException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalCount;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import graphql.ErrorClassification;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AiAutoMemoryGraphQlControllerIntTest.AiAutoMemoryGraphQlTestConfiguration.class,
    AiAutoMemoryGraphQlController.class
})
@GraphQlTest(
    controllers = AiAutoMemoryGraphQlController.class,
    properties = "spring.graphql.schema.locations=classpath*:/graphql/")
class AiAutoMemoryGraphQlControllerIntTest {

    private static final long CURRENT_USER_ID = 7L;

    private static final String SINGLE_FETCH_WITHOUT_PRINCIPAL = """
        query {
            aiAutoMemory(workspaceId: "1", id: "3", environment: 0) {
                id
            }
        }
        """;

    private static final List<String> ALL_MEMORY_FIELDS = List.of(
        "id", "workspaceId", "principalType", "principalId", "name", "title", "description", "memoryType", "content",
        "environmentId", "version", "createdAt", "updatedAt");

    private static final String LIST_WITHOUT_PRINCIPAL = """
        query {
            aiAutoMemories(workspaceId: "1", environment: 0) {
                id
            }
        }
        """;

    private static final String PRINCIPALS_QUERY = """
        query {
            aiAutoMemoryPrincipals(workspaceId: "1", environment: 0) {
                principalType
                principalId
                label
                memoryCount
            }
        }
        """;

    private static final String UPDATE_TITLE_MUTATION = """
        mutation {
            updateAiAutoMemory(
                input: {id: "3", workspaceId: "1", environment: 0, expectedVersion: 4, title: "New title"}
            ) {
                id
            }
        }
        """;

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private AiAutoMemoryService aiAutoMemoryService;

    @MockitoBean
    private ProjectDeploymentService projectDeploymentService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private WorkspaceFacade workspaceFacade;

    @BeforeEach
    void beforeEach() {
        User user = new User();

        user.setId(CURRENT_USER_ID);

        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace = new Workspace();

        workspace.setId(1L);

        when(workspaceFacade.getUserWorkspaces(CURRENT_USER_ID)).thenReturn(List.of(workspace));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testListArgumentsBindToTheOwner() {
        when(aiAutoMemoryService.list(any(), any())).thenReturn(List.of());

        graphQlTester
            .document(
                """
                    query {
                        aiAutoMemories(
                            workspaceId: "1", environment: 2,
                            principal: {principalType: PROJECT_DEPLOYMENT, principalId: 5}
                        ) {
                            id
                        }
                    }
                    """)
            .execute()
            .path("aiAutoMemories")
            .entityList(Object.class)
            .hasSize(0);

        verify(aiAutoMemoryService).list(
            new AiAutoMemoryOwner(1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, Environment.PRODUCTION), null);
    }

    @Test
    void testListSerializesEveryFieldOfAMemory() {
        AiAutoMemoryOwner owner = new AiAutoMemoryOwner(
            1L, AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, Environment.STAGING);

        AiAutoMemory memory = AiAutoMemory.restore(
            owner, 3L, "user_profile", "Profile", "How the user likes replies", AiAutoMemoryType.FEEDBACK,
            "Prefers concise replies.", LocalDateTime.of(2026, 1, 1, 12, 0), LocalDateTime.of(2026, 1, 1, 12, 5), 4L);

        when(aiAutoMemoryService.list(owner, null)).thenReturn(List.of(memory));

        GraphQlTester.Response response = graphQlTester
            .document(
                """
                    query {
                        aiAutoMemories(
                            workspaceId: "1", environment: 1, principal: {principalType: USER, principalId: 7}
                        ) {
                            id
                            workspaceId
                            principalType
                            principalId
                            name
                            title
                            description
                            memoryType
                            content
                            environmentId
                            version
                            createdAt
                            updatedAt
                        }
                    }
                    """)
            .execute();

        response.path("aiAutoMemories[0].id")
            .entity(String.class)
            .isEqualTo("3");
        response.path("aiAutoMemories[0].workspaceId")
            .entity(Long.class)
            .isEqualTo(1L);
        response.path("aiAutoMemories[0].principalType")
            .entity(String.class)
            .isEqualTo("USER");
        response.path("aiAutoMemories[0].principalId")
            .entity(Long.class)
            .isEqualTo(CURRENT_USER_ID);
        response.path("aiAutoMemories[0].name")
            .entity(String.class)
            .isEqualTo("user_profile");
        response.path("aiAutoMemories[0].title")
            .entity(String.class)
            .isEqualTo("Profile");
        response.path("aiAutoMemories[0].description")
            .entity(String.class)
            .isEqualTo("How the user likes replies");
        response.path("aiAutoMemories[0].memoryType")
            .entity(String.class)
            .isEqualTo("FEEDBACK");
        response.path("aiAutoMemories[0].content")
            .entity(String.class)
            .isEqualTo("Prefers concise replies.");
        response.path("aiAutoMemories[0].environmentId")
            .entity(Long.class)
            .isEqualTo((long) Environment.STAGING.ordinal());
        response.path("aiAutoMemories[0].version")
            .entity(Long.class)
            .isEqualTo(4L);
        response.path("aiAutoMemories[0].createdAt")
            .entity(Long.class)
            .isEqualTo(1767268800000L);
        response.path("aiAutoMemories[0].updatedAt")
            .entity(Long.class)
            .isEqualTo(1767269100000L);
    }

    @Test
    void testUpdatePrincipalBindsToTheOwner() {
        when(aiAutoMemoryService.updateById(any(), anyLong(), anyLong(), any()))
            .thenThrow(new AiAutoMemoryNotFoundException("Memory not found"));

        assertSingleError(
            """
                mutation {
                    updateAiAutoMemory(
                        input: {
                            id: "3", workspaceId: "1", environment: 0, expectedVersion: 4, title: "New title",
                            principal: {principalType: USER, principalId: 7}
                        }
                    ) {
                        id
                    }
                }
                """,
            ErrorType.NOT_FOUND, "Memory not found");

        verify(aiAutoMemoryService).updateById(
            eq(new AiAutoMemoryOwner(1L, AiAutoMemoryPrincipalType.USER, 7L, Environment.DEVELOPMENT)), eq(3L), eq(4L),
            any());
    }

    @Test
    void testAHalfSpecifiedPrincipalIsRejectedBeforeReachingTheService() {
        graphQlTester.document(
            """
                query {
                    aiAutoMemories(workspaceId: "1", environment: 0, principal: {principalType: USER}) {
                        id
                    }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors)
                .singleElement()
                .satisfies(error -> assertThat(error.getErrorType()).hasToString("ValidationError")));

        verify(aiAutoMemoryService, never()).list(any(), any());
    }

    @Test
    void testMissingMemoryIsReportedAsNotFoundWithTheServiceMessage() {
        when(aiAutoMemoryService.updateById(any(), anyLong(), anyLong(), any()))
            .thenThrow(new AiAutoMemoryNotFoundException("Memory not found"));

        assertSingleError(UPDATE_TITLE_MUTATION, ErrorType.NOT_FOUND, "Memory not found");
    }

    @Test
    void testInvalidInputIsReportedAsBadRequestWithTheServiceMessage() {
        when(aiAutoMemoryService.updateById(any(), anyLong(), anyLong(), any()))
            .thenThrow(new IllegalArgumentException("AiAutoMemory.title must be a single line"));

        assertSingleError(
            UPDATE_TITLE_MUTATION, ErrorType.BAD_REQUEST, "AiAutoMemory.title must be a single line");
    }

    @Test
    void testConcurrentModificationIsReportedAsConflictWithAMessageForThePage() {
        when(aiAutoMemoryService.updateById(any(), anyLong(), eq(4L), any()))
            .thenThrow(new AiAutoMemoryConcurrentModificationException("notes"));

        assertSingleError(
            UPDATE_TITLE_MUTATION, AiAutoMemoryGraphQlController.CONFLICT,
            "Memory 'notes' was changed by someone else. Reload it and try again.");
    }

    @Test
    void testExpectedVersionReachesTheService() {
        when(aiAutoMemoryService.updateById(any(), anyLong(), anyLong(), any()))
            .thenThrow(new AiAutoMemoryNotFoundException("Memory not found"));

        assertSingleError(UPDATE_TITLE_MUTATION, ErrorType.NOT_FOUND, "Memory not found");

        verify(aiAutoMemoryService).updateById(
            any(), eq(3L), eq(4L), eq(new AiAutoMemoryPatch("New title", null, null, null)));
    }

    @Test
    void testUpdateWithoutExpectedVersionIsRejectedBeforeReachingTheService() {
        graphQlTester.document(
            """
                mutation {
                    updateAiAutoMemory(input: {id: "3", workspaceId: "1", environment: 0, title: "New title"}) {
                        id
                    }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors)
                .singleElement()
                .satisfies(error -> assertThat(error.getErrorType()).hasToString("ValidationError")));

        verify(aiAutoMemoryService, never()).updateById(any(), anyLong(), anyLong(), any());
    }

    @Test
    void testOmittingThePrincipalReadsTheCurrentUsersOwnMemory() {
        graphQlTester.document(SINGLE_FETCH_WITHOUT_PRINCIPAL)
            .execute();

        verify(aiAutoMemoryService).findById(
            new AiAutoMemoryOwner(1L, AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, Environment.DEVELOPMENT), 3L);
    }

    @Test
    void testOwnUserIdIsAddressable() {
        when(aiAutoMemoryService.list(any(), any())).thenReturn(List.of());

        listWithPrincipal("USER", CURRENT_USER_ID).execute();

        verify(aiAutoMemoryService).list(
            new AiAutoMemoryOwner(1L, AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, Environment.DEVELOPMENT), null);
    }

    @Test
    void testAnotherUsersIdReturnsAnEmptyListWithoutQuerying() {
        listWithPrincipal("USER", 99L)
            .execute()
            .path("aiAutoMemories")
            .entityList(Object.class)
            .hasSize(0);

        verify(aiAutoMemoryService, never()).list(any(), any());
    }

    @Test
    void testSingleFetchWithAnotherUsersIdReturnsNull() {
        graphQlTester.document(
            """
                query {
                    aiAutoMemory(
                        workspaceId: "1", id: "3", environment: 0,
                        principal: {principalType: USER, principalId: 99}
                    ) {
                        id
                    }
                }
                """)
            .execute()
            .path("aiAutoMemory")
            .valueIsNull();

        verify(aiAutoMemoryService, never()).findById(any(), anyLong());
    }

    @Test
    void testProjectDeploymentIsReadableByAnyMember() {
        when(aiAutoMemoryService.list(any(), any())).thenReturn(List.of());

        listWithPrincipal("PROJECT_DEPLOYMENT", 5L).execute();

        verify(aiAutoMemoryService).list(
            new AiAutoMemoryOwner(1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, Environment.DEVELOPMENT),
            null);
    }

    @Test
    void testSingleFetchPassesTheEnvironmentToTheService() {
        graphQlTester.document(
            """
                query {
                    aiAutoMemory(
                        workspaceId: "1", id: "3", environment: 2,
                        principal: {principalType: PROJECT_DEPLOYMENT, principalId: 5}
                    ) {
                        id
                    }
                }
                """)
            .execute();

        verify(aiAutoMemoryService).findById(
            new AiAutoMemoryOwner(1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, Environment.PRODUCTION), 3L);
    }

    @Test
    void testIntegrationInstanceIsRejected() {
        listWithPrincipal("INTEGRATION_INSTANCE", 5L)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors)
                .singleElement()
                .satisfies(error -> {
                    assertThat(error.getErrorType()).hasToString(ErrorType.BAD_REQUEST.toString());
                    assertThat(error.getMessage()).contains("INTEGRATION_INSTANCE");
                }));

        verify(aiAutoMemoryService, never()).list(any(), any());
    }

    @Test
    void testUnknownEnvironmentIsRejected() {
        graphQlTester.document(
            """
                query {
                    aiAutoMemories(workspaceId: "1", environment: 7) {
                        id
                    }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors)
                .singleElement()
                .satisfies(error -> {
                    assertThat(error.getErrorType()).hasToString(ErrorType.BAD_REQUEST.toString());
                    assertThat(error.getMessage()).contains("Unknown environment");
                }));
    }

    @Test
    void testUpdatingAProjectDeploymentMemoryAsAdminReachesTheService() {
        authenticateWith("ROLE_ADMIN");

        AiAutoMemoryOwner owner = new AiAutoMemoryOwner(
            1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, Environment.PRODUCTION);

        when(aiAutoMemoryService.updateById(eq(owner), anyLong(), anyLong(), any()))
            .thenReturn(newMemory(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L));

        graphQlTester.document(updateWithPrincipal("PROJECT_DEPLOYMENT", 5L))
            .execute()
            .errors()
            .verify();

        verify(aiAutoMemoryService).updateById(
            owner, 3L, 4L, new AiAutoMemoryPatch("New title", null, null, null));
    }

    @Test
    void testUpdatingAProjectDeploymentMemoryWithoutAdminIsReportedAsNotFound() {
        authenticateWith("ROLE_USER");

        assertSingleError(
            updateWithPrincipal("PROJECT_DEPLOYMENT", 5L), ErrorType.NOT_FOUND, "Memory not found");

        verify(aiAutoMemoryService, never()).updateById(any(), anyLong(), anyLong(), any());
    }

    @Test
    void testDeletingAProjectDeploymentMemoryAsAdminReachesTheService() {
        authenticateWith("ROLE_ADMIN");

        graphQlTester.document(deleteWithPrincipal("PROJECT_DEPLOYMENT", 5L))
            .execute()
            .errors()
            .verify();

        verify(aiAutoMemoryService).deleteById(
            new AiAutoMemoryOwner(1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, Environment.PRODUCTION), 3L);
    }

    @Test
    void testDeletingAProjectDeploymentMemoryWithoutAdminIsReportedAsNotFound() {
        authenticateWith("ROLE_USER");

        assertSingleError(
            deleteWithPrincipal("PROJECT_DEPLOYMENT", 5L), ErrorType.NOT_FOUND, "Memory not found");

        verify(aiAutoMemoryService, never()).deleteById(any(), anyLong());
    }

    @Test
    void testMutatingAnotherUsersMemoryIsReportedAsNotFoundWithoutReachingTheService() {
        assertSingleError(updateWithPrincipal("USER", 99L), ErrorType.NOT_FOUND, "Memory not found");
        assertSingleError(deleteWithPrincipal("USER", 99L), ErrorType.NOT_FOUND, "Memory not found");

        verify(aiAutoMemoryService, never()).updateById(any(), anyLong(), anyLong(), any());
        verify(aiAutoMemoryService, never()).deleteById(any(), anyLong());
    }

    @Test
    void testAnAdminCannotAddressAnotherUsersMemory() {
        authenticateWith("ROLE_ADMIN");

        listWithPrincipal("USER", 99L)
            .execute()
            .path("aiAutoMemories")
            .entityList(Object.class)
            .hasSize(0);

        assertSingleError(updateWithPrincipal("USER", 99L), ErrorType.NOT_FOUND, "Memory not found");
        assertSingleError(deleteWithPrincipal("USER", 99L), ErrorType.NOT_FOUND, "Memory not found");

        verify(aiAutoMemoryService, never()).list(any(), any());
        verify(aiAutoMemoryService, never()).updateById(any(), anyLong(), anyLong(), any());
        verify(aiAutoMemoryService, never()).deleteById(any(), anyLong());
    }

    @Test
    void testPrincipalTypesOtherThanUserAndProjectDeploymentAreUnaddressable() {
        List<String> unaddressable = readSchemaEnumValues()
            .stream()
            .filter(value -> !"USER".equals(value) && !"PROJECT_DEPLOYMENT".equals(value))
            .toList();

        assertThat(unaddressable).isNotEmpty();

        for (String principalType : unaddressable) {
            GraphQlTester.Response response = listWithPrincipal(principalType, 5L)
                .execute();

            response.errors()
                .satisfy(errors -> {
                    if (errors.isEmpty()) {
                        response.path("aiAutoMemories")
                            .entityList(Object.class)
                            .hasSize(0);
                    } else {
                        assertThat(errors)
                            .singleElement()
                            .satisfies(error -> assertThat(error.getMessage()).isNotBlank());
                    }
                });
        }

        verify(aiAutoMemoryService, never()).list(any(), any());
    }

    @Test
    void testListWithoutAPrincipalReadsEveryOwnerItMayAddress() {
        givenPrincipalCounts(new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 2));

        when(aiAutoMemoryService.listAllOwners(1L, Environment.DEVELOPMENT, null))
            .thenReturn(List.of(newMemory(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 3L)));

        assertListedMemoryIds(LIST_WITHOUT_PRINCIPAL, "3");

        verify(aiAutoMemoryService, never()).list(any(), any());
    }

    @Test
    void testListWithoutAPrincipalPassesTheMemoryTypeThrough() {
        givenPrincipalCounts(new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 1));

        when(aiAutoMemoryService.listAllOwners(1L, Environment.DEVELOPMENT, AiAutoMemoryType.FEEDBACK))
            .thenReturn(List.of(newMemory(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 3L)));

        assertListedMemoryIds(
            """
                query {
                    aiAutoMemories(workspaceId: "1", environment: 0, memoryType: FEEDBACK) {
                        id
                    }
                }
                """,
            "3");
    }

    @Test
    void testListWithoutAPrincipalDropsOwnersTheCallerCannotAddress() {
        givenPrincipalCounts(
            new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 1),
            new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, 99L, 1));

        when(aiAutoMemoryService.listAllOwners(1L, Environment.DEVELOPMENT, null))
            .thenReturn(
                List.of(
                    newMemory(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 3L),
                    newMemory(AiAutoMemoryPrincipalType.USER, 99L, 4L)));

        assertListedMemoryIds(LIST_WITHOUT_PRINCIPAL, "3");
    }

    @Test
    void testListWithoutAPrincipalDropsIntegrationInstanceMemories() {
        givenPrincipalCounts(
            new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, 1),
            new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, 5L, 1));

        when(aiAutoMemoryService.listAllOwners(1L, Environment.DEVELOPMENT, null))
            .thenReturn(
                List.of(
                    newMemory(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, 3L),
                    newMemory(AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, 5L, 4L)));

        assertListedMemoryIds(LIST_WITHOUT_PRINCIPAL, "3");
    }

    @Test
    void testPrincipalsExcludeOtherUsersAndIntegrationInstances() {
        givenPrincipalCounts(
            new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 2),
            new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, 99L, 5),
            new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, 1),
            new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, 7L, 3));

        GraphQlTester.Response response = graphQlTester.document(PRINCIPALS_QUERY)
            .execute();

        response.path("aiAutoMemoryPrincipals[*].principalId")
            .entityList(Long.class)
            .containsExactly(CURRENT_USER_ID, 5L);
        response.path("aiAutoMemoryPrincipals[*].principalType")
            .entityList(String.class)
            .containsExactly("USER", "PROJECT_DEPLOYMENT");
    }

    @Test
    void testOwnUserPrincipalIsLabelledWithoutTheWordUser() {
        givenPrincipalCounts(new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 2));

        graphQlTester.document(PRINCIPALS_QUERY)
            .execute()
            .path("aiAutoMemoryPrincipals[0].label")
            .entity(String.class)
            .isEqualTo("My memories");
    }

    @Test
    void testListingsDropEmbeddedConnectedUserDeployments() {
        givenDeployments(deployment(5L, 50L, "Orders"), deployment(6L, 60L, "__EMBEDDED__customer-42"));
        givenProjects(project(50L, "Orders"), project(60L, "__EMBEDDED__customer-42"));
        givenPrincipalCounts(
            new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, 1),
            new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 6L, 1));

        when(aiAutoMemoryService.listAllOwners(1L, Environment.DEVELOPMENT, null))
            .thenReturn(
                List.of(
                    newMemory(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, 3L),
                    newMemory(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 6L, 4L)));

        graphQlTester.document(PRINCIPALS_QUERY)
            .execute()
            .path("aiAutoMemoryPrincipals[*].label")
            .entityList(String.class)
            .containsExactly("Orders");

        assertListedMemoryIds(LIST_WITHOUT_PRINCIPAL, "3");
    }

    @Test
    void testADeletedDeploymentsMemoryStaysReachableForCleanup() {
        givenDeployments();
        givenPrincipalCounts(new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 6L, 1));

        when(aiAutoMemoryService.listAllOwners(1L, Environment.DEVELOPMENT, null))
            .thenReturn(List.of(newMemory(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 6L, 3L)));

        graphQlTester.document(PRINCIPALS_QUERY)
            .execute()
            .path("aiAutoMemoryPrincipals[*].label")
            .entityList(String.class)
            .containsExactly("Deployment 6");

        assertListedMemoryIds(LIST_WITHOUT_PRINCIPAL, "3");

        listWithPrincipal("PROJECT_DEPLOYMENT", 6L).execute();

        verify(aiAutoMemoryService).list(
            new AiAutoMemoryOwner(1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 6L, Environment.DEVELOPMENT),
            null);
    }

    @Test
    void testADeploymentWhoseProjectIsGoneIsNotTreatedAsEmbedded() {
        givenDeployments(deployment(6L, 60L, "Orders"));
        givenProjects();
        givenPrincipalCounts(new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 6L, 1));

        graphQlTester.document(PRINCIPALS_QUERY)
            .execute()
            .path("aiAutoMemoryPrincipals[*].label")
            .entityList(String.class)
            .containsExactly("Orders");
    }

    @Test
    void testEmbeddedConnectedUserDeploymentMemoryIsNotAddressable() {
        authenticateWith("ROLE_ADMIN");

        givenDeployments(deployment(5L, 60L, "__EMBEDDED__customer-42"));
        givenProjects(project(60L, "__EMBEDDED__customer-42"));

        listWithPrincipal("PROJECT_DEPLOYMENT", 5L)
            .execute()
            .path("aiAutoMemories")
            .entityList(Object.class)
            .hasSize(0);

        graphQlTester.document(
            """
                query {
                    aiAutoMemory(
                        workspaceId: "1", id: "3", environment: 0,
                        principal: {principalType: PROJECT_DEPLOYMENT, principalId: 5}
                    ) {
                        id
                    }
                }
                """)
            .execute()
            .path("aiAutoMemory")
            .valueIsNull();

        assertSingleError(
            updateWithPrincipal("PROJECT_DEPLOYMENT", 5L), ErrorType.NOT_FOUND, "Memory not found");
        assertSingleError(
            deleteWithPrincipal("PROJECT_DEPLOYMENT", 5L), ErrorType.NOT_FOUND, "Memory not found");

        verify(aiAutoMemoryService, never()).list(any(), any());
        verify(aiAutoMemoryService, never()).findById(any(), anyLong());
        verify(aiAutoMemoryService, never()).updateById(any(), anyLong(), anyLong(), any());
        verify(aiAutoMemoryService, never()).deleteById(any(), anyLong());
    }

    @Test
    void testEveryEntryPointRejectsAWorkspaceTheCallerIsNotAMemberOf() {
        List<String> documents = List.of(
            """
                query {
                    aiAutoMemories(workspaceId: "2", environment: 0) {
                        id
                    }
                }
                """,
            """
                query {
                    aiAutoMemory(workspaceId: "2", id: "3", environment: 0) {
                        id
                    }
                }
                """,
            """
                query {
                    aiAutoMemoryPrincipals(workspaceId: "2", environment: 0) {
                        principalId
                    }
                }
                """,
            """
                mutation {
                    updateAiAutoMemory(
                        input: {id: "3", workspaceId: "2", environment: 0, expectedVersion: 4, title: "New title"}
                    ) {
                        id
                    }
                }
                """,
            """
                mutation {
                    deleteAiAutoMemory(workspaceId: "2", id: "3", environment: 0)
                }
                """);

        for (String document : documents) {
            graphQlTester.document(document)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors)
                    .as(document)
                    .isNotEmpty());
        }

        verifyNoInteractions(aiAutoMemoryService);
    }

    @Test
    void testEverySchemaFieldOfAMemoryIsCoveredBySerialization() {
        List<String> schemaFields = readSchemaTypeFields();

        assertThat(schemaFields).isNotEmpty();
        assertThat(ALL_MEMORY_FIELDS)
            .as("every AiAutoMemory schema field is requested by testListSerializesEveryFieldOfAMemory")
            .contains(schemaFields.toArray(new String[0]));
    }

    private static List<String> readSchemaTypeFields() {
        Matcher matcher = Pattern.compile("type AiAutoMemory \\{([^}]*)}")
            .matcher(readSchema());

        assertThat(matcher.find()).isTrue();

        return Arrays.stream(matcher.group(1)
            .split("\\n"))
            .map(String::trim)
            .filter(line -> line.contains(":"))
            .map(line -> line.substring(0, line.indexOf(':')))
            .filter(field -> !field.isBlank())
            .toList();
    }

    private void assertListedMemoryIds(String document, String... ids) {
        graphQlTester.document(document)
            .execute()
            .path("aiAutoMemories[*].id")
            .entityList(String.class)
            .containsExactly(ids);
    }

    private void givenPrincipalCounts(AiAutoMemoryPrincipalCount... principalCounts) {
        when(aiAutoMemoryService.listPrincipals(1L, Environment.DEVELOPMENT)).thenReturn(List.of(principalCounts));
    }

    private void givenDeployments(ProjectDeployment... projectDeployments) {
        when(projectDeploymentService.getProjectDeployments(any())).thenAnswer(invocation -> {
            List<Long> deploymentIds = invocation.getArgument(0);

            return Arrays.stream(projectDeployments)
                .filter(projectDeployment -> deploymentIds.contains(projectDeployment.getId()))
                .toList();
        });
    }

    private void givenProjects(Project... projects) {
        when(projectService.getProjects(any())).thenAnswer(invocation -> {
            List<Long> projectIds = invocation.getArgument(0);

            return Arrays.stream(projects)
                .filter(project -> projectIds.contains(project.getId()))
                .toList();
        });
    }

    private static ProjectDeployment deployment(long id, long projectId, String name) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(id);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(projectId);

        return projectDeployment;
    }

    private static Project project(long id, String name) {
        Project project = new Project();

        project.setId(id);
        project.setName(name);

        return project;
    }

    private static String updateWithPrincipal(String principalType, long principalId) {
        return "mutation { updateAiAutoMemory(input: {id: \"3\", workspaceId: \"1\", environment: 2, " +
            "expectedVersion: 4, title: \"New title\", principal: {principalType: " + principalType +
            ", principalId: " + principalId + "}}) { id } }";
    }

    private static String deleteWithPrincipal(String principalType, long principalId) {
        return "mutation { deleteAiAutoMemory(workspaceId: \"1\", id: \"3\", environment: 2, " +
            "principal: {principalType: " + principalType + ", principalId: " + principalId + "}) }";
    }

    private static List<String> readSchemaEnumValues() {
        Matcher matcher = Pattern.compile("enum AiAutoMemoryPrincipalType \\{([^}]*)}")
            .matcher(readSchema());

        assertThat(matcher.find()).isTrue();

        return Arrays.stream(matcher.group(1)
            .split("\\s+"))
            .filter(value -> !value.isBlank())
            .toList();
    }

    private static String readSchema() {
        try (InputStream inputStream = AiAutoMemoryGraphQlControllerIntTest.class.getResourceAsStream(
            "/graphql/ai-auto-memory.graphqls")) {

            return new String(
                Objects.requireNonNull(inputStream, "ai-auto-memory.graphqls")
                    .readAllBytes(),
                StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            throw new IllegalStateException("Unable to read ai-auto-memory.graphqls", ioException);
        }
    }

    private static AiAutoMemory newMemory(AiAutoMemoryPrincipalType principalType, long principalId) {
        return newMemory(principalType, principalId, 3L);
    }

    private static AiAutoMemory newMemory(AiAutoMemoryPrincipalType principalType, long principalId, long id) {
        return AiAutoMemory.restore(
            new AiAutoMemoryOwner(1L, principalType, principalId, Environment.DEVELOPMENT), id, "notes", "Notes", null,
            AiAutoMemoryType.USER, "body", LocalDateTime.of(2026, 1, 1, 12, 0), LocalDateTime.of(2026, 1, 1, 12, 0),
            4L);
    }

    private GraphQlTester.Request<?> listWithPrincipal(String principalType, long principalId) {
        return graphQlTester.document(
            "query { aiAutoMemories(workspaceId: \"1\", environment: 0, principal: {principalType: " + principalType +
                ", principalId: " + principalId + "}) { id } }");
    }

    private static void authenticateWith(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "tester", "password", List.of(new SimpleGrantedAuthority(authority))));
    }

    private void assertSingleError(String document, ErrorClassification errorType, String message) {
        graphQlTester.document(document)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors)
                .singleElement()
                .satisfies(error -> assertError(error, errorType, message)));
    }

    private static void assertError(ResponseError error, ErrorClassification errorType, String message) {
        ErrorClassification errorClassification = error.getErrorType();

        assertThat(errorClassification.toString()).isEqualTo(errorType.toString());
        assertThat(error.getMessage()).isEqualTo(message);
    }

    @Configuration
    static class AiAutoMemoryGraphQlTestConfiguration {

        @Bean
        RuntimeWiringConfigurer longScalarWiringConfigurer() {
            return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.longScalar());
        }
    }
}
