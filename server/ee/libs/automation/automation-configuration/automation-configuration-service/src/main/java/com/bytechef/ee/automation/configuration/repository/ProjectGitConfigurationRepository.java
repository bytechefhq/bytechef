/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.repository;

import com.bytechef.ee.automation.configuration.domain.ProjectGitConfiguration;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnEEVersion
public interface ProjectGitConfigurationRepository extends ListCrudRepository<ProjectGitConfiguration, Long> {

    Optional<ProjectGitConfiguration> findByProjectId(long projectId);

    @Query("""
            SELECT * FROM project_git_configuration
            JOIN project ON project_git_configuration.project_id = project.id
            WHERE workspace_id = :workspaceId
        """)
    List<ProjectGitConfiguration> findAllByWorkspaceId(long workspaceId);
}
