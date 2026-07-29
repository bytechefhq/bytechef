/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.repository;

import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Optional;
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
@ConditionalOnEEVersion
public interface ProjectCodeWorkflowRepository extends ListCrudRepository<ProjectCodeWorkflow, Long> {

    @Query("""
        SELECT * FROM project_code_workflow
        WHERE project_id = :projectId
        ORDER BY id DESC
        LIMIT 1
        """)
    Optional<ProjectCodeWorkflow> findFirstByProjectIdOrderByIdDesc(@Param("projectId") Long projectId);
}
