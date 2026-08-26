/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.facade.EnvironmentPromotionFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PromoteToEnvironmentToolCallbackTest {

    private static final long SOURCE_ID = 7L;
    private static final long TARGET_ID = 70L;

    private final EnvironmentPromotionFacade environmentPromotionFacade = mock(EnvironmentPromotionFacade.class);
    private final EnvironmentService environmentService = new EnvironmentService() {

        @Override
        public List<Environment> getEnvironments() {
            return List.of(Environment.values());
        }
    };

    /**
     * Four concrete verbs rather than one generic tool taking a resourceType: a generic tool lets the model pair a
     * correct id with the wrong type, and ids are per-table so a plausible row usually exists in the other one too.
     */
    @Test
    void testEachResourceTypeGetsItsOwnToolName() {
        assertThat(List.of(
            toolName(PromotionResourceType.API_COLLECTION), toolName(PromotionResourceType.MCP_SERVER),
            toolName(PromotionResourceType.A2A_SERVER), toolName(PromotionResourceType.PROJECT_DEPLOYMENT)))
                .containsExactly(
                    "promoteApiCollection", "promoteMcpServer", "promoteA2aServer", "promoteProjectDeployment");
    }

    @Test
    void testPromotesToTheNamedEnvironmentsOrdinal() {
        stub(PromotionResourceType.API_COLLECTION, result(true, List.of()));

        callback(PromotionResourceType.API_COLLECTION)
            .call("""
                {"sourceId": 7, "targetEnvironment": "PRODUCTION"}""");

        verify(environmentPromotionFacade).promote(
            eq(PromotionResourceType.API_COLLECTION), eq(SOURCE_ID), eq((long) Environment.PRODUCTION.ordinal()),
            eq(Map.of()));
    }

    /**
     * A created counterpart is always disabled, connections or no connections. Reporting only "promoted" would leave
     * the user believing traffic is flowing.
     */
    @Test
    void testACreatedCounterpartIsReportedAsDisabled() {
        stub(PromotionResourceType.MCP_SERVER, result(true, List.of()));

        String output = callback(PromotionResourceType.MCP_SERVER)
            .call("""
                {"sourceId": 7, "targetEnvironment": "STAGING"}""");

        assertThat(output).contains("created DISABLED");
    }

    @Test
    void testAnUpdatedCounterpartIsNotReportedAsDisabled() {
        stub(PromotionResourceType.MCP_SERVER, result(false, List.of()));

        String output = callback(PromotionResourceType.MCP_SERVER)
            .call("""
                {"sourceId": 7, "targetEnvironment": "STAGING"}""");

        assertThat(output).doesNotContain("created DISABLED");
    }

    @Test
    void testUnresolvedConnectionsAreReported() {
        stub(PromotionResourceType.A2A_SERVER, result(false, List.of(41L, 42L)));

        String output = callback(PromotionResourceType.A2A_SERVER)
            .call("""
                {"sourceId": 7, "targetEnvironment": "PRODUCTION"}""");

        assertThat(output).contains("41", "42", "could not be matched in PRODUCTION");
    }

    @Test
    void testConnectionMappingsAreForwardedWithNumericKeys() {
        stub(PromotionResourceType.API_COLLECTION, result(false, List.of()));

        callback(PromotionResourceType.API_COLLECTION)
            .call("""
                {"sourceId": 7, "targetEnvironment": "PRODUCTION", "connectionMappings": {"11": 22}}""");

        verify(environmentPromotionFacade).promote(
            eq(PromotionResourceType.API_COLLECTION), eq(SOURCE_ID), eq((long) Environment.PRODUCTION.ordinal()),
            eq(Map.of(11L, 22L)));
    }

    @Test
    void testAnUnknownEnvironmentNameIsAToolErrorAndWritesNothing() {
        String output = callback(PromotionResourceType.API_COLLECTION)
            .call("""
                {"sourceId": 7, "targetEnvironment": "PREPROD"}""");

        assertThat(output).contains("Unknown environment name");

        verify(environmentPromotionFacade, never()).promote(
            any(), anyLong(),
            anyLong(), any());
    }

    @Test
    void testAMissingSourceIdWritesNothing() {
        String output = callback(PromotionResourceType.API_COLLECTION)
            .call("""
                {"targetEnvironment": "PRODUCTION"}""");

        assertThat(output).contains("sourceId is required");

        verify(environmentPromotionFacade, never()).promote(
            any(), anyLong(),
            anyLong(), any());
    }

    private PromoteToEnvironmentToolCallback callback(PromotionResourceType promotionResourceType) {
        return new PromoteToEnvironmentToolCallback(
            promotionResourceType, environmentPromotionFacade, environmentService);
    }

    private String toolName(PromotionResourceType promotionResourceType) {
        ToolDefinition toolDefinition = callback(promotionResourceType).getToolDefinition();

        return toolDefinition.name();
    }

    private static EnvironmentPromotionResult result(boolean created, List<Long> unresolvedConnectionIds) {
        return new EnvironmentPromotionResult(TARGET_ID, created, null, unresolvedConnectionIds);
    }

    private void stub(PromotionResourceType promotionResourceType, EnvironmentPromotionResult result) {
        when(
            environmentPromotionFacade.preview(
                eq(promotionResourceType), eq(SOURCE_ID), anyLong()))
                    .thenReturn(
                        new EnvironmentPromotionPreview(
                            promotionResourceType, SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, null,
                            null, List.of(), List.of(), List.of()));
        when(
            environmentPromotionFacade.promote(
                eq(promotionResourceType), eq(SOURCE_ID), anyLong(),
                any()))
                    .thenReturn(result);
    }
}
