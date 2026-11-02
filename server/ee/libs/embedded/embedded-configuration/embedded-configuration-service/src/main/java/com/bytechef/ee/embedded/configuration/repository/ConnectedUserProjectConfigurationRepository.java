/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import java.util.Collection;
import java.util.List;
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
public interface ConnectedUserProjectConfigurationRepository
    extends ListCrudRepository<ConnectedUserProjectConfiguration, Long> {

    @Query("""
        SELECT automation_workflow_project_configuration_id AS configuration_id, COUNT(*) AS assignment_count
        FROM connected_user_project_configuration
        WHERE automation_workflow_project_configuration_id IN (:configurationIds)
        GROUP BY automation_workflow_project_configuration_id
        """)
    List<ConfigurationAssignmentCount> countByConfigurationIds(
        @Param("configurationIds") Collection<Long> configurationIds);

    @Modifying
    @Query("""
        DELETE FROM connected_user_project_configuration
        WHERE automation_workflow_project_configuration_id = :configurationId
        """)
    void deleteByConfigurationId(@Param("configurationId") long configurationId);

    List<ConnectedUserProjectConfiguration> findAllByConfigurationId(long configurationId);

    Optional<ConnectedUserProjectConfiguration> findByConnectedUserIdAndProjectId(long connectedUserId, long projectId);

    /**
     * @param configurationId the configuration id
     * @param assignmentCount the number of assignments
     */
    record ConfigurationAssignmentCount(long configurationId, long assignmentCount) {
    }
}
