
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.helios.project.repository;

import com.bytechef.helios.project.domain.Project;
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

    List<Project> findAllByCategoryIdInOrderByName(List<Long> categoryIds);

    @Query("""
            SELECT project.* FROM project
            JOIN project_tag ON project.id = project_tag.project_id
            WHERE project.category_id IN (:categoryIds)
            AND project_tag.tag_id IN (:tagId)
        """)
    List<Project> findAllByCategoryIdsAndTagIdsOrderByName(
        @Param("categoryIds") List<Long> categoryIds, @Param("tagIds") List<Long> tagIds);

    @Query("""
            SELECT project.* FROM project
            JOIN project_tag ON project.id = project_tag.project_id
            WHERE project_tag.tag_id in (:tagIds)
        """)
    List<Project> findAllByTagIdInOrderByName(@Param("tagIds") List<Long> tagIds);

    @Query("""
            SELECT project.* FROM project
            JOIN project_instance ON project.id = project_instance.project_id
            JOIN project_instance_workflow ON project_instance.id = project_instance_workflow.project_instance_id
            JOIN project_instance_workflow_job ON project_instance_workflow.id = project_instance_workflow_job.project_instance_workflow_id
            WHERE project_instance_workflow_job.job_id = :jobId
        """)
    Optional<Project> findByJobId(@Param("jobId") long jobId);

    Optional<Project> findByName(String name);
}
