/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
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
import com.bytechef.ee.automation.promotion.web.graphql.EnvironmentPromotionGraphQlController.EnvironmentPromotionPreviewModel;
import com.bytechef.ee.automation.promotion.web.graphql.EnvironmentPromotionGraphQlController.PromoteToEnvironmentInput;
import com.bytechef.ee.automation.promotion.web.graphql.EnvironmentPromotionGraphQlController.PromotionConnectionMappingInput;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EnvironmentPromotionGraphQlControllerTest {

    private final EnvironmentPromotionFacade environmentPromotionFacade = mock(EnvironmentPromotionFacade.class);

    private final EnvironmentPromotionGraphQlController environmentPromotionGraphQlController =
        new EnvironmentPromotionGraphQlController(environmentPromotionFacade);

    @Test
    void testEnvironmentPromotionPreviewMapsEnvironmentsToOrdinals() {
        PromotionProjectPreview project = new PromotionProjectPreview(10L, "Project", 3, 2);
        PromotionConnectionMapping connection = new PromotionConnectionMapping(
            20L, "Slack Connection", "slack", 1, 21L, List.of("Workflow / Node"));
        EnvironmentPromotionPreview preview = new EnvironmentPromotionPreview(
            PromotionResourceType.API_COLLECTION, 1L, Environment.STAGING, Environment.PRODUCTION, 5L, "Existing",
            List.of(project), List.of(connection), List.of("A warning"));

        when(environmentPromotionFacade.preview(PromotionResourceType.API_COLLECTION, 1L, 2L)).thenReturn(preview);

        EnvironmentPromotionPreviewModel model = environmentPromotionGraphQlController.environmentPromotionPreview(
            PromotionResourceType.API_COLLECTION, 1L, 2L);

        assertThat(model.resourceType()).isEqualTo(PromotionResourceType.API_COLLECTION);
        assertThat(model.sourceId()).isEqualTo(1L);
        assertThat(model.sourceEnvironmentId()).isEqualTo(Environment.STAGING.ordinal());
        assertThat(model.targetEnvironmentId()).isEqualTo(Environment.PRODUCTION.ordinal());
        assertThat(model.existingTargetId()).isEqualTo(5L);
        assertThat(model.existingTargetName()).isEqualTo("Existing");
        assertThat(model.projects()).containsExactly(project);
        assertThat(model.connections()).containsExactly(connection);
        assertThat(model.warnings()).containsExactly("A warning");
    }

    @Test
    void testEnvironmentPromotionPreviewMapsDevelopmentOrdinalZero() {
        // DEVELOPMENT is ordinal 0. Asserting on it (rather than only on STAGING/PRODUCTION above) guards against a
        // mapping that mistakes Environment.ordinal() for a nullable id and silently substitutes null/absent for 0.
        EnvironmentPromotionPreview preview = new EnvironmentPromotionPreview(
            PromotionResourceType.MCP_SERVER, 7L, Environment.DEVELOPMENT, Environment.STAGING, null, null,
            List.of(), List.of(), List.of());

        when(environmentPromotionFacade.preview(PromotionResourceType.MCP_SERVER, 7L, 1L)).thenReturn(preview);

        EnvironmentPromotionPreviewModel model = environmentPromotionGraphQlController.environmentPromotionPreview(
            PromotionResourceType.MCP_SERVER, 7L, 1L);

        assertThat(model.sourceEnvironmentId()).isZero();
        assertThat(model.targetEnvironmentId()).isEqualTo(1L);
        assertThat(model.existingTargetId()).isNull();
        assertThat(model.existingTargetName()).isNull();
    }

    @Test
    void testPromoteToEnvironmentReturnsFacadeResultUnchanged() {
        EnvironmentPromotionResult result = new EnvironmentPromotionResult(99L, true, "https://example.com", List.of());
        PromoteToEnvironmentInput input = new PromoteToEnvironmentInput(
            PromotionResourceType.A2A_SERVER, 1L, 2L, List.of(new PromotionConnectionMappingInput(10L, 20L)));

        when(environmentPromotionFacade.promote(
            eq(PromotionResourceType.A2A_SERVER), eq(1L), eq(2L), eq(Map.of(10L, 20L)))).thenReturn(result);

        EnvironmentPromotionResult actual = environmentPromotionGraphQlController.promoteToEnvironment(input);

        assertThat(actual).isSameAs(result);
    }

    @Test
    void testPromoteToEnvironmentWithNoConnectionMappingsPassesEmptyMap() {
        EnvironmentPromotionResult result = new EnvironmentPromotionResult(5L, false, null, List.of(30L));
        PromoteToEnvironmentInput input = new PromoteToEnvironmentInput(
            PromotionResourceType.PROJECT_DEPLOYMENT, 3L, 4L, List.of());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Long, Long>> captor = ArgumentCaptor.forClass(Map.class);

        when(environmentPromotionFacade.promote(
            eq(PromotionResourceType.PROJECT_DEPLOYMENT), eq(3L), eq(4L), captor.capture())).thenReturn(result);

        EnvironmentPromotionResult actual = environmentPromotionGraphQlController.promoteToEnvironment(input);

        assertThat(actual).isSameAs(result);
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void testPromoteToEnvironmentCapturesEachSourceToTargetPair() {
        EnvironmentPromotionResult result = new EnvironmentPromotionResult(99L, true, null, List.of());
        PromoteToEnvironmentInput input = new PromoteToEnvironmentInput(
            PromotionResourceType.MCP_SERVER, 1L, 2L,
            List.of(
                new PromotionConnectionMappingInput(10L, 20L),
                new PromotionConnectionMappingInput(11L, 21L)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Long, Long>> captor = ArgumentCaptor.forClass(Map.class);

        when(environmentPromotionFacade.promote(
            eq(PromotionResourceType.MCP_SERVER), eq(1L), eq(2L), captor.capture())).thenReturn(result);

        environmentPromotionGraphQlController.promoteToEnvironment(input);

        Map<Long, Long> capturedMappings = captor.getValue();

        assertThat(capturedMappings).containsOnly(entry(10L, 20L), entry(11L, 21L));

        verify(environmentPromotionFacade).promote(
            eq(PromotionResourceType.MCP_SERVER), eq(1L), eq(2L), eq(capturedMappings));
    }
}
