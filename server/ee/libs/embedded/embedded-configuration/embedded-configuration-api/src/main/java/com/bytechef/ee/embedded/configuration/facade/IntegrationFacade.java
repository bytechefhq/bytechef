/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import org.springframework.lang.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationFacade {

    long addWorkflow(long id, String definition);

    long createIntegration(IntegrationDTO integrationDTO);

    void deleteIntegration(long id);

    void deleteWorkflow(String workflowId);

    IntegrationDTO getIntegration(long id);

    List<Category> getIntegrationCategories();

    List<Tag> getIntegrationTags();

    List<IntegrationWorkflowDTO> getIntegrationVersionWorkflows(
        long id, int integrationVersion, boolean includeAllFields);

    IntegrationWorkflowDTO getIntegrationWorkflow(String workflowId);

    IntegrationWorkflowDTO getIntegrationWorkflow(long integrationWorkflowId);

    List<IntegrationWorkflowDTO> getIntegrationWorkflows();

    List<IntegrationWorkflowDTO> getIntegrationWorkflows(long id);

    List<IntegrationDTO> getIntegrations(
        @Nullable Long categoryId, boolean integrationInstanceConfigurations, @Nullable Long tagId,
        @Nullable Status status, boolean includeAllFields);

    void publishIntegration(long id, @Nullable String description);

    void updateIntegration(IntegrationDTO integration);

    void updateIntegrationTags(long id, List<Tag> tags);

    void updateWorkflow(String workflowId, String definition, int version);
}
