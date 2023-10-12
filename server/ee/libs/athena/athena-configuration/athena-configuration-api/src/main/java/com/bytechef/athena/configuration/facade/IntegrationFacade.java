/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.athena.configuration.facade;

import com.bytechef.athena.configuration.dto.IntegrationDTO;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.category.domain.Category;
import com.bytechef.tag.domain.Tag;
import java.util.List;
import org.springframework.lang.NonNull;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationFacade {

    Workflow addWorkflow(long id, @NonNull String definition);

    IntegrationDTO create(@NonNull IntegrationDTO integrationDTO);

    void delete(long id);

    void deleteWorkflow(long id, @NonNull String workflowId);

    IntegrationDTO getIntegration(long id);

    List<Category> getIntegrationCategories();

    List<Tag> getIntegrationTags();

    List<Workflow> getIntegrationWorkflows(long id);

    List<IntegrationDTO> getIntegrations(Long categoryId, Long tagId);

    IntegrationDTO update(long id, @NonNull List<Tag> tags);

    IntegrationDTO update(@NonNull IntegrationDTO integration);
}
