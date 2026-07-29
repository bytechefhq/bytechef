/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.tool.model.IntegrationDetailInfo;
import com.bytechef.ee.embedded.ai.tool.model.IntegrationInfo;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.exception.ExecutionException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class IntegrationToolsTest {

    @Mock
    private IntegrationFacade integrationFacade;

    @Test
    void testListIntegrations() {
        when(integrationFacade.getIntegrations(null, false, null, null, false))
            .thenReturn(List.of(dto(1L, "Slack", "chat", "slack"), dto(2L, "Gmail", "mail", "googleMail")));

        List<IntegrationInfo> result = newTools().listIntegrations();

        assertThat(result).hasSize(2);
        assertThat(result.get(0)
            .name()).isEqualTo("Slack");
        assertThat(result.get(0)
            .componentName()).isEqualTo("slack");
    }

    @Test
    void testGetIntegration() {
        when(integrationFacade.getIntegration(1L)).thenReturn(dto(1L, "Slack", "chat", "slack"));

        IntegrationDetailInfo result = newTools().getIntegration(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Slack");
        assertThat(result.componentName()).isEqualTo("slack");
        assertThat(result.status()).isEqualTo(Status.DRAFT.name());
    }

    @Test
    void testSearchIntegrations() {
        when(integrationFacade.getIntegrations(null, false, null, null, false))
            .thenReturn(List.of(dto(1L, "Slack", "chat", "slack"), dto(2L, "Gmail", "mail", "googleMail")));

        List<IntegrationInfo> result = newTools().searchIntegrations("gmail");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)
            .name()).isEqualTo("Gmail");
    }

    @Test
    void testCreateIntegration() {
        when(integrationFacade.createIntegration(any(IntegrationDTO.class))).thenReturn(9L);
        when(integrationFacade.getIntegration(9L)).thenReturn(dto(9L, "Stripe", "payments", "stripe"));

        IntegrationInfo result = newTools().createIntegration("stripe", 1, "Stripe", "payments", false);

        assertThat(result.id()).isEqualTo(9L);
        assertThat(result.componentName()).isEqualTo("stripe");
        verify(integrationFacade).createIntegration(any(IntegrationDTO.class));
    }

    @Test
    void testDeleteIntegration() {
        when(integrationFacade.getIntegration(1L)).thenReturn(dto(1L, "Slack", "chat", "slack"));

        String result = newTools().deleteIntegration(1L);

        assertThat(result).contains("Slack", "1");
        verify(integrationFacade).deleteIntegration(1L);
    }

    @Test
    void testPublishIntegration() {
        String result = newTools().publishIntegration(1L, "v1");

        assertThat(result).contains("1", "published");
        verify(integrationFacade).publishIntegration(1L, "v1");
    }

    @Test
    void testFailurePropagatesAsExecutionException() {
        when(integrationFacade.getIntegration(1L)).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> newTools().getIntegration(1L))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("Failed to get integration");
    }

    private IntegrationTools newTools() {
        return new IntegrationTools(integrationFacade);
    }

    private static IntegrationDTO dto(Long id, String name, String description, String componentName) {
        return new IntegrationDTO(
            null, false, null, componentName, 1, null, Instant.now(), description, null, id, List.of(), List.of(),
            null, Instant.now(), null, Status.DRAFT, null, false, name, null, List.of(), null, 1);
    }
}
