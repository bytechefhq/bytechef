/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.datasync.repository;

import com.bytechef.automation.datasync.domain.DataSync;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface DataSyncRepository extends ListCrudRepository<DataSync, Long> {

    /**
     * Finds all Data Syncs that belong to the specified project — the syncs whose generated workflow
     * ({@code project_workflow_uuid}) is one of the project's workflows. {@code data_sync} keeps no project column of
     * its own; the project is read from {@code project_workflow}.
     *
     * @param projectId the ID of the project to filter by
     * @return the Data Syncs of the specified project
     */
    @Query("""
        SELECT data_sync.* FROM data_sync
        WHERE data_sync.project_workflow_uuid IN (
            SELECT project_workflow.uuid FROM project_workflow WHERE project_workflow.project_id = :projectId
        )
        ORDER BY data_sync.id ASC
        """)
    List<DataSync> findAllByProjectId(@Param("projectId") long projectId);

    /**
     * Finds the project that owns the given generated workflow. Every version of a project workflow shares its uuid and
     * its project, so the distinct result is a single row.
     *
     * @param projectWorkflowUuid the Data Sync's {@code project_workflow_uuid}
     * @return the owning project's ID, or empty when no project workflow carries the uuid
     */
    @Query("SELECT DISTINCT project_workflow.project_id FROM project_workflow WHERE project_workflow.uuid = :uuid")
    Optional<Long> findProjectIdByProjectWorkflowUuid(@Param("uuid") UUID projectWorkflowUuid);

    /**
     * Batch variant of {@link #findProjectIdByProjectWorkflowUuid(UUID)}: one row per distinct uuid found.
     *
     * @param projectWorkflowUuids the Data Syncs' {@code project_workflow_uuid} values; must not be empty
     * @return the uuid → project pairs found
     */
    @Query(
        value = """
            SELECT DISTINCT project_workflow.uuid, project_workflow.project_id FROM project_workflow
            WHERE project_workflow.uuid IN (:uuids)
            """,
        rowMapperClass = ProjectWorkflowProjectRowMapper.class)
    List<ProjectWorkflowProject> findProjectIdsByProjectWorkflowUuids(
        @Param("uuids") Collection<UUID> projectWorkflowUuids);

    List<DataSync> findByWorkspaceId(Long workspaceId);

    /**
     * A project workflow uuid paired with the project that owns it.
     */
    record ProjectWorkflowProject(UUID projectWorkflowUuid, long projectId) {
    }

    /**
     * Maps a {@code (uuid, project_id)} row of {@code project_workflow} to a {@link ProjectWorkflowProject}.
     */
    class ProjectWorkflowProjectRowMapper implements RowMapper<ProjectWorkflowProject> {

        @Override
        public ProjectWorkflowProject mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            return new ProjectWorkflowProject(
                resultSet.getObject("uuid", UUID.class), resultSet.getLong("project_id"));
        }
    }
}
