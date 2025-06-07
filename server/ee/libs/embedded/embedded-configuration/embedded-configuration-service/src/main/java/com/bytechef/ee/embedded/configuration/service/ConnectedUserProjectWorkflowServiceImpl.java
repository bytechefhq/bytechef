/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ConnectedUserProjectWorkflowServiceImpl implements ConnectedUserProjectWorkflowService {

    private final ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    public ConnectedUserProjectWorkflowServiceImpl(
        ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository) {
        this.connectedUserProjectWorkflowRepository = connectedUserProjectWorkflowRepository;
    }

    @Override
    public ConnectedUserProjectWorkflow create(ConnectedUserProjectWorkflow connectedUserProjectWorkflow) {
        return connectedUserProjectWorkflowRepository.save(connectedUserProjectWorkflow);
    }

    @Override
    public void delete(long id) {
        connectedUserProjectWorkflowRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectedUserProjectWorkflow getConnectedUserProjectWorkflow(long id) {
        return OptionalUtils.get(connectedUserProjectWorkflowRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectedUserProjectWorkflow> getConnectedUserProjectWorkflows() {
        return CollectionUtils.toList(connectedUserProjectWorkflowRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectedUserProjectWorkflow> getConnectedUserProjectWorkflows(
        long connectedUserProjectId) {

        return connectedUserProjectWorkflowRepository.findAllByConnectedUserProjectId(connectedUserProjectId);
    }

    @Override
    public ConnectedUserProjectWorkflow getConnectedUserProjectWorkflows(
        long connectedUserProjectId, long projectWorkflowId) {

        return connectedUserProjectWorkflowRepository.findByConnectedUserProjectIdAndProjectWorkflowId(
            connectedUserProjectId, projectWorkflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isConnectionUsed(long connectionId) {
        return !connectedUserProjectWorkflowRepository
            .findConnectedUserProjectWorkflowConnectionIdsByConnectionId(connectionId)
            .isEmpty();
    }

    @Override
    public void incrementWorkflowVersion(long connectedUserProjectId, long projectWorkflowId) {
        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = getConnectedUserProjectWorkflows(
            connectedUserProjectId, projectWorkflowId);

        Integer workflowVersion = connectedUserProjectWorkflow.getWorkflowVersion();

        if (workflowVersion == null) {
            workflowVersion = 0;
        }

        connectedUserProjectWorkflow.setWorkflowVersion(workflowVersion + 1);

        connectedUserProjectWorkflowRepository.save(connectedUserProjectWorkflow);
    }
}
