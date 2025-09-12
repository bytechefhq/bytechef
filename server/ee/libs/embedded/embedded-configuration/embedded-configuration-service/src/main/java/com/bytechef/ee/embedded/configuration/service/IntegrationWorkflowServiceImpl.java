/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class IntegrationWorkflowServiceImpl implements IntegrationWorkflowService {

    private final IntegrationWorkflowRepository integrationWorkflowRepository;

    public IntegrationWorkflowServiceImpl(IntegrationWorkflowRepository integrationWorkflowRepository) {
        this.integrationWorkflowRepository = integrationWorkflowRepository;
    }

    @Override
    public IntegrationWorkflow addWorkflow(long integrationId, int integrationVersion, String workflowId) {
        return integrationWorkflowRepository.save(
            new IntegrationWorkflow(integrationId, integrationVersion, workflowId));
    }

    @Override
    public void delete(long integrationId, int integrationVersion, String workflowId) {
        integrationWorkflowRepository
            .findByIntegrationIdAndIntegrationVersionAndWorkflowId(integrationId, integrationVersion, workflowId)
            .ifPresent(IntegrationWorkflow -> integrationWorkflowRepository.deleteById(IntegrationWorkflow.getId()));
    }

    @Override
    public void delete(List<Long> ids) {
        integrationWorkflowRepository.deleteAllById(ids);
    }

    @Override
    public IntegrationWorkflow getIntegrationWorkflow(long id) {
        return integrationWorkflowRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("IntegrationWorkflow not found for id: " + id));
    }

    @Override
    public List<Long> getIntegrationWorkflowIds(long integrationId, int integrationVersion) {
        return integrationWorkflowRepository
            .findAllByIntegrationIdAndIntegrationVersion(integrationId, integrationVersion)
            .stream()
            .map(IntegrationWorkflow::getId)
            .toList();
    }

    @Override
    public List<IntegrationWorkflow> getIntegrationWorkflows() {
        return integrationWorkflowRepository.findAll();
    }

    @Override
    public List<IntegrationWorkflow> getIntegrationWorkflows(long integrationId) {
        return integrationWorkflowRepository.findAllByIntegrationId(integrationId);
    }

    @Override
    public List<IntegrationWorkflow> getIntegrationWorkflows(long integrationId, int integrationVersion) {
        return integrationWorkflowRepository.findAllByIntegrationIdAndIntegrationVersion(integrationId,
            integrationVersion);
    }

    @Override
    public String getLastWorkflowId(String workflowUuid) {
        return integrationWorkflowRepository
            .findLastByUuid(UUID.fromString(workflowUuid))
            .map(IntegrationWorkflow::getWorkflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found for uuid: " + workflowUuid));
    }

    @Override
    public String getLastWorkflowId(String workflowUuid, Environment environment) {
        return integrationWorkflowRepository
            .findLastByUuidAndEnvironment(workflowUuid, environment.ordinal())
            .map(IntegrationWorkflow::getWorkflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found for uuid: " + workflowUuid));
    }

    @Override
    public String getWorkflowId(long integrationInstanceId, String workflowUuid) {
        return integrationWorkflowRepository
            .findByIntegrationInstanceIdAndUuid(integrationInstanceId, UUID.fromString(workflowUuid))
            .map(IntegrationWorkflow::getWorkflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found for uuid: " + workflowUuid));
    }

    @Override
    public List<String> getWorkflowIds(long integrationId, int integrationVersion) {
        return integrationWorkflowRepository
            .findAllByIntegrationIdAndIntegrationVersion(integrationId, integrationVersion)
            .stream()
            .map(IntegrationWorkflow::getWorkflowId)
            .toList();
    }

    @Override
    public IntegrationWorkflow getWorkflowIntegrationWorkflow(String workflowId) {
        return integrationWorkflowRepository.findByWorkflowId(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found for id: " + workflowId));
    }

    @Override
    public void publishWorkflow(
        long integrationId, int oldIntegrationVersion, String oldWorkflowId, IntegrationWorkflow integrationWorkflow) {

        Assert.notNull(integrationWorkflow, "'integrationWorkflow' must not be null");

        update(integrationWorkflow);

        integrationWorkflow = new IntegrationWorkflow(
            integrationId, oldIntegrationVersion, oldWorkflowId,
            UUID.fromString(integrationWorkflow.getUuidAsString()));

        integrationWorkflowRepository.save(integrationWorkflow);
    }

    @Override
    public IntegrationWorkflow update(IntegrationWorkflow integrationWorkflow) {
        Assert.notNull(integrationWorkflow, "'IntegrationWorkflow' must not be null");
        Assert.notNull(integrationWorkflow.getId(), "'id' must not be null");

        IntegrationWorkflow curIntegrationWorkflow = integrationWorkflowRepository.findById(integrationWorkflow.getId())
            .orElseThrow(() -> new IllegalArgumentException(
                "IntegrationWorkflow not found for id: " + integrationWorkflow.getId()));

        curIntegrationWorkflow.setIntegrationVersion(integrationWorkflow.getIntegrationVersion());
        curIntegrationWorkflow.setWorkflowId(integrationWorkflow.getWorkflowId());
        curIntegrationWorkflow.setUuid(integrationWorkflow.getUuidAsString());

        return integrationWorkflowRepository.save(curIntegrationWorkflow);
    }
}
