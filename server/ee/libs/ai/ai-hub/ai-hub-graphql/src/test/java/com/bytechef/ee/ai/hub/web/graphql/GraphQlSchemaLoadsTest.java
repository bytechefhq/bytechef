/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.graphql.execution.GraphQlSource;

/**
 * Builds the schema from the three AI Hub schema files plus the platform base schema, so a type or field that
 * references a definition living only in a schema this slice does not ship fails here rather than at application
 * startup.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class GraphQlSchemaLoadsTest {

    private static final List<String> MUTATION_NAMES = List.of(
        "createAiHubChat", "updateAiHubChat", "generateAiHubChatTitle", "deleteAiHubChat",
        "truncateAiHubChatMessages", "appendAiHubChatAssistantMessage", "cancelAiHubRun",
        "recordReferencedAiHubChatArtifact", "deleteAiHubChatArtifact", "resolveAiHubToolApproval",
        "createAiHubToolApprovalRule", "deleteAiHubToolApprovalRule", "createWorkflowChatAiHubChat",
        "bulkArchiveWorkflowChatAiHubChats", "cancelWorkflowChatTurn");

    private static final List<String> QUERY_NAMES = List.of(
        "aiHubChats", "aiHubChatMessages", "aiHubChatArtifactsByAiHubChat", "aiHubToolApprovals",
        "aiHubToolApprovalRules", "aiHubToolApprovalDefaultToolNames");

    @Test
    void testSchemaBuildsWithNoUnresolvedTypes() {
        GraphQLSchema schema = buildSchema();

        assertThat(schema.getType("AiHubChat")).isNotNull();
        assertThat(schema.getType("AiHubChatMessage")).isNotNull();
        assertThat(schema.getType("AiHubChatArtifact")).isNotNull();
        assertThat(schema.getType("AiHubToolApproval")).isNotNull();
        assertThat(schema.getType("AiHubToolApprovalRule")).isNotNull();
    }

    @Test
    void testQueryAndMutationSurfaceIsExactlyTheTrimmedSet() {
        GraphQLSchema schema = buildSchema();

        assertThat(fieldNames(schema.getQueryType())).containsAll(QUERY_NAMES)
            .filteredOn(name -> name.startsWith("aiHub"))
            .containsExactlyInAnyOrderElementsOf(QUERY_NAMES);
        assertThat(fieldNames(schema.getMutationType())).containsAll(MUTATION_NAMES)
            .filteredOn(name -> !name.startsWith("_"))
            .containsExactlyInAnyOrderElementsOf(MUTATION_NAMES);
    }

    @Test
    void testRemovedTypesAndFieldsAreAbsent() {
        GraphQLSchema schema = buildSchema();

        assertThat(schema.getType("AiHubChatVisibility")).isNull();
        assertThat(schema.getType("AiHubChatParticipation")).isNull();
        assertThat(schema.getType("AiHubChatArtifactPage")).isNull();
        assertThat(fieldNames((GraphQLObjectType) schema.getType("AiHubChat")))
            .doesNotContain("visibility", "participation", "ownerName");
        assertThat(fieldNames((GraphQLObjectType) schema.getType("AiHubChatMessage")))
            .doesNotContain("authorName", "authorUserId");
    }

    @Test
    void testWorkflowChatTypesArePresent() {
        GraphQLSchema schema = buildSchema();

        GraphQLEnumType chatKind = (GraphQLEnumType) schema.getType("AiHubChatKind");

        assertThat(chatKind.getValues()).extracting(value -> value.getName())
            .containsExactly("STANDARD", "WORKFLOW_CHAT");
        assertThat(fieldNames((GraphQLObjectType) schema.getType("AiHubChat")))
            .contains("kind", "workflowExecutionId", "projectDeploymentId");
    }

    private static GraphQLSchema buildSchema() {
        Resource[] resources = {
            new ClassPathResource("graphql/base.graphqls"), new ClassPathResource("graphql/long.graphqls"),
            new ClassPathResource("graphql/ai-hub-chat.graphqls"),
            new ClassPathResource("graphql/ai-hub-artifact.graphqls"),
            new ClassPathResource("graphql/ai-hub-tool-approval.graphqls")
        };

        return GraphQlSource.schemaResourceBuilder()
            .schemaResources(resources)
            .configureRuntimeWiring(wiringBuilder -> wiringBuilder.scalar(AiHubGraphQlTestConfiguration.longScalar()))
            .build()
            .schema();
    }

    private static List<String> fieldNames(GraphQLObjectType objectType) {
        return objectType.getFieldDefinitions()
            .stream()
            .map(fieldDefinition -> fieldDefinition.getName())
            .toList();
    }
}
