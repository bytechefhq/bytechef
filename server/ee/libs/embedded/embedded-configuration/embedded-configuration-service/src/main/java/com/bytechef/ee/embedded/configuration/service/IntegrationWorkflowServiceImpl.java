/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.platform.constant.Environment;
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
public class IntegrationWorkflowServiceImpl implements IntegrationWorkflowService {

    private final IntegrationWorkflowRepository integrationWorkflowRepository;

    public IntegrationWorkflowServiceImpl(IntegrationWorkflowRepository integrationWorkflowRepository) {
        this.integrationWorkflowRepository = integrationWorkflowRepository;
    }

    @Override
    public IntegrationWorkflow addWorkflow(long integrationId, int integrationVersion, String workflowId) {
        return addWorkflow(integrationId, integrationVersion, workflowId, String.valueOf(UUID.randomUUID()));
    }

    @Override
    public IntegrationWorkflow addWorkflow(
        long integrationId, int integrationVersion, String workflowId, String workflowReferenceCode) {

        Assert.notNull(workflowId, "'workflowId' must not be null");

        IntegrationWorkflow integrationWorkflow = new IntegrationWorkflow(
            integrationId, integrationVersion, workflowId, workflowReferenceCode);

        return integrationWorkflowRepository.save(integrationWorkflow);
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
        return OptionalUtils.get(integrationWorkflowRepository.findById(id));
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
    public String getLatestWorkflowId(String workflowReferenceCode) {
        return OptionalUtils.get(
            integrationWorkflowRepository
                .findLatestIntegrationWorkflowByWorkflowReferenceCode(workflowReferenceCode)
                .map(IntegrationWorkflow::getWorkflowId));
    }

    @Override
    public String getLatestWorkflowId(String workflowReferenceCode, Environment environment) {
        return OptionalUtils.get(
            integrationWorkflowRepository
                .findLatestByWorkflowReferenceCodeAndEnvironment(workflowReferenceCode, environment.ordinal())
                .map(IntegrationWorkflow::getWorkflowId));
    }

    @Override
    public String getWorkflowId(long integrationInstanceId, String workflowReferenceCode) {
        return OptionalUtils.get(
            integrationWorkflowRepository
                .findByIntegrationInstanceIdAndWorkflowReferenceCode(integrationInstanceId, workflowReferenceCode)
                .map(IntegrationWorkflow::getWorkflowId));
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
        return OptionalUtils.get(integrationWorkflowRepository.findByWorkflowId(workflowId));
    }

    @Override
    public IntegrationWorkflow update(IntegrationWorkflow integrationWorkflow) {
        Assert.notNull(integrationWorkflow, "'IntegrationWorkflow' must not be null");
        Assert.notNull(integrationWorkflow.getId(), "'id' must not be null");

        IntegrationWorkflow curIntegrationWorkflow = OptionalUtils.get(integrationWorkflowRepository.findById(
            integrationWorkflow.getId()));

        curIntegrationWorkflow.setIntegrationVersion(integrationWorkflow.getIntegrationVersion());
        curIntegrationWorkflow.setWorkflowId(integrationWorkflow.getWorkflowId());
        curIntegrationWorkflow.setWorkflowReferenceCode(integrationWorkflow.getWorkflowReferenceCode());

        return integrationWorkflowRepository.save(curIntegrationWorkflow);
    }
}
