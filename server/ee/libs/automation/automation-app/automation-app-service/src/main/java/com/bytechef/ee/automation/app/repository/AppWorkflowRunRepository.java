/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.repository;

import com.bytechef.ee.automation.app.domain.AppWorkflowRun;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
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
public interface AppWorkflowRunRepository extends ListCrudRepository<AppWorkflowRun, Long> {

    Optional<AppWorkflowRun> findByAppIdAndRunId(long appId, String runId);

    @Modifying
    @Query("DELETE FROM app_workflow_run WHERE created_date < :instant")
    int deleteByCreatedDateBefore(@Param("instant") Instant instant);
}
