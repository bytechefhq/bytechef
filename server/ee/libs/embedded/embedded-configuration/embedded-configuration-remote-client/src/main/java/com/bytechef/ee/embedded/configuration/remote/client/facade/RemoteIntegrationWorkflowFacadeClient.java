/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
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
public class RemoteIntegrationWorkflowFacadeClient implements IntegrationWorkflowFacade {

    @Override
    public long addWorkflow(long integrationId, String definition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteWorkflow(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationWorkflowDTO>
        getIntegrationVersionWorkflows(long id, int integrationVersion, boolean includeAllFields) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationWorkflowDTO getIntegrationWorkflow(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationWorkflowDTO getIntegrationWorkflow(long integrationWorkflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationWorkflowDTO> getIntegrationWorkflows() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationWorkflowDTO> getIntegrationWorkflows(long integrationId) {
        return List.of();
    }

    @Override
    public void updateWorkflow(String workflowId, String definition, int version) {
        throw new UnsupportedOperationException();
    }
}
