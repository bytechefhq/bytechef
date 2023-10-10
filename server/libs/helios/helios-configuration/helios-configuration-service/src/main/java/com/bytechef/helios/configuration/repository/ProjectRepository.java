
/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.helios.configuration.repository;

import com.bytechef.helios.configuration.domain.Project;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Repository
public interface ProjectRepository
    extends ListPagingAndSortingRepository<Project, Long>, CrudRepository<Project, Long> {

    List<Project> findAllByCategoryIdOrderByName(Long categoryId);

    @Query("""
            SELECT project.* FROM project
            JOIN project_tag ON project.id = project_tag.project_id
            WHERE project.category_id = :categoryId
            AND project_tag.tag_id = :tagId
        """)
    List<Project> findAllByCategoryIdAndTagIdOrderByName(
        @Param("categoryId") long categoryId, @Param("tagId") long tagId);

    @Query("""
            SELECT project.* FROM project
            JOIN project_tag ON project.id = project_tag.project_id
            WHERE project_tag.tag_id = :tagId
        """)
    List<Project> findAllByTagIdOrderByName(@Param("tagId") long tagId);

    @Query("""
            SELECT project.* FROM project
            JOIN project_instance ON project.id = project_instance.project_id
            WHERE project_instance.id = :projectInstanceId
        """)
    Project findByProjectInstanceId(@Param("projectInstanceId") long projectInstanceId);

    Optional<Project> findByNameIgnoreCase(String name);

    @Query("""
            SELECT project.* FROM project
            JOIN project_workflow ON project.id = project_workflow.project_id
            WHERE project_workflow.workflow_id = :workflowId
        """)
    Optional<Project> findByWorkflowId(@Param("workflowId") String workflowId);
}
