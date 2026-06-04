/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion.Status;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationService {

    Integration create(Integration integration);

    void delete(long id);

    Integration getIntegration(long id);

    Integration getIntegrationInstanceIntegration(long integrationInstanceId);

    Integration getIntegrationInstanceConfigurationIntegration(long integrationInstanceConfigurationId);

    List<Integration> getIntegrations();

    List<Integration> getIntegrations(List<Long> ids);

    List<IntegrationVersion> getIntegrationVersions(Long id);

    List<Integration> getIntegrations(
        @Nullable Long categoryId, List<Long> ids, @Nullable Long tagId, @Nullable Status status);

    Integration getWorkflowIntegration(String workflowId);

    int publishIntegration(long id, @Nullable String description);

    Integration update(long id, List<Long> tagIds);

    Integration update(Integration integration);
}
