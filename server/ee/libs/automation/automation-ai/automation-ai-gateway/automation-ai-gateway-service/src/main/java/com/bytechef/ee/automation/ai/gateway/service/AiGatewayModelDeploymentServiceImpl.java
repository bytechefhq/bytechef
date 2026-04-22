/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewayModelDeploymentRepository;
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
class AiGatewayModelDeploymentServiceImpl implements AiGatewayModelDeploymentService {

    private final AiGatewayModelDeploymentRepository aiGatewayModelDeploymentRepository;

    public AiGatewayModelDeploymentServiceImpl(
        AiGatewayModelDeploymentRepository aiGatewayModelDeploymentRepository) {

        this.aiGatewayModelDeploymentRepository = aiGatewayModelDeploymentRepository;
    }

    @Override
    public AiGatewayModelDeployment create(AiGatewayModelDeployment deployment) {
        Validate.notNull(deployment, "'deployment' must not be null");
        Validate.isTrue(deployment.getId() == null, "'id' must be null");

        return aiGatewayModelDeploymentRepository.save(deployment);
    }

    @Override
    public void delete(long id) {
        aiGatewayModelDeploymentRepository.deleteById(id);
    }

    @Override
    public void deleteByModelId(long modelId) {
        aiGatewayModelDeploymentRepository.deleteAllByModelId(modelId);
    }

    @Override
    public void deleteByRoutingPolicyId(long routingPolicyId) {
        aiGatewayModelDeploymentRepository.deleteAllByRoutingPolicyId(routingPolicyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayModelDeployment> getDeploymentsByRoutingPolicyId(long routingPolicyId) {
        return aiGatewayModelDeploymentRepository.findAllByRoutingPolicyId(routingPolicyId);
    }

    @Override
    public AiGatewayModelDeployment update(AiGatewayModelDeployment deployment) {
        Validate.notNull(deployment, "'deployment' must not be null");

        AiGatewayModelDeployment existingDeployment = aiGatewayModelDeploymentRepository.findById(
            deployment.getId())
            .orElseThrow(
                () -> new IllegalArgumentException("Model deployment not found: " + deployment.getId()));

        existingDeployment.setEnabled(deployment.isEnabled());
        existingDeployment.setMaxRpm(deployment.getMaxRpm());
        existingDeployment.setMaxTpm(deployment.getMaxTpm());
        existingDeployment.setPriorityOrder(deployment.getPriorityOrder());
        existingDeployment.setWeight(deployment.getWeight());

        return aiGatewayModelDeploymentRepository.save(existingDeployment);
    }
}
