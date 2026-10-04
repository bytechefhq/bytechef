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

package com.bytechef.automation.ai.a2a.repository;

import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface A2aProjectWorkflowRepository extends ListCrudRepository<A2aProjectWorkflow, Long> {

    @Query("""
        SELECT * FROM a2a_project_workflow
        WHERE a2a_project_id = :a2aProjectId
        ORDER BY id ASC
        """)
    List<A2aProjectWorkflow> findAllByA2aProjectId(@Param("a2aProjectId") Long a2aProjectId);

    @Query("""
        SELECT * FROM a2a_project_workflow
        WHERE project_deployment_workflow_id = :projectDeploymentWorkflowId
        ORDER BY id ASC
        """)
    List<A2aProjectWorkflow>
        findAllByProjectDeploymentWorkflowId(@Param("projectDeploymentWorkflowId") Long projectDeploymentWorkflowId);
}
