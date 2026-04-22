/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.repository;

import com.bytechef.ee.automation.ai.observability.domain.WorkspaceAiObservabilityExportJob;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportJob;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @version ee
 */
public interface WorkspaceAiObservabilityExportJobRepository
    extends ListCrudRepository<WorkspaceAiObservabilityExportJob, Long> {

    Optional<WorkspaceAiObservabilityExportJob> findByAiObservabilityExportJobId(long aiObservabilityExportJobId);

    @Query("""
        SELECT ai_observability_export_job.*
        FROM ai_observability_export_job
        JOIN workspace_ai_observability_export_job
            ON workspace_ai_observability_export_job.ai_observability_export_job_id = ai_observability_export_job.id
        WHERE workspace_ai_observability_export_job.workspace_id = :workspaceId
        ORDER BY ai_observability_export_job.created_date DESC
        """)
    List<AiObservabilityExportJob> findAllExportJobsByWorkspaceIdOrderByCreatedDateDesc(
        @Param("workspaceId") Long workspaceId);
}
