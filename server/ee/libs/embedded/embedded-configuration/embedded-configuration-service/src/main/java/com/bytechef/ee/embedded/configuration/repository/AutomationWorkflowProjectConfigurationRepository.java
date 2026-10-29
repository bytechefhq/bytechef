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
import java.util.Set;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
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

    List<AutomationWorkflowProjectConfiguration> findAllByProjectIdIn(Set<Long> projectIds);

    Optional<AutomationWorkflowProjectConfiguration> findByProjectIdAndEnvironmentId(
        long projectId, long environmentId);

    @Query("SELECT * FROM automation_workflow_project_configuration WHERE project_id = :projectId AND environment_id = :environmentId FOR UPDATE")
    Optional<AutomationWorkflowProjectConfiguration> findByProjectIdAndEnvironmentIdForUpdate(
        @Param("projectId") long projectId, @Param("environmentId") long environmentId);
}
