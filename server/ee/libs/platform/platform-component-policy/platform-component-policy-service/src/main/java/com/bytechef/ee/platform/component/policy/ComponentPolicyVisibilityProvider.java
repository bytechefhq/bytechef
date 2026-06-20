/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.visibility.ComponentVisibilityProvider;
import com.bytechef.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * EE implementation of {@link ComponentVisibilityProvider} backed by the {@code component_policy} table. The disabled
 * set is cached per tenant for a short window to keep component listings (which probe every component) off the
 * database; administrative toggles take effect within the cache TTL.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ComponentPolicyVisibilityProvider implements ComponentVisibilityProvider {

    private final Cache<String, Set<String>> disabledComponentNamesCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(10))
        .build();
    private final ComponentPolicyService componentPolicyService;

    public ComponentPolicyVisibilityProvider(ComponentPolicyService componentPolicyService) {
        this.componentPolicyService = componentPolicyService;
    }

    @Override
    public boolean isVisible(String componentName) {
        Set<String> disabledComponentNames = disabledComponentNamesCache.get(
            TenantContext.getCurrentTenantId(), tenantId -> componentPolicyService.getDisabledComponentNames());

        return !disabledComponentNames.contains(componentName);
    }
}
