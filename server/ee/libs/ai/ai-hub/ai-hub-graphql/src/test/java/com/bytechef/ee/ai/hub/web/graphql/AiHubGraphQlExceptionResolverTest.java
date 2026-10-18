/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.exception.ForbiddenException;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.ee.ai.hub.exception.TitleGenerationFailedException;
import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubGraphQlExceptionResolverTest {

    private final AiHubGraphQlExceptionResolver resolver = new AiHubGraphQlExceptionResolver();

    @Test
    void testResolvesTitleGenerationFailureToATypedErrorCode() {
        GraphQLError error = resolver.resolveToSingleError(
            new TitleGenerationFailedException("Title generation failed: boom", new RuntimeException("boom")),
            newEnvironment());

        assertThat(error).isNotNull();

        Map<String, Object> extensions = error.getExtensions();

        assertThat(extensions).containsEntry(
            "errorCode", AiHubGraphQlExceptionResolver.TITLE_GENERATION_FAILED_ERROR_CODE);
    }

    /**
     * The provider's own message can carry account and billing detail - an exhausted-credit 400 reading "Your credit
     * balance is too low" is what first exposed this path - so the resolved error must not forward it to the browser.
     */
    @Test
    void testDoesNotForwardTheUpstreamProviderMessage() {
        GraphQLError error = resolver.resolveToSingleError(
            new TitleGenerationFailedException(
                "Title generation failed: 400 Your credit balance is too low to access the Anthropic API",
                new RuntimeException("400")),
            newEnvironment());

        assertThat(error).isNotNull();
        assertThat(error.getMessage())
            .doesNotContain("credit balance")
            .doesNotContain("Anthropic");
    }

    /**
     * Returning null is what lets the remaining resolvers in the chain, including the CE global one, see the exception.
     * Claiming everything here would swallow types this class knows nothing about.
     */
    @Test
    void testDeclinesUnrelatedExceptions() {
        GraphQLError error = resolver.resolveToSingleError(
            new IllegalStateException("unrelated"), newEnvironment());

        assertThat(error).isNull();
    }

    @Test
    void testMapsNotFoundToNotFoundWithoutTheResourceDetail() {
        GraphQLError error = resolver.resolveToSingleError(
            new NotFoundException("ai_hub_chat", 42L, "No chat 42 for user 9"), newEnvironment());

        assertThat(error).isNotNull();
        assertThat(error.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(error.getMessage()).isEqualTo("Not found");
        assertThat(error.getExtensions()).containsEntry("errorCode", "NOT_FOUND");
    }

    @Test
    void testMapsForbiddenToForbiddenWithAFixedMessage() {
        GraphQLError error = resolver.resolveToSingleError(
            new ForbiddenException("ai_hub_chat", 42L, "Chat 42 belongs to user 3"), newEnvironment());

        assertThat(error).isNotNull();
        assertThat(error.getErrorType()).isEqualTo(ErrorType.FORBIDDEN);
        assertThat(error.getMessage()).isEqualTo("Access denied");
    }

    @Test
    void testMapsAccessDeniedToForbidden() {
        GraphQLError error = resolver.resolveToSingleError(new AccessDeniedException("denied"), newEnvironment());

        assertThat(error).isNotNull();
        assertThat(error.getErrorType()).isEqualTo(ErrorType.FORBIDDEN);
    }

    @Test
    void testMapsConflictToTheConflictClassification() {
        GraphQLError error = resolver.resolveToSingleError(
            new ConflictException("Approval has expired"), newEnvironment());

        assertThat(error).isNotNull();
        assertThat(error.getErrorType()).hasToString("CONFLICT");
        assertThat(error.getMessage()).isEqualTo("Approval has expired");
        assertThat(error.getExtensions()).containsEntry("errorCode", "CONFLICT");
    }

    @Test
    void testMapsIllegalArgumentToBadRequest() {
        GraphQLError error = resolver.resolveToSingleError(
            new IllegalArgumentException("Only reference artifacts can be deleted"), newEnvironment());

        assertThat(error).isNotNull();
        assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(error.getMessage()).isEqualTo("Only reference artifacts can be deleted");
    }

    @Test
    void testLeavesAnIllegalArgumentFromANonHubFieldToTheOtherResolvers() {
        GraphQLError error = resolver.resolveToSingleError(
            new IllegalArgumentException("Invalid project id"), newEnvironment("projectById", "Query"));

        assertThat(error).isNull();
    }

    @Test
    void testLeavesAnAccessDeniedFromANonHubFieldToTheOtherResolvers() {
        GraphQLError error = resolver.resolveToSingleError(
            new AccessDeniedException("denied"), newEnvironment("deleteProject", "Mutation"));

        assertThat(error).isNull();
    }

    @Test
    void testMapsIllegalArgumentFromAFieldOfAHubType() {
        GraphQLError error = resolver.resolveToSingleError(
            new IllegalArgumentException("Unknown kind"), newEnvironment("kind", "AiHubChatArtifact"));

        assertThat(error).isNotNull();
        assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @Test
    void testMapsIllegalArgumentFromAHubQuery() {
        GraphQLError error = resolver.resolveToSingleError(
            new IllegalArgumentException("Bad status"), newEnvironment("aiHubChats", "Query"));

        assertThat(error).isNotNull();
        assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    private static DataFetchingEnvironment newEnvironment() {
        return newEnvironment("generateAiHubChatTitle", "Mutation");
    }

    private static DataFetchingEnvironment newEnvironment(String fieldName, String parentTypeName) {
        DataFetchingEnvironment environment = Mockito.mock(DataFetchingEnvironment.class);

        Mockito.when(environment.getField())
            .thenReturn(new Field(fieldName));
        Mockito.when(environment.getParentType())
            .thenReturn(GraphQLObjectType.newObject()
                .name(parentTypeName)
                .build());
        Mockito.when(environment.getExecutionStepInfo())
            .thenReturn(Mockito.mock(ExecutionStepInfo.class));

        return environment;
    }
}
