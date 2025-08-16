/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.repository;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
public interface WorkspaceRepository extends ListCrudRepository<Workspace, Long> {

    @Query("""
        SELECT workspace.* FROM workspace
        JOIN project ON workspace.id = project.workspace_id
        WHERE project.id = :projectId
        """)
    Workspace findByProjectId(long projectId);
}
