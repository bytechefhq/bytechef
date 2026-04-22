/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewayModelRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.Validate;
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
class AiGatewayModelServiceImpl implements AiGatewayModelService {

    private final AiGatewayModelDeploymentService aiGatewayModelDeploymentService;
    private final AiGatewayModelRepository aiGatewayModelRepository;

    public AiGatewayModelServiceImpl(
        AiGatewayModelDeploymentService aiGatewayModelDeploymentService,
        AiGatewayModelRepository aiGatewayModelRepository) {

        this.aiGatewayModelDeploymentService = aiGatewayModelDeploymentService;
        this.aiGatewayModelRepository = aiGatewayModelRepository;
    }

    @Override
    public AiGatewayModel create(AiGatewayModel model) {
        Validate.notNull(model, "'model' must not be null");
        Validate.isTrue(model.getId() == null, "'id' must be null");

        return aiGatewayModelRepository.save(model);
    }

    @Override
    public void delete(long id) {
        aiGatewayModelDeploymentService.deleteByModelId(id);

        aiGatewayModelRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiGatewayModel getModel(long id) {
        return aiGatewayModelRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public AiGatewayModel getModel(long providerId, String name) {
        return aiGatewayModelRepository.findByProviderIdAndName(providerId, name)
            .orElseThrow(() -> new IllegalArgumentException(
                "Model not found: providerId=" + providerId + ", name=" + name));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayModel> getModels() {
        return aiGatewayModelRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayModel> getModelsByProviderId(long providerId) {
        return aiGatewayModelRepository.findAllByProviderId(providerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayModel> getEnabledModels() {
        return aiGatewayModelRepository.findAllByEnabled(true);
    }

    @Override
    public AiGatewayModel update(AiGatewayModel model) {
        Validate.notNull(model, "'model' must not be null");

        AiGatewayModel existingModel = getModel(model.getId());

        existingModel.setAlias(model.getAlias());
        existingModel.setCapabilities(model.getCapabilities());
        existingModel.setContextWindow(model.getContextWindow());
        existingModel.setEnabled(model.isEnabled());
        existingModel.setInputCostPerMTokens(model.getInputCostPerMTokens());
        existingModel.setName(model.getName());
        existingModel.setOutputCostPerMTokens(model.getOutputCostPerMTokens());

        return aiGatewayModelRepository.save(existingModel);
    }
}
