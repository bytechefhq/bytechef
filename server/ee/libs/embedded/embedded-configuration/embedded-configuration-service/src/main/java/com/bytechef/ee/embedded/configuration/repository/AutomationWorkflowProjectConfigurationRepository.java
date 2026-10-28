/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AutomationWorkflowProjectConfigurationRepository
    extends ListCrudRepository<AutomationWorkflowProjectConfiguration, Long> {

    List<AutomationWorkflowProjectConfiguration> findAllByProjectId(long projectId);

    Optional<AutomationWorkflowProjectConfiguration> findByProjectIdAndEnvironmentId(
        long projectId, long environmentId);
}
