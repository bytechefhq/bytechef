/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.web.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionChange;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionCandidate;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionMapping;
import com.bytechef.ee.automation.promotion.dto.PromotionProjectPreview;
import com.bytechef.ee.automation.promotion.dto.PromotionSchemaChange;
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
            "TEST_RESOURCE", 1L, Environment.DEVELOPMENT, Environment.STAGING, 42L, "Existing Resource",
            List.of(new PromotionProjectPreview(100L, "My Project", 3, 2)),
            List.of(
                new PromotionConnectionMapping(200L, "Slack", "slack", 1, 201L, List.of("Workflow / Node"), List.of())),
            List.of("A warning"), List.of(), List.of(), "");

        when(environmentPromotionFacade.preview("TEST_RESOURCE", 1L, 0L, 1L)).thenReturn(preview);

        graphQlTester
            .document("""
                query {
                    environmentPromotionPreview(
                        resourceType: TEST_RESOURCE, sourceId: "1", sourceEnvironmentId: "0",
                        targetEnvironmentId: "1") {
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
            .isEqualTo("TEST_RESOURCE")
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
            .isEqualTo("Existing Resource")
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
    void testPreviewPassesSourceEnvironment() {
        EnvironmentPromotionPreview preview = new EnvironmentPromotionPreview(
            "TEST_RESOURCE", 1L, Environment.DEVELOPMENT, Environment.STAGING, null, null, List.of(), List.of(),
            List.of(), List.of(), List.of(), "");

        when(environmentPromotionFacade.preview("TEST_RESOURCE", 1L, 0L, 1L)).thenReturn(preview);

        graphQlTester
            .document("""
                query {
                    environmentPromotionPreview(
                        resourceType: TEST_RESOURCE, sourceId: "1", sourceEnvironmentId: "0",
                        targetEnvironmentId: "1") {
                        sourceEnvironmentId
                    }
                }
                """)
            .execute()
            .path("environmentPromotionPreview.sourceEnvironmentId")
            .entity(String.class)
            .isEqualTo("0");

        verify(environmentPromotionFacade).preview("TEST_RESOURCE", 1L, 0L, 1L);
    }

    @Test
    void testPreviewReturnsTargetCandidatesAndSchemaChanges() {
        EnvironmentPromotionPreview preview = new EnvironmentPromotionPreview(
            "TEST_RESOURCE", 5L, Environment.DEVELOPMENT, Environment.STAGING, null, null, List.of(),
            List.of(
                new PromotionConnectionMapping(
                    200L, "Slack", "slack", 1, 201L, List.of("Workflow / Node"),
                    List.of(new PromotionConnectionCandidate(201L, "Slack staging")))),
            List.of(),
            List.of(
                new PromotionSchemaChange(
                    "orders", "total", PromotionSchemaChange.Kind.CHANGE_COLUMN_TYPE, "INTEGER", "DECIMAL", true),
                new PromotionSchemaChange(
                    "customers", null, PromotionSchemaChange.Kind.CREATE_TABLE, null, null, false)),
            List.of(), "");

        when(environmentPromotionFacade.preview("TEST_RESOURCE", 5L, 0L, 1L)).thenReturn(preview);

        graphQlTester
            .document("""
                query {
                    environmentPromotionPreview(
                        resourceType: TEST_RESOURCE, sourceId: "5", sourceEnvironmentId: "0",
                        targetEnvironmentId: "1") {
                        connections {
                            targetCandidates {
                                connectionId
                                name
                            }
                        }
                        schemaChanges {
                            tableName
                            columnName
                            kind
                            fromType
                            toType
                        }
                    }
                }
                """)
            .execute()
            .path("environmentPromotionPreview.connections[0].targetCandidates[0].connectionId")
            .entity(String.class)
            .isEqualTo("201")
            .path("environmentPromotionPreview.connections[0].targetCandidates[0].name")
            .entity(String.class)
            .isEqualTo("Slack staging")
            .path("environmentPromotionPreview.schemaChanges[0].tableName")
            .entity(String.class)
            .isEqualTo("orders")
            .path("environmentPromotionPreview.schemaChanges[0].columnName")
            .entity(String.class)
            .isEqualTo("total")
            .path("environmentPromotionPreview.schemaChanges[0].kind")
            .entity(String.class)
            .isEqualTo("CHANGE_COLUMN_TYPE")
            .path("environmentPromotionPreview.schemaChanges[0].fromType")
            .entity(String.class)
            .isEqualTo("INTEGER")
            .path("environmentPromotionPreview.schemaChanges[0].toType")
            .entity(String.class)
            .isEqualTo("DECIMAL")
            .path("environmentPromotionPreview.schemaChanges[1].kind")
            .entity(String.class)
            .isEqualTo("CREATE_TABLE")
            .path("environmentPromotionPreview.schemaChanges[1].columnName")
            .valueIsNull();
    }

    @Test
    void testPromoteToEnvironmentResolvesEveryDeclaredFieldAndFoldsConnectionMappings() {
        EnvironmentPromotionResult result = new EnvironmentPromotionResult(
            55L, true, "https://example.com/test-resource/55", List.of(9L));

        when(environmentPromotionFacade.promote(eq("TEST_RESOURCE"), eq(1L), eq(0L), eq(2L), any(), any()))
            .thenReturn(result);

        graphQlTester
            .document("""
                mutation {
                    promoteToEnvironment(input: {
                        resourceType: TEST_RESOURCE,
                        sourceId: "1",
                        sourceEnvironmentId: "0",
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
            .isEqualTo("https://example.com/test-resource/55")
            .path("promoteToEnvironment.unresolvedConnectionIds")
            .entityList(String.class)
            .containsExactly("9");

        verify(environmentPromotionFacade).promote(
            eq("TEST_RESOURCE"), eq(1L), eq(0L), eq(2L), eq(Map.of(10L, 20L)), isNull());
    }

    @Test
    void testPromoteToEnvironmentWithoutConnectionMappingsPassesAnEmptyMappingAndReturnsAnUpdate() {
        EnvironmentPromotionResult result = new EnvironmentPromotionResult(
            66L, false, "https://example.com/test-resource/66", List.of());

        when(environmentPromotionFacade.promote(eq("TEST_RESOURCE"), eq(3L), eq(0L), eq(1L), any(), any()))
            .thenReturn(result);

        graphQlTester
            .document("""
                mutation {
                    promoteToEnvironment(input: {
                        resourceType: TEST_RESOURCE,
                        sourceId: "3",
                        sourceEnvironmentId: "0",
                        targetEnvironmentId: "1",
                        connectionMappings: []
                    }) {
                        targetId
                        created
                    }
                }
                """)
            .execute()
            .path("promoteToEnvironment.targetId")
            .entity(String.class)
            .isEqualTo("66")
            .path("promoteToEnvironment.created")
            .entity(Boolean.class)
            .isEqualTo(false);

        verify(environmentPromotionFacade).promote(eq("TEST_RESOURCE"), eq(3L), eq(0L), eq(1L), eq(Map.of()), isNull());
    }

    @Test
    void testPreviewReturnsChangesAndHash() {
        EnvironmentPromotionPreview preview = new EnvironmentPromotionPreview(
            "TEST_RESOURCE", 8L, Environment.DEVELOPMENT, Environment.STAGING, null, null, List.of(), List.of(),
            List.of(),
            List.of(
                new PromotionSchemaChange(
                    "orders", "total", PromotionSchemaChange.Kind.DROP_COLUMN, "INTEGER", null, true)),
            List.of(
                new PromotionChange(PromotionChange.Kind.DELETE, "COLUMN", "total", "orders", "total", true),
                new PromotionChange(PromotionChange.Kind.CREATE, "DATA_TABLE", "customers", null, "customers", false)),
            "hash-value");

        when(environmentPromotionFacade.preview("TEST_RESOURCE", 8L, 0L, 1L)).thenReturn(preview);

        graphQlTester
            .document("""
                query {
                    environmentPromotionPreview(
                        resourceType: TEST_RESOURCE, sourceId: "8", sourceEnvironmentId: "0",
                        targetEnvironmentId: "1") {
                        destructiveChangesHash
                        schemaChanges {
                            destructive
                        }
                        changes {
                            kind
                            assetType
                            key
                            parentName
                            name
                            destructive
                        }
                    }
                }
                """)
            .execute()
            .path("environmentPromotionPreview.destructiveChangesHash")
            .entity(String.class)
            .isEqualTo("hash-value")
            .path("environmentPromotionPreview.schemaChanges[0].destructive")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("environmentPromotionPreview.changes[0].kind")
            .entity(String.class)
            .isEqualTo("DELETE")
            .path("environmentPromotionPreview.changes[0].assetType")
            .entity(String.class)
            .isEqualTo("COLUMN")
            .path("environmentPromotionPreview.changes[0].key")
            .entity(String.class)
            .isEqualTo("total")
            .path("environmentPromotionPreview.changes[0].parentName")
            .entity(String.class)
            .isEqualTo("orders")
            .path("environmentPromotionPreview.changes[0].name")
            .entity(String.class)
            .isEqualTo("total")
            .path("environmentPromotionPreview.changes[0].destructive")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("environmentPromotionPreview.changes[1].kind")
            .entity(String.class)
            .isEqualTo("CREATE")
            .path("environmentPromotionPreview.changes[1].parentName")
            .valueIsNull();
    }

    @Test
    void testPromotePassesExpectedHash() {
        EnvironmentPromotionResult result = new EnvironmentPromotionResult(
            88L, false, "https://example.com/test-resource/88", List.of());

        when(environmentPromotionFacade.promote(eq("TEST_RESOURCE"), eq(4L), eq(0L), eq(1L), any(), any()))
            .thenReturn(result);

        graphQlTester
            .document("""
                mutation {
                    promoteToEnvironment(input: {
                        resourceType: TEST_RESOURCE,
                        sourceId: "4",
                        sourceEnvironmentId: "0",
                        targetEnvironmentId: "1",
                        connectionMappings: [],
                        expectedDestructiveChangesHash: "expected-hash"
                    }) {
                        targetId
                    }
                }
                """)
            .execute()
            .path("promoteToEnvironment.targetId")
            .entity(String.class)
            .isEqualTo("88");

        verify(environmentPromotionFacade).promote(
            eq("TEST_RESOURCE"), eq(4L), eq(0L), eq(1L), eq(Map.of()), eq("expected-hash"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        EnvironmentPromotionFacade environmentPromotionFacade() {
            return mock(EnvironmentPromotionFacade.class);
        }
    }
}
