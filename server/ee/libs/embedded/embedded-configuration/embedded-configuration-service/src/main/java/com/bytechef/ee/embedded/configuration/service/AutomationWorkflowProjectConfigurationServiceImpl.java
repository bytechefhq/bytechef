/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.repository.AutomationWorkflowProjectConfigurationRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class AutomationWorkflowProjectConfigurationServiceImpl
    implements AutomationWorkflowProjectConfigurationService {

    private static final String ROLLOUT_LOCK_KEY_PREFIX = "automation-workflow-project-configuration-rollout:";

    private final AutomationWorkflowProjectConfigurationRepository automationWorkflowProjectConfigurationRepository;
    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectConfigurationServiceImpl(
        AutomationWorkflowProjectConfigurationRepository automationWorkflowProjectConfigurationRepository,
        JdbcTemplate jdbcTemplate) {

        this.automationWorkflowProjectConfigurationRepository = automationWorkflowProjectConfigurationRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AutomationWorkflowProjectConfiguration create(
        long projectId, long environmentId, int projectVersion, boolean enabled, boolean automationHubVisible) {

        return automationWorkflowProjectConfigurationRepository.save(
            new AutomationWorkflowProjectConfiguration(
                projectId, environmentId, projectVersion, enabled, automationHubVisible));
    }

    @Override
    public void delete(long id) {
        automationWorkflowProjectConfigurationRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AutomationWorkflowProjectConfiguration> fetchConfiguration(long projectId, long environmentId) {
        return automationWorkflowProjectConfigurationRepository.findByProjectIdAndEnvironmentId(
            projectId, environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public AutomationWorkflowProjectConfiguration getConfiguration(long id) {
        return automationWorkflowProjectConfigurationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(
                "Automation workflow project configuration with id " + id + " does not exist"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationWorkflowProjectConfiguration> getConfigurations(long projectId) {
        return automationWorkflowProjectConfigurationRepository.findAllByProjectId(projectId);
    }

    @Override
    public List<AutomationWorkflowProjectConfiguration> getConfigurations(Set<Long> projectIds) {
        return automationWorkflowProjectConfigurationRepository.findAllByProjectIdIn(projectIds);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockRollout(long projectId, long environmentId) {
        jdbcTemplate.queryForList("SELECT pg_advisory_xact_lock(?)", getRolloutLockKey(projectId, environmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWorkflowEnabled(AutomationWorkflowProjectConfiguration configuration, UUID workflowUuid) {
        return configuration.isWorkflowEnabled(workflowUuid);
    }

    @Override
    public AutomationWorkflowProjectConfiguration update(
        long id, int projectVersion, boolean enabled, boolean automationHubVisible) {

        AutomationWorkflowProjectConfiguration configuration = getConfiguration(id);

        configuration.setProjectVersion(projectVersion);
        configuration.setEnabled(enabled);
        configuration.setAutomationHubVisible(automationHubVisible);

        return automationWorkflowProjectConfigurationRepository.save(configuration);
    }

    @Override
    public AutomationWorkflowProjectConfiguration updateEnabled(long id, boolean enabled) {
        AutomationWorkflowProjectConfiguration configuration = getConfiguration(id);

        configuration.setEnabled(enabled);

        return automationWorkflowProjectConfigurationRepository.save(configuration);
    }

    @Override
    public AutomationWorkflowProjectConfiguration updateWorkflowEnabled(long id, UUID workflowUuid, boolean enabled) {
        AutomationWorkflowProjectConfiguration configuration = getConfiguration(id);

        Set<AutomationWorkflowProjectConfigurationWorkflow> workflows = new HashSet<>(configuration.getWorkflows());

        Optional<AutomationWorkflowProjectConfigurationWorkflow> existingWorkflowOptional = workflows.stream()
            .filter(workflow -> Objects.equals(workflow.getWorkflowUuid(), workflowUuid))
            .findFirst();

        if (existingWorkflowOptional.isPresent()) {
            existingWorkflowOptional.get()
                .setEnabled(enabled);
        } else {
            workflows.add(new AutomationWorkflowProjectConfigurationWorkflow(workflowUuid, enabled));
        }

        configuration.setWorkflows(workflows);

        return automationWorkflowProjectConfigurationRepository.save(configuration);
    }

    static long getRolloutLockKey(long projectId, long environmentId) {
        MessageDigest messageDigest;

        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }

        String lockName = ROLLOUT_LOCK_KEY_PREFIX + projectId + ":" + environmentId;

        byte[] digest = messageDigest.digest(lockName.getBytes(StandardCharsets.UTF_8));

        ByteBuffer digestBuffer = ByteBuffer.wrap(digest);

        return digestBuffer.getLong();
    }
}
