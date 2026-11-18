/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.auto.memory.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.auto.memory.domain.EmbeddedAiAutoMemory;
import com.bytechef.ee.embedded.ai.auto.memory.domain.EmbeddedAiAutoMemoryConnectedUser;
import com.bytechef.ee.embedded.ai.auto.memory.facade.EmbeddedAiAutoMemoryFacade;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    EmbeddedAiAutoMemoryGraphQlControllerIntTest.ScalarConfiguration.class,
    EmbeddedAiAutoMemoryGraphQlController.class
})
@GraphQlTest(
    controllers = EmbeddedAiAutoMemoryGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class EmbeddedAiAutoMemoryGraphQlControllerIntTest {

    @MockitoBean
    private EmbeddedAiAutoMemoryFacade embeddedAiAutoMemoryFacade;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testListSerializesTheMemoryWithItsConnectedUserAndIntegration() {
        when(embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemories(Environment.PRODUCTION, null, null))
            .thenReturn(List.of(newEmbeddedAiAutoMemory()));

        GraphQlTester.Response response = graphQlTester
            .document(
                """
                    query {
                        embeddedAiAutoMemories(environment: 2) {
                            id
                            name
                            title
                            description
                            memoryType
                            content
                            environmentId
                            version
                            createdAt
                            updatedAt
                            integrationInstanceId
                            connectedUser { id name email externalId }
                            integration { id name componentName }
                        }
                    }
                    """)
            .execute();

        Map<String, Object> memory = response.path("embeddedAiAutoMemories[0]")
            .entity(Map.class)
            .get();

        assertThat(memory)
            .containsEntry("id", "3")
            .containsEntry("name", "customer_plan")
            .containsEntry("title", "Plan")
            .containsEntry("description", "Which plan the customer is on")
            .containsEntry("memoryType", "USER")
            .containsEntry("content", "Customer is on the Pro plan.")
            .containsEntry("environmentId", 2)
            .containsEntry("version", 4)
            .containsEntry("integrationInstanceId", 11)
            .containsEntry("connectedUser", Map.of(
                "id", "21", "name", "Ada", "email", "ada@example.com", "externalId", "ext-ada"))
            .containsEntry("integration", Map.of("id", "31", "name", "Memory Demo", "componentName", "memoryDemo"));
        assertThat(memory.get("createdAt")).isNotNull();
        assertThat(memory.get("updatedAt")).isNotNull();
    }

    @Test
    void testListReturnsNullConnectedUserAndIntegrationWhenTheyAreGone() {
        AiAutoMemory aiAutoMemory = newAiAutoMemory();

        when(embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemories(Environment.PRODUCTION, null, null))
            .thenReturn(List.of(
                new EmbeddedAiAutoMemory(aiAutoMemory, null, null, null, null, null, null, null)));

        graphQlTester
            .document(
                "query { embeddedAiAutoMemories(environment: 2) { id connectedUser { id } integration { id } } }")
            .execute()
            .path("embeddedAiAutoMemories[0].connectedUser")
            .valueIsNull()
            .path("embeddedAiAutoMemories[0].integration")
            .valueIsNull();
    }

    @Test
    void testListPassesTheFiltersThrough() {
        when(embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemories(Environment.DEVELOPMENT, 21L, AiAutoMemoryType.USER))
            .thenReturn(List.of());

        graphQlTester
            .document(
                """
                    query {
                        embeddedAiAutoMemories(environment: 0, connectedUserId: "21", memoryType: USER) { id }
                    }
                    """)
            .execute()
            .path("embeddedAiAutoMemories")
            .entityList(Object.class)
            .hasSize(0);

        verify(embeddedAiAutoMemoryFacade).getEmbeddedAiAutoMemories(
            Environment.DEVELOPMENT, 21L, AiAutoMemoryType.USER);
    }

    @Test
    void testConnectedUsersListsEveryUserWithItsMemoryCount() {
        when(embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemoryConnectedUsers(Environment.STAGING))
            .thenReturn(List.of(new EmbeddedAiAutoMemoryConnectedUser(21L, "Ada", "ada@example.com", "ext-ada", 2)));

        Map<String, Object> connectedUser = graphQlTester
            .document(
                """
                    query {
                        embeddedAiAutoMemoryConnectedUsers(environment: 1) { id name email externalId memoryCount }
                    }
                    """)
            .execute()
            .path("embeddedAiAutoMemoryConnectedUsers[0]")
            .entity(Map.class)
            .get();

        assertThat(connectedUser)
            .containsEntry("id", "21")
            .containsEntry("name", "Ada")
            .containsEntry("email", "ada@example.com")
            .containsEntry("externalId", "ext-ada")
            .containsEntry("memoryCount", 2);
    }

    @Test
    void testUpdateMapsTheInputToAPatch() {
        when(embeddedAiAutoMemoryFacade.updateEmbeddedAiAutoMemory(
            eq(Environment.PRODUCTION), eq(3L), eq(4L), eq(
                new AiAutoMemoryPatch("New title", "New description", AiAutoMemoryType.FEEDBACK, "New content"))))
                    .thenReturn(newEmbeddedAiAutoMemory());

        graphQlTester
            .document(
                """
                    mutation {
                        updateEmbeddedAiAutoMemory(input: {
                            id: "3", environment: 2, expectedVersion: 4, title: "New title",
                            description: "New description", memoryType: FEEDBACK, content: "New content"
                        }) { id }
                    }
                    """)
            .execute()
            .path("updateEmbeddedAiAutoMemory.id")
            .entity(String.class)
            .isEqualTo("3");
    }

    @Test
    void testDeleteReturnsTrue() {
        graphQlTester
            .document("mutation { deleteEmbeddedAiAutoMemory(id: \"3\", environment: 2) }")
            .execute()
            .path("deleteEmbeddedAiAutoMemory")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(embeddedAiAutoMemoryFacade).deleteEmbeddedAiAutoMemory(Environment.PRODUCTION, 3L);
    }

    @Test
    void testUnknownEnvironmentIsABadRequest() {
        graphQlTester
            .document("mutation { deleteEmbeddedAiAutoMemory(id: \"3\", environment: 9) }")
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors)
                .singleElement()
                .satisfies(error -> assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST)));
    }

    @Test
    void testMissingMemoryIsABadRequest() {
        when(embeddedAiAutoMemoryFacade.updateEmbeddedAiAutoMemory(
            eq(Environment.PRODUCTION), anyLong(), anyLong(), eq(new AiAutoMemoryPatch("x", null, null, null))))
                .thenThrow(new IllegalArgumentException("Embedded AI memory 3 not found"));

        graphQlTester
            .document(
                """
                    mutation {
                        updateEmbeddedAiAutoMemory(input: {id: "3", environment: 2, expectedVersion: 4, title: "x"}) {
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
                    assertThat(error.getMessage()).isEqualTo("Embedded AI memory 3 not found");
                }));
    }

    private static AiAutoMemory newAiAutoMemory() {
        return AiAutoMemory.restore(
            new AiAutoMemoryOwner(1049L, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, 11L, Environment.PRODUCTION),
            3L, "customer_plan", "Plan", "Which plan the customer is on", AiAutoMemoryType.USER,
            "Customer is on the Pro plan.", LocalDateTime.of(2026, 1, 1, 12, 0), LocalDateTime.of(2026, 1, 1, 12, 5),
            4L);
    }

    private static EmbeddedAiAutoMemory newEmbeddedAiAutoMemory() {
        return new EmbeddedAiAutoMemory(
            newAiAutoMemory(), 21L, "Ada", "ada@example.com", "ext-ada", 31L, "Memory Demo", "memoryDemo");
    }

    @TestConfiguration
    static class ScalarConfiguration {

        @Bean
        RuntimeWiringConfigurer longScalarWiringConfigurer() {
            return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.longScalar());
        }
    }
}
