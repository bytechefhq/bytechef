/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy.service;

import com.bytechef.ee.platform.component.policy.ComponentPolicy;
import com.bytechef.ee.platform.component.policy.ComponentPolicyService;
import com.bytechef.ee.platform.component.policy.repository.ComponentPolicyRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ComponentPolicyServiceImpl implements ComponentPolicyService {

    private final ComponentPolicyRepository componentPolicyRepository;

    public ComponentPolicyServiceImpl(ComponentPolicyRepository componentPolicyRepository) {
        this.componentPolicyRepository = componentPolicyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEnabled(String componentName) {
        return componentPolicyRepository.findById(componentName)
            .map(ComponentPolicy::isEnabled)
            .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getDisabledComponentNames() {
        return componentPolicyRepository.findByEnabled(false)
            .stream()
            .map(ComponentPolicy::getComponentName)
            .collect(Collectors.toSet());
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ComponentPolicy updateComponentPolicy(String componentName, boolean enabled) {
        ComponentPolicy componentPolicy = componentPolicyRepository.findById(componentName)
            .orElseGet(() -> new ComponentPolicy(componentName));

        componentPolicy.setEnabled(enabled);

        return componentPolicyRepository.save(componentPolicy);
    }
}
