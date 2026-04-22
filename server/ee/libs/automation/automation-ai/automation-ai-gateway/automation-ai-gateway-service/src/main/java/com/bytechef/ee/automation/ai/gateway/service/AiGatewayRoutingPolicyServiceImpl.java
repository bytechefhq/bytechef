/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewayRoutingPolicyRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
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
class AiGatewayRoutingPolicyServiceImpl implements AiGatewayRoutingPolicyService {

    private final AiGatewayModelDeploymentService aiGatewayModelDeploymentService;
    private final AiGatewayRoutingPolicyRepository aiGatewayRoutingPolicyRepository;

    public AiGatewayRoutingPolicyServiceImpl(
        AiGatewayModelDeploymentService aiGatewayModelDeploymentService,
        AiGatewayRoutingPolicyRepository aiGatewayRoutingPolicyRepository) {

        this.aiGatewayModelDeploymentService = aiGatewayModelDeploymentService;
        this.aiGatewayRoutingPolicyRepository = aiGatewayRoutingPolicyRepository;
    }

    @Override
    public AiGatewayRoutingPolicy create(AiGatewayRoutingPolicy policy) {
        Validate.notNull(policy, "'policy' must not be null");
        Validate.isTrue(policy.getId() == null, "'id' must be null");

        return aiGatewayRoutingPolicyRepository.save(policy);
    }

    @Override
    public void delete(long id) {
        aiGatewayModelDeploymentService.deleteByRoutingPolicyId(id);

        aiGatewayRoutingPolicyRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiGatewayRoutingPolicy getRoutingPolicy(long id) {
        return aiGatewayRoutingPolicyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Routing policy not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public AiGatewayRoutingPolicy getRoutingPolicyByName(String name) {
        return aiGatewayRoutingPolicyRepository.findByName(name)
            .orElseThrow(() -> new IllegalArgumentException("Routing policy not found: " + name));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayRoutingPolicy> getRoutingPolicies() {
        return aiGatewayRoutingPolicyRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayRoutingPolicy> getRoutingPolicies(Collection<Long> ids) {
        return aiGatewayRoutingPolicyRepository.findAllById(ids);
    }

    @Override
    public AiGatewayRoutingPolicy update(AiGatewayRoutingPolicy policy) {
        Validate.notNull(policy, "'policy' must not be null");

        AiGatewayRoutingPolicy existingPolicy = getRoutingPolicy(policy.getId());

        existingPolicy.setConfig(policy.getConfig());
        existingPolicy.setEnabled(policy.isEnabled());
        existingPolicy.setFallbackModel(policy.getFallbackModel());
        existingPolicy.setName(policy.getName());
        existingPolicy.setStrategy(policy.getStrategy());

        return aiGatewayRoutingPolicyRepository.save(existingPolicy);
    }
}
