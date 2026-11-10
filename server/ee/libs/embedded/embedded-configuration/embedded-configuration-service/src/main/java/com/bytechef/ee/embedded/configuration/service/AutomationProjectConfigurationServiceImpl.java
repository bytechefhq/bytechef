/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.repository.AutomationProjectConfigurationRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
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
public class AutomationProjectConfigurationServiceImpl
    implements AutomationProjectConfigurationService {

    private static final String ROLLOUT_LOCK_KEY_PREFIX = "automation-project-configuration-rollout:";

    private final AutomationProjectConfigurationRepository automationProjectConfigurationRepository;
    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public AutomationProjectConfigurationServiceImpl(
        AutomationProjectConfigurationRepository automationProjectConfigurationRepository,
        JdbcTemplate jdbcTemplate) {

        this.automationProjectConfigurationRepository = automationProjectConfigurationRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AutomationProjectConfiguration create(
        long projectId, long environmentId, String name, @Nullable String permissionExpression, int projectVersion,
        boolean enabled, boolean automationHubVisible) {

        return automationProjectConfigurationRepository.save(
            new AutomationProjectConfiguration(
                projectId, environmentId, name, permissionExpression, projectVersion, enabled, automationHubVisible));
    }

    @Override
    public void delete(long id) {
        automationProjectConfigurationRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AutomationProjectConfiguration getConfiguration(long id) {
        return automationProjectConfigurationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(
                "Automation project configuration with id " + id + " does not exist"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationProjectConfiguration> getConfigurations(long projectId) {
        return automationProjectConfigurationRepository.findAllByProjectIdOrderByIdAsc(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationProjectConfiguration> getConfigurations(long projectId, long environmentId) {
        return automationProjectConfigurationRepository.findAllByProjectIdAndEnvironmentIdOrderByIdAsc(
            projectId, environmentId);
    }

    @Override
    public List<AutomationProjectConfiguration> getConfigurations(Set<Long> projectIds) {
        return automationProjectConfigurationRepository.findAllByProjectIdInOrderByIdAsc(projectIds);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockRollout(long projectId, long environmentId) {
        jdbcTemplate.queryForList("SELECT pg_advisory_xact_lock(?)", getRolloutLockKey(projectId, environmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWorkflowEnabled(AutomationProjectConfiguration configuration, UUID workflowUuid) {
        return configuration.isWorkflowEnabled(workflowUuid);
    }

    @Override
    public AutomationProjectConfiguration update(
        long id, String name, @Nullable String permissionExpression, int projectVersion, boolean enabled,
        boolean automationHubVisible) {

        AutomationProjectConfiguration configuration = getConfiguration(id);

        configuration.setName(name);
        configuration.setPermissionExpression(permissionExpression);
        configuration.setProjectVersion(projectVersion);
        configuration.setEnabled(enabled);
        configuration.setAutomationHubVisible(automationHubVisible);

        return automationProjectConfigurationRepository.save(configuration);
    }

    @Override
    public AutomationProjectConfiguration updateEnabled(long id, boolean enabled) {
        AutomationProjectConfiguration configuration = getConfiguration(id);

        configuration.setEnabled(enabled);

        return automationProjectConfigurationRepository.save(configuration);
    }

    @Override
    public AutomationProjectConfiguration updateWorkflowEnabled(long id, UUID workflowUuid, boolean enabled) {
        return updateWorkflow(id, workflowUuid, workflow -> workflow.setEnabled(enabled));
    }

    @Override
    public AutomationProjectConfiguration updateWorkflowProvidedValues(
        long id, UUID workflowUuid, Map<String, Long> providedConnections, Map<String, Object> providedInputs) {

        return updateWorkflow(id, workflowUuid, workflow -> {
            workflow.setProvidedConnections(providedConnections);
            workflow.setProvidedInputs(providedInputs);
        });
    }

    private AutomationProjectConfiguration updateWorkflow(
        long id, UUID workflowUuid, Consumer<AutomationProjectConfigurationWorkflow> workflowUpdater) {

        AutomationProjectConfiguration configuration = getConfiguration(id);

        Set<AutomationProjectConfigurationWorkflow> workflows = new HashSet<>(configuration.getWorkflows());

        Optional<AutomationProjectConfigurationWorkflow> existingWorkflowOptional =
            configuration.fetchWorkflow(workflowUuid);

        if (existingWorkflowOptional.isPresent()) {
            workflowUpdater.accept(existingWorkflowOptional.get());
        } else {
            AutomationProjectConfigurationWorkflow workflow =
                new AutomationProjectConfigurationWorkflow(workflowUuid, true);

            workflowUpdater.accept(workflow);

            workflows.add(workflow);
        }

        configuration.setWorkflows(workflows);

        return automationProjectConfigurationRepository.save(configuration);
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
