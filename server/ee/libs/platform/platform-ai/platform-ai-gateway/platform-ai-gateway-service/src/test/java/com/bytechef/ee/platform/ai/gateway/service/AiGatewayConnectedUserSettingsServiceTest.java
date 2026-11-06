/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayConnectedUserSettings;
import com.bytechef.ee.platform.ai.gateway.repository.AiGatewayConnectedUserSettingsRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiGatewayConnectedUserSettingsServiceTest {

    private static final long CONNECTED_USER_ID = 5L;

    @Mock
    private AiGatewayConnectedUserSettingsRepository aiGatewayConnectedUserSettingsRepository;

    private AiGatewayConnectedUserSettingsService aiGatewayConnectedUserSettingsService;

    @BeforeEach
    void setUp() {
        aiGatewayConnectedUserSettingsService =
            new AiGatewayConnectedUserSettingsServiceImpl(aiGatewayConnectedUserSettingsRepository);
    }

    @Test
    void testAssignRoutingPolicyCreatesTheRowWhenAbsent() {
        when(aiGatewayConnectedUserSettingsRepository.findByConnectedUserId(CONNECTED_USER_ID))
            .thenReturn(Optional.empty());

        aiGatewayConnectedUserSettingsService.assignRoutingPolicy(CONNECTED_USER_ID, 10L);

        AiGatewayConnectedUserSettings saved = captureSaved();

        assertThat(saved.getConnectedUserId()).isEqualTo(CONNECTED_USER_ID);
        assertThat(saved.getRoutingPolicyId()).isEqualTo(10L);
        assertThat(saved.getBudgetCap()).isNull();
    }

    @Test
    void testAssignRoutingPolicyReplacesThePreviousPlanAndKeepsTheCap() {
        AiGatewayConnectedUserSettings existing = new AiGatewayConnectedUserSettings(CONNECTED_USER_ID);

        existing.setRoutingPolicyId(10L);
        existing.setBudgetCap(new BigDecimal("25.00"));

        when(aiGatewayConnectedUserSettingsRepository.findByConnectedUserId(CONNECTED_USER_ID))
            .thenReturn(Optional.of(existing));

        aiGatewayConnectedUserSettingsService.assignRoutingPolicy(CONNECTED_USER_ID, 11L);

        AiGatewayConnectedUserSettings saved = captureSaved();

        assertThat(saved.getRoutingPolicyId()).isEqualTo(11L);
        assertThat(saved.getBudgetCap()).isEqualByComparingTo("25.00");
    }

    @Test
    void testUnassignRoutingPolicyClearsThePlanAndKeepsTheRow() {
        AiGatewayConnectedUserSettings existing = new AiGatewayConnectedUserSettings(CONNECTED_USER_ID);

        existing.setRoutingPolicyId(10L);

        when(aiGatewayConnectedUserSettingsRepository.findByConnectedUserId(CONNECTED_USER_ID))
            .thenReturn(Optional.of(existing));

        aiGatewayConnectedUserSettingsService.unassignRoutingPolicy(CONNECTED_USER_ID);

        assertThat(captureSaved().getRoutingPolicyId()).isNull();
        verify(aiGatewayConnectedUserSettingsRepository, never()).deleteByConnectedUserId(CONNECTED_USER_ID);
    }

    @Test
    void testUnassignRoutingPolicyWithNoRowIsANoOp() {
        when(aiGatewayConnectedUserSettingsRepository.findByConnectedUserId(CONNECTED_USER_ID))
            .thenReturn(Optional.empty());

        aiGatewayConnectedUserSettingsService.unassignRoutingPolicy(CONNECTED_USER_ID);

        verify(aiGatewayConnectedUserSettingsRepository, never()).save(any());
    }

    @Test
    void testUpdateBudgetCapCreatesTheRowWhenAbsent() {
        when(aiGatewayConnectedUserSettingsRepository.findByConnectedUserId(CONNECTED_USER_ID))
            .thenReturn(Optional.empty());

        aiGatewayConnectedUserSettingsService.updateBudgetCap(CONNECTED_USER_ID, new BigDecimal("40.00"));

        AiGatewayConnectedUserSettings saved = captureSaved();

        assertThat(saved.getBudgetCap()).isEqualByComparingTo("40.00");
        assertThat(saved.getRoutingPolicyId()).isNull();
    }

    @Test
    void testUpdateBudgetCapRejectsANegativeCap() {
        assertThatThrownBy(
            () -> aiGatewayConnectedUserSettingsService.updateBudgetCap(CONNECTED_USER_ID, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("budgetCap must not be negative");

        verify(aiGatewayConnectedUserSettingsRepository, never()).save(any());
    }

    private AiGatewayConnectedUserSettings captureSaved() {
        ArgumentCaptor<AiGatewayConnectedUserSettings> settingsCaptor =
            ArgumentCaptor.forClass(AiGatewayConnectedUserSettings.class);

        verify(aiGatewayConnectedUserSettingsRepository).save(settingsCaptor.capture());

        return settingsCaptor.getValue();
    }
}
