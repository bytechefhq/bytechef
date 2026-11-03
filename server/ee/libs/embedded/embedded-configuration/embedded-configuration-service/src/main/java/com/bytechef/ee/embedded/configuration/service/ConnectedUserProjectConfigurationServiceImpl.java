/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectConfigurationRepository.ConfigurationAssignmentCount;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
@ConditionalOnEEVersion
public class ConnectedUserProjectConfigurationServiceImpl implements ConnectedUserProjectConfigurationService {

    private final ConnectedUserProjectConfigurationRepository connectedUserProjectConfigurationRepository;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectConfigurationServiceImpl(
        ConnectedUserProjectConfigurationRepository connectedUserProjectConfigurationRepository) {

        this.connectedUserProjectConfigurationRepository = connectedUserProjectConfigurationRepository;
    }

    @Override
    public ConnectedUserProjectConfiguration assign(long connectedUserId, long projectId, long configurationId) {
        Optional<ConnectedUserProjectConfiguration> existingAssignmentOptional =
            connectedUserProjectConfigurationRepository.findByConnectedUserIdAndProjectId(connectedUserId, projectId);

        if (existingAssignmentOptional.isPresent()) {
            ConnectedUserProjectConfiguration existingAssignment = existingAssignmentOptional.get();

            existingAssignment.setConfigurationId(configurationId);

            return connectedUserProjectConfigurationRepository.save(existingAssignment);
        }

        return connectedUserProjectConfigurationRepository.save(
            new ConnectedUserProjectConfiguration(connectedUserId, projectId, configurationId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> countByConfigurationIds(Collection<Long> configurationIds) {
        Map<Long, Integer> counts = new HashMap<>();

        if (configurationIds.isEmpty()) {
            return counts;
        }

        for (ConfigurationAssignmentCount configurationAssignmentCount : connectedUserProjectConfigurationRepository
            .countByConfigurationIds(configurationIds)) {

            counts.put(
                configurationAssignmentCount.configurationId(), (int) configurationAssignmentCount.assignmentCount());
        }

        return counts;
    }

    @Override
    public void delete(long id) {
        connectedUserProjectConfigurationRepository.deleteById(id);
    }

    @Override
    public void deleteByConfigurationId(long configurationId) {
        connectedUserProjectConfigurationRepository.deleteByConfigurationId(configurationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConnectedUserProjectConfiguration> fetchAssignment(long connectedUserId, long projectId) {
        return connectedUserProjectConfigurationRepository.findByConnectedUserIdAndProjectId(
            connectedUserId, projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectedUserProjectConfiguration> getAssignments(long configurationId) {
        return connectedUserProjectConfigurationRepository.findAllByConfigurationId(configurationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectedUserProjectConfiguration> getConnectedUserAssignments(long connectedUserId) {
        return connectedUserProjectConfigurationRepository.findAllByConnectedUserId(connectedUserId);
    }
}
