/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.repository;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Optional;
import java.util.UUID;
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
public interface CodeWorkflowContainerRepository extends ListCrudRepository<CodeWorkflowContainer, Long> {

    Optional<CodeWorkflowContainer> findByUuid(UUID uuid);

    @Query("""
        SELECT code_workflow_container.* FROM code_workflow_container
        JOIN code_workflow ON code_workflow.code_workflow_container_id = code_workflow_container.id
        WHERE code_workflow.id = :workflowId
        """)
    Optional<CodeWorkflowContainer> findByWorkflowId(@Param("workflowId") UUID workflowId);
}
