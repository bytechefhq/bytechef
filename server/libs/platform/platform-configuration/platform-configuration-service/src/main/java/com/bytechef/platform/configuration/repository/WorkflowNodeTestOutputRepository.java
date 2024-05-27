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

package com.bytechef.platform.configuration.repository;

import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface WorkflowNodeTestOutputRepository
    extends ListCrudRepository<WorkflowNodeTestOutput, Long> {

    List<WorkflowNodeTestOutput> findByWorkflowId(String workflowId);

    Optional<WorkflowNodeTestOutput> findByWorkflowIdAndWorkflowNodeName(String workflowId, String workflowNodeName);

    @Modifying
    @Query("UPDATE workflow_node_test_output SET workflow_id = :newWorkflowId WHERE workflow_id = :oldWorkflowId")
    void updateWorkflowId(@Param("oldWorkflowId") String oldWorkflowId, @Param("newWorkflowId") String newWorkflowId);
}
