/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.configuration.remote.client.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteIntegrationFacadeClient implements IntegrationFacade {

    @Override
    public Workflow addWorkflow(long id, @NonNull String definition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkIntegrationStatus(long id, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationDTO create(@NonNull IntegrationDTO integrationDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteIntegration(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteWorkflow(@NonNull String workflowId) {
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
    public List<Workflow> getIntegrationWorkflows() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Workflow> getIntegrationWorkflows(long id) {
        return List.of();
    }

    @Override
    public List<Workflow> getIntegrationVersionWorkflows(long id, int integrationVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationDTO> getIntegrations(
        Long categoryId, boolean integrationInstances, Long tagId, Status status) {

        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationDTO updateIntegration(@NonNull IntegrationDTO integration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateIntegrationTags(long id, @NonNull List<Tag> tags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WorkflowDTO updateWorkflow(String workflowId, String definition, int version) {
        throw new UnsupportedOperationException();
    }
}
