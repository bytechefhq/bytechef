/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SourceBinding;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionMapping;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PromotionPreviewsTest {

    private final ConnectionService connectionService = mock(ConnectionService.class);

    @Test
    void testNoBindingsProduceNoMappingsAndNoConnectionLookup() {
        assertThat(PromotionPreviews.connectionMappings(List.of(), Map.of(), Map.of(), connectionService)).isEmpty();

        verifyNoInteractions(connectionService);
    }

    @Test
    void testBindingsAreGroupedByConnectionAndLoadedInOneCall() {
        when(connectionService.getConnections(List.of(11L, 12L)))
            .thenReturn(List.of(connection(11L, "Sheets", "googleSheets", 1), connection(12L, "Slack", "slack", 2)));

        List<PromotionConnectionMapping> connectionMappings = PromotionPreviews.connectionMappings(
            List.of(
                new SourceBinding("uuid-1", "Orders", "googleSheets_1", "connectionId", 11L),
                new SourceBinding("uuid-2", "Invoices", "googleSheets_1", "connectionId", 11L),
                new SourceBinding("uuid-2", "Invoices", "slack_1", "connectionId", 12L)),
            Map.of(), Map.of(), connectionService);

        assertThat(connectionMappings)
            .containsExactly(
                new PromotionConnectionMapping(
                    11L, "Sheets", "googleSheets", 1, null,
                    List.of("Orders › googleSheets_1", "Invoices › googleSheets_1")),
                new PromotionConnectionMapping(12L, "Slack", "slack", 2, null, List.of("Invoices › slack_1")));

        verify(connectionService).getConnections(List.of(11L, 12L));
    }

    @Test
    void testOneNodeBoundUnderTwoKeysIsListedOnce() {
        when(connectionService.getConnections(List.of(11L)))
            .thenReturn(List.of(connection(11L, "Sheets", "googleSheets", 1)));

        List<PromotionConnectionMapping> connectionMappings = PromotionPreviews.connectionMappings(
            List.of(
                new SourceBinding("uuid-1", "Orders", "googleSheets_1", "connectionId", 11L),
                new SourceBinding("uuid-1", "Orders", "googleSheets_1", "otherConnectionId", 11L)),
            Map.of(), Map.of(), connectionService);

        assertThat(connectionMappings)
            .singleElement()
            .extracting(PromotionConnectionMapping::usedBy)
            .isEqualTo(List.of("Orders › googleSheets_1"));
    }

    @Test
    void testExistingTargetBindingOutranksSuggestion() {
        when(connectionService.getConnections(List.of(11L)))
            .thenReturn(List.of(connection(11L, "Sheets", "googleSheets", 1)));

        List<PromotionConnectionMapping> connectionMappings = PromotionPreviews.connectionMappings(
            List.of(new SourceBinding("uuid-1", "Orders", "googleSheets_1", "connectionId", 11L)),
            Map.of(11L, 91L), Map.of(11L, 92L), connectionService);

        assertThat(connectionMappings)
            .singleElement()
            .extracting(PromotionConnectionMapping::suggestedTargetConnectionId)
            .isEqualTo(91L);
    }

    @Test
    void testSuggestionIsUsedWhenTheTargetHasNoBindingYet() {
        when(connectionService.getConnections(List.of(11L)))
            .thenReturn(List.of(connection(11L, "Sheets", "googleSheets", 1)));

        List<PromotionConnectionMapping> connectionMappings = PromotionPreviews.connectionMappings(
            List.of(new SourceBinding("uuid-1", "Orders", "googleSheets_1", "connectionId", 11L)),
            Map.of(), Map.of(11L, 92L), connectionService);

        assertThat(connectionMappings)
            .singleElement()
            .extracting(PromotionConnectionMapping::suggestedTargetConnectionId)
            .isEqualTo(92L);
    }

    @Test
    void testExtraUsagesContributeConnectionsWithNoSourceBinding() {
        when(connectionService.getConnections(List.of(11L, 13L)))
            .thenReturn(List.of(connection(11L, "Sheets", "googleSheets", 1), connection(13L, "Jira", "jira", 1)));

        List<PromotionConnectionMapping> connectionMappings = PromotionPreviews.connectionMappings(
            List.of(new SourceBinding("uuid-1", "Orders", "googleSheets_1", "connectionId", 11L)),
            Map.of(), Map.of(), Map.of(13L, List.of("component: jira")), connectionService);

        assertThat(connectionMappings)
            .extracting(PromotionConnectionMapping::sourceConnectionId)
            .containsExactly(11L, 13L);
        assertThat(connectionMappings.get(1)
            .usedBy()).containsExactly("component: jira");
    }

    @Test
    void testABindingWithNoWorkflowUuidStillProducesAMapping() {
        when(connectionService.getConnections(List.of(11L)))
            .thenReturn(List.of(connection(11L, "Sheets", "googleSheets", 1)));

        List<PromotionConnectionMapping> connectionMappings = PromotionPreviews.connectionMappings(
            List.of(new SourceBinding(null, "Orders", "googleSheets_1", "connectionId", 11L)), Map.of(), Map.of(),
            connectionService);

        assertThat(connectionMappings)
            .singleElement()
            .extracting(PromotionConnectionMapping::sourceConnectionId)
            .isEqualTo(11L);
    }

    private static Connection connection(long id, String name, String componentName, int connectionVersion) {
        Connection connection = new Connection();

        connection.setId(id);
        connection.setName(name);
        connection.setComponentName(componentName);
        connection.setConnectionVersion(connectionVersion);

        return connection;
    }
}
