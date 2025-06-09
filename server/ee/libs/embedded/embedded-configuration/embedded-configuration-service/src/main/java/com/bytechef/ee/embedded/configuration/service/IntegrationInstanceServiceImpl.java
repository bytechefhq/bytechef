/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationInstanceServiceImpl implements IntegrationInstanceService {

    private final IntegrationInstanceRepository integrationInstanceRepository;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceServiceImpl(IntegrationInstanceRepository integrationInstanceRepository) {
        this.integrationInstanceRepository = integrationInstanceRepository;
    }

    @Override
    public IntegrationInstance create(IntegrationInstance integrationInstance) {
        return integrationInstanceRepository.save(integrationInstance);
    }

    @Override
    public IntegrationInstance create(
        long connectedUserId, long connectionId, long integrationInstanceConfigurationId) {

        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectedUserId(connectedUserId);
        integrationInstance.setConnectionId(connectionId);
        integrationInstance.setIntegrationInstanceConfigurationId(integrationInstanceConfigurationId);
        integrationInstance.setEnabled(true);

        return integrationInstanceRepository.save(integrationInstance);
    }

    @Override
    public void delete(long id) {
        integrationInstanceRepository.deleteById(id);
    }

    @Override
    public Optional<IntegrationInstance> fetchIntegrationInstance(
        long connectedUserId, String componentName, Environment environment) {

        return integrationInstanceRepository.findFirstByConnectedUserIdIdAndComponentNameAndEnvironment(
            connectedUserId, componentName, environment.ordinal());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationInstance> getConnectedUserIntegrationInstances(long connectedUserId) {
        return integrationInstanceRepository.findAllByConnectedUserId(connectedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationInstance> getConnectedUserIntegrationInstances(long connectedUserId, boolean enabled) {
        return integrationInstanceRepository.findAllByConnectedUserIdAndEnabled(connectedUserId, enabled);
    }

    @Override
    public List<IntegrationInstance> getConnectedUserIntegrationInstances(
        long connectedUserId, Environment environment) {

        return integrationInstanceRepository.findAllByConnectedUserIdAndEnvironment(
            connectedUserId, environment.ordinal());
    }

    @Override
    public List<IntegrationInstance> getIntegrationInstances(
        long connectedUserId, String componentName, Environment environment) {

        return integrationInstanceRepository.findAllByConnectedUserIdIdAndComponentNameAndEnvironment(
            connectedUserId, componentName, environment.ordinal());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationInstance> getConnectedUserIntegrationInstances(List<Long> connectedUserIds) {
        return integrationInstanceRepository.findAllByConnectedUserIdIn(connectedUserIds);
    }

    @Override
    public IntegrationInstance getIntegrationInstance(
        long connectedUserId, List<String> componentNames, Environment environment) {

        return OptionalUtils.get(
            integrationInstanceRepository.findFirstByConnectedUserIdIdAndComponentNamesAndEnvironment(
                connectedUserId, componentNames, environment.ordinal()));
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationInstance getIntegrationInstance(long id) {
        return integrationInstanceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Integration instance not found"));
    }

    @Override
    public IntegrationInstance getIntegrationInstance(
        long connectedUserId, String workflowId, Environment environment) {

        return integrationInstanceRepository
            .findByWorkflowIdAndEnvironment(connectedUserId, workflowId, environment.ordinal())
            .orElseThrow(() -> new IllegalArgumentException("Integration instance not found"));
    }

    @Override
    public List<IntegrationInstance> getIntegrationInstances(long integrationInstanceConfigurationId) {
        return integrationInstanceRepository.findAllByIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
    }

    @Override
    public void updateEnabled(long id, boolean enable) {
        IntegrationInstance integrationInstance = getIntegrationInstance(id);

        integrationInstance.setEnabled(enable);

        integrationInstanceRepository.save(integrationInstance);
    }
}
