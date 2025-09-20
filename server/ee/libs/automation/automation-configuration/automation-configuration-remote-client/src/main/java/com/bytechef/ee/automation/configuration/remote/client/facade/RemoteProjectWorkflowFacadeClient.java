/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.facade;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.dto.SharedWorkflowDTO;
import com.bytechef.automation.configuration.dto.WorkflowTemplateDTO;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteProjectWorkflowFacadeClient implements ProjectWorkflowFacade {

    @Override
    public ProjectWorkflow addWorkflow(long projectId, String definition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSharedWorkflow(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteWorkflow(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String duplicateWorkflow(long projectId, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportSharedWorkflow(String workflowId, String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectWorkflowDTO getProjectWorkflow(long projectWorkflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectWorkflowDTO getProjectWorkflow(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflowDTO> getProjectWorkflows() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflowDTO> getProjectWorkflows(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflowDTO> getProjectVersionWorkflows(
        long projectId, int projectVersion, boolean includeAllFields) {

        throw new UnsupportedOperationException();
    }

    @Override
    public SharedWorkflowDTO getSharedWorkflow(String workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WorkflowTemplateDTO getWorkflowTemplate(String id, boolean sharedWorkflow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long importWorkflowTemplate(long projectId, String workflowUuid, boolean sharedWorkflow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateWorkflow(String workflowId, String definition, int version) {
        throw new UnsupportedOperationException();
    }
}
