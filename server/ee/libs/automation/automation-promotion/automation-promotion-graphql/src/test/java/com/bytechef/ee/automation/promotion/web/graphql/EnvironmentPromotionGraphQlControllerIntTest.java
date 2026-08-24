/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.web.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionMapping;
import com.bytechef.ee.automation.promotion.dto.PromotionProjectPreview;
import com.bytechef.ee.automation.promotion.facade.EnvironmentPromotionFacade;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * Loads the real {@code environment-promotion.graphqls} schema (via {@code spring.graphql.schema.locations}) into a
 * live {@link graphql.schema.GraphQLSchema} and executes queries/mutations against it end to end, proving the schema
 * parses, the {@code extend type Query/Mutation} blocks merge, and every field the schema declares is actually resolved
 * by {@link EnvironmentPromotionGraphQlController} — a mismatch here would only otherwise surface at server startup.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    EnvironmentPromotionGraphQlControllerIntTest.TestConfig.class,
    EnvironmentPromotionGraphQlController.class
})
@GraphQlTest(
    controllers = EnvironmentPromotionGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class EnvironmentPromotionGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private EnvironmentPromotionFacade environmentPromotionFacade;

    @Test
    void testEnvironmentPromotionPreviewResolvesEveryDeclaredField() {
        EnvironmentPromotionPreview preview = new EnvironmentPromotionPreview(
            PromotionResourceType.API_COLLECTION, 1L, Environment.DEVELOPMENT, Environment.STAGING, 42L,
            "Existing Collection", List.of(new PromotionProjectPreview(100L, "My Project", 3, 2)),
            List.of(new PromotionConnectionMapping(200L, "Slack", "slack", 1, 201L, List.of("Workflow / Node"))),
            List.of("A warning"));

        when(environmentPromotionFacade.preview(PromotionResourceType.API_COLLECTION, 1L, 1L)).thenReturn(preview);

        graphQlTester
            .document("""
                query {
                    environmentPromotionPreview(resourceType: API_COLLECTION, sourceId: "1", targetEnvironmentId: "1") {
                        resourceType
                        sourceId
                        sourceEnvironmentId
                        targetEnvironmentId
                        existingTargetId
                        existingTargetName
                        projects {
                            projectId
                            projectName
                            sourceProjectVersion
                            targetProjectVersion
                        }
                        connections {
                            sourceConnectionId
                            sourceConnectionName
                            componentName
                            connectionVersion
                            suggestedTargetConnectionId
                            usedBy
                        }
                        warnings
                    }
                }
                """)
            .execute()
            .path("environmentPromotionPreview.resourceType")
            .entity(String.class)
            .isEqualTo("API_COLLECTION")
            .path("environmentPromotionPreview.sourceEnvironmentId")
            .entity(String.class)
            .isEqualTo("0")
            .path("environmentPromotionPreview.targetEnvironmentId")
            .entity(String.class)
            .isEqualTo("1")
            .path("environmentPromotionPreview.existingTargetId")
            .entity(String.class)
            .isEqualTo("42")
            .path("environmentPromotionPreview.existingTargetName")
            .entity(String.class)
            .isEqualTo("Existing Collection")
            .path("environmentPromotionPreview.projects[0].projectName")
            .entity(String.class)
            .isEqualTo("My Project")
            .path("environmentPromotionPreview.projects[0].targetProjectVersion")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("environmentPromotionPreview.connections[0].sourceConnectionName")
            .entity(String.class)
            .isEqualTo("Slack")
            .path("environmentPromotionPreview.connections[0].usedBy")
            .entityList(String.class)
            .containsExactly("Workflow / Node")
            .path("environmentPromotionPreview.warnings")
            .entityList(String.class)
            .containsExactly("A warning");
    }

    @Test
    void testPromoteToEnvironmentResolvesEveryDeclaredFieldAndFoldsConnectionMappings() {
        EnvironmentPromotionResult result = new EnvironmentPromotionResult(
            55L, true, "https://example.com/mcp", List.of(9L));

        when(environmentPromotionFacade.promote(eq(PromotionResourceType.MCP_SERVER), eq(1L), eq(2L), any()))
            .thenReturn(result);

        graphQlTester
            .document("""
                mutation {
                    promoteToEnvironment(input: {
                        resourceType: MCP_SERVER,
                        sourceId: "1",
                        targetEnvironmentId: "2",
                        connectionMappings: [
                            { sourceConnectionId: "10", targetConnectionId: "20" }
                        ]
                    }) {
                        targetId
                        created
                        targetUrl
                        unresolvedConnectionIds
                    }
                }
                """)
            .execute()
            .path("promoteToEnvironment.targetId")
            .entity(String.class)
            .isEqualTo("55")
            .path("promoteToEnvironment.created")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("promoteToEnvironment.targetUrl")
            .entity(String.class)
            .isEqualTo("https://example.com/mcp")
            .path("promoteToEnvironment.unresolvedConnectionIds")
            .entityList(String.class)
            .containsExactly("9");

        verify(environmentPromotionFacade).promote(
            eq(PromotionResourceType.MCP_SERVER), eq(1L), eq(2L), eq(Map.of(10L, 20L)));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        EnvironmentPromotionFacade environmentPromotionFacade() {
            return mock(EnvironmentPromotionFacade.class);
        }
    }
}
