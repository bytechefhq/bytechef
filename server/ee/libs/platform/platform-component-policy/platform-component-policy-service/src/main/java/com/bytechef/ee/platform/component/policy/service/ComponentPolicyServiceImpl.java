/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy.service;

import com.bytechef.ee.platform.component.policy.ComponentOperationPolicy;
import com.bytechef.ee.platform.component.policy.ComponentPolicy;
import com.bytechef.ee.platform.component.policy.ComponentPolicyService;
import com.bytechef.ee.platform.component.policy.repository.ComponentOperationPolicyRepository;
import com.bytechef.ee.platform.component.policy.repository.ComponentPolicyRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
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

    private static final Logger log = LoggerFactory.getLogger(ComponentPolicyServiceImpl.class);

    private final ComponentOperationPolicyRepository componentOperationPolicyRepository;
    private final ComponentPolicyRepository componentPolicyRepository;

    public ComponentPolicyServiceImpl(
        ComponentPolicyRepository componentPolicyRepository,
        ComponentOperationPolicyRepository componentOperationPolicyRepository) {
        this.componentPolicyRepository = componentPolicyRepository;
        this.componentOperationPolicyRepository = componentOperationPolicyRepository;
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

    @Override
    @Transactional(readOnly = true)
    public Set<String> getDisabledOperationKeys() {
        return StreamSupport.stream(componentOperationPolicyRepository.findAll()
            .spliterator(), false)
            .map(ComponentOperationPolicy::toKey)
            .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComponentOperationPolicy> getComponentOperationPolicies(String componentName) {
        return componentOperationPolicyRepository.findAllByComponentName(componentName);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void updateComponentOperationPolicy(
        String componentName, ComponentOperationPolicy.OperationType operationType, String operationName,
        boolean enabled) {

        Optional<ComponentOperationPolicy> componentOperationPolicyOptional =
            componentOperationPolicyRepository.findByComponentNameAndOperationTypeAndOperationName(
                componentName, operationType.ordinal(), operationName);

        if (enabled) {
            componentOperationPolicyOptional.ifPresent(componentOperationPolicyRepository::delete);
        } else if (componentOperationPolicyOptional.isEmpty()) {
            try {
                componentOperationPolicyRepository.save(
                    new ComponentOperationPolicy(componentName, operationType, operationName));
            } catch (DuplicateKeyException duplicateKeyException) {
                // A concurrent disable for the same operation already inserted the deny-list row between our
                // find and this save; the row exists either way, so the update is treated as idempotent.
                log.debug(
                    "Concurrent disable already inserted the deny-list row for {}#{}#{} — treating as idempotent",
                    componentName, operationType, operationName);
            }
        }
    }
}
