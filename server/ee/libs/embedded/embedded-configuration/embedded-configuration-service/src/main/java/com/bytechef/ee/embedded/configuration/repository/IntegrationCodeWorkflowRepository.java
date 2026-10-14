/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.IntegrationCodeWorkflow;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
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
public interface IntegrationCodeWorkflowRepository extends ListCrudRepository<IntegrationCodeWorkflow, Long> {

    @Query("SELECT DISTINCT integration_id FROM integration_code_workflow")
    List<Long> findDistinctIntegrationIds();

    @Query("""
        SELECT * FROM integration_code_workflow
        WHERE integration_id = :integrationId
        ORDER BY id DESC
        LIMIT 1
        """)
    Optional<IntegrationCodeWorkflow> findFirstByIntegrationIdOrderByIdDesc(
        @Param("integrationId") long integrationId);
}
