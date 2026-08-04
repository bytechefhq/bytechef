/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy;

import com.bytechef.ee.platform.component.policy.ComponentOperationPolicy.OperationType;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.visibility.ComponentVisibilityProvider;
import com.bytechef.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ComponentPolicyVisibilityProvider implements ComponentVisibilityProvider {

    private record DisabledPolicies(Set<String> componentNames, Set<String> operationKeys) {
    }

    private final Cache<String, DisabledPolicies> disabledPoliciesCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(10))
        .build();
    private final ComponentPolicyService componentPolicyService;

    public ComponentPolicyVisibilityProvider(ComponentPolicyService componentPolicyService) {
        this.componentPolicyService = componentPolicyService;
    }

    @Override
    public boolean isVisible(String componentName) {
        DisabledPolicies disabledPolicies = getDisabledPolicies();

        return !disabledPolicies.componentNames()
            .contains(componentName);
    }

    @Override
    public boolean isActionVisible(String componentName, String actionName) {
        DisabledPolicies disabledPolicies = getDisabledPolicies();

        return !disabledPolicies.componentNames()
            .contains(componentName) &&
            !disabledPolicies.operationKeys()
                .contains(ComponentOperationPolicy.key(
                    componentName, OperationType.ACTION, actionName));
    }

    @Override
    public boolean isTriggerVisible(String componentName, String triggerName) {
        DisabledPolicies disabledPolicies = getDisabledPolicies();

        return !disabledPolicies.componentNames()
            .contains(componentName) &&
            !disabledPolicies.operationKeys()
                .contains(ComponentOperationPolicy.key(
                    componentName, OperationType.TRIGGER, triggerName));
    }

    private DisabledPolicies getDisabledPolicies() {
        return disabledPoliciesCache.get(
            TenantContext.getCurrentTenantId(), tenantId -> loadDisabledPolicies());
    }

    private DisabledPolicies loadDisabledPolicies() {
        return new DisabledPolicies(
            componentPolicyService.getDisabledComponentNames(), componentPolicyService.getDisabledOperationKeys());
    }
}
