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

package com.bytechef.automation.configuration.repository;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface ProjectDeploymentRepository
    extends ListPagingAndSortingRepository<ProjectDeployment, Long>, CrudRepository<ProjectDeployment, Long>,
    CustomProjectDeploymentRepository {

    // TODO get rid of api_collection and connected_user_project in this query, find another way to filter out records
    @Query("""
        SELECT project_deployment.project_id FROM project_deployment
        WHERE project_deployment.id NOT IN (SELECT api_collection.project_deployment_id FROM api_collection)
        AND project_deployment.project_id NOT IN (SELECT project.id FROM project JOIN connected_user_project ON connected_user_project.project_id = project.id)""")
    List<Long> findAllProjectDeploymentProjectIds();

    Optional<ProjectDeployment> findByProjectIdAndEnvironment(long projectId, int environment);
}
