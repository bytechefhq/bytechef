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

import com.bytechef.helios.configuration.domain.ProjectInstance;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface ProjectInstanceRepository
    extends ListPagingAndSortingRepository<ProjectInstance, Long>, CrudRepository<ProjectInstance, Long> {

    @Query("SELECT project_instance.project_id FROM project_instance")
    List<Long> findAllProjectId();

    List<ProjectInstance> findAllByProjectIdOrderByNameAscEnabledDesc(long projectId);

    @Query("""
            SELECT project_instance.* FROM project_instance
            JOIN project_instance_tag ON project_instance.id = project_instance_tag.project_instance_id
            WHERE project_instance.project_id = :projectId
            AND project_instance_tag.tag_id = :tagId
            ORDER BY project_instance.name ASC, project_instance.enabled DESC
        """)
    List<ProjectInstance> findAllByProjectIdAndTagIdOrderByNameAscEnabledDesc(
        @Param("projectId") long projectId, @Param("tagId") long tagId);

    @Query("""
            SELECT project_instance.* FROM project_instance
            JOIN project_instance_tag ON project_instance.id = project_instance_tag.project_instance_id
            WHERE project_instance_tag.tag_id = :tagId
            ORDER BY project_instance.name ASC, project_instance.enabled DESC
        """)
    List<ProjectInstance> findAllByTagIdOrderByNameAscEnabledDesc(@Param("tagId") long tagId);
}
