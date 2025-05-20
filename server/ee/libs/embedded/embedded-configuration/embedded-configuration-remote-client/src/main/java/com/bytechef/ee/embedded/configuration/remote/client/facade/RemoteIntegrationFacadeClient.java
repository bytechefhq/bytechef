/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteIntegrationFacadeClient implements IntegrationFacade {

    @Override
    public long addWorkflow(long id, String definition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long createIntegration(IntegrationDTO integrationDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteIntegration(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteWorkflow(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationDTO getIntegration(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Category> getIntegrationCategories() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Tag> getIntegrationTags() {
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
    public List<IntegrationWorkflowDTO> getIntegrationWorkflows(long id) {
        return List.of();
    }

    @Override
    public List<IntegrationDTO> getIntegrations(
        Long categoryId, boolean integrationInstanceConfigurations, Long tagId, Status status,
        boolean includeAllFields) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void publishIntegration(long id, String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateIntegration(IntegrationDTO integration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateIntegrationTags(long id, List<Tag> tags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateWorkflow(String workflowId, String definition, int version) {
        throw new UnsupportedOperationException();
    }
}
