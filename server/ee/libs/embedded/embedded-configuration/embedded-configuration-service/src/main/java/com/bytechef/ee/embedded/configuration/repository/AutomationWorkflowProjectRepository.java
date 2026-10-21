/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProject;
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
public interface AutomationWorkflowProjectRepository extends ListCrudRepository<AutomationWorkflowProject, Long> {

    void deleteByProjectId(long projectId);

    Optional<AutomationWorkflowProject> findByProjectId(long projectId);

    @Query("""
        SELECT COUNT(*) > 0
        FROM automation_workflow_project awp
        JOIN project_workflow pw ON awp.project_id = pw.project_id
        WHERE pw.workflow_id = :workflowId
        """)
    boolean existsByWorkflowId(@Param("workflowId") String workflowId);
}
