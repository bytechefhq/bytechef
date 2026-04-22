/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayRoutingPolicyService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiGatewayRoutingPolicyFacade}. Delegates to the shared {@code AiGatewayRoutingPolicyService}
 * and carries the {@code ADMIN} authorization guard so it is enforced for every caller of the facade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiGatewayRoutingPolicyFacadeImpl implements AiGatewayRoutingPolicyFacade {

    private final AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService;

    @SuppressFBWarnings("EI")
    AiGatewayRoutingPolicyFacadeImpl(AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService) {
        this.aiGatewayRoutingPolicyService = aiGatewayRoutingPolicyService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayRoutingPolicy getRoutingPolicy(long id) {
        return aiGatewayRoutingPolicyService.getRoutingPolicy(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiGatewayRoutingPolicy> getRoutingPolicies() {
        return aiGatewayRoutingPolicyService.getRoutingPolicies();
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayRoutingPolicy create(AiGatewayRoutingPolicy policy) {
        return aiGatewayRoutingPolicyService.create(policy);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void delete(long id) {
        aiGatewayRoutingPolicyService.delete(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayRoutingPolicy update(AiGatewayRoutingPolicy policy) {
        return aiGatewayRoutingPolicyService.update(policy);
    }
}
