
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

import com.bytechef.helios.project.domain.ProjectInstance;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Repository
public interface ProjectInstanceRepository
    extends PagingAndSortingRepository<ProjectInstance, Long>, CrudRepository<ProjectInstance, Long> {

    @Query("SELECT project_instance.project_id FROM project_instance")
    List<Long> findAllProjectId();

    List<ProjectInstance> findAllByProjectIdInOrderByName(List<Long> projectIds);

    @Query("""
            SELECT project_instance.* FROM project_instance
            JOIN project_instance_tag ON project_instance.id = project_instance_tag.project_instance_id
            WHERE project_instance.project_id IN (:projectIds)
            AND project_instance_tag.tag_id IN (:tagId)
        """)
    List<ProjectInstance> findAllByProjectIdsAndTagIdsOrderByName(List<Long> projectIds, List<Long> tagIds);

    @Query("""
            SELECT project_instance.* FROM project_instance
            JOIN project_instance_tag ON project_instance.id = project_instance_tag.project_instance_id
            WHERE project_instance_tag.tag_id in (:tagIds)
        """)
    List<ProjectInstance> findAllByTagIdInOrderByName(List<Long> tagIds);
}
