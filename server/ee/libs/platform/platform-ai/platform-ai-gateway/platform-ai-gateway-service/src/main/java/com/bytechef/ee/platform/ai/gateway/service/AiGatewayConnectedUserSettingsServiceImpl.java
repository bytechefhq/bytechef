/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayConnectedUserSettings;
import com.bytechef.ee.platform.ai.gateway.repository.AiGatewayConnectedUserSettingsRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiGatewayConnectedUserSettingsServiceImpl implements AiGatewayConnectedUserSettingsService {

    private final AiGatewayConnectedUserSettingsRepository aiGatewayConnectedUserSettingsRepository;

    AiGatewayConnectedUserSettingsServiceImpl(
        AiGatewayConnectedUserSettingsRepository aiGatewayConnectedUserSettingsRepository) {

        this.aiGatewayConnectedUserSettingsRepository = aiGatewayConnectedUserSettingsRepository;
    }

    @Override
    public void assignRoutingPolicy(long connectedUserId, long routingPolicyId) {
        AiGatewayConnectedUserSettings settings = getOrCreate(connectedUserId);

        settings.setRoutingPolicyId(routingPolicyId);

        aiGatewayConnectedUserSettingsRepository.save(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRoutingPolicyId(long routingPolicyId) {
        return aiGatewayConnectedUserSettingsRepository.countByRoutingPolicyId(routingPolicyId);
    }

    @Override
    public void deleteByConnectedUserId(long connectedUserId) {
        aiGatewayConnectedUserSettingsRepository.deleteByConnectedUserId(connectedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiGatewayConnectedUserSettings> fetchByConnectedUserId(long connectedUserId) {
        return aiGatewayConnectedUserSettingsRepository.findByConnectedUserId(connectedUserId);
    }

    @Override
    public void unassignRoutingPolicy(long connectedUserId) {
        Optional<AiGatewayConnectedUserSettings> settingsOptional =
            aiGatewayConnectedUserSettingsRepository.findByConnectedUserId(connectedUserId);

        if (settingsOptional.isEmpty()) {
            return;
        }

        AiGatewayConnectedUserSettings settings = settingsOptional.get();

        settings.setRoutingPolicyId(null);

        aiGatewayConnectedUserSettingsRepository.save(settings);
    }

    @Override
    public void updateBudgetCap(long connectedUserId, @Nullable BigDecimal budgetCap) {
        if (budgetCap != null && budgetCap.signum() < 0) {
            throw new IllegalArgumentException("budgetCap must not be negative");
        }

        AiGatewayConnectedUserSettings settings = getOrCreate(connectedUserId);

        settings.setBudgetCap(budgetCap);

        aiGatewayConnectedUserSettingsRepository.save(settings);
    }

    private AiGatewayConnectedUserSettings getOrCreate(long connectedUserId) {
        return aiGatewayConnectedUserSettingsRepository.findByConnectedUserId(connectedUserId)
            .orElseGet(() -> new AiGatewayConnectedUserSettings(connectedUserId));
    }
}
