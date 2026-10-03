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
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryConcurrentModificationException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryNotFoundException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import graphql.ErrorClassification;
import java.time.LocalDateTime;
import java.util.List;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Runs the controller through Spring GraphQL, so argument binding and the controller's exception handlers are exercised
 * the way a client sees them rather than by calling the methods directly.
 *
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

    /**
     * Serializes a real memory through the schema: enum fields as their names, {@code version} as the number the client
     * sends back as {@code expectedVersion}, and timestamps as UTC epoch milliseconds, which the client decodes as UTC.
     */
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

    /**
     * A principal is a type and an id together; the schema refuses half of one before the controller runs.
     */
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

    /**
     * The Memories page sends the version its edit was based on, and a memory changed since must come back as a message
     * the user can act on — not the agent-facing one, which tells the reader to redo an edit, wrong after a delete.
     */
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

    /**
     * An update without the version it was based on would overwrite whatever another writer stored meanwhile, so the
     * schema refuses it before the controller runs.
     */
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
